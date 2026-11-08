package com.pdms.controller;

import com.pdms.dto.DocumentRequest;
import com.pdms.dto.DocumentResponse;
import com.pdms.entity.User;
import com.pdms.model.ExpiryStatus;
import com.pdms.service.DocumentService;
import com.pdms.service.FileStorageService;
import com.pdms.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private UserService userService;

    private User adminUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        adminUser = new User("System Administrator", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        adminUser.setId(1L);

        session = new MockHttpSession();
        session.setAttribute(AuthController.SESSION_USER_KEY, 1L);

        when(userService.getUserById(1L)).thenReturn(adminUser);
    }

    @Test
    @DisplayName("GET /api/documents should return list of documents")
    void testGetAllDocuments() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId(1L);
        doc.setDocumentName("Test Passport");
        doc.setCategory("Identity");
        doc.setStatus(ExpiryStatus.VALID);
        doc.setStatusLabel("Valid");

        when(documentService.getAllDocuments(any(), any(), any(), any(), any(), any())).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/documents").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].documentName").value("Test Passport"))
                .andExpect(jsonPath("$.data[0].category").value("Identity"));
    }

    @Test
    @DisplayName("POST /api/documents should create document with multipart form data")
    void testCreateDocument() throws Exception {
        DocumentResponse response = new DocumentResponse();
        response.setId(2L);
        response.setDocumentName("Car Insurance");
        response.setCategory("Vehicle");
        response.setStatus(ExpiryStatus.EXPIRING_SOON);

        MockMultipartFile image = new MockMultipartFile(
                "image", "card.png", "image/png", "sample data".getBytes()
        );

        when(documentService.createDocument(any(DocumentRequest.class), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/documents")
                        .file(image)
                        .param("documentName", "Car Insurance")
                        .param("category", "Vehicle")
                        .param("expiryDate", LocalDate.now().plusDays(10).toString())
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.documentName").value("Car Insurance"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} should soft delete document")
    void testDeleteDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document moved to Trash"));
    }

    @Test
    @DisplayName("PUT /api/documents/{id}/restore should restore document")
    void testRestoreDocument() throws Exception {
        DocumentResponse doc = new DocumentResponse();
        doc.setId(1L);
        doc.setDocumentName("Test Passport");
        when(documentService.restoreDocument(eq(1L), any())).thenReturn(doc);

        mockMvc.perform(put("/api/documents/1/restore").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document restored successfully"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id}/permanent should permanently remove document")
    void testPermanentlyDeleteDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1/permanent").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document permanently deleted from vault and disk"));
    }

    @Test
    @DisplayName("GET /api/documents/trash should return trash list")
    void testGetTrashDocuments() throws Exception {
        when(documentService.getTrashDocuments(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/documents/trash").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/documents/calendar should return calendar documents")
    void testGetCalendarDocuments() throws Exception {
        when(documentService.getCalendarDocuments(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/documents/calendar").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
