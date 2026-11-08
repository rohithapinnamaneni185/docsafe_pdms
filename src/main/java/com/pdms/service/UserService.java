package com.pdms.service;

import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.UpdateMemberRequest;
import com.pdms.dto.UserResponse;
import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.ForbiddenException;
import com.pdms.exception.ResourceNotFoundException;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository,
                       DocumentRepository documentRepository,
                       FileStorageService fileStorageService,
                       @Autowired(required = false) PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = (passwordEncoder != null) ? passwordEncoder : new BCryptPasswordEncoder();
    }

    public UserService(UserRepository userRepository, DocumentRepository documentRepository) {
        this(userRepository, documentRepository, null, new BCryptPasswordEncoder());
    }

    /**
     * Authenticates a user with email and password.
     */
    @Transactional(readOnly = false)
    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new BadRequestException("Email and password are required.");
        }

        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        boolean matches = false;
        if (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$") || user.getPassword().startsWith("$2y$")) {
            matches = passwordEncoder.matches(password.trim(), user.getPassword());
        } else if (user.getPassword().equals(password.trim())) {
            matches = true;
            try {
                user.setPassword(passwordEncoder.encode(password.trim()));
                userRepository.save(user);
            } catch (Exception ignored) {}
        }

        if (!matches) {
            throw new BadRequestException("Invalid email or password.");
        }

        return user;
    }

    /**
     * Retrieves all family members with their document counts.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getFamilyMembers() {
        List<Object[]> results = userRepository.findAllUsersWithDocumentCount();
        List<UserResponse> list = new ArrayList<>();
        for (Object[] row : results) {
            User user = (User) row[0];
            Long docCount = (Long) row[1];
            list.add(UserResponse.fromEntity(user, docCount != null ? docCount : 0));
        }
        return list;
    }

    /**
     * Creates a new family member account with max 3 non-self members quota enforcement.
     */
    public UserResponse createFamilyMember(CreateMemberRequest request, User currentUser) {
        if (!currentUser.isAdmin()) {
            throw new BadRequestException("Only the primary administrator can add family members.");
        }

        // Quota check: Allow up to 3 non-self members in free tier
        long nonSelfCount = userRepository.countNonSelfMembers();
        if (nonSelfCount >= 3) {
            throw new ForbiddenException("Family member limit reached. Please upgrade to DocuSafe Premium to add more members.");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("A family member with this email already exists: " + email);
        }

        User member = new User();
        member.setFullName(request.getFullName().trim());
        member.setEmail(email);
        member.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        member.setRelationship(request.getRelationship().trim());
        member.setRole("ROLE_MEMBER");

        User saved = userRepository.save(member);
        return UserResponse.fromEntity(saved, 0);
    }

    /**
     * Updates an existing family member profile (fullName, relationship, email)
     * without resetting their password.
     */
    public UserResponse updateFamilyMember(Long memberId, UpdateMemberRequest request, User currentUser) {
        if (!currentUser.isAdmin() && !currentUser.getId().equals(memberId)) {
            throw new ForbiddenException("You do not have permission to update this profile.");
        }

        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + memberId));

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, memberId)) {
            throw new BadRequestException("A user with this email already exists: " + email);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setRelationship(request.getRelationship().trim());

        User saved = userRepository.save(user);
        long docCount = documentRepository.countByUserIdAndIsDeletedFalse(saved.getId());
        return UserResponse.fromEntity(saved, docCount);
    }

    /**
     * Deletes a family member account and their documents without restrictions.
     */
    public void deleteFamilyMember(Long memberId, User currentUser) {
        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Family member not found with ID: " + memberId));

        // Clean up member's documents and associated physical files
        List<Document> memberDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(memberId);
        if (fileStorageService != null) {
            for (Document doc : memberDocs) {
                if (doc.getFileName() != null && !doc.getFileName().isBlank()) {
                    try {
                        fileStorageService.deleteFile(doc.getFileName());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        documentRepository.deleteAll(memberDocs);

        userRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    /**
     * Gets or creates the primary default administrator account.
     */
    @Transactional
    public User getOrCreateDefaultAdmin() {
        return userRepository.findByEmailIgnoreCase("admin@docusafe.local")
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setFullName("System Administrator (Self)");
                    admin.setEmail("admin@docusafe.local");
                    admin.setPassword("admin123");
                    admin.setRole("ROLE_ADMIN");
                    admin.setRelationship("Self");
                    return userRepository.save(admin);
                });
    }

    /**
     * Updates and saves the user's theme setting.
     */
    @Transactional
    public User updateTheme(Long userId, String theme) {
        User user = getUserById(userId);
        user.setTheme(theme);
        return userRepository.save(user);
    }
}
