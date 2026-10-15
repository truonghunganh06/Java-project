# Bản đồ dự án — topicmanagement (Hệ thống quản lý đề tài khóa luận)

> Tài liệu này được tạo MỘT LẦN để làm việc với AI tốn ít token hơn.
> Quy tắc: đọc file này trước, chỉ mở file code cụ thể khi cần. Chi tiết API xem `API_SURFACE.md`.
> Cập nhật 2026-10-15 từ code nhánh `mynuong` (đã reset đè lên nhánh `tigo`).

## Tổng quan
- **Là gì:** Web app quản lý quy trình đề tài khóa luận tốt nghiệp (đăng ký, phân công GV, phản biện, nộp báo cáo, chấm điểm).
- **Stack:** Java 21, Spring Boot 3.4.3, Spring Security 6 (RBAC) + AuthInterceptor, Spring Data JPA/Hibernate, Thymeleaf, PostgreSQL (Neon cloud), Maven.
- **Vị trí:** code chính trong `src/main/java/topicmanagement/`
- **Chạy:** `./mvnw spring-boot:run` (hoặc `run.bat` trên Windows) → http://localhost:8082
- **Entry point:** `topicmanagement.TopicManagementApplication`

## Kiến trúc (phân tầng chuẩn)
`Controller → Service → Repository → Entity` + `DTO` + `config` + `exception`
- **controller/** (10 file): nhận HTTP request, trả Thymeleaf view hoặc JSON (2 REST controller: `TopicApiController`, `RegistrationApiController`).
- **service/** (15 file): logic nghiệp vụ, `@Transactional`. Gồm `CustomUserDetailsService` (Spring Security) và `ExportService` (xuất Excel/PDF).
- **repository/** (20 file): Spring Data JPA interfaces.
- **entity/** (20 file): JPA entities — xem danh sách bảng bên dưới.
- **dto/**: `RegistrationDtos.java`
- **config/** (5 file): `SecurityConfig` (Security 6), `AuthInterceptor` (RBAC), `WebMvcConfig`, `AppConfig`, `OpenApiConfig` (Swagger).
- **exception/** (4 file): `GlobalExceptionHandler` (@ControllerAdvice), `BusinessRuleException`, `ResourceNotFoundException`, `StudentNotInGroupException`.

## Các entity = các bảng chính
`AppUser`, `Role`, `Student`, `Lecturer`, `Department`, `Division`, `Major`, `Topic`, `TopicRegistration`, `RegistrationPeriod`, `StudentGroup`, `GroupMember`, `GroupInvitation`, `GroupLeaveRequest`, `GroupDissolveRequest`, `Submission`, `Council`, `CouncilMember`, `Evaluation`, `Announcement`

## Vai trò & controller tương ứng
| Vai trò | Controller | Templates |
|---|---|---|
| ADMIN | `AdminController` | `templates/admin/` (3) |
| DEPARTMENT_HEAD | `DepartmentHeadController` | `templates/department-head/` (10) |
| LECTURER | `LecturerController` | `templates/lecturer/` (9) |
| STUDENT | `StudentController` | `templates/student/` (7) |
| Chung | `AuthController`, `DashboardController` | `templates/auth/` (1) |
| API | `TopicApiController`, `RegistrationApiController`, `AiAssistantController` | JSON |

## Luồng nghiệp vụ chính
1. Trưởng khoa tạo **RegistrationPeriod** (đợt đăng ký) → GV đề xuất **Topic** → Trưởng khoa duyệt.
2. SV tạo **StudentGroup**, mời thành viên (**GroupInvitation**), đăng ký **Topic** (**TopicRegistration**).
3. GVHD duyệt nhóm → nhóm làm → nộp **Submission** → chấm điểm (**Evaluation**: GVHD, phản biện, hội đồng).
4. Trưởng khoa lập **Council** (hội đồng), phân công phản biện, công bố điểm.

## Auth & phân quyền
- Đăng nhập form + Google OAuth (`AuthController`, `GoogleAuthService`).
- Spring Security 6 (`SecurityConfig`, `CustomUserDetailsService`) + `AuthInterceptor` kiểm tra role theo URL.
- Endpoint test thủ công: `GET /auth/switch-role?username=...` — **GIỮ LẠI, cấm xóa khi refactor/tối ưu**.

## Lưu ý khi tối ưu (đã thống nhất với tigo)
- **Cấm sửa file trong `src/main/resources/templates/`** — chỉ sửa Java.
- **Cấm xóa endpoint `/auth/switch-role`**.
- Đổi `FetchType.EAGER` → `LAZY` thì giữ nguyên `AppUser.role` là EAGER; chỗ nào lỗi `LazyInitializationException` thì fix bằng `@EntityGraph`/`JOIN FETCH`, không revert về EAGER.
- `spring.jpa.open-in-view=true` đang bật; `ddl-auto=none` (không đụng schema).
