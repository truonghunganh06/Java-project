# Rà soát quy trình backend — 20/10/2026

Đây là ảnh chụp trước khi sửa. Kết quả sửa và bằng chứng chạy thật cho hội đồng/nhắc hạn/email được ghi riêng trong [backend-workflow-verification.md](backend-workflow-verification.md); các lỗi trong báo cáo ban đầu không đồng nghĩa vẫn còn ở bản hiện tại.

## Phạm vi và mức bằng chứng

Rà luồng đợt đăng ký → đề xuất/duyệt đề tài → nhóm → đăng ký → báo cáo → hội đồng/phản biện → điểm → công bố/tra cứu → thông báo. Đối chiếu controller, service, entity, cấu hình phân quyền và giao diện liên quan.

Đây là kết quả đọc mã nguồn, **chưa phải biên bản chạy thành công trên Neon**. Không khởi động ứng dụng, không chạy test kết nối database, không thêm tài khoản/dữ liệu, không gửi email. Các bước kiểm chứng bên dưới là bước cần thực hiện tiếp, chưa được đánh dấu đạt.

Giả định vận hành: ứng dụng Spring Boot MVC đơn khối, nhiều tài khoản thao tác cùng lúc, dùng Neon và tài khoản đã thống nhất. Không đề xuất thêm Redis, queue hay service phụ.

## Bốn câu hỏi cho từng quy trình

