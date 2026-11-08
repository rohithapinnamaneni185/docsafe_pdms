package com.pdms.config;

import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(UserRepository userRepository,
                           DocumentRepository documentRepository,
                           @Autowired(required = false) PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = (passwordEncoder != null) ? passwordEncoder : new BCryptPasswordEncoder();
    }

    @Override
    public void run(String... args) {
        // Ensure default Admin user exists with Rohitha (Self) credentials and mobile
        User admin = userRepository.findByEmailIgnoreCase("admin@docusafe.local").orElseGet(() -> {
            User newAdmin = new User();
            newAdmin.setFullName("Rohitha (Self)");
            newAdmin.setEmail("admin@docusafe.local");
            newAdmin.setPassword(passwordEncoder.encode("admin123"));
            newAdmin.setMobileNumber("9876543210");
            newAdmin.setRelationship("Self");
            newAdmin.setRole("ROLE_ADMIN");
            newAdmin.setTheme("theme-default");
            return userRepository.save(newAdmin);
        });

        boolean adminUpdated = false;
        if (!"Rohitha (Self)".equals(admin.getFullName())) {
            admin.setFullName("Rohitha (Self)");
            adminUpdated = true;
        }
        if (admin.getMobileNumber() == null || !admin.getMobileNumber().equals("9876543210")) {
            admin.setMobileNumber("9876543210");
            adminUpdated = true;
        }
        if (admin.getTheme() == null) {
            admin.setTheme("theme-default");
            adminUpdated = true;
        }
        if (admin.getPassword() != null && !admin.getPassword().startsWith("$2a$") && !admin.getPassword().startsWith("$2b$")) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            adminUpdated = true;
        }
        if (adminUpdated) {
            admin = userRepository.save(admin);
        }

        // Ensure any unassigned documents belong to Admin
        List<Document> allDocs = documentRepository.findAll();
        boolean needsSave = false;
        for (Document doc : allDocs) {
            if (doc.getUser() == null) {
                doc.setUser(admin);
                needsSave = true;
            }
        }
        if (needsSave) {
            documentRepository.saveAll(allDocs);
        }
    }
}
