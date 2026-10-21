# CODEBASE MAP — Bản merge-tigo (SAU KHI GỘP 2 NHÁNH)

> Viết lại ngày 2026-10-21 từ code nhánh `merge-tigo` (= nền nhánh `mynuong` của Mỹ Nương + các fix giữ lại từ nhánh `tigo`).
> Bản đồ cũ của riêng nhánh mynuong (schema 18 bảng) nằm ở `CODEBASE_MAP_Mynuong.md` — chỉ còn giá trị tham khảo lịch sử.
> Nguồn đối chiếu: export `src-5.zip` sau merge (thư mục `~/workspace/java-project/cmp-merge`), đã rà soát độc lập.
> Quy tắc dùng: đọc file này trước, chỉ mở file code cụ thể khi cần chi tiết.

## 1. Tổng quan

- **Stack:** Spring Boot 3.4.3, Java 21, Thymeleaf + Bootstrap 5, Spring Data JPA/Hibernate, PostgreSQL (Neon cloud). Kiến trúc phân tầng: Controller → Service → Repository → Entity (+ DTO trong `dto/RegistrationDtos.java`).
- **Package gốc:** `topicmanagement`. Chạy: `.\mvnw.cmd clean spring-boot:run`, port **8082**, profile `dev`.
- **Cấu hình:** `application.properties` giữ bản tigo — giá trị thật cho Google OAuth + Gmail SMTP (KHÔNG ghi secret vào tài liệu này), SMTP timeout 5s, `spring.jpa.hibernate.ddl-auto=none` (Hibernate KHÔNG tự tạo/sửa/xóa bảng — schema quản lý ngoài).
- **Class chính:** `TopicManagementApplication` bật đủ 3 tính năng: `@EnableCaching`, `@EnableScheduling` (nhắc hạn tự động), `@EnableAsync` (gửi mail nền).
- **Vai trò (Role.code):** ADMIN, DEPARTMENT_HEAD (trưởng khoa), LECTURER (giảng viên — gồm GVHD/GVPB/thành viên hội đồng), STUDENT.

## 2. Database — 15 entity

| Entity | Bảng | Trường / quan hệ chính |
|---|---|---|
| Role | roles | id, code, name |
| AppUser | users | role, username, passwordHash (BCrypt), email, fullName, phone, trạng thái active |
| Department | departments | code, name, description — đơn vị quản lý cao nhất (KHÔNG còn Division/Major) |
| Lecturer | lecturers | user, department, lecturerCode, academicTitle, degree |
| Student | students | user, department, studentCode, className, academicYear, gpa |
| RegistrationPeriod | registration_periods | name, periodType (MON_HOC/TLCN/KLTN/NCKH...), department, 6 mốc: lecturerProposalStart/End, studentRegistrationStart/End, reviewerDeadline, councilDefenseDate; minStudents/maxStudents; status + isActive. Đợt coi là đang mở khi `status != CLOSED` |
| Topic | topics | period, department, supervisor (GVHD), coSupervisor, reviewer (GVPB), council; code, title, description, requirements, status, rejectionReason, maxStudents; **điểm nằm tại đây:** advisorScore, reviewerScore, councilScore, finalScore, isFinalized. Có `@Version` (optimistic lock) |
| StudentGroup | student_groups | period, groupCode, groupName, leader (Student), topic, status (DRAFT/REGISTERED/APPROVED/REJECTED/DEFENDING/COMPLETED + DISSOLVED do luồng giải tán đặt), `@Version` |
| GroupMember | group_members | group, student, roleInGroup (LEADER/MEMBER), joinedAt |
| TopicRegistration | topic_registrations | topic, group, registeredBy, status, rejectionReason, approvedAt |
| Submission | submissions | topic, group, submittedByStudent, title, filePath, originalFileName, contentType, fileSize, submissionRound, grade, feedback |
| Council | councils | period, department, code, name, defenseDate, location. Có helper `hasMember(AppUser)` / `hasMember(Lecturer)` (giữ từ nhánh tigo) |
| CouncilMember | council_members | council, lecturer |
| Evaluation | evaluations | topic, group, lecturer, evaluationType (SUPERVISOR/REVIEWER/COUNCIL), score, comments, isSubmitted, evaluatedAt. **UNIQUE(topic_id, group_id, lecturer_id, evaluation_type)**, group_id NOT NULL |
| Announcement | announcements | eventKey, department, period, author, title, content, attachmentPath, ghim (pin) |

