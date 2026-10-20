# Rà soát dự án quản lý đề tài - 19/10/2026

Dự án quản lý đăng ký đề tài, nhóm sinh viên, báo cáo, hội đồng và điểm qua Spring MVC, Thymeleaf và Neon PostgreSQL. Giả định sử dụng: nhiều sinh viên và giảng viên cùng thao tác trong một khoa/trường, với dữ liệu thật cần được giữ nguyên.

Đã dùng Graphify để lập bản đồ 75 file Java chính (1.532 nút, 4.937 quan hệ), Ponytail Audit để rà soát, Postman API Engineer để đối chiếu các đường API, và PDF để đọc Rubric.pdf. Đây là đánh giá từ mã nguồn; chưa xác nhận bằng chạy ứng dụng hoặc kiểm tra dữ liệu Neon.

## Cần sửa trước khi sử dụng với dữ liệu thật

1. **Có thể đăng nhập tài khoản khác qua chức năng đổi vai trò.**
   - **Đang làm gì:** [AuthController.java:118](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/AuthController.java:118) tìm người dùng theo username rồi lập phiên đăng nhập. [SecurityConfig.java:74](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/config/SecurityConfig.java:74) cho phép truy cập công khai `/auth/**`.
   - **Lỗi:** Không yêu cầu mật khẩu hay kiểm tra quyền của người gọi; người chưa đăng nhập có thể chọn một tài khoản có sẵn, kể cả quản trị.
   - **Sửa nhỏ nhất:** Xóa đường đổi tài khoản và các liên kết đổi tài khoản ở trang đăng nhập/topbar.
   - **Nếu bỏ qua:** Người ngoài có thể truy cập quyền quản trị và thay đổi dữ liệu thật.

2. **Hai mật khẩu cố định được chấp nhận cho mọi tài khoản đang hoạt động.**
   - **Đang làm gì:** [SecurityConfig.java:44](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/config/SecurityConfig.java:44) cung cấp bộ kiểm tra mật khẩu dùng chung; [AuthService.java:104](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/AuthService.java:104) có cùng nhánh dự phòng.
   - **Lỗi:** Khi đối chiếu BCrypt thất bại, mã vẫn chấp nhận `123456` hoặc `admin123` mà không đối chiếu mật khẩu của tài khoản.
   - **Sửa nhỏ nhất:** Dùng BCrypt chuẩn và bỏ nhánh mật khẩu chung; xử lý dữ liệu mật khẩu cũ qua quy trình chuyển đổi/đặt lại có kiểm soát.
   - **Nếu bỏ qua:** Đổi mật khẩu cá nhân không ngăn được người khác đăng nhập bằng mật khẩu chung.

3. **Mật khẩu Neon nằm trong file đang được Git theo dõi.**
   - **Đang làm gì:** [application.properties:7](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/resources/application.properties:7) chứa cấu hình kết nối, bao gồm mật khẩu trực tiếp ở dòng 9.
   - **Lỗi:** Người có quyền đọc mã nguồn hoặc bản sao repository có thể lấy thông tin truy cập cơ sở dữ liệu. Chưa xác minh repository có công khai hay mật khẩu còn hiệu lực.
   - **Sửa nhỏ nhất:** Đổi mật khẩu Neon, chuyển thông tin bí mật sang biến môi trường, và xử lý bản sao/lịch sử đã chứa mật khẩu khi cần.
   - **Nếu bỏ qua:** Dữ liệu thật phụ thuộc vào độ bảo mật của mọi nơi lưu bản sao repository.

4. **Khởi động ứng dụng tự xóa phiếu chấm và đặt lại điểm.**
   - **Đang làm gì:** [CouncilService.java:50](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/CouncilService.java:50) dùng `@PostConstruct` gọi cleanup, xóa evaluations và đặt điểm thành null nếu đề tài không còn nhóm liên kết.
   - **Lỗi:** Việc khởi động đồng thời trở thành thao tác sửa dữ liệu; một đề tài có lịch sử điểm nhưng đã mất liên kết nhóm sẽ bị xóa lịch sử chấm mà không có người duyệt.
   - **Sửa nhỏ nhất:** Bỏ cleanup tự chạy khi khởi động; chỉ sửa dữ liệu bằng SQL/migration được kiểm tra rõ phạm vi.
   - **Nếu bỏ qua:** Restart server có thể gây mất dữ liệu chấm điểm, trái với yêu cầu quản lý dữ liệu của dự án.

