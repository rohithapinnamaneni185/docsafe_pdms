package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.DocumentRequest;
import com.pdms.dto.DocumentResponse;
import com.pdms.entity.User;
import com.pdms.service.DocumentService;
import com.pdms.service.FileStorageService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    public DocumentController(DocumentService documentService, 
                              FileStorageService fileStorageService,
                              UserService userService) {
        this.documentService = documentService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    /**
     * GET /api/documents
     * Query, category, status, and family member filters with sorting.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAllDocuments(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        Long targetId = memberId != null ? memberId : userId;
        List<DocumentResponse> documents = documentService.getAllDocuments(query, category, status, sort, targetId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + documents.size() + " documents", documents));
    }

    /**
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        DocumentResponse doc = documentService.getDocumentById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved successfully", doc));
    }

    /**
     * POST /api/documents
     * Multipart form data for document details and uploaded image.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> createDocument(
            @Valid @ModelAttribute DocumentRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        DocumentResponse created = documentService.createDocument(request, image, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document '" + created.getDocumentName() + "' saved successfully", created));
    }

    /**
     * PUT /api/documents/{id}
     * Updates document details and optionally replaces or removes the image.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable Long id,
            @Valid @ModelAttribute DocumentRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        DocumentResponse updated = documentService.updateDocument(id, request, image, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Document '" + updated.getDocumentName() + "' updated successfully", updated));
    }

    /**
     * DELETE /api/documents/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        documentService.deleteDocument(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Document moved to Trash", null));
    }

    /**
     * GET /api/documents/trash
     * Fetches all soft-deleted documents for the current session.
     */
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getTrashDocuments(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long userId,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        Long targetId = memberId != null ? memberId : userId;
        List<DocumentResponse> trash = documentService.getTrashDocuments(targetId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + trash.size() + " items from Trash", trash));
    }

    /**
     * PUT /api/documents/{id}/restore
     * Restores a soft-deleted document back to the active vault.
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<DocumentResponse>> restoreDocument(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        DocumentResponse restored = documentService.restoreDocument(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Document restored successfully", restored));
    }

    /**
     * DELETE /api/documents/{id}/permanent
     * Permanently removes the document record and deletes the uploaded file from disk.
     */
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteDocument(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        documentService.permanentlyDeleteDocument(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Document permanently deleted from vault and disk", null));
    }

    /**
     * GET /api/documents/calendar
     * Retrieves active documents with valid expiry dates for the calendar view.
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getCalendarDocuments(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) Long userId,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        Long targetId = memberId != null ? memberId : userId;
        List<DocumentResponse> docs = documentService.getCalendarDocuments(targetId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + docs.size() + " calendar documents", docs));
    }

    /**
     * GET /api/documents/search?query=...
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> searchDocuments(
            @RequestParam String query,
            @RequestParam(required = false) Long memberId,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        List<DocumentResponse> documents = documentService.getAllDocuments(query, null, null, null, memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Search completed", documents));
    }

    /**
     * GET /api/documents/images/{fileName}
     * Safely streams uploaded document image with appropriate MIME content-type.
     */
    @GetMapping("/images/{fileName:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = "application/octet-stream";
        try {
            Path filePath = resource.getFile().toPath();
            String detected = Files.probeContentType(filePath);
            if (detected != null) {
                contentType = detected;
            }
        } catch (IOException e) {
            if (fileName.toLowerCase().endsWith(".png")) {
                contentType = "image/png";
            } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600, must-revalidate")
                .body(resource);
    }

    /**
     * POST /api/documents/seed-samples
     * Seeds fictional sample records for demonstration.
     */
    @PostMapping("/seed-samples")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> seedSamples(HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        List<DocumentResponse> samples = documentService.seedSampleDocuments(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Sample documents loaded successfully (" + samples.size() + " items)", samples));
    }
}