**Đã xóa khỏi code ở bản này:** `GroupInvitation`, `GroupDissolveRequest`, `GroupLeaveRequest` (+ repository), `Division`, `Major` (+ repository), `DataSeederService`, `DbHealthTracker`. Ba bảng `group_invitations`, `group_dissolve_requests`, `group_leave_requests` vẫn còn trong DB Neon (ddl-auto=none nên không ai drop) — dữ liệu cũ nằm đó, không code nào đọc nữa.

**Repository (15):** tương ứng từng entity + `UserRepository`. Query nằm ở tầng repository (derived query + JPQL `@Query`, không có file .sql, không native query). Điểm đáng chú ý: `TopicRepository.searchTopics` có `countQuery` riêng cho phân trang; các khóa chống tranh chấp kiểu pessimistic (findForMembership/findForQuota/findForPublication) dùng khi thêm thành viên/đăng ký/công bố.

## 3. Bảo mật & đăng nhập (config/SecurityConfig.java + AuthController)

- Phân quyền theo thư mục: `/admin/**` → ADMIN; `/department-head/**` → DEPARTMENT_HEAD, ADMIN; `/lecturer/**` → LECTURER, DEPARTMENT_HEAD, ADMIN (nên trưởng khoa vào thẳng `/lecturer/councils` chấm hội đồng được); `/student/**` → STUDENT, ADMIN; `/api/**` → đã đăng nhập; còn lại → đã đăng nhập.
- `AdminController` thêm khóa mức class: `@PreAuthorize("hasRole('ADMIN')")`.
- **Mật khẩu:** chỉ chấp nhận BCrypt — password encoder từ chối mọi giá trị không phải BCrypt hash (đã bỏ fallback so sánh plain-text).
- **Đăng nhập local:** form `/login` (GET/POST), phiên làm việc giữ `currentUser` trong HttpSession + SecurityContext.
- **Đăng nhập Google:** `/auth/google` → `/auth/google/callback` (`GoogleAuthService`): chỉ cho vào khi **email Google khớp email của tài khoản admin đã tạo sẵn**; email lạ bị từ chối kèm thông báo liên hệ quản trị viên. KHÔNG tự tạo tài khoản, KHÔNG giới hạn domain trường (đã chốt giữ nguyên 2026-10-21).
- **`/switch-account?username=...`** (permitAll): endpoint đổi tài khoản nhanh để test tay — gõ URL là vào thẳng tài khoản bất kỳ, không cần mật khẩu. Chỉ phục vụ test nội bộ, cùng họ với `/auth/switch-role` ngày xưa.
- CSRF bật cho route giao diện (form POST phải có token `_csrf`); trang lỗi + tài nguyên tĩnh được permitAll.

## 4. Controllers & routes

