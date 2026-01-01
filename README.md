# 🔌 ElHelper Backend API

**ElHelper** is a comprehensive backend platform designed for electronics engineers. It combines a specialized **Knowledge Base (Wiki)**, an **Engineering Calculator** with formula management, and a **Project Management System** capable of versioning and BOM (Bill of Materials) generation.

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Security](https://img.shields.io/badge/Security-OAuth2_%26_JWT-red)
![Deploy](https://img.shields.io/badge/Deploy-Render-black)

---

## 🛠 Tech Stack

- **Core:** Java 21, Spring Boot 4.x (Web, Data JPA, Security, Validation, Mail)
- **Database:** PostgreSQL
- **Security:** JWT (Access/Refresh tokens), Google OAuth2
- **Email Service:** SMTP (Brevo / Gmail) with async support
- **Tools:** Maven, Lombok, Docker

---

## 📂 Key Features & Modules

### 1. Authentication & Users (`Auth`, `User`)
- **Hybrid Auth:** Standard Email/Password registration & Google OAuth2.
- **Passwordless Login:** Magic Links sent via email.
- **Security Flows:** Email confirmation, Forgot/Reset password, Token refresh.
- **Role Management:** Users can request `ADMIN` rights via a dedicated approval workflow (`AdminRequest`).
- **Profile:** Avatar upload (Multipart), Settings management, Profile editing.

### 2. Engineering Suite (`Calculation`, `Formula`, `Project`)
- **Formula Library:** Database of physical/math formulas with attached schemes (images).
- **Calculator:** logic to execute calculations based on formulas.
- **Project System:**
  - Group calculations into projects.
  - **Versioning:** Create immutable snapshots (versions) of projects.
  - **BOM Generation:** Auto-generate and download **CSV** Bill of Materials for the project.

### 3. Knowledge Base (`Theory`, `Category`, `Comment`)
- **Theory (Wiki):** Educational articles with Markdown/HTML support.
- **Search:** Advanced search by query, category, language (`uk`/`en`), and sorting.
- **Community:** Commenting system for articles.

---

## 🔌 API Endpoints

### 🔐 Auth Controller (`/api/auth`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/register` | Register new user |
| `POST` | `/login` | Login (returns JWT) |
| `POST` | `/link-login` | Send Magic Link to email |
| `GET` | `/link-login` | Login via Magic Link token |
| `PATCH` | `/confirm` | Confirm email address |
| `POST` | `/confirm` | Resend confirmation token |
| `POST` | `/forgot-password` | Request password reset |
| `POST` | `/reset-password` | Set new password |

### 👤 User Controller (`/api/users`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/me` | Get current user details |
| `GET` | `/{id}` | Get public profile by ID |
| `PATCH` | `/profile` | Update profile info |
| `POST` | `/avatar` | Upload avatar image |
| `POST` | `/password` | Change password |
| `POST` | `/my-admin-request` | Submit request for Admin role |
| `GET` | `/admin-request` | Get all requests (Super Admin only) |
| `POST` | `/admin-request/{id}` | Approve/Reject admin request |

### 🚀 Project & BOM (`/api/projects`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/` | Create new project |
| `GET` | `/` | Get active projects |
| `GET` | `/archive` | Get project history |
| `POST` | `/{id}/version` | **Create new version (Snapshot)** |
| `GET` | `/{id}/bom` | **Download BOM (CSV file)** |

### 📐 Calculations (`/api/calculations`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/test` | Dry-run calculation (no save) |
| `POST` | `/` | Execute and save calculation |
| `GET` | `/{projectId}` | Get calculations by project |

### 📚 Theory & Formulas (`/api/theory`, `/api/formulas`)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/theory/search` | Search articles (`?query=...`) |
| `POST` | `/api/theory/create` | Create new article |
| `GET` | `/api/formulas` | Get all formulas |
| `POST` | `/api/formulas/scheme` | Upload formula scheme (Admin) |

---

## ⚙️ Configuration (Environment Variables)

Create a `.env` file or configure these variables in your deployment environment (e.g., Render):

```properties
# --- Database ---
DB_URL=jdbc:postgresql://host:5432/elhelperdb
DB_USERNAME=admin
DB_PASSWORD=secret

# --- Mail (SMTP) ---
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=your_smtp_login
MAIL_PASSWORD=your_smtp_key

# --- Google OAuth2 ---
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret

# --- JWT Security ---
JWT_SECRET=your_very_long_secret_key
JWT_EXPIRATION=86400000
