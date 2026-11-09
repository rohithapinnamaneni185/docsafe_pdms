package com.pdms.config;

import com.pdms.entity.Document;
import com.pdms.entity.User;
import com.pdms.repository.DocumentRepository;
import com.pdms.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserRepository userRepository, DocumentRepository documentRepository,
                           PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate,
                           @Value("${app.seed.default-password:Password@123}") String seedPassword) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder != null ? passwordEncoder : new BCryptPasswordEncoder();
        this.seedPassword = seedPassword;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        migrateLegacyPasswordColumn();
        User admin = seedUser("rohithapinnamaneni185@gmail.com", "Rohitha", "ROLE_ADMIN");
        seedUser("25r21a05l4@mlrit.ac.in", "Rohitha", "ROLE_MEMBER");

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

    private void migrateLegacyPasswordColumn() {
        Integer legacyColumnCount = columnCount("password");
        if (legacyColumnCount != null && legacyColumnCount > 0) {
            jdbcTemplate.update("UPDATE users SET password_hash = password "
                    + "WHERE (password_hash IS NULL OR password_hash = '') "
                    + "AND password IS NOT NULL AND password <> ''");
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN password VARCHAR(255) NULL");
        }
        Integer legacyOtpColumnCount = columnCount("otp_code");
        if (legacyOtpColumnCount != null && legacyOtpColumnCount > 0
                && columnCount("otp_expiry") > 0) {
            jdbcTemplate.update("UPDATE users SET otp_code = NULL, otp_expiry = NULL");
        }
        userRepository.findAll().stream()
                .filter(user -> user.getPassword() != null && !user.getPassword().isBlank())
                .filter(user -> !isBcryptHash(user.getPassword()))
                .forEach(user -> {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    userRepository.save(user);
                });
    }

    private Integer columnCount(String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = ?",
                Integer.class, columnName);
    }

    private User seedUser(String email, String name, String role) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(seedPassword));
            newUser.setTheme("theme-default");
            log.info("Creating default {} account.", role);
            return newUser;
        });

        user.setFullName(name);
        user.setRelationship("Self");
        user.setRole(role);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(seedPassword));
        } else if (!isBcryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getTheme() == null || user.getTheme().isBlank()) {
            user.setTheme("theme-default");
        }
        return userRepository.save(user);
    }

    private boolean isBcryptHash(String value) {
        return value != null && (value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$"));
    }
}
