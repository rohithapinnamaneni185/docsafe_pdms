package com.pdms.controller;

import com.pdms.dto.AddFamilyMemberRequest;
import com.pdms.dto.ApiResponse;
import com.pdms.dto.VerifyFamilyMemberOtpRequest;
import com.pdms.dto.UserResponse;
import com.pdms.entity.User;
import com.pdms.service.FamilyMemberOtpService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/api/family")
public class FamilyMemberController {

    private final FamilyMemberOtpService familyMemberOtpService;
    private final UserService userService;

    public FamilyMemberController(FamilyMemberOtpService familyMemberOtpService, UserService userService) {
        this.familyMemberOtpService = familyMemberOtpService;
        this.userService = userService;
    }

    @PostMapping("/initiate-add")
    public ResponseEntity<ApiResponse<Void>> initiateAdd(
            @Valid @RequestBody AddFamilyMemberRequest request,
            HttpSession session) {
        User administrator = AuthController.getAuthenticatedUserOrThrow(session, userService);
        familyMemberOtpService.initiate(request, administrator);
        return ResponseEntity.ok(ApiResponse.ok(
                "Verification code sent to " + request.email().trim().toLowerCase(Locale.ROOT), null));
    }

    @PostMapping("/verify-and-create")
    public ResponseEntity<ApiResponse<UserResponse>> verifyAndCreate(
            @Valid @RequestBody VerifyFamilyMemberOtpRequest request,
            HttpSession session) {
        User administrator = AuthController.getAuthenticatedUserOrThrow(session, userService);
        UserResponse created = familyMemberOtpService.verifyAndCreate(request, administrator);
        return ResponseEntity.ok(ApiResponse.ok("Family member added successfully.", created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            HttpSession session) {
        User administrator = AuthController.getAuthenticatedUserOrThrow(session, userService);
        userService.deleteFamilyMember(id, administrator);
        return ResponseEntity.ok(ApiResponse.ok("Family member removed.", null));
    }
}