5. **Dữ liệu nghiệp vụ giả vẫn được sinh trong mã Java.**
   - **Đang làm gì:** [StudentService.java:93](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/StudentService.java:93) tự tạo hồ sơ với lớp, niên khóa, GPA cố định và chuyên ngành đầu tiên. [AdminService.java:107](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/AdminService.java:107) cũng tự gán các giá trị này, học vị Tiến sĩ và bộ môn đầu tiên; logic lặp lại khi đổi vai trò. [DepartmentHeadService.java:61](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/DepartmentHeadService.java:61) còn dựng khoa mặc định khi không có dữ liệu.
   - **Lỗi:** Thiếu hồ sơ được biến thành dữ liệu có vẻ hợp lệ nhưng không được xác nhận, vi phạm trực tiếp AGENTS.md. Chọn phần tử đầu tiên còn có thể đưa người dùng sang sai khoa/bộ môn.
   - **Sửa nhỏ nhất:** Chỉ đọc hồ sơ hiện có hoặc yêu cầu nhập/xác nhận trường bắt buộc; thiếu dữ liệu thì báo lỗi rõ ràng, không tự điền.
   - **Nếu bỏ qua:** Phân quyền theo khoa, điều kiện đăng ký và báo cáo thống kê có thể dựa trên dữ liệu sai.

6. **Không chọn file vẫn được ghi nhận nộp báo cáo thành công.**
   - **Đang làm gì:** [StudentController.java:520](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/StudentController.java:520) chuyển sang hàm nhận tên file và kích thước cố định nếu request không có file. [StudentService.java:528](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/StudentService.java:528) lưu Submission mà không lưu file thật.
   - **Lỗi:** Nhóm trưởng có thể tạo một bài nộp hợp lệ trên màn hình nhưng đường tải file không tồn tại.
   - **Sửa nhỏ nhất:** Bắt buộc có file thật tại controller/service và bỏ nhánh ghi nhận file giả; cập nhật những test đang gọi nhánh này.
   - **Nếu bỏ qua:** Giảng viên thấy đã nộp nhưng không có báo cáo để đọc hoặc chấm.

7. **Tải báo cáo chưa kiểm tra quyền đối với từng bài nộp.**
   - **Đang làm gì:** [StudentController.java:536](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/StudentController.java:536) và [LecturerController.java:613](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/LecturerController.java:613) lấy Submission theo id rồi trả file.
   - **Lỗi:** Có kiểm tra đăng nhập/vai trò, nhưng không kiểm tra sinh viên có thuộc nhóm nộp bài hoặc giảng viên có được phân công đối với đề tài đó.
   - **Sửa nhỏ nhất:** Kiểm tra liên kết người dùng - nhóm/đề tài trước khi đọc file; trả lỗi khi không có quyền.
   - **Nếu bỏ qua:** Một sinh viên hoặc giảng viên có thể tải báo cáo ngoài phạm vi của mình bằng cách thay id bài nộp.

8. **Giảng viên không được phân công vẫn có thể chấm phản biện.**
   - **Đang làm gì:** [CouncilService.java:474](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/CouncilService.java:474) chỉ chặn người hướng dẫn tự chấm phản biện; [LecturerController.java:494](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/LecturerController.java:494) nhận topicId từ request.
   - **Lỗi:** Chưa kiểm tra người chấm có trùng với `topic.reviewer`; một giảng viên khác không hướng dẫn đề tài vẫn đi qua nhánh này.
   - **Sửa nhỏ nhất:** Đối chiếu lecturerId với giảng viên phản biện được phân công ngay trong service dùng chung.
   - **Nếu bỏ qua:** Điểm phản biện có thể được thêm bởi người không có trách nhiệm chấm.

9. **Điểm nhập vào chưa bị giới hạn tại server.**
    - **Đang làm gì:** [CouncilService.java:439](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/CouncilService.java:439) lưu `score` trực tiếp; [LecturerService.java:281](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/LecturerService.java:281) lưu điểm bài nộp trực tiếp.
    - **Lỗi:** Gửi giá trị như -1 hoặc 100 có thể đi tới lệnh lưu; giới hạn trên ô nhập HTML không bảo vệ request gửi trực tiếp.
    - **Sửa nhỏ nhất:** Kiểm tra điểm hữu hạn, không null và thuộc thang điểm được hệ thống chấp thuận ở cả hai hàm ghi điểm.
    - **Nếu bỏ qua:** Điểm ngoài thang có thể đi vào điểm tổng kết và bản xuất báo cáo.

