# Bản đồ dự án — topicmanagement (Hệ thống quản lý đề tài khóa luận)

> Tài liệu này được tạo MỘT LẦN để làm việc với AI tốn ít token hơn.
> Quy tắc: đọc file này trước, chỉ mở file code cụ thể khi cần. Chi tiết API xem `API_SURFACE.md`.

## Tổng quan
- **Là gì:** Web app quản lý quy trình đề tài khóa luận tốt nghiệp (đăng ký, phân công GV, phản biện, nộp báo cáo, chấm điểm).
- **Stack:** Java 21, Spring Boot 3.4.3, Spring Security 6 (RBAC), Spring Data JPA/Hibernate, Thymeleaf, PostgreSQL (Neon cloud), Maven.
- **Vị trí:** `~/workspace/java-project/Java-project/` — code chính trong `src/main/java/topicmanagement/`
- **Chạy:** `./mvnw spring-boot:run` (hoặc `run.bat` trên Windows) → http://localhost:8080
- **Entry point:** `topicmanagement.TopicManagementApplication`

## Kiến trúc (phân tầng chuẩn)
`Controller → Service → Repository → Entity` + `DTO` + `config` + `exception`
- **controller/** (10 file): nhận HTTP request, trả Thymeleaf view hoặc JSON (2 REST controller: `TopicApiController`, `RegistrationApiController`).
- **service/** (16 file): logic nghiệp vụ, `@Transactional`.
- **repository/** (21 file): Spring Data JPA interfaces.
- **entity/** (20 file): JPA entities — xem danh sách bảng bên dưới.
- **dto/**: `RegistrationDtos.java`
- **config/** (5 file): `SecurityConfig` (Security 6), `AuthInterceptor` (RBAC), `WebMvcConfig`, `AppConfig`, `OpenApiConfig` (Swagger).
- **exception/** (4 file): `GlobalExceptionHandler` (@ControllerAdvice).

## Các entity = các bảng chính
`AppUser`, `Role`, `Student`, `Lecturer`, `Department`, `Division`, `Major`, `Topic`, `TopicRegistration`, `RegistrationPeriod`, `StudentGroup`, `GroupMember`, `GroupInvitation`, `GroupLeaveRequest`, `GroupDissolveRequest`, `Submission`, `Council`, `CouncilMember`, `Evaluation`, `Announcement`

## Vai trò & controller tương ứng
| Vai trò | Controller | Templates |
|---|---|---|
| ADMIN | `AdminController` | `templates/admin/` (3) |
| DEPARTMENT_HEAD | `DepartmentHeadController` | `templates/department-head/` (10) |
| LECTURER | `LecturerController` | `templates/lecturer/` (9) |
| STUDENT | `StudentController` | `templates/student/` (7) |
| Chung | `AuthController` (login, Google OAuth), `DashboardController` | `templates/auth/` (1), `templates/fragments/` (10), `templates/error/` (2) |

## Service đáng chú ý
- `AuthService`, `CustomUserDetailsService` — xác thực
- `GoogleAuthService` — đăng nhập Google OAuth2
- `GroupRegistrationService`, `StudentService`, `LecturerService`, `DepartmentHeadService`, `AdminService`, `CouncilService`, `AnnouncementService`
- `ExportService` — xuất Excel (Apache POI) / PDF (OpenPDF)
- `FileStorageService` — lưu file nộp bài (`uploads/submissions/`)
- `AiAssistantService` — có controller + test riêng (`AiAssistantController`)
- `DataSeederService` — seed dữ liệu (lưu ý AGENTS.md: KHÔNG hardcode mock data vào source)

## Cấu hình
- `application.properties` (+ `application-dev/prod.properties`): DB Neon PostgreSQL, HikariCP, JPA ddl-auto, upload size, Google OAuth2 keys. ⚠️ Chứa secret — không copy nguyên file vào chat.

## Test
11 file test trong `src/test/java/topicmanagement/` — bao phủ: workflow nhóm, đăng ký đề tài, hội đồng/bảo vệ, phân quyền, security.

## Tài liệu có sẵn trong repo
`README.md` (tổng quan + tài khoản test), `AGENTS.md` (quy tắc: cấm mock data), `SKYLEARN_DESIGN_SYSTEM.md`, `TEST_ACCOUNTS_GUIDE.md`, `Tailieu/`, `Rubric.pdf`

## Cách dùng bản đồ này (cho AI)
1. Đọc file này trước mọi task (~1k token thay vì cả dự án).
2. Tra `API_SURFACE.md` để biết endpoint/method/field cụ thể.
3. Chỉ `read` file .java liên quan trực tiếp đến task.
4. Dùng `grep`/`find` để định vị trước khi đọc.
5. Không bao giờ đọc `target/`, `node_modules`, file `.class`, `.pdf`.
