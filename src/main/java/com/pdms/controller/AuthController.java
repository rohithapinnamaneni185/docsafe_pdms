package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.dto.LoginRequest;
import com.pdms.dto.ResendOtpRequest;
import com.pdms.dto.UpdateMemberRequest;
import com.pdms.dto.UserResponse;
import com.pdms.dto.VerifyOtpRequest;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.service.OtpService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGGED_IN_USER_ID";
    private final UserService userService;
    private final OtpService otpService;

    @Autowired
    public AuthController(UserService userService, OtpService otpService) {
        this.userService = userService;
        this.otpService = otpService;
    }

    public AuthController(UserService userService) {
        this(userService, null);
    }

    /**
     * POST /api/auth/login-initiate
     * Step 1 of Two-Factor Authentication: validates email or mobile number + password,
     * generates a 6-digit OTP, dispatches SMS & email simulation, and returns pending session.
     */
    @PostMapping("/login-initiate")
    public ResponseEntity<ApiResponse<LoginInitiateResponse>> loginInitiate(
            @Valid @RequestBody LoginInitiateRequest request) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        LoginInitiateResponse response = otpService.initiateLogin(request);
        return ResponseEntity.ok(ApiResponse.ok("Verification code dispatched to your registered email and mobile number.", response));
    }

    /**
     * POST /api/auth/verify-otp
     * Step 2 of Two-Factor Authentication: verifies the 6-digit OTP code against the pending session,
     * sets the active HTTP session, and returns user details.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<UserResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpSession session) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        User user = otpService.verifyOtp(request.getTempToken(), request.getOtp());
        session.setAttribute(SESSION_USER_KEY, user.getId());
        UserResponse resp = UserResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.ok("Authentication successful. Welcome, " + user.getFullName() + "!", resp));
    }

    /**
     * POST /api/auth/resend-otp
     * Regenerates and resends a fresh 6-digit OTP to the pending verification session.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<LoginInitiateResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        LoginInitiateResponse response = otpService.resendOtp(request.getTempToken());
        return ResponseEntity.ok(ApiResponse.ok("A fresh 6-digit verification code has been dispatched.", response));
    }

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());
        session.setAttribute(SESSION_USER_KEY, user.getId());
        UserResponse resp = UserResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.ok("Welcome back, " + user.getFullName() + "!", resp));
    }

    /**
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_KEY);
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully.", null));
    }

    /**
     * GET /api/auth/me
     * Returns the currently active user session.
     * Returns HTTP 401 Unauthorized if no active session exists.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Session expired or unauthenticated. Please log in."));
        }
        try {
            User user = userService.getUserById(userId);
            return ResponseEntity.ok(ApiResponse.ok("User session active", UserResponse.fromEntity(user)));
        } catch (Exception e) {
            session.removeAttribute(SESSION_USER_KEY);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Session user no longer exists."));
        }
    }

    /**
     * POST /api/auth/switch-vault/{userId}
     * One-click active vault switching between family members without requiring password re-entry.
     */
    @PostMapping("/switch-vault/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> switchVault(@PathVariable Long userId, HttpSession session) {
        User user = userService.getUserById(userId);
        session.setAttribute(SESSION_USER_KEY, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Active vault switched to " + user.getFullName(), UserResponse.fromEntity(user)));
    }

    /**
     * POST /api/auth/family-members
     * Adds a new family member account. (Admin only)
     */
    @PostMapping("/family-members")
    public ResponseEntity<ApiResponse<UserResponse>> addFamilyMember(
            @Valid @RequestBody CreateMemberRequest request,
            HttpSession session) {
        User currentUser = getAuthenticatedUserOrThrow(session, userService);
        UserResponse created = userService.createFamilyMember(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Family member '" + created.getFullName() + "' added successfully.", created));
    }

    /**
     * PUT /api/auth/family-members/{id}
     * Updates an existing member profile without resetting their password.
     */
    @PutMapping("/family-members/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateFamilyMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request,
            HttpSession session) {
        User currentUser = getAuthenticatedUserOrThrow(session, userService);
        UserResponse updated = userService.updateFamilyMember(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Profile for '" + updated.getFullName() + "' updated successfully.", updated));
    }

    /**
     * DELETE /api/auth/family-members/{id}
     * Removes a family member account and their documents. (Admin only)
     */
    @DeleteMapping({"/family-members/{id}", "/members/{id}"})
    public ResponseEntity<ApiResponse<Void>> deleteFamilyMember(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = getAuthenticatedUserOrThrow(session, userService);
        userService.deleteFamilyMember(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Family member removed successfully.", null));
    }

    /**
     * PUT /api/auth/profile/theme
     * Updates and persists the user's chosen theme.
     */
    @PutMapping("/profile/theme")
    public ResponseEntity<ApiResponse<UserResponse>> updateTheme(
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestParam(required = false) String theme,
            HttpSession session) {
        User currentUser = getAuthenticatedUserOrThrow(session, userService);
        String selectedTheme = (body != null && body.containsKey("theme")) ? body.get("theme") : theme;
        if (selectedTheme == null || selectedTheme.isBlank()) {
            selectedTheme = "theme-default";
        }
        User updated = userService.updateTheme(currentUser.getId(), selectedTheme.trim());
        return ResponseEntity.ok(ApiResponse.ok("Theme updated to " + selectedTheme, UserResponse.fromEntity(updated)));
    }

    /**
     * Helper to obtain the authenticated User entity from the session.
     * Throws UnauthorizedException if no active authenticated session exists.
     */
    public static User getAuthenticatedUserOrThrow(HttpSession session, UserService userService) {
        Long userId = (Long) session.getAttribute(SESSION_USER_KEY);
        if (userId == null) {
            throw new UnauthorizedException("Access denied. Please authenticate with your email/mobile and OTP.");
        }
        try {
            return userService.getUserById(userId);
        } catch (Exception e) {
            session.removeAttribute(SESSION_USER_KEY);
            throw new UnauthorizedException("Session invalid. Please sign in again.");
        }
    }
}