10. **Một số thao tác của trưởng khoa chưa kiểm tra phạm vi khoa/bộ môn.**
    - **Đang làm gì:** [DepartmentHeadController.java:922](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/DepartmentHeadController.java:922) công bố kết quả theo topicId, chỉ kiểm tra vai trò trước khi gọi service. Các đường assignReviewer và assignTopicToCouncil có cùng dạng truyền id trực tiếp.
    - **Lỗi:** Riêng đường công bố không đối chiếu khoa/bộ môn của người dùng với đề tài; `publishTopicResults` cũng không nhận người dùng hoặc phạm vi được phép.
    - **Sửa nhỏ nhất:** Dùng quy tắc phạm vi đã có trong DepartmentHeadService để kiểm tra đề tài trước mỗi thao tác; áp dụng tương tự cho hội đồng/giảng viên đích.
    - **Nếu bỏ qua:** Trưởng khoa A có thể gửi id đề tài khoa B để thay đổi trạng thái công bố.

11. **Đăng ký đề tài chưa đối chiếu đợt của nhóm với đợt của đề tài.**
    - **Đang làm gì:** [StudentService.java:363](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/StudentService.java:363) nhận nhóm đã được controller tìm từ đợt đang hoạt động và gán đề tài vào nhóm ở dòng 454.
    - **Lỗi:** Không có bước yêu cầu hai periodId bằng nhau hoặc kiểm tra lại phạm vi khoa đối với đề tài đích. `getMyActiveGroup` còn trả về nhóm của đợt khác khi không tìm thấy nhóm trong đợt được yêu cầu.
    - **Sửa nhỏ nhất:** Tìm nhóm đúng đợt của đề tài, trả null nếu không có và xác nhận đợt/khoa trước khi lưu.
    - **Nếu bỏ qua:** Nhóm của đợt A có thể mang đề tài của đợt B, làm sai thống kê và quyền tra cứu theo đợt.

12. **Nhóm dưới 3 sinh viên vẫn có thể đăng ký và được duyệt.**
    - **Đang làm gì:** [StudentService.java:402](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/StudentService.java:402) chỉ kiểm tra số thành viên vượt tối đa; [LecturerService.java:254](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/LecturerService.java:254) đổi trạng thái duyệt mà không kiểm tra sĩ số.
    - **Lỗi:** Một nhóm chỉ có trưởng nhóm có thể qua cả hai bước, trái ràng buộc nhóm 3-5 sinh viên trong rubric. Đường xóa thành viên cũ cũng không áp dụng quy trình xin rút khi nhóm đã được duyệt.
    - **Sửa nhỏ nhất:** Cho phép tạo nhóm nháp ít người, nhưng chặn đăng ký/duyệt khi chưa đủ sĩ số; chuyển đường xóa thành viên sang quy tắc giai đoạn đã có trong GroupRegistrationService.
    - **Nếu bỏ qua:** Nhóm thực hiện đề tài không đáp ứng yêu cầu và có thể bị rút thành viên mà không qua giảng viên.

13. **Đăng nhập Google không kiểm tra state đã lưu.**
    - **Đang làm gì:** [AuthController.java:74](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/AuthController.java:74) tạo state và lưu trong phiên; callback tại dòng 90 nhận state nhưng không đối chiếu trước khi đổi code.
    - **Lỗi:** Callback không được ràng buộc với yêu cầu đăng nhập khởi tạo từ phiên hiện tại.
    - **Sửa nhỏ nhất:** Yêu cầu state khớp giá trị trong phiên, tiêu thụ một lần rồi xóa; không xử lý code nếu state thiếu/sai.
    - **Nếu bỏ qua:** Khi bật Google OAuth, phiên đăng nhập có thể bị gắn với một luồng xác thực không do người dùng hiện tại khởi tạo.

14. **Google OAuth tự tạo tài khoản ngoài phạm vi đã thống nhất.**
    - **Đang làm gì:** [GoogleAuthService.java:133](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/GoogleAuthService.java:133) tự tạo AppUser nếu không tìm thấy email. Cấu hình danh sách domain mặc định đang trống nên không giới hạn domain khi bật Google.
    - **Lỗi:** Một email Google chưa có trong danh sách tài khoản được chấp thuận vẫn có thể được thêm vào Neon, rồi kích hoạt tạo hồ sơ tự điền ở phát hiện 5.
    - **Sửa nhỏ nhất:** Trong phạm vi AGENTS.md hiện tại, chỉ cho đăng nhập Google khi email đã liên kết với tài khoản hoạt động được chấp thuận trong Neon.
    - **Nếu bỏ qua:** Khi cấu hình Google, phạm vi tài khoản sẽ tự mở rộng ngoài danh sách vận hành đã thống nhất.

## Cần hoàn thiện để đáp ứng nghiệp vụ và rubric

