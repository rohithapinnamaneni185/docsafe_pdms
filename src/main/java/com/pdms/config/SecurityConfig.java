package com.pdms.config;

import com.pdms.service.GoogleOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository,
            GoogleOAuth2SuccessHandler successHandler) throws Exception {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        OAuth2AuthorizationRequestResolver accountChooserResolver = new OAuth2AuthorizationRequestResolver() {
            @Override
            public org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request) {
                return addAccountChooser(defaultResolver.resolve(request));
            }

            @Override
            public org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest resolve(
                    jakarta.servlet.http.HttpServletRequest request, String clientRegistrationId) {
                return addAccountChooser(defaultResolver.resolve(request, clientRegistrationId));
            }

            private org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest addAccountChooser(
                    org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest request) {
                if (request == null) {
                    return null;
                }
                Map<String, Object> parameters = new HashMap<>(request.getAdditionalParameters());
                parameters.put("prompt", "select_account");
                return org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
                        .from(request)
                        .additionalParameters(parameters)
                        .build();
            }
        };

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/index.html", "/css/**", "/js/**", "/assets/**",
                                "/api/auth/**", "/api/csrf", "/oauth2/**", "/login/oauth2/**",
                                "/h2-console/**")
                        .permitAll()
                        // Application APIs enforce access with the logged-in user stored in HttpSession.
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/auth/**", "/h2-console/**")
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.authorizationRequestResolver(accountChooserResolver))
                        .successHandler(successHandler));

        return http.build();
    }
}
