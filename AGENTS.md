# Quy tắc phát triển dự án (Project Rules & Guidelines)

Tài liệu này tổng hợp toàn bộ các tiêu chuẩn kỹ thuật và nghiệp vụ bắt buộc được trích xuất từ **Rubric đánh giá đồ án cuối kỳ (Rubric.pdf)** và **Tài liệu bài giảng học phần Lập trình Web - WEPR330479 (Tailieu/Lab 4 - Spring Security, Chapter 03-07)**. Toàn bộ các tác nhân AI khi làm việc trên dự án phải **TUÂN THỦ TUYỆT ĐỐI**, không được đi lệch hay áp dụng các giải pháp chống chế.

---

## 1. Nguyên tắc Dữ liệu & Cơ sở dữ liệu (Data & Database Strict Policy)
- **TUYỆT ĐỐI KHÔNG TẠO MOCK DATA / DỮ LIỆU ẢO TRONG MÃ NGUỒN**: Không được hardcode tài khoản, người dùng, khoa, bộ môn, đề tài hay bất kỳ dữ liệu nghiệp vụ nào vào các file Java (`DataSeederService`, controllers, services, migration scripts).
- **TUYỆT ĐỐI KHÔNG TỰ Ý TẠO FILE AUTO-TEST / INTEGRATION TEST**: Nghiêm cấm mọi tác nhân AI tự ý sinh ra các file test tự động trong `src/test/java/` (như các test class tích hợp, test workflow, security test tự biên soạn). Các test này khi chạy sẽ tự động insert hàng loạt dữ liệu rác (`ext_gv_...`, `KT_VAL_...`, `DT-QUOTA-...`) làm ô nhiễm CSDL Neon PostgreSQL và làm hỏng luồng manual test của người dùng. Thư mục `src/test/java/topicmanagement/` chỉ duy trì duy nhất file chuẩn của Spring Boot là `TopicManagementApplicationTests.java`.
- **TUYỆT ĐỐI KHÔNG TỰ Ý SINH RA FILE NGHIỆP VỤ / SERVICE / HELPER DƯ THỪA**: Tuyệt đối không tạo thêm các service "ma" hay class tự chế (như `DbHealthTracker`, file seeder tạm, cleanup service, migration scripts, tracker ngầm...). Mọi logic nghiệp vụ bắt buộc phải tích hợp chuẩn mực vào các Service/Controller hiện có theo đúng mô hình MVC phân tầng.
- **DỮ LIỆU DUY NHẤT LƯU TRỮ TRÊN NEON POSTGRESQL**: Mọi dữ liệu thực tế (người dùng, vai trò, đợt đăng ký, đề tài, nhóm, hội đồng, điểm số) phải được lưu trữ và truy vấn trực tiếp từ cơ sở dữ liệu Neon PostgreSQL.
- **PHẠM VI TÀI KHOẢN CHUẨN**: Hệ thống chỉ vận hành trên phạm vi các tài khoản đã thống nhất trong cơ sở dữ liệu (theo [TEST_ACCOUNTS_GUIDE.md](file:///d:/Web/Java-project/TEST_ACCOUNTS_GUIDE.md)), không sinh thêm tài khoản test hay tài khoản ảo.
- **KHÔNG ĐỂ LẠI FILE TẠM NGOÀI THƯ MỤC GỐC**: Tuyệt đối không lưu các file test, dump HTML, cookie jar (`topics_live.html`, `cookies.txt`, `switch.html`...) ở thư mục gốc của dự án. Mọi file scratch chỉ được phép đặt trong thư mục tạm của agent.

---

## 2. Tiêu chuẩn Kiến trúc & Kỹ thuật theo Rubric & Bài giảng (Spring Boot & MVC)

### 2.1. Cấu trúc chuẩn Spring MVC (Mục 3 Rubric)
- Mã nguồn bắt buộc tuân thủ mô hình phân tầng chặt chẽ:
  $$\text{Controller} \longrightarrow \text{Service} \longrightarrow \text{Repository} \longrightarrow \text{Entity / Model}$$
- **Không dồn logic nghiệp vụ vào Controller**: Toàn bộ nghiệp vụ kiểm tra điều kiện, tính điểm, ràng buộc đợt phải nằm ở tầng Service. Controller chỉ tiếp nhận request, chuyển đổi Model và điều hướng View / REST response.
- Áp dụng tối thiểu các kỹ thuật Spring Boot cốt lõi:
  - **Spring Data JPA / Hibernate**: Khai báo Repository kế thừa `JpaRepository`, dùng `Optional<T>` xử lý trường hợp null, tối ưu query (custom JPQL/HQL hoặc derive method).
  - **Validation (JSR-380)**: Dùng `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max` trên DTO/Entity.
  - **Dependency Injection & IoC**: Sử dụng Constructor Injection (hoặc Lombok `@RequiredArgsConstructor`), không dùng Field Injection bừa bãi.

### 2.2. Tiêu chuẩn Bảo mật Spring Security 6 (Lab 4 & Chapter 07)
- **Mã hóa mật khẩu**: Sử dụng `BCryptPasswordEncoder`, tuyệt đối không lưu mật khẩu dạng plain-text.
- **Phân quyền đa tầng (Defense-in-depth)**:
  - Cấu hình phân quyền URL tại `SecurityFilterChain` qua Lambda DSL (`authorizeHttpRequests`).
  - Kích hoạt `@EnableMethodSecurity` và áp dụng `@PreAuthorize("hasRole(...)")` hoặc `@PreAuthorize("hasAnyRole(...)")` tại các Controller/Service nhạy cảm.
- **Cơ chế CSRF & Form Thymeleaf chuẩn mực (Lab 4, Trang 11-12)**:
  - **BẮT BUỘC dùng cú pháp Thymeleaf `th:action="@{...}"` cho 100% các form POST**: Khi dùng `th:action`, Thymeleaf sẽ tự động sinh URL context-path và **tự động inject token ẩn CSRF** (`<input type="hidden" name="_csrf" value="..." />`) tại tầng server trước khi render HTML.
  - **TUYỆT ĐỐI KHÔNG DÙNG HACK SCRIPT Ở CLIENT**: Không được tạo form trần rồi dùng JavaScript bắt sự kiện submit để chèn CSRF token. Đây là cách làm chống chế, đi ngược giáo trình chuẩn.
  - Sử dụng dialect `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"` cùng các thẻ `sec:authorize="hasRole(...)"`, `sec:authentication="name"` để ẩn/hiện thành phần UI theo vai trò.

### 2.3. Quy chuẩn Frontend & Giao diện (UI/UX)
- **TUYỆT ĐỐI KHÔNG DÙNG TAILWIND PLAY CDN (`cdn.tailwindcss.com`)**: Không được nhúng script compiler runtime phía client. Nó gây cảnh báo đỏ trên console trình duyệt, làm chậm trang và vi phạm quy định chấm đồ án môn học.
- **Sử dụng tài nguyên tĩnh chuẩn mực**:
  - Giao diện xây dựng trên nền tảng **Bootstrap 5.3 + FontAwesome 6 + Vanilla CSS**.
  - Toàn bộ stylesheet tùy chỉnh phải nằm trong file CSS tĩnh nội bộ (`src/main/resources/static/css/`), liên kết qua thẻ `<link rel="stylesheet" th:href="@{/css/...}">`.

---

## 3. Tiêu chuẩn Nghiệp vụ Bắt buộc (Mục 4 Rubric)

Mọi luồng chức năng của hệ thống phải hiện thực đúng và đầy đủ các nghiệp vụ sau:
1. **Đợt đăng ký 2 giai đoạn**:
   - Giai đoạn 1: Giảng viên đề xuất và Trưởng khoa/Bộ môn duyệt đề tài.
   - Giai đoạn 2: Sau khi đề tài được duyệt, hệ thống mới mở cổng cho sinh viên đăng ký.
2. **Ràng buộc Nhóm sinh viên**:
   - Mỗi nhóm phải có từ **3 đến 5 sinh viên**.
   - Có đúng **1 nhóm trưởng** đại diện nộp báo cáo.
   - Sinh viên đã tham gia vào Nhóm A thì **tuyệt đối không được phép tham gia Nhóm B**.
3. **Quy định Hội đồng Đánh giá / Phản biện**:
   - Mỗi Hội đồng phải có từ **3 đến 5 giảng viên** (Chủ tịch, Thư ký, Ủy viên...).
   - **Ràng buộc xung đột lợi ích (Bắt buộc)**: Giảng viên không được phép chấm điểm (hoặc ngồi hội đồng) đề tài do chính mình hướng dẫn (GVHD chính hoặc phụ). Hệ thống phải tự động loại bỏ GVHD khỏi danh sách chọn thành viên hội đồng.
   - Điểm đánh giá tính theo trung bình cộng các thành viên hội đồng.
4. **Công bố kết quả & Tra cứu điểm**:
   - Hỗ trợ công bố kết quả và gửi email thông báo tự động cho từng sinh viên trong nhóm.
   - **Phân vùng tra cứu**: Sinh viên chỉ được tra cứu điểm và kết quả của đợt mà nhóm mình đã đăng ký tham gia; không được phép tra cứu điểm của các đợt khác.

---

## 4. Tiêu chuẩn Tối ưu Hiệu năng & Giám sát (Performance & Optimization Guidelines)

1. **Cân bằng luồng Tomcat & HikariCP Pool (Đồng bộ tài nguyên)**:
   - Cơ sở dữ liệu Neon hoạt động ở Pooled Mode với dung lượng kết nối giới hạn (`maximum-pool-size=15`).
   - Luồng xử lý của Tomcat bắt buộc được giới hạn đồng bộ (`server.tomcat.threads.max=50`, `server.tomcat.threads.min-spare=10`, `server.tomcat.accept-count=100`).
   - Tuyệt đối không để Tomcat dùng thiết lập mặc định (200 luồng) gây tranh chấp kết nối và cạn kiệt tài nguyên pooler.

2. **Tối ưu hóa Truy vấn & Chỉ mục CSDL (Database Indexing & N+1 Prevention)**:
   - **Bắt buộc dùng `@EntityGraph` hoặc `JOIN FETCH`**: Tuyệt đối không để quan hệ `ManyToOne`/`OneToOne` bị lặp query (N+1). Mọi phương thức truy vấn danh sách đề tài, đăng ký, hội đồng, nhóm sinh viên phải nạp trước các entity liên quan.
   - **Bảo toàn Batch Fetch**: Giữ cấu hình `spring.jpa.properties.hibernate.default_batch_fetch_size=30`.
   - **Đánh chỉ mục (Index) đầy đủ**: Toàn bộ các cột khóa ngoại, cột trạng thái và cột thường xuyên nằm trong mệnh đề `WHERE` / `ORDER BY` (như `period_id`, `department_id`, `status`, `supervisor_id`, `group_id`, `topic_id`) phải được khai báo index rõ ràng trong `@Table(indexes = { ... })` và đảm bảo tồn tại trên Neon PostgreSQL (`CREATE INDEX IF NOT EXISTS`).

3. **Chiến lược Bộ nhớ đệm (Caching Policy)**:
   - Chỉ sử dụng In-Memory Cache chuẩn của Spring (`@EnableCaching`, `@Cacheable`, `@CacheEvict`) cho các bảng danh mục ít biến động (`activePeriods`, `allMajors`, `announcements`).
   - **TUYỆT ĐỐI KHÔNG DÙNG REDIS, MESSAGE QUEUE, HAY GATEWAY PHÂN TÁN**: Giữ kiến trúc Monolithic Spring Boot MVC tinh gọn, độc lập, không phụ thuộc vào hạ tầng bên thứ ba làm tăng rủi ro khi hội đồng chấm thi chạy thử đồ án.

4. **Giám sát Sức khỏe & Chỉ số (Actuator & Metrics)**:
   - Kích hoạt Spring Boot Actuator (`health`, `info`, `metrics`, `hikaricp`) tại `/actuator`.
   - Đảm bảo các endpoint giám sát được cấu hình mở an toàn trong Spring Security để phục vụ công tác kiểm thử và đo lường tải của hệ thống.