15. **Có thể công bố điểm khi hội đồng chưa chấm đủ.**
    - **Đang làm gì:** [CouncilService.java:568](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/CouncilService.java:568) tính điểm hội đồng từ bất kỳ tập phiếu không rỗng nào; `publishTopicResults` không kiểm tra đủ người chấm và `publishAllResults` chỉ xét trạng thái/nhóm/finalScore.
    - **Lỗi:** Ví dụ hội đồng có 3 người nhưng mới 1 người chấm, cùng điểm hướng dẫn, vẫn có thể tạo finalScore theo nhánh hiện tại và được công bố.
    - **Sửa nhỏ nhất:** Xác định các phiếu bắt buộc theo loại đợt và danh sách thành viên thực sự được phân công; dùng cùng điều kiện cho công bố lẻ và hàng loạt.
    - **Nếu bỏ qua:** Sinh viên có thể thấy kết quả mang nhãn chính thức trước khi quy trình chấm hoàn thành.

16. **Chưa có gửi email khi công bố kết quả.**
    - **Đang làm gì:** [CouncilService.java:631](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/service/CouncilService.java:631) và publishAllResults chỉ cập nhật trạng thái công bố trong DB.
    - **Thiếu:** Không thấy phần gửi email, cấu hình mail hoặc lời gọi dịch vụ gửi mail trong nguồn. Rubric yêu cầu gửi email cho từng sinh viên trong nhóm khi công bố kết quả.
    - **Sửa nhỏ nhất:** Bổ sung gửi thông báo tới email thật của thành viên sau khi giao dịch công bố thành công, có ghi nhận lỗi để gửi lại khi cần.
    - **Nếu bỏ qua:** Có trang tra cứu điểm nhưng chưa hoàn thành phần thông báo kết quả trong tiêu chí chức năng nâng cao.

17. **Sinh viên chưa tra cứu được kết quả theo từng đợt đã tham gia.**
    - **Đang làm gì:** [StudentController.java:567](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/StudentController.java:567) chỉ lấy một nhóm qua `getMyActiveGroup(student, null)`; giao diện results không có lựa chọn đợt.
    - **Thiếu:** Sinh viên tham gia nhiều đợt không có cách xem lại một đợt cũ cụ thể từ trang kết quả.
    - **Sửa nhỏ nhất:** Thêm bộ chọn chỉ gồm các đợt sinh viên thực sự tham gia, truy vấn nhóm đúng periodId và chặn các đợt ngoài danh sách đó.
    - **Nếu bỏ qua:** Chưa đáp ứng tiêu chí tra cứu điểm theo đợt đăng ký; kết quả cũ bị che bởi nhóm đang hoạt động.

18. **Quản trị hồ sơ chuyên môn GV/SV chưa đầy đủ.**
    - **Đang làm gì:** [AdminController.java:161](C:/Users/Laptop/Documents/GitHub/Java-project/src/main/java/topicmanagement/controller/AdminController.java:161) sửa tài khoản chỉ nhận tên, email, điện thoại, vai trò và trạng thái; phần nhân sự trong DepartmentHeadController chủ yếu là xem danh sách. Bản cập nhật mới đã có thêm/sửa/khóa bộ môn nên không còn tính bộ môn là phần thiếu.
    - **Thiếu:** Chưa thấy cập nhật các trường hồ sơ thật như chuyên ngành/lớp/GPA của SV và bộ môn/học vị của GV. Mã hiện tại bù bằng tự gán dữ liệu tại phát hiện hồ sơ giả.
    - **Sửa nhỏ nhất:** Hoàn thiện các form/service đang có để quản lý dữ liệu hồ sơ thật; không thêm seeder hay tài khoản mới để bù dữ liệu.
    - **Nếu bỏ qua:** Quản trị tài khoản chưa đủ thay cho quản trị hồ sơ GV/SV theo rubric, và khó sửa phân công khoa/bộ môn sai từ giao diện.

19. **Chưa thấy bộ tài liệu nộp đồ án trong repository.**
    - **Đang làm gì:** Repo hiện có README, hướng dẫn tài khoản và [sơ đồ ERD dạng HTML](C:/Users/Laptop/Documents/GitHub/Java-project/docs/database_er_diagram.html). Chưa tìm thấy báo cáo Word, sơ đồ Use-case hoặc đặc tả FR/NFR trong các file được rà soát.
    - **Thiếu:** Rubric yêu cầu báo cáo Word, phân chia công việc, hướng dẫn cài đặt có minh chứng, Use-case đầy đủ vai trò và ERD mô tả bảng/cột. Đây là thiếu trong repo được kiểm tra, không khẳng định nhóm chưa làm ở nơi khác.
    - **Sửa nhỏ nhất:** Gom tài liệu hiện có vào bộ nộp, bổ sung phần thiếu, kiểm tra sơ đồ khớp schema thực tế và đóng gói theo tên ZIP rubric quy định.
    - **Nếu bỏ qua:** Dù mã chạy được, nhóm vẫn có thể mất điểm phần báo cáo và thiết kế. Link deploy được ghi là không bắt buộc trong quy định nộp bài, nhưng có mặt ở mô tả các mức đánh giá chức năng; nên xác nhận cách áp dụng với giảng viên.

