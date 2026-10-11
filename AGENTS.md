# Quy tắc phát triển dự án (Project Rules & Guidelines)

## 1. Nguyên tắc Dữ liệu & Cơ sở dữ liệu (Data & Database Strict Policy)
- **TUYỆT ĐỐI KHÔNG TẠO MOCK DATA / DỮ LIỆU ẢO TRONG MÃ NGUỒN**: Không được hardcode tài khoản, người dùng, khoa, bộ môn, đề tài hay bất kỳ dữ liệu nghiệp vụ nào vào các file Java (`DataSeederService`, controllers, services, migration scripts).
- **KHÔNG TỰ Ý SINH RA FILE ẢO / SERVICE DƯ THỪA**: Tuyệt đối không tạo các file seeder tạm, cleanup service hay migration service tự chế làm ô nhiễm cấu trúc mã nguồn.
- **DỮ LIỆU DUY NHẤT LƯU TRỮ TRÊN NEON POSTGRESQL**: Mọi dữ liệu thực tế (người dùng, vai trò, đợt đăng ký, đề tài, nhóm, hội đồng, điểm số) phải được lưu trữ và truy vấn trực tiếp từ cơ sở dữ liệu Neon PostgreSQL.
- **PHẠM VI TÀI KHOẢN CHUẨN**: Hệ thống chỉ vận hành trên phạm vi các tài khoản đã thống nhất trong cơ sở dữ liệu, không sinh thêm tài khoản test hay tài khoản ảo.

## 2. Nguyên tắc Tự chủ & Chất lượng mã nguồn (Code Quality & Autonomy)
- Giữ cho mã nguồn Java tinh gọn, cấu trúc sạch sẽ theo chuẩn Spring Boot.
- Khi cần thao tác dữ liệu cơ sở dữ liệu, ưu tiên quản lý qua câu lệnh SQL chuẩn hoặc migration tool chuyên dụng, không chèn dữ liệu test vào mã nguồn ứng dụng đang chạy.
