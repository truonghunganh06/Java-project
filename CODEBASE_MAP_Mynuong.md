# CODEBASE MAP — Java-project (nhánh mynuong, bản 2026-10-20)

> Nguồn: src do tigo gửi ngày 2026-10-20 (code mynuong mới nhất, đã refactor về schema 18 bảng).
> Package gốc: `topicmanagement`. Main class: `TopicManagementApplication`.

## 1. Tổng quan

| Hạng mục | Giá trị |
|---|---|
| Stack | Spring Boot 3.4.3 + Thymeleaf + Spring Security + Spring Data JPA |
| DB | PostgreSQL (Neon), `ddl-auto=none` — schema quản lý bằng SQL tay (`docs/database_schema.sql`, `docs/schema.dbml`) |
| Port mặc định | 8080 (application.properties), profile `dev` |
| Schema | **18 bảng** — KHÔNG còn `divisions` (bộ môn) và `majors` (chuyên ngành). Mọi thứ gom về 1 cấp: **Department (Khoa)** |
| Role | ADMIN, DEPARTMENT_HEAD, LECTURER, STUDENT (không còn DIVISION_HEAD trong phân quyền route) |

## 2. Cấu trúc thư mục

```
src/main/java/topicmanagement/
├── config/       (5)  AppConfig, AuthInterceptor, OpenApiConfig, SecurityConfig, WebMvcConfig
├── controller/   (10) xem mục 4
├── dto/          (2)  RegistrationDtos, TopbarNotificationDto
├── entity/       (18) xem mục 3
├── exception/    (4)  BusinessRuleException, ResourceNotFoundException, StudentNotInGroupException, GlobalExceptionHandler
├── repository/   (18) 1 repository / entity
└── service/      (14) xem mục 5
src/main/resources/
├── application.properties / -dev / -prod
└── templates/    (44 file) admin/, department-head/, lecturer/, student/, auth/, fragments/, error/
```

## 3. Entity → bảng (18)

| Entity | Bảng | FK chính |
|---|---|---|
| AppUser | users | role_id → roles |
| Role | roles | — |
| Department | departments | — |
| Lecturer | lecturers | user_id → users, department_id → departments |
| Student | students | user_id → users, department_id → departments |
| RegistrationPeriod | registration_periods | department_id |
| Topic | topics | period_id, department_id, supervisor_id / co_supervisor_id / reviewer_id → lecturers, council_id → councils |
| StudentGroup | student_groups | period_id, leader_id → students, topic_id → topics |
| GroupMember | group_members | group_id, student_id |
| GroupInvitation | group_invitations | group_id, student_id, invited_by_student_id |
| TopicRegistration | topic_registrations | topic_id, group_id, registered_by_student_id |
| Submission | submissions | topic_id, group_id, submitted_by_student_id (CHỈ còn field này — không còn `student_id`) |
| Council | councils | period_id, department_id |
| CouncilMember | council_members | council_id, lecturer_id |
| Evaluation | evaluations | topic_id, group_id, lecturer_id |
| Announcement | announcements | department_id, period_id, author_id → users |
| GroupLeaveRequest | group_leave_requests | group_id, student_id, resolved_by_lecturer_id |
| GroupDissolveRequest | group_dissolve_requests | group_id, topic_id, resolved_by_lecturer_id |

## 4. Controller → route

### AuthController (không prefix)
- `GET/POST /login`, `GET /logout`
- `GET /auth/google` → `GET /auth/google/callback` (Google OAuth qua GoogleAuthService)
- ⚠️ **KHÔNG có `/auth/switch-role`** trong bản này.

### DashboardController — trang chung sau login
`GET /switch-semester`, `/schedule`, `/councils`, `/topics`, `/results`, `/announcements`

### AdminController — `/admin` (role ADMIN)
- Users: `GET /users`, `POST /users/create`, `/{id}/edit`, `/{id}/student-profile`, `/{id}/lecturer-profile`, `/{id}/toggle-status`, `/{id}/reset-password`
- Departments: `GET /departments`, `POST /departments/create`, `/{id}/edit`, `/{id}/toggle-status`
- `GET /dashboard`, `GET /schedule`

### DepartmentHeadController — `/department-head` (1368 dòng, lớn nhất)
- Periods: `GET /periods`, `POST /periods/create`, `/{id}/status`, `/{id}/update`, `/{id}/delete`, `GET /{id}/json`
- Topics: `GET /topics`, `POST /topics/{id}/approve` (dòng 582), `/{id}/reject` (dòng 603), `/{id}/quick-edit-approve`, `/{id}/assign-reviewer`, `/{id}/assign-council`
- Councils: `GET /councils`, `POST /councils/create`, `/{id}/assign-topic(s-batch)`, `/{id}/unassign-topic`
- Results: `GET /results`, `POST /results/{id}/publish`, `/results/publish-all`, export CSV/Excel/PDF
- Announcements: `GET /announcements`, `POST /announcements/create`, `/{id}/delete`, `/{id}/toggle-pin`
- Personnel: `GET /personnel`, `/lecturers`, `/students`, `POST /groups/merge`

