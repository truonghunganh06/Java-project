# Sáu lỗi trước khi bổ sung dữ liệu hàng loạt

Ngày kiểm tra: 20/10/2026.

## Thay đổi

| Lỗi | Cách sửa ở backend |
|---|---|
| Nhóm mới nhận điểm của nhóm cũ khi dùng lại đề tài | Phiếu điểm được lấy theo nhóm hiện tại, đúng đề tài, đúng đợt và trạng thái hợp lệ; trang sinh viên, quản lý, xuất kết quả dùng chung bộ lọc. Lịch sử nhóm cũ được giữ. |
| Điểm của người đã hết phân công vẫn được tính | Chỉ nhận phiếu đã nộp của GVHD chính/phụ, phản biện và thành viên hội đồng đang được phân công. Phải đủ người chấm; hội đồng đủ 3–5 người. Đổi phân công tính lại điểm; đề tài đã công bố phải thu hồi trước khi đổi. |
| Hai nhóm cùng lấy đề tài hoặc vượt chỉ tiêu GVHD | Khóa hàng nhóm, đề tài và GVHD trong giao dịch trước khi kiểm tra và lưu. Các GVHD được khóa theo thứ tự ID. Neon có khóa duy nhất trên đề tài của nhóm. |
| Vượt 5 thành viên hoặc sinh viên vào hai nhóm khi thao tác đồng thời | Khóa hàng sinh viên rồi nhóm, đọc lại dữ liệu trước khi kiểm tra. Áp dụng cho tạo nhóm, nhập mã, thêm thành viên trực tiếp, nhận lời mời và gộp nhóm. |
| Nộp điểm lần đầu đồng thời tạo phiếu trùng | Khóa đề tài; tìm/cập nhật phiếu theo đề tài–nhóm–giảng viên–loại điểm. Neon có khóa duy nhất cùng bốn cột và yêu cầu các khóa này không rỗng. |
| Trưởng khoa ghim/xóa thông báo khoa khác | Service kiểm tra tài khoản hoạt động, vai trò và khoa sở hữu thông báo. Thông báo toàn trường do admin quản lý. Controller truyền người thao tác; trang hiển thị lỗi bị từ chối. |

## Bằng chứng đã kiểm tra

- Biên dịch thành công 72 file nguồn, Java release 21.
- 39 kiểm tra cô lập đạt: lọc điểm cũ, thay phân công, đủ người chấm, làm tròn, lọc danh sách xuất kết quả, đăng ký/nhận lời mời/nộp điểm đồng thời và quyền thông báo.
- Các kiểm tra này gọi service với repository trong bộ nhớ và mô phỏng khóa hàng. Không phải kiểm thử tải đồng thời trên PostgreSQL thật; không tạo file trong `src/test/java`, không ghi dữ liệu nghiệp vụ và không gửi email.
- Chạy ứng dụng đã sửa trên cổng 8081 kết nối Neon thành công.
- Dùng tài khoản hiện có `head_qtkd`: POST ghim và xóa thông báo CNTT ID 42 đều hiển thị lỗi không thuộc khoa quản lý. Các trang đề tài, hội đồng, kết quả trả về thành công.
- Dùng tài khoản hiện có `sv_cntt01`: yêu cầu vào nhóm với mã không tồn tại chạy được truy vấn khóa sinh viên, trả lỗi phù hợp, không tạo thành viên.
- Đối chiếu Neon trước/sau: thông báo ID 42 vẫn thuộc khoa 1, không ghim; nhóm và phiếu điểm vẫn bằng 0.
- Neon đã xác nhận hai unique index `uk_student_groups_topic`, `uk_evaluation_group_lecturer_type`; bốn cột khóa của phiếu điểm đều NOT NULL. Trước khi áp dụng đã khóa bảng và kiểm tra trùng/khóa rỗng; không xóa hoặc sửa bản ghi nghiệp vụ để ép ràng buộc chạy.
- `git diff --check` đạt.

## Giới hạn và bước dùng tiếp

Chưa thêm dữ liệu hàng loạt. Neon hiện chưa có nhóm/phiếu điểm nên chưa kiểm chứng một chu trình chấm điểm đầy đủ bằng dữ liệu nghiệp vụ thật hoặc tải nhiều người trên PostgreSQL. Cần chạy một quy trình thật với tài khoản hiện có trước khi mở rộng dữ liệu.

Ứng dụng kiểm tra cổng 8081 đã được dừng sau kiểm tra. Chưa thay thế bản JAR dùng để khởi chạy chính; cần đóng gói và chạy lại bằng nguồn mới để nhận các sửa đổi.

Các công cụ kiểm tra tạm nằm trong thư mục bị Git bỏ qua `target/codex-workflow`; không thuộc mã nguồn triển khai.
