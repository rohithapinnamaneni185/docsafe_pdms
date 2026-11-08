package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.CreateMemberRequest;
import com.pdms.dto.LoginInitiateRequest;
import com.pdms.dto.LoginInitiateResponse;
import com.pdms.dto.LoginOtpResponse;
import com.pdms.dto.LoginRequest;
import com.pdms.dto.RegisterRequest;
import com.pdms.dto.ResendOtpRequest;
import com.pdms.dto.UpdateMemberRequest;
import com.pdms.dto.UserResponse;
import com.pdms.dto.VerifyOtpRequest;
import com.pdms.dto.VerifyOtpResponse;
import com.pdms.entity.User;
import com.pdms.exception.BadRequestException;
import com.pdms.exception.UnauthorizedException;
import com.pdms.repository.UserRepository;
import com.pdms.service.OtpService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public static final String SESSION_USER_KEY = "LOGGED_IN_USER_ID";
    private final UserService userService;
    private final OtpService otpService;
    private final UserRepository userRepository;

    @Autowired
    public AuthController(UserService userService, OtpService otpService, UserRepository userRepository) {
        this.userService = userService;
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    public AuthController(UserService userService) {
        this(userService, null, null);
    }

    /**
     * POST /api/auth/login-request
     * Step 1 of Two-Factor Authentication: validates email and password.
     * If email is not registered, returns 401 with isNewUser: true.
     * If invalid password, returns 401 Unauthorized with isNewUser: false.
     * If valid, generates a 6-digit OTP, dispatches via email, and returns {"success": true, "message": "OTP sent to your email", "demoOtp": "..."}.
     */
    @PostMapping("/login-request")
    public ResponseEntity<LoginOtpResponse> loginRequest(
            @RequestBody Map<String, String> body) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            email = body.get("identifier");
        }
        String password = body.get("password");

        String cleanEmail = (email != null) ? email.trim().toLowerCase() : "";
        boolean emailExists = (userRepository != null) && !cleanEmail.isBlank()
                && userRepository.findByEmailIgnoreCase(cleanEmail).isPresent();

        LoginRequest req = new LoginRequest(cleanEmail, password);
        try {
            LoginOtpResponse response = otpService.loginRequest(req);
            return ResponseEntity.ok(response);
        } catch (UnauthorizedException ex) {
            if (!emailExists && !cleanEmail.isBlank()) {
                LoginOtpResponse resp = new LoginOtpResponse(false, "No vault account found for " + cleanEmail + ". Please create an account.", null, cleanEmail, null);
                resp.setNewUser(true);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
            }
            LoginOtpResponse resp = new LoginOtpResponse(false, "Invalid email or password. Please try again.", null, cleanEmail, null);
            resp.setNewUser(false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }
    }

    /**
     * Overloaded loginRequest accepting strongly-typed LoginRequest DTO.
     */
    public ResponseEntity<LoginOtpResponse> loginRequest(LoginRequest request) {
        return loginRequest(Map.of("email", request.getEmail() != null ? request.getEmail() : "",
                                   "password", request.getPassword() != null ? request.getPassword() : ""));
    }

    /**
     * POST /api/auth/login-initiate
     * Initiates login and OTP delivery using strongly-typed LoginInitiateRequest DTO.
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
     * POST /api/auth/register-request
     * Registers a new account with BCrypt password hashing and initiates 2FA email verification.
     */
    @PostMapping("/register-request")
    public ResponseEntity<LoginOtpResponse> registerRequest(
            @Valid @RequestBody RegisterRequest request) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        LoginOtpResponse response = otpService.registerRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/google-login
     * Authenticates or registers via Google account identity and sets active session.
     */
    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<UserResponse>> googleLogin(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        String email = body.get("email");
        String fullName = body.get("fullName");
        User user = otpService.googleLogin(email, fullName);
        session.setAttribute(SESSION_USER_KEY, user.getId());
        UserResponse resp = UserResponse.fromEntity(user);
        return ResponseEntity.ok(ApiResponse.ok("Google authentication successful. Welcome, " + user.getFullName() + "!", resp));
    }

    /**
     * POST /api/auth/verify-otp
     * Step 2 of Two-Factor Authentication: validates 6-digit OTP for the user's email,
     * establishes active user session, and returns {"success": true, "redirectUrl": "/dashboard.html", "data": ...}.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @RequestBody Map<String, String> body,
            HttpSession session) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        String email = body.get("email");
        String tempToken = body.get("tempToken");
        String otp = body.get("otp");

        User user;
        if (email != null && !email.isBlank()) {
            user = otpService.verifyOtpByEmail(email, otp);
        } else if (tempToken != null && !tempToken.isBlank()) {
            user = otpService.verifyOtp(tempToken, otp);
        } else {
            throw new BadRequestException("Email address or session token is required to verify OTP.");
        }

        session.setAttribute(SESSION_USER_KEY, user.getId());
        UserResponse resp = UserResponse.fromEntity(user);
        return ResponseEntity.ok(VerifyOtpResponse.success("Authentication successful. Welcome, " + user.getFullName() + "!", "/dashboard.html", resp));
    }

    /**
     * Overloaded verifyOtp accepting strongly-typed VerifyOtpRequest DTO.
     */
    public ResponseEntity<VerifyOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpSession session) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        User user = otpService.verifyOtp(request);
        session.setAttribute(SESSION_USER_KEY, user.getId());
        UserResponse resp = UserResponse.fromEntity(user);
        return ResponseEntity.ok(VerifyOtpResponse.success("Authentication successful. Welcome, " + user.getFullName() + "!", "/dashboard.html", resp));
    }

    /**
     * POST /api/auth/resend-otp
     * Regenerates and resends a fresh 6-digit OTP to user's email.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<LoginOtpResponse> resendOtp(
            @RequestBody Map<String, String> body) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        String email = body.get("email");
        String tempToken = body.get("tempToken");

        LoginOtpResponse response;
        if (email != null && !email.isBlank()) {
            response = otpService.resendOtpByEmail(email);
        } else if (tempToken != null && !tempToken.isBlank()) {
            var compat = otpService.resendOtp(tempToken);
            response = new LoginOtpResponse(true, "OTP sent to your email", compat.getDebugOtp(), compat.getDestination(), compat.getTempToken());
        } else {
            throw new BadRequestException("Email address is required to resend OTP.");
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Overloaded resendOtp accepting strongly-typed ResendOtpRequest DTO.
     */
    public ResponseEntity<ApiResponse<LoginInitiateResponse>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        if (otpService == null) {
            throw new BadRequestException("OTP Service is currently unavailable.");
        }
        LoginInitiateResponse response = otpService.resendOtp(request);
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
