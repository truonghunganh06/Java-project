# BẢNG THIẾT KẾ CƠ SỞ DỮ LIỆU CHUẨN HOÁ (TOPICHUB - 15 BẢNG CỐT LÕI)
> **Cơ sở dữ liệu:** PostgreSQL (Neon Pooled Mode)  
> **Kiến trúc:** 100% Quản trị theo Khoa (*departments*), tinh gọn tối đa luồng nhóm sinh viên (thêm trực tiếp qua MSSV, giải phóng hoàn toàn các bảng quy trình mời duyệt 2 chiều).  
> **Đồng bộ:** Khớp 1:1 với Entity / Repository Spring Boot JPA & Quy chuẩn Rubric đồ án.

---

## 1. Biểu đồ quan hệ thực thể (ERD - Crow's Foot Notation)

```mermaid
erDiagram
    %% ==========================================
    %% PHÂN VÙNG 1: TỔ CHỨC & TÀI KHOẢN (5 BẢNG)
    %% ==========================================
    roles ||--o{ users : "has_role"
    departments ||--o{ lecturers : "belongs_to"
    departments ||--o{ students : "belongs_to"
    departments ||--o{ registration_periods : "governs"
    departments ||--o{ topics : "managed_by"
    departments ||--o{ councils : "hosts"
    departments ||--o{ announcements : "broadcasts"
    users ||--|| lecturers : "profile"
    users ||--|| students : "profile"
    users ||--o{ announcements : "author"

    %% ==========================================
    %% PHÂN VÙNG 2: ĐỢT ĐĂNG KÝ & ĐỀ TÀI (3 BẢNG)
    %% ==========================================
    registration_periods ||--o{ topics : "belongs_to"
    registration_periods ||--o{ student_groups : "period_for"
    registration_periods ||--o{ councils : "schedules"
    registration_periods ||--o{ announcements : "tagged_in"

    lecturers ||--o{ topics : "supervisor"
    lecturers ||--o{ topics : "co_supervisor"
    lecturers ||--o{ topics : "reviewer"

    %% ==========================================
    %% PHÂN VÙNG 3: NHÓM SINH VIÊN TINH GỌN (3 BẢNG)
    %% ==========================================
    students ||--o{ student_groups : "leads"
    student_groups ||--|{ group_members : "contains (3-5 SV)"
    students ||--o{ group_members : "participates_in"
    student_groups ||--o| topics : "assigned_topic"
    topics ||--o{ topic_registrations : "registered_in"
    student_groups ||--o{ topic_registrations : "registers"
    students ||--o{ topic_registrations : "submitted_by"

    %% ==========================================
    %% PHÂN VÙNG 4: TIẾN ĐỘ, HỘI ĐỒNG & CHẤM ĐIỂM (4 BẢNG)
    %% ==========================================
    student_groups ||--o{ submissions : "submits"
    topics ||--o{ submissions : "submission_for"
    students ||--o{ submissions : "uploaded_by"

    councils ||--|{ council_members : "consists_of (3-5 GV)"
    lecturers ||--o{ council_members : "serves_as"
    councils ||--o{ topics : "defends"

    topics ||--o{ evaluations : "evaluated_topic"
    student_groups ||--o{ evaluations : "evaluated_group"
    lecturers ||--o{ evaluations : "scored_by"

    %% ==========================================
    %% ĐỊNH NGHĨA CHI TIẾT THỰC THỂ (15 BẢNG)
    %% ==========================================
    roles {
        bigint id PK
        varchar code UQ "ADMIN, HOD, LECTURER, STUDENT"
        varchar name "Tên vai trò hiển thị"
    }

    users {
        bigint id PK
        varchar username UQ "Tài khoản đăng nhập"
        varchar password_hash "Mật khẩu BCrypt"
        varchar full_name "Họ và tên"
        varchar email UQ "Email liên hệ"
        varchar phone "Số điện thoại"
        bigint role_id FK "Liên kết roles.id"
        boolean is_active "Trạng thái hoạt động"
        timestamp created_at
    }

    departments {
        bigint id PK
        varchar code UQ "Mã khoa (CNTT, KHTN...)"
        varchar name "Tên đầy đủ của khoa"
        text description
        boolean is_active
    }

    lecturers {
        bigint id PK
        bigint user_id FK,UQ "Tài khoản đăng nhập"
        bigint department_id FK "Khoa quản lý trực tiếp"
        varchar lecturer_code UQ "Mã số giảng viên"
        varchar academic_title "Học hàm (PGS, GS...)"
        varchar degree "Học vị (ThS, TS...)"
        integer max_topics_quota "Số đề tài hướng dẫn tối đa"
    }

    students {
        bigint id PK
        bigint user_id FK,UQ "Tài khoản đăng nhập"
        bigint department_id FK "Khoa đào tạo"
        varchar student_code UQ "Mã số sinh viên (MSSV)"
        varchar class_name "Lớp sinh hoạt"
        varchar academic_year "Niên khóa (K20, K21...)"
        numeric gpa "Điểm trung bình tích lũy"
    }

    registration_periods {
        bigint id PK
        varchar name "Tên đợt (Khóa luận K2026)"
        bigint department_id FK "Khoa tổ chức"
        varchar period_type "KLTN / TTTN"
        varchar academic_year "Năm học"
        integer semester "Học kỳ"
        timestamptz lecturer_proposal_start "GĐ1: Bắt đầu GV đề xuất"
        timestamptz lecturer_proposal_end "GĐ1: Hạn cuối GV đề xuất & Trưởng khoa duyệt"
        timestamptz student_registration_start "GĐ2: Bắt đầu SV đăng ký"
        timestamptz student_registration_end "GĐ2: Hạn chót SV đăng ký"
        timestamptz reviewer_deadline "Hạn nộp điểm phản biện"
        timestamptz council_defense_date "Ngày bảo vệ hội đồng"
        varchar status "UPCOMING, ACTIVE, CLOSED"
        integer min_students "Sĩ số tối thiểu (3)"
        integer max_students "Sĩ số tối đa (5)"
    }

    announcements {
        bigint id PK
        varchar event_key UQ "Mã sự kiện duy nhất"
        bigint department_id FK "Khoa thông báo"
        bigint period_id FK "Đợt liên quan"
        bigint author_id FK "Người đăng thông báo"
        varchar title "Tiêu đề bài viết"
        text content "Nội dung thông báo"
        varchar category "Phân loại thông báo"
        boolean is_pinned "Ghim lên đầu trang"
        timestamptz created_at
    }

    topics {
        bigint id PK
        varchar code UQ "Mã đề tài"
        varchar title "Tên đề tài nghiên cứu"
        text description "Mô tả mục tiêu"
        text requirements "Yêu cầu cần đạt"
        bigint department_id FK "Khoa quản lý"
        bigint period_id FK "Đợt đăng ký"
        bigint supervisor_id FK "Giảng viên hướng dẫn chính"
        bigint co_supervisor_id FK "Giảng viên đồng hướng dẫn"
        bigint reviewer_id FK "Giảng viên phản biện (GVPB)"
        bigint council_id FK "Hội đồng đánh giá"
        integer min_students "Sĩ số min (1-3 SV)"
        integer max_students "Sĩ số max"
        varchar status "PENDING, APPROVED, REJECTED"
        float advisor_score "Điểm GVHD"
        float reviewer_score "Điểm GVPB"
        float council_score "Điểm Hội đồng"
        float final_score "Điểm tổng kết cuối cùng"
        boolean is_finalized "Đã chốt kết quả và công bố"
    }

    student_groups {
        bigint id PK
        varchar group_code UQ "Mã nhóm tham gia"
        varchar group_name "Tên nhóm đồ án"
        bigint period_id FK "Đợt đăng ký"
        bigint leader_id FK "Sinh viên trưởng nhóm"
        bigint topic_id FK "Đề tài được giao thực hiện"
        varchar status "DRAFT, REGISTERED, APPROVED, DEFENDED"
    }

    group_members {
        bigint id PK
        bigint group_id FK "Nhóm sinh viên (CASCADE)"
        bigint student_id FK "Sinh viên thành viên"
        varchar role_in_group "LEADER hoặc MEMBER"
        timestamptz joined_at
    }

    topic_registrations {
        bigint id PK
        bigint topic_id FK "Đề tài xin đăng ký"
        bigint group_id FK "Nhóm nộp đơn"
        bigint registered_by_student_id FK "Nhóm trưởng đại diện đăng ký"
        varchar status "PENDING, APPROVED, REJECTED"
        timestamp registered_at
        timestamp approved_at
    }

    submissions {
        bigint id PK
        bigint topic_id FK "Đề tài nộp báo cáo"
        bigint group_id FK "Nhóm sinh viên nộp"
        bigint submitted_by_student_id FK "Nhóm trưởng đại diện nộp"
        varchar title "Tiêu đề đợt nộp"
        varchar submission_round "ROUND_1, ROUND_2, FINAL"
        text file_path "Đường dẫn file lưu trữ thực tế"
        varchar original_file_name "Tên gốc file nộp (.pdf, .zip)"
        bigint file_size "Dung lượng file"
        numeric grade "Điểm đánh giá tiến độ"
        text feedback "Nhận xét của GVHD"
        timestamp submitted_at
    }

    councils {
        bigint id PK
        varchar code UQ "Mã hội đồng"
        varchar name "Tên hội đồng bảo vệ"
        bigint department_id FK "Khoa quản lý"
        bigint period_id FK "Đợt bảo vệ"
        bigint chairman_id FK "Chủ tịch hội đồng (GV)"
        bigint secretary_id FK "Thư ký hội đồng (GV)"
        varchar location "Địa điểm phòng bảo vệ"
        timestamptz defense_date "Thời gian bảo vệ"
        varchar status "ACTIVE, COMPLETED"
    }

    council_members {
        bigint id PK
        bigint council_id FK "Hội đồng bảo vệ"
        bigint lecturer_id FK "Giảng viên thành viên (loại trừ GVHD)"
        varchar role "CHAIRMAN, SECRETARY, MEMBER"
        timestamptz joined_at
    }

    evaluations {
        bigint id PK
        bigint topic_id FK "Đề tài được chấm"
        bigint group_id FK "Nhóm sinh viên thực hiện"
        bigint lecturer_id FK "Giảng viên chấm điểm"
        varchar evaluation_type "ADVISOR, REVIEWER, COUNCIL"
        float score "Điểm số theo thang 10.0"
        text comments "Nhận xét và góp ý"
        boolean is_submitted "Đã nộp điểm chính thức"
        timestamptz evaluated_at
    }
```

