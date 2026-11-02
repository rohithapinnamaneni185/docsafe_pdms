package com.pdms.service;

import com.pdms.dto.DashboardSummary;
import com.pdms.dto.DocumentRequest;
import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.model.ExpiryStatus;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentService documentService;

    private Document sampleDoc;
    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("System Administrator", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        adminUser.setId(1L);

        memberUser = new User("Jane Doe", "jane@docusafe.local", "pass123", "Spouse", "ROLE_MEMBER");
        memberUser.setId(2L);

        sampleDoc = new Document();
        sampleDoc.setId(1L);
        sampleDoc.setUser(adminUser);
        sampleDoc.setDocumentName("Test Passport");
        sampleDoc.setCategory("Identity");
        sampleDoc.setIssuingOrganization("Passport Seva");
        sampleDoc.setReferenceNumber("A1234567");
        sampleDoc.setIssueDate(LocalDate.now().minusYears(1));
        sampleDoc.setExpiryDate(LocalDate.now().plusYears(4));
        sampleDoc.setFileName("passport.png");
    }

    @Test
    @DisplayName("Should correctly calculate expiry status as VALID when > 30 days remaining")
    void testCalculateStatus_Valid() {
        LocalDate expiryDate = LocalDate.now().plusDays(60);
        ExpiryStatus status = ExpiryStatus.calculate(expiryDate);
        assertEquals(ExpiryStatus.VALID, status);
        assertEquals("Valid", status.getLabel());
    }

    @Test
    @DisplayName("Should correctly calculate expiry status as EXPIRING_SOON when <= 30 days remaining")
    void testCalculateStatus_ExpiringSoon() {
        LocalDate expiryDate = LocalDate.now().plusDays(15);
        ExpiryStatus status = ExpiryStatus.calculate(expiryDate);
        assertEquals(ExpiryStatus.EXPIRING_SOON, status);
        assertEquals("Expiring Soon", status.getLabel());
    }

    @Test
    @DisplayName("Should correctly calculate expiry status as EXPIRED when date is before today")
    void testCalculateStatus_Expired() {
        LocalDate expiryDate = LocalDate.now().minusDays(5);
        ExpiryStatus status = ExpiryStatus.calculate(expiryDate);
        assertEquals(ExpiryStatus.EXPIRED, status);
        assertEquals("Expired", status.getLabel());
    }

    @Test
    @DisplayName("Should correctly calculate expiry status as NO_EXPIRY when expiry date is null")
    void testCalculateStatus_NoExpiry() {
        ExpiryStatus status = ExpiryStatus.calculate(null);
        assertEquals(ExpiryStatus.NO_EXPIRY, status);
        assertEquals("No Expiry Date", status.getLabel());
    }

    @Test
    @DisplayName("Should throw BadRequestException if issue date is after expiry date")
    void testCreateDocument_InvalidDateRange() {
        DocumentRequest request = new DocumentRequest();
        request.setDocumentName("Invalid Document");
        request.setCategory("Identity");
        request.setIssueDate(LocalDate.now().plusDays(10));
        request.setExpiryDate(LocalDate.now().plusDays(5));

        assertThrows(BadRequestException.class, () -> documentService.createDocument(request, null, adminUser));
    }

    @Test
    @DisplayName("Should soft delete document and set deleted timestamp without deleting file")
    void testDeleteDocument_SoftDeletes() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDoc));

        documentService.deleteDocument(1L, adminUser);

        assertTrue(sampleDoc.isDeleted());
        assertNotNull(sampleDoc.getDeletedAt());
        verify(documentRepository, times(1)).save(sampleDoc);
        verify(fileStorageService, never()).deleteFile(any());
        verify(documentRepository, never()).delete(any(Document.class));
    }

    @Test
    @DisplayName("Should permanently delete document row and physical file from disk")
    void testPermanentlyDeleteDocument_DeletesFileAndRow() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDoc));

        documentService.permanentlyDeleteDocument(1L, adminUser);

        verify(fileStorageService, times(1)).deleteFile("passport.png");
        verify(documentRepository, times(1)).delete(sampleDoc);
    }

    @Test
    @DisplayName("Should compute correct metrics in dashboard summary for admin")
    void testDashboardSummary() {
        Document validDoc = new Document("Doc 1", "Identity", "Org", "Ref1", LocalDate.now().minusDays(10), LocalDate.now().plusDays(40), "Note");
        Document expiringDoc = new Document("Doc 2", "Vehicle", "Org", "Ref2", LocalDate.now().minusDays(10), LocalDate.now().plusDays(10), "Note");
        Document expiredDoc = new Document("Doc 3", "Insurance", "Org", "Ref3", LocalDate.now().minusDays(50), LocalDate.now().minusDays(2), "Note");
        Document noExpiryDoc = new Document("Doc 4", "Education", "Org", "Ref4", LocalDate.now().minusDays(10), null, "Note");

        when(documentRepository.findByIsDeletedFalse()).thenReturn(List.of(validDoc, expiringDoc, expiredDoc, noExpiryDoc));
        when(documentRepository.countByIsDeletedTrue()).thenReturn(2L);
        when(documentRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(documentRepository.findTop5ByIsDeletedFalseAndExpiryDateBetweenOrderByExpiryDateAsc(any(), any())).thenReturn(Collections.emptyList());

        DashboardSummary summary = documentService.getDashboardSummary(null, adminUser);

        assertEquals(4, summary.getTotalDocuments());
        assertEquals(1, summary.getValidDocuments());
        assertEquals(1, summary.getExpiringSoonDocuments());
        assertEquals(1, summary.getExpiredDocuments());
        assertEquals(1, summary.getNoExpiryDocuments());
        assertEquals(2, summary.getTrashDocuments());
    }
}
