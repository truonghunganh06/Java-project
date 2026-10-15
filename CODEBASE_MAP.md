# Bản đồ dự án — topic_management (Hệ thống quản lý đề tài khóa luận) — BẢN V2

> Tài liệu này được tạo MỘT LẦN để làm việc với AI tốn ít token hơn.
> Quy tắc: đọc file này trước, chỉ mở file code cụ thể khi cần. Chi tiết API xem `API_SURFACE.md`.
> ⚠️ Bản v2 (2026-10-15): đổi tên package `topicmanagement` → `topic_management`, **bỏ hẳn Spring Security**, port đổi thành **8082**.

## Tổng quan
- **Là gì:** Web app quản lý quy trình đề tài khóa luận tốt nghiệp (đăng ký, phân công GV, phản biện, nộp báo cáo, chấm điểm).
- **Stack:** Java 21, Spring Boot 3.4.3, Spring Data JPA/Hibernate, Thymeleaf, PostgreSQL (Neon cloud), Maven. Không còn Spring Security — auth bằng session + `AuthInterceptor` tự viết.
- **Vị trí:** `~/workspace/java-project/v2/Java-project/` — code chính trong `src/main/java/topic_management/`
- **Chạy:** `./mvnw spring-boot:run` (hoặc `run.bat` trên Windows) → http://localhost:8082
- **Entry point:** `topic_management.TopicManagementApplication`

## Kiến trúc (phân tầng chuẩn)
`Controller → Service → Repository → Entity` + `DTO` + `config` + `exception`
- **controller/** (9 file): nhận HTTP request, trả Thymeleaf view hoặc JSON (2 REST controller: `TopicApiController`, `RegistrationApiController`). Đã bỏ `GlobalModelAttributes`.
- **service/** (15 file): logic nghiệp vụ, `@Transactional`.
- **repository/** (20 file): Spring Data JPA interfaces.
- **entity/** (20 file): JPA entities — xem danh sách bảng bên dưới.
- **dto/**: `RegistrationDtos.java`
- **config/** (3 file): `AuthInterceptor` (session auth/RBAC tự viết), `WebMvcConfig`, `OpenApiConfig` (Swagger). Đã bỏ `SecurityConfig`, `AppConfig`.
- **exception/** (1 file): `StudentNotInGroupException`. Đã bỏ `GlobalExceptionHandler`, `BusinessRuleException`, `ResourceNotFoundException`.

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
- `AuthService` — xác thực (session-based, không còn `CustomUserDetailsService`)
- `GoogleAuthService` — đăng nhập Google OAuth2
- `GroupRegistrationService`, `StudentService`, `LecturerService`, `DepartmentHeadService`, `AdminService`, `CouncilService`, `AnnouncementService`
- `FileStorageService` — lưu file nộp bài (`uploads/submissions/`)
- `AiAssistantService` — có controller + test riêng (`AiAssistantController`)
- `DataSeederService` — seed dữ liệu (lưu ý AGENTS.md: KHÔNG hardcode mock data vào source)
- `DataCleanupService`, `DatabaseMigrationService`, `DbHealthTracker` — mới trong v2
- Đã bỏ: `ExportService` (xuất Excel/PDF), `CustomUserDetailsService`

## Cấu hình
- `application.properties` (+ `application-dev/prod.properties`): DB Neon PostgreSQL, HikariCP, JPA ddl-auto, upload size, Google OAuth2 keys. ⚠️ Chứa secret — không copy nguyên file vào chat.

## Test
10 file test trong `src/test/java/topic_management/`

## Tài liệu có sẵn trong repo
`README.md` (tổng quan + tài khoản test), `AGENTS.md` (quy tắc: cấm mock data), `SKYLEARN_DESIGN_SYSTEM.md`, `TEST_ACCOUNTS_GUIDE.md`, `Tailieu/`, `Rubric.pdf`

## Cách dùng bản đồ này (cho AI)
1. Đọc file này trước mọi task (~1k token thay vì cả dự án).
2. Tra `API_SURFACE.md` để biết endpoint/method/field cụ thể.
3. Chỉ `read` file .java liên quan trực tiếp đến task.
4. Dùng `grep`/`find` để định vị trước khi đọc.
5. Không bao giờ đọc `target/`, `node_modules`, file `.class`, `.pdf`.

## Vấn đề tối ưu còn tồn đọng trong v2 (quét ngày 2026-10-15)
Bản v2 chưa áp dụng các fix tối ưu của bản cũ (mất theo khi đổi package). Theo thứ tự ưu tiên:
1. **28 association còn FetchType.EAGER** — nguy hiểm nhất: `StudentGroup.members`, `Council.members` (OneToMany EAGER). Giữ `AppUser.role` EAGER (AuthInterceptor đọc ngoài transaction).
2. Admin dashboard: `getAllUsers()` rồi `.limit(5)` → dùng `findTop5ByOrderByCreatedAtDesc()`.
3. `/admin/users`: chưa phân trang, filter bằng Java → thêm `searchUsers(roleCode, keyword, Pageable)`.
4. Dashboard Trưởng khoa: load full topics rồi filter Java 10+ lần → viết count query theo status/khoảng điểm.
5. `getDepartmentStats/getDivisionStats`: `findAll()` chỉ để `.size()` → dùng `count()`.
6. Thông báo dashboard: load all rồi `.limit(3-5)` → query TopN.
7. N+1: `LecturerService.getGroupsForLecturerTopics` (findByTopicId trong loop), trang lịch bảo vệ (findByGroupId trong loop) → dùng `findBy...In()`.
8. `GroupRegistrationService`: `save()` trong loop → `saveAll()`.