---

## 2. Danh mục 15 Bảng Cơ Sở Dữ Liệu Chi Tiết

| STT | Tên bảng | Phân vùng | Mô tả nghiệp vụ cốt lõi |
|:---:|:---|:---|:---|
| **1** | `roles` | Người dùng & Phân quyền | Danh mục vai trò: `ADMIN`, `HOD` (Trưởng khoa), `LECTURER`, `STUDENT`. |
| **2** | `users` | Người dùng & Phân quyền | Tài khoản hệ thống, mật khẩu mã hóa BCrypt, trạng thái kích hoạt. |
| **3** | `departments` | Tổ chức học thuật | Quản trị tập trung cấp Khoa đào tạo (CNTT, KHTN, Kinh tế...). |
| **4** | `lecturers` | Hồ sơ học thuật | Hồ sơ giảng viên, học hàm, học vị, hạn ngạch đề tài tối đa (`max_topics_quota`). |
| **5** | `students` | Hồ sơ học thuật | Hồ sơ sinh viên, MSSV, lớp sinh hoạt, niên khóa, GPA. |
| **6** | `registration_periods` | Đợt đăng ký & Đề tài | Quản lý đợt đăng ký 2 giai đoạn (GĐ1: GV đề xuất; GĐ2: SV đăng ký), ràng buộc thời hạn và sĩ số nhóm. |
| **7** | `announcements` | Đợt đăng ký & Đề tài | Kênh thông báo học vụ, bài viết hướng dẫn, tài liệu đính kèm. |
| **8** | `topics` | Đề tài (Core) | Trung tâm quản lý đề tài: tên đề tài, mô tả, yêu cầu, GVHD, GVPB, Hội đồng, điểm số các vòng. |
| **9** | `student_groups` | Nhóm sinh viên | Nhóm sinh viên thực hiện đề tài, mã nhóm (`group_code`), nhóm trưởng (`leader_id`). |
| **10** | `group_members` | Nhóm sinh viên | Thành viên nhóm (ràng buộc 3–5 SV), vai trò `LEADER` / `MEMBER`. |
| **11** | `topic_registrations` | Nhóm sinh viên | Phiếu đăng ký đề tài của nhóm sinh viên gửi GVHD phê duyệt. |
| **12** | `submissions` | Tiến độ & Báo cáo | Báo cáo tiến độ đề tài qua từng đợt, lưu trữ file thực tế, chỉ nhóm trưởng mới có quyền nộp. |
| **13** | `councils` | Hội đồng bảo vệ | Hội đồng đánh giá bảo vệ đề tài trực thuộc Khoa, ngày giờ, phòng bảo vệ. |
| **14** | `council_members` | Hội đồng bảo vệ | Thành viên hội đồng (3–5 giảng viên), **ràng buộc tự động loại trừ GVHD** tránh xung đột lợi ích. |
| **15** | `evaluations` | Đánh giá & Điểm số | Bảng điểm chi tiết của GVHD, GVPB và từng ủy viên hội đồng (thang điểm 0.0 - 10.0). |