20. **Thiếu test kiểm tra nghiệp vụ và phân quyền.**
    - **Đang làm gì:** Sau cập nhật mới, [TopicManagementApplicationTests.java:6](C:/Users/Laptop/Documents/GitHub/Java-project/src/test/java/topicmanagement/TopicManagementApplicationTests.java:6) là file test duy nhất, chỉ có `contextLoads()`.
    - **Thiếu:** Test này không chứng minh đăng nhập sai bị chặn, quyền tải/chấm điểm đúng, nhóm đủ sĩ số hoặc đăng ký đúng đợt. Những test cũ có thao tác sửa schema đã bị xóa khỏi snapshot hiện tại, nên không còn được tính là lỗi đang tồn tại.
    - **Sửa nhỏ nhất:** Thêm các test hồi quy tập trung vào lỗi thật ở trên; test tích hợp dùng một Neon branch riêng được chỉ định và chỉ dữ liệu/tài khoản được chấp thuận. Không đưa tài khoản giả hoặc seeder vào mã nguồn ứng dụng.
    - **Nếu bỏ qua:** Sửa lỗi bảo mật/nghiệp vụ có thể bị làm hỏng lại mà lần chạy test sau vẫn báo thành công.

## Kết quả kiểm tra và giới hạn

- Đã lập bản đồ toàn bộ nguồn Java chính; graph không có đầu mút quan hệ bị thiếu theo kiểm tra của Graphify. Đọc sâu xác thực, dữ liệu người dùng, nhóm/đăng ký, bài nộp/tải file, chấm điểm/công bố và các test liên quan; đối chiếu đủ 4 trang Rubric.pdf.
- Mình không sửa mã nguồn hay dữ liệu. Repo được cập nhật bởi tác nhân khác trong lúc rà soát; đã đọc lại các phần bị ảnh hưởng tại commit `983c911e46bf2e30c8cd86d4ec11e7be12701183`, loại phát hiện về test sửa schema và ghi nhận bộ môn/ERD đã được bổ sung. Git working tree cuối lượt kiểm tra sạch; báo cáo và bản đồ nằm ngoài repository.
- Trước khi repo cập nhật, đã thử chạy riêng `AiAssistantServiceTest` ở chế độ Maven offline; wrapper dừng trước build/test vì bị từ chối quyền tạo `C:\Users\Laptop\.m2`. File test đó hiện không còn; không có kết quả test pass/fail hay build thành công cho snapshot cuối.
- Không chạy ứng dụng hoặc test SpringBootTest vì startup có thao tác sửa/xóa dữ liệu như phát hiện cleanup khi khởi động. Không kết nối DB, không thêm tài khoản và không sửa dữ liệu Neon.
- Chưa kiểm tra schema/ràng buộc thực tế trên Neon, tình trạng mật khẩu/backup, website đã deploy, gửi mail/Google trực tiếp, giao diện trình duyệt hoặc tải đồng thời. Vì chưa đọc schema Neon, chưa kết luận tính duy nhất khi nhiều nhóm đăng ký cùng lúc được DB bảo vệ hay chưa.
- Không audit CVE của thư viện, toàn bộ CSS/JavaScript, chi tiết định dạng Excel/PDF hoặc mọi hàm trong các lớp lớn. Không chấm điểm tổng đồ án từ kiểm tra tĩnh.
- Đồ thị được tạo bằng AST tại máy, không dùng gọi LLM để trích xuất tài liệu: chi phí token LLM của bước tạo graph là 0; con số này không bao gồm cuộc hội thoại rà soát.

**Kết luận:** Có khung nghiệp vụ và phân tầng rõ ràng, nhưng cần đóng đường đăng nhập không mật khẩu và bảo vệ dữ liệu Neon trước; tiếp theo sửa hồ sơ/file giả, phân quyền theo bản ghi, quy tắc nhóm/điểm và các phần thiếu theo rubric.

**Không kiểm tra:** Chạy thật, dữ liệu Neon và các phần giới hạn nêu trên. Rủi ro lớn nhất hiện tại là chiếm quyền tài khoản và thay đổi/mất dữ liệu thật.