| Controller | Base | Việc chính |
|---|---|---|
| AuthController | — | GET/POST `/login`, `/auth/google` + `/auth/google/callback`, `/logout`, `/switch-account` |
| DashboardController | — | `/`, `/dashboard`, `/switch-semester`, `/schedule`, `/councils`, `/topics`, `/results`, `/announcements` (trang chung theo vai trò) |
| AdminController | `/admin` | `/dashboard`, `/schedule`, `/users` (+ create, `{id}/edit`, student-profile, lecturer-profile, toggle-status, reset-password), `/departments` (+ create/edit/toggle-status) |
| DepartmentHeadController | `/department-head` | `/dashboard`, `/schedule`, `/periods` (+ create, `{id}/status`, `{id}/json`, update, delete), `/topics` (+ approve, reject, quick-edit-approve, assign-reviewer, assign-council), `/councils` (+ create, assign-topics-batch, assign-topic, unassign-topic), `/results` (+ `{id}/publish`, publish-all, export-csv/excel/pdf), `/announcements` (+ create/delete/toggle-pin), `/personnel`, `/lecturers`, `/students`, `/students/groups/{id}/unassign-topic`, `/groups/merge` |
| LecturerController | `/lecturer` | `/dashboard`, `/schedule`, `/topics` (+ create/edit/delete), `/registrations` (+ approve/reject duyệt nhóm đăng ký), `/topics/{id}/grade-advisor`, `/reviews` + `/reviews/{topicId}/grade` (chấm GVPB), `/councils` + `/councils/{topicId}/grade` (chấm hội đồng), `/submissions` (+ grade, download), `/leave-requests/{id}/approve|reject`, `/dissolve-requests/{id}/approve|reject` (2 cặp này là vỏ cũ — xem mục 8) |
| StudentController | `/student` | `/dashboard`, `/schedule`, `/group` (+ create, invite, join, remove-member, invitations accept/reject/cancel [vỏ cũ], draft-leave, draft-dissolve, transfer-leadership, leave-request, dissolve-request), `/topics` + `/topics/{id}/register`, `/my-topic`, `/submissions` (+ upload, `{id}/download`), `/results` |
| RegistrationApiController | `/api/v2/registrations` | API JSON cho phân hệ nhóm: invite, invitations (my/list/accept/reject/cancel), members (xóa), transfer-leadership, dissolve, leave-requests, dissolve-requests, groups/under-enrolled, groups/merge. Nhiều endpoint là vỏ tương thích — ruột theo mô hình mới ở mục 6.3 |
| TopicApiController | `/api/topics` | GET `/{id}` — chi tiết đề tài dạng map, **đã vá lộ điểm**: chỉ trả finalScore/rejectionReason khi `canUserViewTopicScore` cho phép |
| AiAssistantController | `/api/ai` | POST `/chat` — trợ lý hỏi đáp (xem 6.8) |
| GlobalModelAttributes | — | @ControllerAdvice nạp dữ liệu chung cho mọi trang (user hiện tại, học kỳ, thông báo...) |

## 5. Services (14)

| Service | Trách nhiệm |
|---|---|
| AuthService / CustomUserDetailsService | Đăng nhập local, nạp user cho Spring Security |
| GoogleAuthService | Xử lý callback Google, khớp email → tài khoản có sẵn |
| AdminService | CRUD tài khoản/khoa phía admin; ràng buộc **1 khoa chỉ 1 trưởng khoa**; form tạo user có dropdown khoa |
| DepartmentHeadService | Thống kê dashboard trưởng khoa (có key `totalGroups` giữ từ nhánh tigo), quản lý đợt/đề tài/hội đồng theo khoa |
| LecturerService | Đề tài của giảng viên, nhóm thuộc đề tài mình hướng dẫn, chấm điểm quá trình |
| StudentService | Hồ sơ SV, nhóm của tôi, **inviteMemberByCode / joinGroupByCode**, đăng ký đề tài, nộp báo cáo, tra điểm (`canUserViewTopicScore`, `getGroupEvaluations` có guard), danh sách đợt đã tham gia |
| GroupRegistrationService | Vỏ nghiệp vụ nhóm: thành viên, chuyển trưởng nhóm, rời/giải tán, gộp nhóm (`mergeGroups` giữ nhóm nguồn ở trạng thái DISSOLVED để bảo toàn lịch sử điểm) |
| CouncilService | Hội đồng: tạo/phân công (có `actor` + kiểm tra phạm vi khoa `validateCouncilScope`), chấm điểm (`submitEvaluation`), tính điểm tổng, công bố kết quả, dọn phiếu mồ côi (`cleanupOrphanEvaluationsAndScores`) |
| EmailService | Gửi mail **bất đồng bộ @Async** — xem mục 7 |
| AnnouncementService | Thông báo theo khoa/đợt, ghim, feed top 5 |
| ExportService | Xuất kết quả CSV/Excel/PDF cho trưởng khoa |
| FileStorageService | Lưu/đọc file nộp bài (thư mục uploads) |
| AiAssistantService | Trợ lý hỏi đáp theo **luật từ khóa + dữ liệu hệ thống** (không gọi AI ngoài): gợi ý phân công GVPB theo tải, giải thích công thức điểm, tra cứu quy chế |

## 6. Luồng nghiệp vụ chính