| Quy trình | Ai kích hoạt? | Backend đang xử lý gì? | Database/file lưu gì? | Bằng chứng cần lấy khi chạy thật |
|---|---|---|---|---|
| Tạo và sửa đợt | Trưởng khoa/bộ môn qua controller | `DepartmentHeadService.createPeriod/updatePeriod` lưu mốc thời gian, loại đợt, trạng thái; phần kiểm tra thứ tự ngày hiện ở controller | `registration_periods` | ID đợt, các mốc đã lưu; mở lại từ phiên đăng nhập khác vẫn đúng; ngày sai bị từ chối |
| Mở/đóng giai đoạn theo thời gian | Thời gian hiện tại khi code được gọi; thay đổi trạng thái lưu có thao tác riêng | `RegistrationPeriod.isLecturerProposalOpen/isStudentRegistrationOpen` kiểm tra ngày; `getDisplayStatus` tính nhãn hiển thị, không ghi DB | Các mốc và `status/is_active`; không thấy bản ghi sự kiện chuyển giai đoạn | Gọi thao tác ngay trước/sau hạn; sau hạn phải bị chặn ở server, dù trang cũ còn mở |
| Đề xuất đề tài | Giảng viên | `LecturerService.createTopic` kiểm tra giai đoạn nếu có đợt, gắn GVHD và lưu `PENDING` | `topics` | ID đề tài và trạng thái; trưởng khoa thấy chính bản ghi đó; ngoài hạn bị từ chối |
| Duyệt/từ chối đề tài | Trưởng khoa/bộ môn | `DepartmentHeadService.approveTopic/rejectTopic` kiểm tra phạm vi rồi cập nhật trạng thái/lý do | `topics` | Đề tài trước duyệt không đăng ký được; sau duyệt chỉ đăng ký được nếu đúng thời gian và đúng khoa |
| Tạo nhóm, mời, nhận lời mời | Sinh viên tạo nhóm; nhóm trưởng mời; người nhận chấp nhận | `StudentService.createGroup`; `GroupRegistrationService.sendInvitation/acceptInvitation` kiểm tra người nhận, trùng nhóm trong đợt, khoa, số thành viên | `student_groups`, `group_members`, `group_invitations` | Hai tài khoản thấy cùng nhóm; lời mời đổi trạng thái, thành viên được lưu; tài khoản thứ ba không phản hồi thay được |
| Đăng ký đề tài cho nhóm | Nhóm trưởng | `StudentService.registerTopicForGroup` kiểm tra đề tài đã duyệt, khoa/đợt, quyền trưởng nhóm, 3–5 thành viên, đề tài đã có nhóm và quota GVHD | Gắn `student_groups.topic_id`, chuyển nhóm `REGISTERED` | ID nhóm/đề tài khớp sau đăng nhập lại; nhóm thiếu người, khác khoa, ngoài hạn bị từ chối |
| GVHD duyệt nhóm thực hiện | GVHD chính/phụ | `LecturerService.approveGroupTopic` kiểm tra người hướng dẫn và sĩ số khi duyệt | Trạng thái `student_groups` | GVHD đúng duyệt được; GV khác không duyệt được; nhóm được duyệt mới nộp báo cáo được |
| Rút nhóm, hủy nhóm, ghép nhóm, chuyển trưởng nhóm | SV/nhóm trưởng đề nghị; GVHD duyệt tùy trạng thái | `GroupRegistrationService` có xử lý và lưu yêu cầu; duyệt rút có thể chuyển nhóm thành `PENDING_MEMBER`; chuyển trưởng cập nhật cả nhóm và vai trò thành viên | Nhóm, thành viên, yêu cầu rút/hủy, liên kết đề tài | Trạng thái yêu cầu và quan hệ trước/sau; luôn đúng một trưởng nhóm; người đã rời không còn quyền của nhóm; không chỉ kiểm tra thông báo trên màn hình |
| Nộp và tải báo cáo | Nhóm trưởng nộp; người có quyền tải | `StudentService.submitGroupReport` kiểm tra trưởng nhóm, nhóm/đề tài được duyệt; `FileStorageService` lưu file; controller tải kiểm tra thành viên | Metadata trong `submissions`; **nội dung file ở `uploads/submissions` trên máy chạy backend** | ID bài nộp, đường dẫn, dung lượng; tải lại đúng nội dung; thành viên thường không nộp được, người ngoài không tải được |
| Hội đồng và phân công phản biện | Trưởng khoa/bộ môn | `CouncilService.createCouncil` kiểm tra danh sách 3–5, trùng giảng viên và vai trò; `assignTopicToCouncil/assignReviewer` chặn GVHD chính/phụ | `councils`, `council_members`, liên kết hội đồng/phản biện trên đề tài | Danh sách thành viên thực lưu đủ; gán GVHD bị từ chối; gán khác khoa/đợt phải bị từ chối ở mọi đường gọi — hiện có thiếu sót, xem bên dưới |
| Chấm và tính điểm | GVHD, GVPB, thành viên hội đồng theo phân công | `CouncilService.submitEvaluation` kiểm tra vai trò, xung đột, thang 0–10; lưu phiếu và gọi tính điểm. `LecturerService.gradeSubmission` là điểm bài nộp riêng | `evaluations`, điểm tổng hợp trên `topics`; điểm báo cáo ở `submissions.grade` | Phiếu theo đúng giảng viên, điểm hội đồng khớp TBC; sửa phiếu tính lại; không nhầm điểm bài nộp với điểm tổng kết |
| Công bố và tra cứu kết quả | Trưởng khoa công bố; sinh viên tra cứu | `publishTopicResults/publishAllResults` kiểm tra chấm đủ, lưu cờ công bố, gọi email; trang kết quả SV kiểm tra đợt đã tham gia và service ẩn phiếu chưa công bố | `topics.is_finalized`; chưa thấy lịch sử gửi mail theo người nhận | Đăng nhập SV đúng nhóm thấy điểm sau công bố; SV khác/đợt khác không lấy được điểm qua cả trang và API; kiểm tra email trong hộp thư, không dựa vào thông báo thành công |
| Thông báo chung và đã đọc | Người quản lý tạo thông báo; người dùng mở/đánh dấu đọc | `AnnouncementService` lưu thông báo thật; thông báo đề tài chờ duyệt được dựng khi tải trang trưởng khoa; đã đọc do JavaScript lưu cục bộ | `announcements` có lưu DB; đã đọc chỉ ở `localStorage`, không có bản ghi theo người nhận trong luồng đã đọc | Đăng nhập trên trình duyệt khác, đổi tài khoản trong cùng trình duyệt; kiểm tra phạm vi người nhận và tính độc lập trạng thái đọc |
| Nhắc hạn tự động và ghi chú lịch | Mong muốn: backend tự chạy theo hạn; hiện ghi chú do người dùng nhập | Không thấy `@Scheduled`, `@EnableScheduling` hoặc scheduler backend tương đương trong `src/main`; ghi chú lịch dùng `localStorage` | Chưa thấy lưu sự kiện nhắc hạn/giao nhận; ghi chú lịch không đi qua repository trong luồng đã xem | Đóng mọi trang nhưng giữ backend chạy, chờ qua mốc; phải có bản ghi nhắc đúng người, không trùng; khởi động lại phải xử lý được mốc bỏ lỡ — hiện chưa có luồng này |

