# SYSTEM INVARIANTS & ARCHITECTURAL CONSTRAINTS (V2.0)
# DỰ ÁN: HỆ THỐNG QUẢN LÝ ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC (SPRING BOOT 3 & REACT/NEXT.JS)

> **MỤC ĐÍCH**: Đây là bộ quy tắc bất biến (Strict System Invariants) có hiệu lực bắt buộc đối với toàn bộ mã nguồn Backend (Spring Boot 3) và Frontend (React/Next.js). Bất kỳ sửa đổi, bổ sung hoặc sinh mã nào vi phạm các nguyên tắc dưới đây đều bị coi là lỗi kiến trúc nghiêm trọng.

---

## 1. QUY TẮC TÍNH ĐIỂM BẤT BIẾN (GRADING RULES)
* **Chỉ dùng Trung bình cộng (Arithmetic Mean)**:
  * **CẤM TUYỆT ĐỐI**: Không sử dụng công thức trọng số phần trăm cố định (như $40\% - 60\%$, $30\% - 70\%$) trong bất kỳ hoàn cảnh nào.
  * **Làm tròn số học**: Tất cả điểm thành phần và điểm tổng kết cuối cùng bắt buộc làm tròn đến **2 chữ số thập phân (`0.01`)** sử dụng `BigDecimal` với chế độ làm tròn `RoundingMode.HALF_UP`.
* **Công thức chuẩn hóa theo từng loại đợt (`PeriodType`)**:
  * `MON_HOC`: $\text{FinalScore} = \text{Score}_{\text{GVHD}}$ *(hoặc TBC nếu có 2 GVHD)*
  * `TLCN`: $\text{FinalScore} = \frac{\text{Score}_{\text{GVHD}} + \text{Score}_{\text{GVPB}}}{2}$
  * `KLTN`:
    * $\text{Score}_{\text{Council}} = \frac{1}{N} \sum_{i=1}^{N} \text{Score}_{\text{Member}_i}$ ($N$ là số thành viên Hội đồng chấm điểm, $3 \le N \le 5$)
    * $\text{FinalScore} = \frac{\text{Score}_{\text{GVHD}} + \text{Score}_{\text{GVPB}} + \text{Score}_{\text{Council}}}{3}$

---

## 2. PHÂN CẤP QUẢN LÝ HỌC THUẬT (ACADEMIC HIERARCHY & SCOPING)
* **Mô hình 4 tầng độc lập**:
  $$\text{Faculty (Khoa)} \longrightarrow \text{Division (Bộ môn)} \longrightarrow \text{Staff & Topic (Giảng viên \& Đề tài)} \longrightarrow \text{Student \& Major (Sinh viên \& Chuyên ngành)}$$
* **Ranh giới thẩm quyền Bộ môn (`Division Scoping`)**:
  * Thẩm định duyệt Đề tài (`Topic`), phân công Giảng viên Phản biện (`GVPB`), và thành lập Hội đồng Đánh giá (`Council`) **BẮT BUỘC thuộc thẩm quyền của Bộ môn (`Division`)**.
  * **CẤM**: Tuyệt đối không gộp chung đề tài hoặc phân công GVPB ở cấp Khoa (`Faculty`). Trưởng khoa chỉ quản lý cấu hình Đợt, giám sát tiến độ vĩ mô và phê duyệt/công bố kết quả cuối cùng (`PUBLISHED`).

---

## 3. MÁY TRẠNG THÁI PHÂN NHÁNH & CHỐNG DEADLOCK (STATE MACHINE & ANTI-DEADLOCK)
* **Phân nhánh độc lập theo `PeriodType`**:
  * **Nhánh `MON_HOC` / `NCKH`**: Sau khi GVHD nhập điểm $\ge 5.0$, chuyển thẳng sang tổng kết đợt (`READY_FOR_FINAL` $\rightarrow$ `PUBLISHED`). Tuyệt đối không sinh trạng thái chờ phản biện hay hội đồng.
  * **Nhánh `TLCN`**: GVHD chấm $\ge 5.0 \rightarrow$ Phân công GVPB $\rightarrow$ GVPB chấm $\ge 5.0 \rightarrow$ Chuyển thẳng sang tổng kết đợt. **CẤM ép đợt TLCN đi qua Hội đồng bảo vệ**.
  * **Nhánh `KLTN`**: Bắt buộc đủ 3 trạm kiểm soát: $\text{GVHD} \ge 5.0 \longrightarrow \text{GVPB} \ge 5.0 \longrightarrow \text{Council Defense}$.
* **Cơ chế Fail-Fast (Dừng thi sớm)**:
  * Điểm GVHD $< 5.0 \rightarrow$ Khóa đề tài ở trạng thái `FAILED_GVHD`, không phân công GVPB.
  * Điểm GVPB $< 5.0 \rightarrow$ Khóa đề tài ở trạng thái `FAILED_GVPB`, không xếp vào danh sách Hội đồng.

---

## 4. TỰ ĐỘNG HÓA & THẨM QUYỀN HỘI ĐỒNG (COUNCIL AUTOMATION)
* **Quy trình chấm độc lập**: Các thành viên Hội đồng (Chủ tịch, Thư ký, Ủy viên) nộp phiếu điểm cá nhân độc lập vào hệ thống.
* **Cảnh báo độ lệch điểm tự động**: Hệ thống tự động tính toán $\text{Score}_{\text{Council}}$ và kích hoạt cờ cảnh báo nếu $\max(\text{Score}) - \min(\text{Score}) > 2.0$.
* **Quyền hạn Chủ tịch Hội đồng**:
  * Chủ tịch Hội đồng **KHÔNG ĐƯỢC PHÉP** ghi đè trực tiếp điểm của thành viên khác.
  * Chủ tịch chỉ có thẩm quyền: Rà soát bảng điểm tự động, nhập nhận xét/biên bản chung và bấm nút **Khóa điểm Hội đồng (`COUNCIL_FINALIZED`)**.
* **Công bố điểm**: Chỉ có tài khoản Trưởng khoa (`Dean`) mới có quyền thực hiện action công bố kết quả (`PUBLISHED`).

---

## 5. BẢO MẬT DỮ LIỆU & KIỂM SOÁT TRANH CHẤP (SECURITY & CONCURRENCY)
* **Kiểm soát truy cập & Chống IDOR (Insecure Direct Object References)**:
  * Mọi API thao tác với `Topic`, `Registration`, `Council`, `Evaluation` bắt buộc phải kiểm tra quyền sở hữu và ranh giới theo `division_id`, `faculty_id`, hoặc `user_id` của `SecurityContext`.
  * Trưởng bộ môn chỉ được xem/duyệt/phân công các đề tài thuộc `division_id` của chính mình.
* **Kiểm soát tranh chấp bằng Optimistic Locking (`@Version`)**:
  * Bắt buộc khai báo trường `@Version private Long version;` trên các Entity có tần suất ghi tranh chấp cao: `Topic` và `GroupRegistration`.
  * Bắt buộc xử lý ngoại lệ `OptimisticLockingFailureException` tại Service Layer khi có nhiều sinh viên/nhóm đăng ký cùng một đề tài tại cùng một thời điểm.
* **Chuẩn hóa tầng nghiệp vụ Spring Boot 3**:
  * Mọi DTO tiếp nhận dữ liệu đầu vào bắt buộc sử dụng Bean Validation (`@NotNull`, `@NotBlank`, `@DecimalMin`, `@DecimalMax`, `@Size`).
  * Tất cả các thao tác ghi dữ liệu nhiều bước phải được bọc trong `@Transactional(rollbackFor = Exception.class)`.
