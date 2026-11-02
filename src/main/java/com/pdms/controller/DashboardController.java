package com.pdms.controller;

import com.pdms.dto.ApiResponse;
import com.pdms.dto.DashboardSummary;
import com.pdms.entity.User;
import com.pdms.service.DocumentService;
import com.pdms.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DocumentService documentService;
    private final UserService userService;

    public DashboardController(DocumentService documentService, UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    /**
     * GET /api/dashboard/summary
     * Returns user-scoped metrics, expiry status breakdown, category distribution,
     * recent documents, and expiring soon list.
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary(
            @RequestParam(required = false) Long memberId,
            HttpSession session) {
        User currentUser = AuthController.getAuthenticatedUserOrThrow(session, userService);
        DashboardSummary summary = documentService.getDashboardSummary(memberId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics retrieved successfully", summary));
    }
}