## Các khoảng trống cần xử lý trước khi coi quy trình là hoàn chỉnh

### 1. API chi tiết đề tài trả điểm chưa công bố và thiếu giới hạn theo người xem — ưu tiên cao

- Bằng chứng: `TopicApiController.java:21` chỉ yêu cầu đã đăng nhập rồi gọi `getTopicDetailMap(id)`. `StudentService.java:627` đưa `finalScore` vào response chỉ dựa vào đề tài có nhóm; không kiểm tra `isFinalized`, người xem hoặc đợt đã tham gia. `SecurityConfig` cho tài khoản đã đăng nhập đi vào `/api/**`.
- Tình huống: một sinh viên đã đăng nhập gọi `GET /api/topics/{id}` của đề tài có nhóm và đã tính điểm nhưng chưa công bố. Response vẫn có điểm. Điều kiện ẩn trên trang kết quả không bảo vệ response này.
- Sửa tối thiểu: kiểm tra quyền xem điểm ở backend theo tài khoản, nhóm/đợt và cờ công bố trước khi đưa trường điểm vào response; giữ quyền xem hợp lệ của giảng viên/người quản lý.
- Nếu bỏ qua: việc công bố và phân vùng tra cứu chỉ được bảo vệ ở một số màn hình, không xuyên suốt hệ thống.

### 2. Hai đường gán hội đồng kiểm tra phạm vi không đồng nhất — ưu tiên cao

- Bằng chứng: `DepartmentHeadController.java:693` kiểm tra phạm vi đề tài và khoa hội đồng, nhưng `DepartmentHeadController.java:853` gọi thẳng cùng service sau kiểm tra đăng nhập; nhánh hủy gán tại dòng 874 cũng thiếu kiểm tra phạm vi đối tượng. `CouncilService.java:297` kiểm tra xung đột GVHD nhưng không kiểm tra cùng khoa/đợt.
- Tình huống: tài khoản trưởng khoa hợp lệ gửi ID đề tài/hội đồng ngoài phạm vi vào đường `/department-head/councils/{id}/assign-topic`. Không có kiểm tra sở hữu đối tượng trong chuỗi xử lý này để chặn trường hợp đó. Cùng khoa nhưng khác đợt cũng chưa được service chặn.
- Sửa tối thiểu: dùng một kiểm tra chung tại service hiện có cho người thao tác, phạm vi khoa/bộ môn, đợt và đối tượng tồn tại; áp dụng cho cả gán và hủy gán.
- Nếu bỏ qua: dữ liệu có thể được gắn sai khoa/đợt dù danh sách trên giao diện đã lọc.

### 3. Nhắc hạn chưa có tác nhân backend thực hiện

- Bằng chứng: tìm trong `src/main` không thấy tác vụ lịch backend. `RegistrationPeriod.java:243` chỉ tính trạng thái khi được gọi. `AnnouncementService.createAnnouncement` hiện là luồng tạo thông báo chủ động.
- Tình huống: không ai mở web hoặc gửi request khi đến hạn; không có đoạn xử lý tự chạy để tạo và gửi nhắc hạn.
- Sửa tối thiểu: thêm lịch kiểm tra vào service hiện có, truy vấn các mốc cần xử lý từ Neon, lưu dấu xử lý để chống tạo trùng; thống nhất múi giờ và xử lý bù sau khởi động. Xác định riêng nhắc trước hạn, đến hạn và hành vi quá hạn.
- Nếu bỏ qua: thêm nhiều đợt/đề tài vẫn không tạo ra thông báo tự động. Backend phải đang chạy để xử lý đúng giờ.

