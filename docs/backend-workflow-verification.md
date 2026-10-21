# Backend hội đồng, nhắc hạn và email — 20/10/2026

## Hành vi sau sửa

- Tạo/sửa/gán/hủy gán hội đồng kiểm tra người quản lý đúng khoa tại service. Giảng viên phải tồn tại, đang hoạt động, cùng khoa; hội đồng có 3–5 người khác nhau, đúng một Chủ tịch và một Thư ký.
- Đề tài và hội đồng phải cùng khoa, cùng đợt. GVHD chính/phụ bị từ chối khi xuất hiện trong thành viên hội đồng của đề tài đó. Đổi thành viên hội đồng cũng kiểm tra các đề tài đã gán.
- Cả gán một đề tài và gán nhiều đề tài dùng chung kiểm tra. Gán nhiều hoặc tạo hội đồng kèm đề tài là một giao dịch: một đề tài không hợp lệ thì toàn bộ thao tác bị hủy.
- `AnnouncementService` kiểm tra các đợt đang hoạt động mỗi 60 giây, bắt đầu sau khi ứng dụng chạy 10 giây. Nhắc trước hạn 24 giờ; tạo thông báo đến hạn nếu đã qua mốc, kể cả mốc bị bỏ lỡ trong lúc backend tắt.
- Nhắc hạn đề xuất/đăng ký áp dụng cho mọi loại đợt; phản biện cho TLCN/KLTN; bảo vệ cho KLTN. Nội dung ghi giờ Việt Nam. Đây là thông báo chung theo khoa/đợt, không phải email nhắc hạn cá nhân.
- Thông báo lưu trong `announcements`. `event_key` gồm ID đợt, loại hạn, thời điểm tuyệt đối và giai đoạn SOON/DUE. Chỉ mục UNIQUE kết hợp `ON CONFLICT DO NOTHING` chống trùng khi kiểm tra lại, khởi động lại hoặc có nhiều tiến trình. Sửa mốc hạn tạo khóa sự kiện mới.
- Nhập phiếu phản biện kiểm tra hạn ở service, không chỉ ở giao diện.
- Thiếu SMTP, thiếu người nhận/email hoặc SMTP từ chối đều tính là chưa gửi được. Giao diện báo riêng số email được máy chủ tiếp nhận và số thất bại; không nói đã vào hộp thư người nhận.
- Công bố khóa bản ghi đề tài, ghi và commit kết quả trước khi gửi email. Công bố lại cùng trạng thái không gửi lại email. Email lỗi không hoàn tác kết quả điểm đã lưu và chưa có cơ chế tự thử lại email lỗi.

## Thay đổi schema trên Neon

Đã thực hiện bổ sung, không xóa hay ghi đè dữ liệu đang có:

```sql
ALTER TABLE announcements ADD COLUMN IF NOT EXISTS event_key varchar(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_announcements_event_key
    ON announcements(event_key);
```

`spring.jpa.hibernate.ddl-auto=none` được giữ nguyên. Khi dùng một database khác, cần bổ sung cột và chỉ mục trên trước khi chạy bản sửa. Định nghĩa bảng trong `docs/database_schema.sql` đã được cập nhật cho database mới.

## Dữ liệu thật đã hoàn thiện qua chức năng ứng dụng

- Đợt QTKD hiện có: **490**.
- Hội đồng đã tạo: **57**, mã **HD-5799**, tên lấy từ tên đợt QTKD hiện có.
- Ba thành viên thực lưu: **270** (Chủ tịch), **480** (Thư ký), **481** (Ủy viên), cùng khoa QTKD. GVHD của hai đề tài bên dưới là **276**, không có trong hội đồng.
- Hai đề tài hiện có **355**, **356** đã được gán vào hội đồng **57**, phản biện **481**, thông qua form lập/phân công của ứng dụng với tài khoản `head_qtkd`.
- Không tạo thêm tài khoản, đề tài minh họa, nhóm thiếu người, báo cáo giả hay điểm số giả.

## Kiểm chứng đã thực hiện

