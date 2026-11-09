package com.pdms.controller;

import com.pdms.dto.DocumentRequest;
import com.pdms.dto.DocumentResponse;
import com.pdms.entity.User;
import com.pdms.model.ExpiryStatus;
import com.pdms.service.DocumentService;
import com.pdms.service.FileStorageService;
import com.pdms.service.UserService;
import com.pdms.config.SecurityConfig;
import com.pdms.service.GoogleOAuth2SuccessHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WebMvcTest(DocumentController.class)
@Import(SecurityConfig.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private UserService userService;

    @MockBean
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private User adminUser;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        adminUser = new User("System Administrator", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        adminUser.setId(1L);

        session = new MockHttpSession();
        session.setAttribute(AuthController.SESSION_USER_KEY, 1L);

        when(userService.getUserById(1L)).thenReturn(adminUser);
        lenient().when(clientRegistrationRepository.findByRegistrationId("google"))
                .thenReturn(ClientRegistration.withRegistrationId("google")
                        .clientId("test-client")
                        .clientSecret("test-secret")
                        .clientName("Google")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .scope("openid", "email", "profile")
                        .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                        .tokenUri("https://oauth2.googleapis.com/token")
                        .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                        .userNameAttributeName("sub")
                        .build());
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
                        .with(csrf())
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
        mockMvc.perform(delete("/api/documents/1").with(csrf()).session(session))
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

        mockMvc.perform(put("/api/documents/1/restore").with(csrf()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document restored successfully"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id}/permanent should permanently remove document")
    void testPermanentlyDeleteDocument() throws Exception {
        mockMvc.perform(delete("/api/documents/1/permanent").with(csrf()).session(session))
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

    @Test
    @DisplayName("Google sign-in requests the standard account chooser")
    void googleSignInRequestsAccountChooser() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> {
                    String redirectUrl = result.getResponse().getRedirectedUrl();
                    assertNotNull(redirectUrl);
                    assertTrue(redirectUrl.contains("prompt=select_account"));
                });
    }
}