### 4. Công bố thành công không chứng minh email đã gửi

- Bằng chứng: `EmailService.java:129` thiếu cấu hình thì chỉ log `EMAIL-SIMULATION` rồi return; dòng 145 bắt lỗi gửi và không trả kết quả cho bên gọi. `CouncilService.java:657` vẫn trả thành công khi công bố; mỗi lần nhận công bố `true` đều gọi gửi, không kiểm tra chuyển từ chưa công bố sang đã công bố.
- Tình huống: thiếu SMTP hoặc gửi thất bại nhưng điểm vẫn công bố thành công; bấm công bố lại có thể gửi trùng.
- Sửa tối thiểu: phân biệt kết quả lưu điểm với kết quả gửi mail, bỏ thông điệp giả lập thành công, ghi nhận kết quả gửi để thử lại có kiểm soát; chỉ phát thông báo ở lần chuyển trạng thái phù hợp.
- Nếu bỏ qua: người quản lý tưởng sinh viên đã nhận được email, nhưng không có bằng chứng hoặc cách biết ai chưa nhận.

### 5. Đã đọc và ghi chú lịch chỉ tồn tại trong trình duyệt

- Bằng chứng: `templates/fragments/scripts.html:744` dùng khóa chung `topic_hub_read_announcements`; dòng 1469 và 1573 lưu ghi chú trong `localStorage`.
- Tình huống: đọc thông báo ở máy A rồi mở máy B vẫn chưa đọc; đổi tài khoản cùng trình duyệt có thể dùng chung danh sách đã đọc. Ghi chú không được backend đọc để gửi nhắc.
- Sửa tối thiểu: nếu đây là chức năng nghiệp vụ cần đồng bộ, lưu trạng thái theo người dùng và thông báo/ghi chú vào Neon, kiểm tra quyền tại service. Chỉ chia khóa theo tài khoản trong trình duyệt sẽ chưa đáp ứng yêu cầu lưu tập trung.
- Nếu bỏ qua: chuông thông báo và lịch có thể hiển thị khác nhau giữa tài khoản/thiết bị và không tạo tác vụ tự động.

### 6. Hạn phản biện chưa được thực thi khi ghi phiếu

- Bằng chứng: `RegistrationPeriod.java:288` có `isReviewerDeadlinePassed`, nhưng tìm caller trong `src/main/java` không thấy nơi dùng. `CouncilService.submitEvaluation` không kiểm tra hạn; `LecturerController.gradeReviewer` gọi thẳng service này.
- Tình huống: giảng viên phản biện đã được phân công vẫn gửi điểm sau `reviewerDeadline`; luồng hiện tại không từ chối dựa trên mốc đó. Luồng này cũng chưa chặn sửa điểm sau công bố.
- Sửa tối thiểu: thống nhất quy tắc khóa sau hạn/sau công bố và cơ chế mở lại hợp lệ, rồi kiểm tra tại `submitEvaluation`, không chỉ ẩn nút trên giao diện.
- Nếu bỏ qua: mốc hạn chỉ mang tính hiển thị đối với việc ghi phiếu. Cần xác nhận nghiệp vụ mở lại trước khi triển khai khóa.

### 7. Chưa bảo đảm sĩ số hội đồng thực lưu khớp danh sách gửi lên

- Bằng chứng: `CouncilService.createCouncil` kiểm tra kích thước `lecturerIds` nhưng chỉ lưu thành viên khi tìm được giảng viên; ID không tồn tại bị bỏ qua. `updateCouncil` cũng có cách lưu tương tự.
- Tình huống: gửi ba ID khác nhau nhưng một ID không tồn tại; danh sách đầu vào qua kiểm tra kích thước nhưng chỉ hai thành viên được lưu.
- Sửa tối thiểu: xác thực tất cả giảng viên tồn tại và hợp lệ trước khi lưu hội đồng/thay danh sách thành viên; lỗi thì từ chối toàn bộ giao dịch.
- Nếu bỏ qua: có hội đồng dưới ba thành viên dù form đã kiểm tra đủ ba lựa chọn.