### 6.1 Đợt đăng ký (RegistrationPeriod)
Trưởng khoa tạo đợt với 6 mốc thời gian; backend chỉ kiểm tra **thứ tự các mốc trong cùng đợt** (đề xuất ≤ đăng ký ≤ hạn GVPB ≤ ngày bảo vệ), không kiểm tra trùng/ chồng lấn giữa các đợt, cho phép ngày bằng nhau. Trạng thái hiển thị suy từ lịch + status; đợt mở khi chưa CLOSED. `@EnableScheduling` chạy nhắc hạn tự động gần các mốc.

### 6.2 Đề tài
Giảng viên đề xuất đề tài trong đợt (`/lecturer/topics/create`) → trưởng khoa duyệt/từ chối (có mail báo kết quả — mục 7). Đề tài thuộc 1 đợt + 1 khoa; nhóm chỉ đăng ký được đề tài **cùng đợt** với nhóm.

### 6.3 Nhóm & thành viên — MÔ HÌNH MỚI (15 bảng)
- Tạo nhóm: SV tạo nhóm trong đợt (mình là LEADER), hoặc được cộng thẳng/join bằng mã nhóm (`groupCode`).
- **Mời = cộng thẳng:** trưởng nhóm nhập mã SV → `StudentService.inviteMemberByCode` thêm ngay thành GroupMember, **không còn lời mời chờ chấp nhận**. Vẫn giữ đủ kiểm tra: SV phải cùng khoa với khoa của đợt, chưa thuộc nhóm khác trong đợt, trần thành viên theo `getMaxAllowedMembers` = `topic.maxStudents` (nếu nhóm đã có đề tài) hoặc `period.maxStudents`, kẹp **[3,5]**, mặc định 5; có pessimistic lock chống tranh chấp.
- Join bằng mã nhóm (`joinGroupByCode`): cùng bộ kiểm tra khoa/trần.
- **Rời nhóm / giải tán = tự phục vụ, không duyệt:** thành viên rời là ra ngay (`requestLeaveApprovedGroup` → `removeMember`); trưởng nhóm giải tán là **giải tán tức thì** (`requestDissolveApprovedGroup`): đề tài reset về APPROVED (xóa 4 cột điểm, isFinalized=false) để nhóm khác đăng ký, xóa topic_registrations + toàn bộ members, nhóm → DISSOLVED. Phiếu `evaluations` không bị xóa (thành dữ liệu mồ côi, không hiển thị được nữa).
- Nhóm DRAFT (chưa gắn đề tài đã duyệt): có đường riêng `dissolveGroupDraft` / `leaveGroupDraft`.
- Gộp nhóm thiếu người: trưởng khoa xem nhóm under-enrolled (dưới minStudents) và `mergeGroups` — nhóm nguồn giữ lại ở trạng thái DISSOLVED, không xóa hẳn.
- Trần hiển thị trên trang nhóm lấy theo trần thật của nhóm (badge động), ô mời ẩn khi đạt trần.

### 6.4 Đăng ký đề tài
Nhóm trưởng đăng ký đề tài đã duyệt trong cùng đợt. **SV chưa có nhóm → hệ thống tự tạo nhóm** ("Nhóm của <tên SV>") rồi đăng ký tiếp. Đã **bỏ luật tối thiểu 3 SV** khi đăng ký — chỉ còn trần tối đa. GVHD duyệt đăng ký ở `/lecturer/registrations`.

### 6.5 Nộp bài (Submission)
Chỉ nhóm trưởng nộp báo cáo tiến độ/khóa luận cho cả nhóm (nhiều vòng), file qua `FileStorageService`; giảng viên tải xuống + chấm điểm vòng ở `/lecturer/submissions`.

