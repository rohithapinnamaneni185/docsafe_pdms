package com.pdms.service;

import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.ResourceNotFoundException;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User regularMember;

    @BeforeEach
    void setUp() {
        adminUser = new User("System Administrator", "admin@docusafe.local", "admin123", "Self", "ROLE_ADMIN");
        adminUser.setId(1L);

        regularMember = new User("Jane Doe", "jane@docusafe.local", "pass123", "Spouse", "ROLE_MEMBER");
        regularMember.setId(2L);
    }

    @Test
    @DisplayName("Should reassign documents before deleting a family member")
    void deleteFamilyMember_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularMember));

        Document docWithFile = new Document();
        docWithFile.setId(10L);
        docWithFile.setUser(regularMember);
        docWithFile.setFileName("uuid-test-scan.png");

        when(documentRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(docWithFile));

        userService.deleteFamilyMember(2L, adminUser);

        assertSame(adminUser, docWithFile.getUser());
        verify(documentRepository).saveAll(List.of(docWithFile));
        verify(documentRepository, never()).deleteAll(any());
        verifyNoInteractions(fileStorageService);
        verify(userRepository, times(1)).delete(regularMember);
    }

    @Test
    @DisplayName("Should reject deletion if current user is not admin")
    void deleteFamilyMember_NonAdminThrows() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.deleteFamilyMember(2L, regularMember));
        assertTrue(ex.getMessage().contains("Only the primary administrator"));
        verifyNoInteractions(documentRepository);
    }

    @Test
    @DisplayName("Should reject deletion when attempting to delete self")
    void deleteFamilyMember_CannotDeleteSelf() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.deleteFamilyMember(1L, adminUser));
        assertEquals("Cannot remove primary vault administrator.", ex.getMessage());
        verifyNoInteractions(documentRepository);
    }

    @Test
    @DisplayName("Should reject deletion when target has admin role")
    void deleteFamilyMember_CannotDeleteAdminAccount() {
        User anotherAdmin = new User("Admin Two", "admin2@docusafe.local", "pass", "Self", "ROLE_ADMIN");
        anotherAdmin.setId(5L);

        when(userRepository.findById(5L)).thenReturn(Optional.of(anotherAdmin));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                userService.deleteFamilyMember(5L, adminUser));
        assertEquals("Cannot remove primary vault administrator.", ex.getMessage());
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow deleting a non-admin account whose relationship is Self")
    void deleteFamilyMember_AllowsSelfRelationshipForNonAdmin() {
        User familyAccount = new User("Family Account", "family@example.com", "pass", "Self", "ROLE_MEMBER");
        familyAccount.setId(6L);
        when(userRepository.findById(6L)).thenReturn(Optional.of(familyAccount));
        when(documentRepository.findByUserIdOrderByCreatedAtDesc(6L)).thenReturn(List.of());

        assertDoesNotThrow(() -> userService.deleteFamilyMember(6L, adminUser));

        verify(userRepository).delete(familyAccount);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException if member not found")
    void deleteFamilyMember_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                userService.deleteFamilyMember(99L, adminUser));
    }
}