1. Biên dịch mã ứng dụng, không chạy `SpringBootTest` kết nối database.
2. Đạt **35 kiểm tra cô lập** trong thư mục tạm `target/codex-workflow`, ngoài `src/test/java`: sai khoa/đợt, xung đột GVHD chính/phụ, thiếu/thừa/trùng giảng viên, ID không tồn tại, tài khoản khóa, vai trò hội đồng sai, quyền hủy gán, mốc 24 giờ/đúng hạn/bù hạn, khác múi giờ nhưng cùng thời điểm, thông báo lặp, SMTP thiếu/thất bại/tiếp nhận, commit trước gửi mail, công bố lại và ghi phiếu trước/sau hạn. Các repository/người gửi mail trong kiểm tra này chỉ ở bộ nhớ; không ghi database hay gửi email thật.
3. Chạy backend kiểm tra riêng tại cổng **8081**, kết nối Neon hiện có. Không dừng ứng dụng của người dùng tại cổng 8080.
4. Scheduler tự tạo hai thông báo thật **41**, **42** cho hạn đề xuất đã qua của đợt **490**, **489**. Đọc lại từ Neon sau nhiều chu kỳ và sau khởi động lại: mỗi `event_key` vẫn có đúng **một** bản ghi.
5. Từ form gán nhiều đề tài, chọn **355** (QTKD) và **351** (CNTT) vào hội đồng **57**: bị từ chối. Truy vấn Neon ngay sau đó xác nhận **355** cũng chưa bị gán, chứng minh toàn bộ giao dịch đã rollback.
6. Sau ca bị từ chối, chọn **355**, **356**: gán thành công. Đọc lại Neon xác nhận `council_id=57`, `reviewer_id=481` cho cả hai; đề tài **351** vẫn không bị thay đổi.
7. Đọc lại hội đồng: có đúng ba bản ghi thành viên và ba giảng viên phân biệt.
8. Với bản biên dịch cuối: đường gán một đề tài dùng khóa hội đồng hoạt động trên Neon; đường công bố dùng khóa đề tài từ chối đề tài chưa có nhóm, không gửi email; trang thông báo render được thông báo tự sinh từ database.

Kiểm tra công bố bổ sung: xác nhận gửi mail chỉ sau commit, công bố lặp không gửi lại và commit thất bại không gửi mail. Khóa dòng đề tài/hội đồng được dùng trong giao dịch để tránh thay đổi đồng thời làm mất hiệu lực kiểm tra.

Lưu ý chạy ứng dụng: tiến trình cổng 8080 đang sử dụng JAR trong `target`, nên thao tác đóng gói không đổi tên được tệp này trên Windows. Biên dịch đã thành công; backend kiểm chứng chạy từ các class đã biên dịch. Cần dừng/khởi động lại ứng dụng chính và đóng gói lại để nạp bản sửa; không tự dừng tiến trình người dùng trong lần kiểm tra này.

Sau kiểm chứng, tiến trình riêng cổng 8081 được dừng; ứng dụng chính cổng 8080 được giữ nguyên.

## Phần chưa thể hoàn tất

Chưa chạy được chuỗi SV lập nhóm → đăng ký → nộp báo cáo → chấm → công bố với dữ liệu thật: Neon hiện có hai SV CNTT, một SV QTKD gán khoa rõ ràng và một hồ sơ `sv1` chưa gán khoa trực tiếp (trước thay đổi mô hình, hồ sơ này có chuyên ngành QTKD). Không khoa nào có đủ ba tài khoản SV hợp lệ theo mô hình hiện tại.

Không tự sửa khoa của sinh viên, thêm tài khoản hay hạ sĩ số. Cần bổ sung tài khoản sinh viên thực đã được thống nhất và hoàn chỉnh hồ sơ trước khi tiếp tục chuỗi này. Do chưa có nhóm, chưa có công bố kết quả hoặc email điểm thực gửi đến sinh viên; các ca SMTP ở trên là kiểm tra cô lập.

Trong lúc thực hiện có thay đổi đồng thời vào mã nguồn, gồm chức năng tạo/gán nhiều đề tài và bỏ mô hình bộ môn. Đã tích hợp kiểm tra vào các đường gọi mới, xử lý xung đột và sửa tham chiếu khoa không tồn tại trong kiểm tra quyền xem điểm để bản hiện tại biên dịch được.

Nguồn cơ chế nền: [Spring scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html), [PostgreSQL ON CONFLICT](https://www.postgresql.org/docs/current/sql-insert.html).
