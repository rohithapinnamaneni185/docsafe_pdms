package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.UpdateMemberRequest;
import com.pdms.dto.UserResponse;
import com.pdms.entity.User;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/users/family-members
     * Returns all registered family members with their document counts.
     */
    @GetMapping({"/family-members", "/family"})
    public ResponseEntity<ApiResponse<List<UserResponse>>> getFamilyMembers(HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        List<UserResponse> members = userService.getFamilyMembers();
        return ResponseEntity.ok(ApiResponse.ok("Retrieved " + members.size() + " family members", members));
    }

    /**
     * POST /api/users/family-members or /api/users/family
     * Adds a new family member account. (Admin only)
     */
    @PostMapping({"/family-members", "/family"})
    public ResponseEntity<ApiResponse<UserResponse>> addFamilyMember(
            @Valid @RequestBody CreateMemberRequest request,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        UserResponse created = userService.createFamilyMember(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Family member '" + created.getFullName() + "' added successfully.", created));
    }

    /**
     * PUT /api/users/family-members/{id} or /api/users/{id}
     * Updates an existing member profile without resetting their password.
     */
    @PutMapping({"/family-members/{id}", "/{id}"})
    public ResponseEntity<ApiResponse<UserResponse>> updateFamilyMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        UserResponse updated = userService.updateFamilyMember(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Profile for '" + updated.getFullName() + "' updated successfully.", updated));
    }

    /**
     * DELETE /api/users/family-members/{id}
     * Removes a family member account and their documents. (Admin only)
     */
    @DeleteMapping({"/family-members/{id}", "/{id}"})
    public ResponseEntity<ApiResponse<Void>> deleteFamilyMember(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        userService.deleteFamilyMember(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Family member removed successfully.", null));
    }
}