### 6.6 Chấm điểm & công bố
- Điểm thành phần ghi vào `evaluations` qua `CouncilService.submitEvaluation` với cổng cứng: đề tài phải có nhóm; REVIEWER phải đúng người được phân công và không phải GVHD; COUNCIL phải là thành viên hội đồng của đề tài và không phải GVHD. **Không kiểm tra ngày chấm.**
- Công thức theo loại đợt: `MON_HOC/NCKH` = điểm GVHD; **TLCN = (GVHD + GVPB) / 2**; **KLTN = (GVHD + GVPB + điểm TB hội đồng) / 3**. Thiếu thành phần → "Chưa đủ điểm", không công bố được.
- Trưởng khoa công bố ở `/department-head/results` (từng đề tài hoặc publish-all): đặt `isFinalized = true` rồi kích **mail kết quả @Async** cho từng SV trong nhóm.
- **SV chỉ thấy điểm khi `is_finalized = true`**, và chỉ với đề tài/đợt mình tham gia (`canUserViewTopicScore`, `getGroupEvaluations(group, viewer, periodId)`); trang tra cứu: `/student/results` chọn theo đợt đã tham gia. Kết quả các đợt cũ nằm nguyên trong DB (topics + evaluations), đóng đợt không xóa.

### 6.7 Thông báo (Announcement)
Theo khoa/đợt, có ghim; feed mỗi trang lấy top 5 mới nhất; thông báo nhắc hạn tự động tách khỏi feed thường của trưởng khoa.

### 6.8 Trợ lý AI
Widget chat (fragment `fragments/ai_chat.html`) gọi `/api/ai/chat` → `AiAssistantService.processQuery`: khớp từ khóa trong câu hỏi + đọc dữ liệu hệ thống (tải phản biện của giảng viên, công thức điểm...) để trả lời kèm gợi ý. Không phải LLM ngoài.

## 7. Email (service/EmailService.java — giữ nguyên bản tigo)

Tất cả gửi **bất đồng bộ @Async**, truyền ID rồi tự nạp dữ liệu trong transaction nền (không truyền entity từ request thread — tránh lỗi lazy "This statement has been closed"). SMTP Gmail, timeout 5s (cấu hình ở properties). SMTP lỗi → có cơ chế giả lập/log dự phòng.

| Method | Khi nào |
|---|---|
| `sendWelcomeEmail(user, rawPassword)` | Admin tạo tài khoản |
| `sendTopicApprovalEmail(topic, approved, reason)` | Trưởng khoa duyệt/từ chối đề tài |
| `sendGroupInvitationEmail(invitee, group, leader)` | Còn tồn tại nhưng **không còn ai gọi** (mô hình cộng thẳng đã bỏ lời mời) |
| `sendTopicResultEmailsToGroup(topicId, groupId)` | Công bố kết quả — gửi cho mọi SV trong nhóm; CouncilService gọi method này khi publish |

## 8. Nợ kỹ thuật / điểm cần biết khi đụng vào

- **Vỏ endpoint cũ còn sót:** `RegistrationApiController` + `GroupRegistrationService` + `LecturerController` vẫn còn các method/route kiểu approve/reject leave-request, dissolve-request, invitations accept/reject — ruột đã chuyển sang mô hình tự phục vụ (approve trả message thành công rỗng, `getMyPendingInvitations` trả list rỗng). Đừng tưởng hệ thống còn luồng duyệt.
- **Template `student/group.html`** còn sót markup mục "lời mời đang chờ" của thiết kế cũ — controller không nạp dữ liệu nữa nên không bao giờ hiển thị.
- **Câu chữ cũ chưa sửa:** một số message lỗi vẫn nhắc "gửi yêu cầu xin rút lên GVHD duyệt" dù luồng duyệt đã bỏ.
- **Modal admin/users** (chỉnh sửa tài khoản, đặt lại mật khẩu): có prompt agy riêng để căn giữa màn hình (modal đang bị kẹt trong container) — trạng thái tính tới 2026-10-21 tối: đã giao prompt, chờ kết quả chạy.
- **Test:** còn 1 file test (`TopicManagementApplicationTests`); 2 test cũ của nhánh tigo đã bị xóa khi merge. Nhóm kiểm thử thủ công là chính.
- **Trang Lịch giảng viên** dùng fragment `fragments/full_calendar.html` — bản merge đã có fix `rt.title`/`rt.code` (bản mynuong gốc còn lỗi `titleVi`/`topicCode` làm trắng trang).
- **Lịch/ghi chú cá nhân** của người dùng lưu ở localStorage trình duyệt, không theo tài khoản.
- `/auth/switch-role` của v1 **không tồn tại** ở bản này; đường tắt test hiện tại là `/switch-account`.
