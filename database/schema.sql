-- ====================================================================
-- Personal Document Management System (PDMS) Database Schema
-- Multi-Member Vault & Authentication Support
-- Database: personal_document_db
-- MySQL 8.0+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS personal_document_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE personal_document_db;

-- --------------------------------------------------------------------
-- Table structure for table: users
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    mobile_number VARCHAR(15) DEFAULT NULL,
    password VARCHAR(255) NOT NULL,
    relationship VARCHAR(50) NOT NULL DEFAULT 'Self',
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_MEMBER',
    theme VARCHAR(50) NOT NULL DEFAULT 'theme-default',
    otp_code VARCHAR(6) DEFAULT NULL,
    otp_expiry DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_user_email (email),
    INDEX idx_user_mobile (mobile_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Table structure for table: otp_verifications
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS otp_verifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at DATETIME NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_otp_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Default Admin Account (admin@docusafe.local / admin123)
-- --------------------------------------------------------------------
INSERT INTO users (id, full_name, email, mobile_number, password, relationship, role, theme, created_at)
SELECT 1, 'Rohitha (Self)', 'admin@docusafe.local', '9876543210', 'admin123', 'Self', 'ROLE_ADMIN', 'theme-default', NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@docusafe.local');

-- --------------------------------------------------------------------
-- Table structure for table: documents
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT 1,
    document_name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    issuing_organization VARCHAR(150) DEFAULT NULL,
    reference_number VARCHAR(100) DEFAULT NULL,
    issue_date DATE DEFAULT NULL,
    expiry_date DATE DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    image_path VARCHAR(255) DEFAULT NULL,
    file_name VARCHAR(255) DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,
    file_type VARCHAR(100) DEFAULT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_expiry_date (expiry_date),
    INDEX idx_document_name (document_name),
    INDEX idx_user_id (user_id),
    INDEX idx_is_deleted (is_deleted),
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------------------
-- Sample Fictional Documents for Testing & Demonstration
-- --------------------------------------------------------------------
INSERT INTO documents (user_id, document_name, category, issuing_organization, reference_number, issue_date, expiry_date, notes, created_at, updated_at)
VALUES 
(1, 'National Driving License', 'Vehicle', 'Regional Transport Office (RTO)', 'DL-0420180092314', '2019-03-15', DATE_ADD(CURRENT_DATE(), INTERVAL 25 DAY), 'Commercial light motor vehicle endorsement included. Renewal required soon.', NOW(), NOW()),
(1, 'Comprehensive Car Insurance', 'Insurance', 'HDFC ERGO General Insurance', 'POL-CAR-88291039', '2025-11-01', DATE_ADD(CURRENT_DATE(), INTERVAL 14 DAY), 'Includes zero depreciation and roadside assistance cover.', NOW(), NOW()),
(1, 'Passport - Republic of India', 'Identity', 'Ministry of External Affairs', 'Z5948392', '2018-06-20', DATE_ADD(CURRENT_DATE(), INTERVAL 540 DAY), '36-page regular passport booklet.', NOW(), NOW()),
(1, 'B.Tech Degree Certificate', 'Education', 'National Institute of Technology', 'DEG-CSE-2024-0881', '2024-07-10', NULL, 'Original degree certificate in Computer Science & Engineering. Lifetime validity.', NOW(), NOW()),
(1, 'Family Floater Health Insurance', 'Medical', 'Star Health and Allied Insurance', 'SH-FAM-991204', '2024-10-15', DATE_SUB(CURRENT_DATE(), INTERVAL 10 DAY), 'Policy expired 10 days ago. Immediate premium payment needed to avoid grace lapse.', NOW(), NOW()),
(1, 'MacBook Pro Extended Warranty', 'Warranty', 'Apple India Private Limited', 'AP-CARE-771204', '2024-01-10', DATE_ADD(CURRENT_DATE(), INTERVAL 180 DAY), 'AppleCare+ 3-year warranty for M3 Pro 16-inch laptop.', NOW(), NOW()),
(1, 'Vehicle Registration Certificate (RC)', 'Vehicle', 'Transport Department', 'KA-01-MJ-4092', '2021-04-12', DATE_ADD(CURRENT_DATE(), INTERVAL 1200 DAY), 'Smart card registration certificate for sedan.', NOW(), NOW()),
(1, 'PAN Card (Permanent Account Number)', 'Identity', 'Income Tax Department', 'ABCDE1234F', '2017-09-05', NULL, 'Permanent tax identifier card. Does not expire.', NOW(), NOW());
