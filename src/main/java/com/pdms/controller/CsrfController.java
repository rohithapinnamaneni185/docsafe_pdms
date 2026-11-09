package com.pdms.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    public String csrfToken(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }
}
