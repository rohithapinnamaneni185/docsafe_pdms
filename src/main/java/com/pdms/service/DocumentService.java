package com.pdms.service;

import com.pdms.dto.DashboardSummary;
import com.pdms.dto.DocumentRequest;
import com.pdms.dto.DocumentResponse;
import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.ResourceNotFoundException;
import com.pdms.model.DocumentCategory;
import com.pdms.model.ExpiryStatus;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service orchestrating document CRUD operations, multi-member scoping,
 * business validations, authoritative expiry calculations, and dashboard aggregations.
 */
@Service
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public DocumentService(DocumentRepository documentRepository, 
                           UserRepository userRepository, 
                           FileStorageService fileStorageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Retrieves documents filtered by query, category, status, sort, and user scope.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments(String query, String category, String status, String sort, 
                                                 Long filterUserId, User currentUser) {
        Long targetUserId = resolveTargetUserId(filterUserId, currentUser);

        List<Document> docs = documentRepository.searchDocuments(query, category, targetUserId);

        // Filter by calculated status if specified
        List<DocumentResponse> responses = docs.stream()
                .map(DocumentResponse::fromEntity)
                .filter(resp -> {
                    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
                        return true;
                    }
                    return resp.getStatus().name().equalsIgnoreCase(status.trim());
                })
                .collect(Collectors.toList());

        // Sort results
        Comparator<DocumentResponse> comparator = getComparator(sort);
        responses.sort(comparator);

        return responses;
    }

    private Long resolveTargetUserId(Long requestedUserId, User currentUser) {
        if (currentUser.isAdmin()) {
            return requestedUserId; // can be null (meaning all) or specific member
        }
        return currentUser.getId(); // regular members can only view their own documents
    }

    private Comparator<DocumentResponse> getComparator(String sort) {
        if (sort == null || sort.isBlank()) {
            return Comparator.comparing(DocumentResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return switch (sort.toLowerCase()) {
            case "name,asc" -> Comparator.comparing(d -> d.getDocumentName().toLowerCase());
            case "name,desc" -> Comparator.comparing((DocumentResponse d) -> d.getDocumentName().toLowerCase()).reversed();
            case "expirydate,asc" -> Comparator.comparing(DocumentResponse::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case "expirydate,desc" -> Comparator.comparing(DocumentResponse::getExpiryDate, Comparator.nullsLast(Comparator.reverseOrder()));
            case "createdat,asc" -> Comparator.comparing(DocumentResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(DocumentResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
    }

    /**
     * Retrieves a single document by ID, checking access rights.
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(Long id, User currentUser) {
        Document document = findDocumentOrThrow(id);
        checkDocumentAccess(document, currentUser);
        return DocumentResponse.fromEntity(document);
    }

    /**
     * Creates and persists a new document, including optional image upload and user assignment.
     */
    public DocumentResponse createDocument(DocumentRequest request, MultipartFile file, User currentUser) {
        validateDates(request.getIssueDate(), request.getExpiryDate());

        Document doc = new Document();
        mapRequestToEntity(request, doc);

        // Assign document owner
        if (currentUser.isAdmin() && request.getUserId() != null) {
            User targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BadRequestException("Selected family member not found with ID: " + request.getUserId()));
            doc.setUser(targetUser);
        } else {
            doc.setUser(currentUser);
        }

        if (file != null && !file.isEmpty()) {
            FileStorageService.StoredFileInfo fileInfo = fileStorageService.storeFile(file);
            doc.setFileName(fileInfo.fileName());
            doc.setImagePath(fileInfo.relativePath());
            doc.setFileSize(fileInfo.fileSize());
            doc.setFileType(fileInfo.contentType());
        }

        Document saved = documentRepository.save(doc);
        return DocumentResponse.fromEntity(saved);
    }

    /**
     * Updates an existing document, handling text updates, user reassignment, and image replacement/removal.
     */
    public DocumentResponse updateDocument(Long id, DocumentRequest request, MultipartFile file, User currentUser) {
        Document doc = findDocumentOrThrow(id);
        checkDocumentAccess(doc, currentUser);
        validateDates(request.getIssueDate(), request.getExpiryDate());

        mapRequestToEntity(request, doc);

        // Admin can reassign ownership
        if (currentUser.isAdmin() && request.getUserId() != null) {
            User targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BadRequestException("Selected family member not found with ID: " + request.getUserId()));
            doc.setUser(targetUser);
        }

        // Handle image replacement or removal
        if (Boolean.TRUE.equals(request.getRemoveImage())) {
            if (doc.getFileName() != null) {
                fileStorageService.deleteFile(doc.getFileName());
                doc.setFileName(null);
                doc.setImagePath(null);
                doc.setFileSize(null);
                doc.setFileType(null);
            }
        }

        if (file != null && !file.isEmpty()) {
            if (doc.getFileName() != null) {
                fileStorageService.deleteFile(doc.getFileName());
            }
            FileStorageService.StoredFileInfo fileInfo = fileStorageService.storeFile(file);
            doc.setFileName(fileInfo.fileName());
            doc.setImagePath(fileInfo.relativePath());
            doc.setFileSize(fileInfo.fileSize());
            doc.setFileType(fileInfo.contentType());
        }

        Document updated = documentRepository.save(doc);
        return DocumentResponse.fromEntity(updated);
    }

    /**
     * Soft-deletes a document (marks isDeleted = true and records deletedAt).
     * Physical file is preserved in case of accidental deletion or restoration.
     */
    public void deleteDocument(Long id, User currentUser) {
        Document doc = findDocumentOrThrow(id);
        checkDocumentAccess(doc, currentUser);
        doc.setDeleted(true);
        doc.setDeletedAt(java.time.LocalDateTime.now());
        documentRepository.save(doc);
    }

    /**
     * Retrieves all soft-deleted documents (Trash) scoped to user.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getTrashDocuments(Long filterUserId, User currentUser) {
        Long targetUserId = resolveTargetUserId(filterUserId, currentUser);
        List<Document> trash = documentRepository.findTrashDocuments(targetUserId);
        return trash.stream().map(DocumentResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Restores a soft-deleted document back to the active vault.
     */
    public DocumentResponse restoreDocument(Long id, User currentUser) {
        Document doc = findDocumentOrThrow(id);
        checkDocumentAccess(doc, currentUser);
        doc.setDeleted(false);
        doc.setDeletedAt(null);
        Document restored = documentRepository.save(doc);
        return DocumentResponse.fromEntity(restored);
    }

    /**
     * Permanently deletes a document from the database and removes its uploaded file from disk.
     */
    public void permanentlyDeleteDocument(Long id, User currentUser) {
        Document doc = findDocumentOrThrow(id);
        checkDocumentAccess(doc, currentUser);
        if (doc.getFileName() != null) {
            fileStorageService.deleteFile(doc.getFileName());
        }
        documentRepository.delete(doc);
    }

    /**
     * Retrieves active documents with valid expiry dates for the calendar view.
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getCalendarDocuments(Long filterUserId, User currentUser) {
        Long targetUserId = resolveTargetUserId(filterUserId, currentUser);
        List<Document> calendarDocs = documentRepository.findCalendarDocuments(targetUserId);
        return calendarDocs.stream().map(DocumentResponse::fromEntity).collect(Collectors.toList());
    }

    /**
     * Aggregates real-time metrics for the dashboard summary scoped to the user or requested filter.
     */
    @Transactional(readOnly = true)
    public DashboardSummary getDashboardSummary(Long filterUserId, User currentUser) {
        Long targetUserId = resolveTargetUserId(filterUserId, currentUser);

        List<Document> userDocs = (targetUserId == null) ? 
                documentRepository.findByIsDeletedFalse() : 
                documentRepository.findByUserIdAndIsDeletedFalse(targetUserId);

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);

        long total = userDocs.size();
        long valid = 0;
        long expiringSoon = 0;
        long expired = 0;
        long noExpiry = 0;

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        for (DocumentCategory cat : DocumentCategory.values()) {
            categoryCounts.put(cat.getDisplayName(), 0L);
        }

        for (Document doc : userDocs) {
            String cat = doc.getCategory();
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0L) + 1L);

            ExpiryStatus status = ExpiryStatus.calculate(doc.getExpiryDate());
            switch (status) {
                case VALID -> valid++;
                case EXPIRING_SOON -> expiringSoon++;
                case EXPIRED -> expired++;
                case NO_EXPIRY -> noExpiry++;
            }
        }

        long trashCount = (targetUserId == null) ?
                documentRepository.countByIsDeletedTrue() :
                documentRepository.countByUserIdAndIsDeletedTrue(targetUserId);

        // Recent 5 active documents
        List<DocumentResponse> recent = (targetUserId == null ? 
                documentRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc() : 
                documentRepository.findTop5ByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(targetUserId))
                .stream()
                .map(DocumentResponse::fromEntity)
                .collect(Collectors.toList());

        // Top active documents expiring soon sorted by date asc
        List<DocumentResponse> expiringSoonList = (targetUserId == null ? 
                documentRepository.findTop5ByIsDeletedFalseAndExpiryDateBetweenOrderByExpiryDateAsc(today, in30Days) : 
                documentRepository.findTop5ByUserIdAndIsDeletedFalseAndExpiryDateBetweenOrderByExpiryDateAsc(targetUserId, today, in30Days))
                .stream()
                .map(DocumentResponse::fromEntity)
                .collect(Collectors.toList());

        return new DashboardSummary(total, valid, expiringSoon, expired, noExpiry, trashCount, categoryCounts, recent, expiringSoonList);
    }

    /**
     * Seeds fictional demonstration documents for the given user.
     */
    public List<DocumentResponse> seedSampleDocuments(User currentUser) {
        LocalDate today = LocalDate.now();
        List<Document> samples = List.of(
                createSample(currentUser, "National Driving License", "Vehicle", "Regional Transport Office (RTO)", 
                        "DL-0420180092314", today.minusYears(5), today.plusDays(25), 
                        "Commercial light motor vehicle endorsement included. Renewal required soon."),
                createSample(currentUser, "Comprehensive Car Insurance", "Insurance", "HDFC ERGO General Insurance", 
                        "POL-CAR-88291039", today.minusMonths(11), today.plusDays(14), 
                        "Includes zero depreciation, engine protect, and roadside assistance cover."),
                createSample(currentUser, "Passport - Republic of India", "Identity", "Ministry of External Affairs", 
                        "Z5948392", today.minusYears(6), today.plusDays(540), 
                        "36-page regular passport booklet with official visa stamps."),
                createSample(currentUser, "B.Tech Degree Certificate", "Education", "National Institute of Technology", 
                        "DEG-CSE-2024-0881", today.minusMonths(8), null, 
                        "Original Bachelor of Technology degree certificate in Computer Science & Engineering. Lifetime validity."),
                createSample(currentUser, "Family Floater Health Insurance", "Medical", "Star Health and Allied Insurance", 
                        "SH-FAM-991204", today.minusYears(1), today.minusDays(10), 
                        "Policy expired 10 days ago. Immediate premium payment needed to avoid grace lapse."),
                createSample(currentUser, "MacBook Pro Extended Warranty", "Warranty", "Apple India Private Limited", 
                        "AP-CARE-771204", today.minusMonths(6), today.plusDays(180), 
                        "AppleCare+ 3-year warranty for M3 Pro 16-inch laptop.")
        );

        List<Document> saved = documentRepository.saveAll(samples);
        return saved.stream().map(DocumentResponse::fromEntity).collect(Collectors.toList());
    }

    private Document createSample(User user, String name, String cat, String org, String ref, LocalDate issue, LocalDate expiry, String notes) {
        Document doc = new Document(name, cat, org, ref, issue, expiry, notes);
        doc.setUser(user);
        return doc;
    }

    private void checkDocumentAccess(Document doc, User currentUser) {
        if (currentUser.isAdmin()) {
            return; // Admin can view/modify any document
        }
        if (doc.getUser() == null || !doc.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Access denied: You do not have permission to access this document.");
        }
    }

    private Document findDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found with ID: " + id));
    }

    private void validateDates(LocalDate issueDate, LocalDate expiryDate) {
        if (issueDate != null && expiryDate != null) {
            if (issueDate.isAfter(expiryDate)) {
                throw new BadRequestException("Issue date (" + issueDate + ") cannot be after expiry date (" + expiryDate + ").");
            }
        }
    }

    private void mapRequestToEntity(DocumentRequest request, Document doc) {
        doc.setDocumentName(request.getDocumentName().trim());
        doc.setCategory(request.getCategory().trim());
        doc.setIssuingOrganization(request.getIssuingOrganization() != null ? request.getIssuingOrganization().trim() : null);
        doc.setReferenceNumber(request.getReferenceNumber() != null ? request.getReferenceNumber().trim() : null);
        doc.setIssueDate(request.getIssueDate());
        doc.setExpiryDate(request.getExpiryDate());
        doc.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
    }
}
