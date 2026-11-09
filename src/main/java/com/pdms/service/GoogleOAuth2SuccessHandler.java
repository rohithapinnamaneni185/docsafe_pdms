package com.pdms.service;

import com.pdms.controller.AuthController;
import com.pdms.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public GoogleOAuth2SuccessHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OAuth2User principal)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google did not return a user profile.");
            return;
        }

        String email = principal.getAttribute("email");
        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google did not return an email address.");
            return;
        }
        if (!Boolean.TRUE.equals(principal.getAttribute("email_verified"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Google account email is not verified.");
            return;
        }

        String name = principal.getAttribute("name");
        User user = userService.getOrCreateGoogleUser(email, name);
        request.getSession(true).setAttribute(AuthController.SESSION_USER_KEY, user.getId());
        response.sendRedirect("/");
    }
}