## Chuẩn bị dữ liệu sau rà soát

1. Đối chiếu `TEST_ACCOUNTS_GUIDE.md` với tài khoản đang tồn tại thực tế bằng thao tác đọc. Tài liệu chỉ liệt kê hai SV CNTT và một SV QTKD; chưa đủ căn cứ để tạo nhóm ba SV cùng khoa. Không suy ra Neon cũng chỉ có số đó, không tự tạo thêm tài khoản.
2. Kiểm kê các đợt, đề tài, nhóm và phân công hiện có. Chọn bản ghi phù hợp để tiếp tục; không ghi đè mốc của đợt đang được sử dụng chỉ để làm minh họa.
3. Chọn một chuỗi dữ liệu hợp lệ: một đợt đúng khoa → đề tài do GVHD thuộc phạm vi đề xuất → người quản lý duyệt → nhóm 3–5 tài khoản hợp lệ nhận lời mời → trưởng nhóm đăng ký → GVHD duyệt nhóm → nộp file thật.
4. Với chuỗi có hội đồng, xác nhận đủ 3–5 giảng viên hợp lệ ngoài GVHD chính/phụ; phân công phản biện đúng quy định. Tài liệu tài khoản chưa chứng minh có đủ người cho một chuỗi KLTN đầy đủ.
5. Chấm điểm qua chức năng hiện có theo từng người được phân công; đối chiếu từng phiếu và phép tính. Công bố rồi kiểm tra bằng tài khoản sinh viên đúng nhóm, tài khoản ngoài nhóm và tài khoản khác khoa/đợt.
6. Chỉ kiểm tra gửi email sau khi xác nhận địa chỉ người nhận và cấu hình gửi phù hợp. Giữ bằng chứng thực nhận; không dùng log giả lập làm kết quả đạt.
7. Với nhắc hạn, triển khai phần backend còn thiếu trước, rồi chọn mốc kiểm thử được thống nhất. Không thể chứng minh nhắc hạn bằng việc đổi nhãn hoặc thêm sự kiện trên lịch frontend.

## Mẫu bằng chứng cho mỗi lần chạy thủ công

- Người thực hiện: tài khoản/vai trò, không ghi mật khẩu.
- Đối tượng: ID đợt, đề tài, nhóm, hội đồng/phiếu liên quan.
- Trước thao tác: trạng thái và quan hệ đang có.
- Thao tác: chức năng/đường gọi, thời điểm và múi giờ.
- Sau thao tác: kết quả đọc lại từ backend/Neon trong phiên khác; không chỉ toast hoặc ảnh màn hình.
- Ca bị từ chối: ngoài hạn, sai khoa/đợt, sai người, thiếu sĩ số hoặc xung đột; xác nhận dữ liệu không bị thay đổi.
- Nếu có gửi thông báo: người nhận, ID thông báo, trạng thái gửi/đọc; với email có bằng chứng nhận thực.
- Kết luận: Đạt / Không đạt / Chưa chạy. Không ghi Đạt chỉ từ việc đọc thấy lời gọi `save` hoặc log thành công.

## Kết luận và giới hạn

Nhiều nghiệp vụ đã có xử lý backend và lưu qua repository; thiếu lớn nằm ở xử lý theo thời gian, lưu trạng thái thông báo theo người dùng, và bảo vệ dữ liệu thống nhất giữa các đường gọi. Ưu tiên đóng lỗ hổng trả điểm và gán hội đồng trước, tiếp đó hoàn thiện hạn/thông báo/email, rồi mới bổ sung dữ liệu và thực hiện chuỗi kiểm chứng.

Chưa kiểm tra: dữ liệu/schema/index thực tế trên Neon, cấu hình SMTP đang triển khai, hộp thư người nhận, chạy trình duyệt, gửi request thực, cạnh tranh khi nhiều người ghi đồng thời, toàn bộ quản trị tài khoản và đăng nhập. File báo cáo hiện nằm trên ổ đĩa backend; độ bền lưu trữ khi triển khai chưa được xác minh.