### LecturerController — `/lecturer`
- Topics: `GET /topics`, `GET/POST /topics/create`, `GET/POST /topics/{id}/edit`, `POST /topics/{id}/delete`
- Registrations: `GET /registrations`, `POST /registrations/{id}/approve|reject`
- Chấm điểm: `POST /topics/{topicId}/grade-advisor`, `/reviews/{topicId}/grade`, `/councils/{topicId}/grade`, `/submissions/{id}/grade`
- Submissions: `GET /submissions`, `GET /submissions/{id}/download`
- Yêu cầu nhóm: `POST /leave-requests/{id}/approve|reject`, `/dissolve-requests/{id}/approve|reject`

### StudentController — `/student`
- Group: `GET /group`, `POST /group/create`, `/invite`, `/invitations/{id}/accept|reject|cancel`, `/leave-request`, `/dissolve-request`, `/transfer-leadership`, `/join`, `/remove-member`
- Topics: `GET /topics`, `POST /topics/{id}/register`, `GET /my-topic`
- Submissions: `GET /submissions`, `POST /submissions/upload`, `GET /submissions/{id}/download`
- `GET /dashboard`, `/schedule`, `/results`

### API (JSON)
- RegistrationApiController — `/api/v2/registrations`: mời/nhận/từ chối thành viên, leave/dissolve requests, merge groups
- TopicApiController — `/api/topics`
- AiAssistantController — `/api/ai` (`POST /chat`)

### GlobalModelAttributes
@ControllerAdvice — bơm dữ liệu chung (user hiện tại, thông báo topbar) cho mọi view.

## 5. Service (14)

| Service | Dòng | Trách nhiệm |
|---|---|---|
| GroupRegistrationService | 890 | Nhóm: mời thành viên, leave/dissolve, merge — `sendInvitation(AppUser, Long, String)` dòng 97 |
| CouncilService | 786 | Hội đồng, công bố điểm; gọi `EmailService.sendTopicResultEmailsToGroup` (dòng 698, 733) |
| AiAssistantService | 659 | Chat AI hỗ trợ |
| StudentService | 602 | Luồng sinh viên: nhóm, nộp bài (`submitGroupReport`) |
| AdminService | 449 | CRUD user/department; tự tạo hồ sơ con Student/Lecturer khi tạo account |
| LecturerService | 394 | Luồng giảng viên; `getSubmissionsByLecturer` dòng 277 |
| ExportService | 388 | Xuất CSV/Excel/PDF kết quả |
| DepartmentHeadService | 366 | Thống kê khoa, duyệt đề tài theo department |
| GoogleAuthService | 165 | OAuth Google — `processStudentLoginByEmail` dòng 129: **chỉ cho email đã có account, KHÔNG auto-create** |
| EmailService | 149 | Gửi mail báo điểm (`sendTopicResultEmailsToGroup`); SMTP qua env `SPRING_MAIL_USERNAME/PASSWORD`; chưa config thì chạy chế độ simulation (log) |
| FileStorageService | 130 | Lưu file upload |
| AuthService | 126 | Đăng nhập thường |
| AnnouncementService | 95 | Thông báo (findTop5 đã tối ưu sẵn) |
| CustomUserDetailsService | 41 | Spring Security |

## 6. Bảo mật (SecurityConfig)

- `/`, `/login`, `/auth/**` — permitAll
- `/admin/**` — ADMIN
- `/department-head/**` — DEPARTMENT_HEAD, ADMIN
- `/lecturer/**` — LECTURER, DEPARTMENT_HEAD, ADMIN
- `/student/**` — STUDENT, ADMIN
- `/api/**` — authenticated (CSRF bật cho route web)

## 7. Luồng chính

1. **Đăng nhập**: form thường (AuthService) hoặc Google OAuth (AuthController → GoogleAuthService). Domain mail trường cấu hình ở `app.auth.student-email-domains` (đang để trống = không giới hạn).
2. **Đợt đăng ký**: Trưởng khoa tạo RegistrationPeriod theo khoa → GV đề xuất Topic → Trưởng khoa duyệt/từ chối → SV lập nhóm + đăng ký → GVHD duyệt đăng ký → nộp Submission → chấm Evaluation (GVHD/phản biện/hội đồng) → Trưởng khoa publish kết quả → EmailService gửi mail báo điểm.
3. **Mail hiện có**: chỉ có mail báo điểm. Chưa có: mail chào mừng, mail duyệt/từ chối đề tài, mail lời mời nhóm.

## 8. Ghi chú khác biệt so với nhánh tigo cũ

- Đã bỏ `Major`, `Division` entity + mọi logic lọc theo bộ môn/chuyên ngành.
- `Submission` đã bỏ field `student` thừa (khớp DB — hết lỗi `student_id does not exist`).
- Dropdown chọn khoa/chuyên ngành ở form tạo user của nhánh tigo cũ **không còn cần** theo schema mới (Student/Lecturer chỉ gắn department).
- Repo có sẵn `docs/schema.dbml` + `docs/database_schema.sql` + `PROJECT_AUDIT.md` + `AGENTS.md` (quy ước làm việc của nhóm).