---

## 3. Tổng kết tinh giản kiến trúc

1. **Xóa bỏ 3 bảng quy trình thừa (`group_invitations`, `group_leave_requests`, `group_dissolve_requests`):**
   - Thay vì luồng "bắt tay 2 chiều" (mời $\to$ chờ $\to$ xác nhận $\to$ xin rút $\to$ xin giải tán rườm rà), nhóm trưởng chỉ cần nhập trực tiếp MSSV của thành viên để bổ sung vào nhóm.
   - Thành viên có thể chủ động nhập mã nhóm (`group_code`) để gia nhập nhóm.
   - Nhóm trưởng có quyền xóa thành viên tự do khi nhóm còn ở giai đoạn soạn thảo (`DRAFT`).
2. **Xóa bỏ 2 bảng danh mục cũ (`divisions`, `majors`):**
   - Loại bỏ cơ chế phân cấp Bộ môn và Chuyên ngành gây trùng lặp và phân mảnh.
   - Toàn bộ giảng viên, sinh viên, đề tài và hội đồng được quản lý tập trung trực tiếp theo **Khoa (`departments`)**.
3. **Độ tương thích CSDL:**
   - 100% Khớp đúng với CSDL Neon PostgreSQL đang chạy thực tế (15 tables).
   - 100% Khớp đúng với Entity / Service của Spring Boot MVC, biên dịch thành công không lỗi (`mvnw test-compile` BUILD SUCCESS).
