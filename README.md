# DocuSafe — Multi-Member Vault & Personal Document Management System (PDMS)

> A modern, secure, and full-stack Personal & Family Document Management System developed for a **B.Tech 2nd-Year Computer Science & Engineering (CSE)** project demonstration.

DocuSafe allows families and individuals to securely store personal documents (Driver's Licenses, Vehicle Registrations, Health & Vehicle Insurances, Educational Certificates, Passports, Warranties), upload digital image scans, manage individual family member accounts with strict vault isolation, search and filter records, and automatically track document expiry dates with real-time countdown alerts.

---

## 1. Localhost Link & Direct Access

Access the complete system directly in your browser at:

**[http://localhost:8080](http://localhost:8080)**

*No login screen or password required—DocuSafe loads directly into the main Dashboard as the Primary Administrator (`System Administrator (Self)`).*

---

## 2. Key Features

1. **Direct Access & One-Click Vault Switcher**:
   - The application opens immediately into the active dashboard with zero login barriers.
   - Use the **"Active Vault"** selector in the top header or click **"Manage Vault"** on any member card to instantly switch between household members (Admin, Spouse, Child, etc.).
   - Vault metrics, document lists, and categories automatically update in real time to match the active member.
2. **Family Member Vaults & Access Isolation**:
   - **Primary Admin**: Has access to all documents across the entire household. Can view, edit, filter documents by family member, and add new family members.
   - **Family Member**: Document access is strictly isolated—when viewing as a member, only that member's documents and metrics appear.
3. **Family Members Management**:
   - Dedicated "Family Members" view in the sidebar.
   - Add new members with **Full Name**, **Email**, and **Relationship** (e.g., Father, Mother, Spouse, Child, Self).
   - Real-time document count badge per member and one-click **"Manage Vault"** action.
4. **Document Expiry Tracking**:
   - Real-time countdowns (`X days left`, `Expired X days ago`).
   - Categorized status badges: **Valid**, **Expiring Soon** (<= 30 days), and **Expired**.
5. **Secure Digital File Vault**:
   - Upload scans and photos (PNG, JPG, PDF, TXT) saved server-side in `uploads/` with UUID-based filenames and path traversal protection.
   - Modal preview with high-res zoom and direct download.

---

## 3. Technology Stack

- **Frontend**: HTML5, Vanilla CSS3 (Custom SaaS Design System in Navy & Indigo, Responsive Mobile/Desktop), Vanilla JavaScript (No React/Node.js dependencies).
- **Backend**: Java 27 / Spring Boot 3.3.5 (Spring Web, Spring Data JPA, Hibernate, Bean Validation, HikariCP, HTTP Session Auth).
- **Database**: MySQL Server 8.0 (`personal_document_db`).
- **File Vault**: Dedicated server-side storage in `uploads/` directory with UUID-based unique filenames, path traversal protection, and file cleanup on deletion.
- **Build Tool**: Maven Wrapper (`mvnw.cmd`).

---

## 4. Project Folder Structure

```text
PERSONAL_DOCUMENT_MANAGEMENT_SYSTEM/
├── pom.xml                               # Maven project definition and dependencies
├── mvnw                                  # Maven wrapper script for Linux/macOS
├── mvnw.cmd                              # Maven wrapper batch script for Windows
├── run.bat                               # One-click Windows application launcher
├── README.md                             # Complete documentation and setup guide
├── database/
│   └── schema.sql                        # MySQL schema creation (users & documents)
├── uploads/                              # Dedicated server-side directory for uploaded scans
├── src/
│   ├── main/
│   │   ├── java/com/pdms/
│   │   │   ├── PdmsApplication.java      # Spring Boot application entry point
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java  # Auto-seeds default Admin account on startup
│   │   │   │   └── WebConfig.java        # Static resource handlers & CORS config
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java   # Login, Logout, /api/auth/me session checks
│   │   │   │   ├── UserController.java   # Family member CRUD & listing
│   │   │   │   ├── DocumentController.java # User-scoped Document CRUD & image serving
│   │   │   │   └── DashboardController.java# User-scoped dashboard analytics & metrics
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java      # Unified REST JSON response envelope
│   │   │   │   ├── LoginRequest.java     # Authentication credentials payload
│   │   │   │   ├── CreateMemberRequest.java # Family member creation payload
│   │   │   │   ├── UserResponse.java     # User profile & document count DTO
│   │   │   │   ├── DashboardSummary.java # Aggregated stats & counts DTO
│   │   │   │   ├── DocumentRequest.java  # Document creation/update payload + member ID
│   │   │   │   └── DocumentResponse.java # Document response with owner metadata
│   │   │   ├── entity/
│   │   │   │   ├── User.java             # JPA Entity mapped to 'users' table
│   │   │   │   └── Document.java         # JPA Entity mapped to 'documents' table
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java # Centralized REST error handling
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   ├── ForbiddenException.java
│   │   │   │   └── FileStorageException.java
│   │   │   ├── model/
│   │   │   │   ├── DocumentCategory.java # Enum for categories
│   │   │   │   └── ExpiryStatus.java     # Enum with authoritative expiry rules
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java   # User queries & document count aggregations
│   │   │   │   └── DocumentRepository.java # User-scoped queries & count queries
│   │   │   └── service/
│   │   │       ├── UserService.java      # Auth & family member management logic
│   │   │       ├── DocumentService.java  # User-scoped CRUD & expiry calculations
│   │   │       └── FileStorageService.java# Secure file upload, storage & deletion
│   │   └── resources/
│   │       ├── application.properties    # Active MySQL and server configuration
│   │       └── static/
│   │           ├── index.html            # Clean SaaS layout (Login, Dashboard, Members)
│   │           ├── css/
│   │           │   └── style.css         # Responsive SaaS design system & cards
│   │           └── js/
│   │               ├── api.js            # Auth, Members, and Documents REST client
│   │               └── app.js            # Frontend state, session watcher & UI controller
│   └── test/
│       ├── java/com/pdms/
│       │   ├── controller/
│       │   │   └── DocumentControllerTest.java # MockMvc REST endpoint integration tests
│       │   └── service/
│       │       └── DocumentServiceTest.java    # Expiry calculation & unit tests
│       └── resources/
│           └── mockito-extensions/
│               └── org.mockito.plugins.MockMaker # JDK 27 subclass mock maker configuration
```

---

## 5. Database Setup (MySQL)

### Step 1: Open MySQL Command Line Client or Terminal
```cmd
mysql -u root -p
```
*(Enter your MySQL root password when prompted)*

### Step 2: Create or Verify Database
```sql
CREATE DATABASE IF NOT EXISTS personal_document_db;
```

### Step 3: Run the Schema
Run `database/schema.sql` to create `users` and `documents` tables:
```cmd
mysql -u root -p personal_document_db < database/schema.sql
```
*(Or let Spring Boot Data JPA automatically verify/update the tables on startup).*

### Step 4: Configure Database Credentials
Edit `src/main/resources/application.properties` if your MySQL password is not `mysql`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/personal_document_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=mysql
```

---

## 6. How to Run the Application

### Option A: Using the One-Click Launcher (Recommended for Windows)
Simply double-click or run from PowerShell/CMD:
```cmd
.\run.bat
```

### Option B: Using Maven Wrapper
```cmd
mvnw.cmd compile
.\mvnw.cmd spring-boot:run
```

### Option C: Running the Unit Tests
```cmd
mvnw.cmd test
```

---

## 7. How to Test Multi-User Flow & Vault Isolation

1. Open your browser and navigate to **[http://localhost:8080](http://localhost:8080)**.
2. **Sign In as Administrator**:
   - Use `admin@docusafe.local` / `admin123` (or click **"Use Admin Demo Account"**).
   - You will enter the main Admin Dashboard with full access to all household documents.
3. **Add a Family Member**:
   - Click **"Family Members"** in the sidebar.
   - Click **"+ Add Member"**.
   - Enter details:
     - Full Name: `Jane Doe`
     - Email: `jane@docusafe.local`
     - Password: `password123`
     - Relationship: `Spouse`
   - Click **"Save Family Member"**. The new member card appears immediately with 0 documents.
4. **Test Document Isolation as Family Member**:
   - Click the user avatar in the top right or sidebar footer and click **"Sign Out"**.
   - Sign in using Jane's credentials: `jane@docusafe.local` / `password123`.
   - Observe Jane's Dashboard: All metrics (**Total Documents: 0**) and categories are completely isolated. She does not see Admin's documents.
   - Click **"+ Add Document"** and create a document (e.g. `Jane Passport`, Category: `Passport`).
   - The document is automatically assigned to Jane. Her dashboard now displays **Total Documents: 1**.
5. **Verify Primary Admin Oversight**:
   - Sign out and log back in as `admin@docusafe.local`.
   - Admin now sees all documents including Jane's document tagged with a `Jane Doe (Spouse)` badge.
   - Admin can use the **Family Member** filter dropdown on the Dashboard or All Documents page to filter documents specifically by member.

---

## 8. Expiry Calculation Rules (Authoritative Backend)

- **Expired**: Expiry date is strictly before today's date (`expiryDate < today`).
- **Expiring Soon**: Expiry date is today or within the next 30 days (`today <= expiryDate <= today + 30 days`).
- **Valid**: Expiry date is more than 30 days away (`expiryDate > today + 30 days`).
- **No Expiry Date**: Expiry date is not provided (for lifetime documents like Degree Certificates or PAN Cards).
#   d o c s a f e _ p d m s  
 