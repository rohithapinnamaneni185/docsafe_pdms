package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.UpdateMemberRequest;
import com.pdms.dto.UserResponse;
import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.OtpVerificationRepository;
import com.pdms.repository.UserRepository;
import com.pdms.service.FileStorageService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Family Member Vault management (/api/members).
 */
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final OtpVerificationRepository otpVerificationRepository;

    @Autowired
    public MemberController(UserService userService,
                            UserRepository userRepository,
                            DocumentRepository documentRepository,
                            @Autowired(required = false) FileStorageService fileStorageService,
                            @Autowired(required = false) OtpVerificationRepository otpVerificationRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.otpVerificationRepository = otpVerificationRepository;
    }

    /**
     * GET /api/members
     * Returns all registered family members with their document counts.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getMembers(HttpSession session) {
        AuthController.getAuthenticatedUserOrThrow(session, userService);
        List<UserResponse> members = userService.getFamilyMembers();
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + members.size() + " family members", members));
    }

    /**
     * POST /api/members
     * Adds a new family member account. (Admin only)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> addMember(
            @Valid @RequestBody CreateMemberRequest request,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        UserResponse created = userService.createFamilyMember(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Family member '" + created.getFullName() + "' added successfully.", created));
    }

    /**
     * PUT /api/members/{id}
     * Updates an existing member profile without resetting their password.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateMember(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateMemberRequest request,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        UserResponse updated = userService.updateFamilyMember(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Profile for '" + updated.getFullName() + "' updated successfully.", updated));
    }

    /**
     * DELETE /api/members/{id}
     * Removes ANY family member profile without restrictions, including profiles marked as "Self" or "Admin".
     * Cleans up all associated documents, physical files, and OTP verification records to respect foreign keys.
     */
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMember(
            @PathVariable("id") Long id,
            HttpSession session) {
        AuthController.getAuthenticatedUserOrThrow(session, userService);

        // 1. Check if the member exists
        User member = userRepository.findById(id).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Family member not found with ID: " + id));
        }

        // 2. Cascade delete documents linked to this member and their physical storage files
        List<Document> memberDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(id);
        if (memberDocs != null && !memberDocs.isEmpty()) {
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
        }

        // 3. Delete associated OTP verification records for this user's email
        if (otpVerificationRepository != null && member.getEmail() != null) {
            try {
                otpVerificationRepository.deleteByEmailIgnoreCase(member.getEmail());
            } catch (Exception ignored) {
            }
        }

        // 4. Delete the member record from the database table
        userRepository.delete(member);

        // 5. If the deleted member was the currently authenticated session user, switch session to another member
        Long sessionUserId = (Long) session.getAttribute(AuthController.SESSION_USER_KEY);
        if (sessionUserId != null && sessionUserId.equals(id)) {
            List<User> remaining = userRepository.findAll();
            if (!remaining.isEmpty()) {
                session.setAttribute(AuthController.SESSION_USER_KEY, remaining.get(0).getId());
            } else {
                session.removeAttribute(AuthController.SESSION_USER_KEY);
            }
        }

        // 6. Return HTTP 200 OK
        return ResponseEntity.ok(ApiResponse.ok("Member removed successfully.", null));
    }
}

