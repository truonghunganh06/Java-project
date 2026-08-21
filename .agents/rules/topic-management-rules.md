---
trigger: always_on
---

# TOPIC MANAGEMENT SYSTEM - WORKSPACE CONTEXT & RULES

## 1. Tech Stack & Architecture
- **Language**: Java 21+
- **Framework**: Spring Boot 3.x (Spring Web MVC, Spring Data JPA)
- **Template Engine**: Thymeleaf + Bootstrap 5.3 + FontAwesome 6
- **Database**: PostgreSQL (Hosted on Neon DB)
- **Boilerplate**: Lombok (@Getter, @Setter, @RequiredArgsConstructor, etc.)
- **Build Tool**: Maven

## 2. Project Package Hierarchy
Base package: `topic_management`
- `topic_management.entity`: JPA Entities mapping to Neon DB.
- `topic_management.repository`: Spring Data JPA Repositories (Interfaces).
- `topic_management.service`: Business logic layer.
- `topic_management.controller`: Spring MVC Controllers handling HTTP routes and returning Thymeleaf views.
- `src/main/resources/templates/`:
  - `auth/login.html`: Authentication UI.
  - `admin/`: Admin & Department Head dashboards and management pages.
  - `lecturer/`: Lecturer topic proposals, registration approvals, grading.
  - `student/`: Student topic catalog, registration, report submission.
- `src/main/resources/application.properties`: Database connection & server config.

## 3. Database Entities & Schema Rules
- **Roles**: `ADMIN`, `DEPARTMENT_HEAD`, `LECTURER`, `STUDENT`.
- **Entities**:
  - `AppUser`: `id`, `username`, `passwordHash`, `fullName`, `email`, `phone`, `role_id`, `isActive`, `createdAt`.
  - `Role`: `id`, `code`, `name`.
  - `Department`: `id`, `code`, `name`, `description`, `isActive`.
  - `Major`: `id`, `department_id`, `code`, `name`, `isActive`.
  - `Lecturer`: `id`, `user_id`, `department_id`, `lecturerCode`, `academicTitle`, `degree`, `maxTopicsQuota`.
  - `Student`: `id`, `user_id`, `major_id`, `studentCode`, `className`, `academicYear`, `gpa`.
  - `Topic`: `id`, `major_id`, `supervisor_id`, `code`, `title`, `description`, `requirements`, `maxStudents`, `status` (`PENDING`, `APPROVED`, `REJECTED`), `rejectionReason`, `academicYear`, `semester`.
  - `TopicRegistration`: `id`, `topic_id`, `student_id`, `status` (`PENDING`, `APPROVED`, `REJECTED`), `registeredAt`, `approvedAt`.
  - `Submission`: `id`, `topic_id`, `student_id`, `title`, `filePath`, `fileSize`, `submissionRound`, `grade`, `feedback`, `submittedAt`.

## 4. Coding & Agent Behavioral Rules
- **Do not modify working database credentials** in `application.properties` unless explicitly requested.
- **Maintain Session-Based Auth**: Check `session.getAttribute("currentUser")` and validate roles in controllers. Redirect unauthorized requests to `/login`.
- **UI Consistency**: Use Bootstrap 5 classes, clean card layouts, data tables with actionable buttons, and Vietnamese text for all frontend labels.
- **Form Handling**: Always use POST methods with CSRF awareness where appropriate, and return clear feedback using `RedirectAttributes` (`successMessage` / `errorMessage`).
- **No Over-Engineering**: Avoid adding extra security filters (like Spring Security complex filter chains) unless requested, to keep the current session-based mechanism lightweight and stable.