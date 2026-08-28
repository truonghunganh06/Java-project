package topic_management.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DataSeederService — Seed dữ liệu đề tài mẫu đa khoa.
 * Chỉ thêm khi đề tài có code đó chưa tồn tại trong DB.
 * Mục đích: hỗ trợ test chức năng lọc đề tài theo khoa và phân quyền.
 */
@Service
@Order(10) // Chạy sau DataCleanupService
public class DataSeederService {

    private final TopicRepository topicRepository;
    private final MajorRepository majorRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final LecturerRepository lecturerRepository;

    public DataSeederService(
            TopicRepository topicRepository,
            MajorRepository majorRepository,
            DepartmentRepository departmentRepository,
            RegistrationPeriodRepository periodRepository,
            LecturerRepository lecturerRepository) {
        this.topicRepository = topicRepository;
        this.majorRepository = majorRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.lecturerRepository = lecturerRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedSampleTopics() {
        try {
            List<Department> allDepts = departmentRepository.findAll();
            if (allDepts.isEmpty()) {
                System.out.println("[SEEDER] Chưa có khoa nào trong DB, bỏ qua seed đề tài.");
                return;
            }

            List<RegistrationPeriod> activePeriods = periodRepository.findByIsActiveTrueOrderByIdDesc();
            if (activePeriods.isEmpty()) {
                System.out.println("[SEEDER] Chưa có đợt đăng ký nào đang mở, bỏ qua seed đề tài.");
                return;
            }

            // Lấy danh sách giảng viên (dùng làm GVHD)
            List<Lecturer> allLecturers = lecturerRepository.findAll();
            Lecturer defaultLecturer = allLecturers.isEmpty() ? null : allLecturers.get(0);

            int seededCount = 0;

            // ======================================================================
            // SEED ĐỀ TÀI CHO TỪNG KHOA
            // ======================================================================
            for (Department dept : allDepts) {
                if (!Boolean.TRUE.equals(dept.getIsActive())) continue;

                // Tìm major thuộc khoa này
                List<Major> deptMajors = majorRepository.findAll().stream()
                        .filter(m -> m.getDepartment() != null && dept.getId().equals(m.getDepartment().getId()))
                        .toList();
                if (deptMajors.isEmpty()) continue;

                // Tìm đợt đăng ký phù hợp với khoa này
                RegistrationPeriod period = activePeriods.stream()
                        .filter(p -> p.getDepartment() != null && dept.getId().equals(p.getDepartment().getId()))
                        .findFirst()
                        .orElse(null);
                // Nếu không có đợt riêng cho khoa, thử dùng đợt chung (không gán khoa)
                if (period == null) {
                    period = activePeriods.stream()
                            .filter(p -> p.getDepartment() == null)
                            .findFirst()
                            .orElse(activePeriods.get(0));
                }

                // Tìm giảng viên thuộc khoa này nếu có
                final Department deptFinal = dept;
                Lecturer deptLecturer = allLecturers.stream()
                        .filter(l -> l.getDepartment() != null && deptFinal.getId().equals(l.getDepartment().getId()))
                        .findFirst()
                        .orElse(defaultLecturer);

                Major major = deptMajors.get(0);
                String deptCode = dept.getCode() != null ? dept.getCode().toUpperCase() : "DEPT" + dept.getId();

                // Danh sách đề tài mẫu theo loại khoa
                String[][] sampleTopics = buildSampleTopics(deptCode, dept.getName());

                for (String[] topicData : sampleTopics) {
                    String code = topicData[0];
                    String title = topicData[1];
                    String description = topicData[2];
                    String requirements = topicData[3];

                    // Tránh seed trùng
                    if (topicRepository.existsByCode(code)) continue;

                    Topic t = new Topic();
                    t.setCode(code);
                    t.setTitle(title);
                    t.setDescription(description);
                    t.setRequirements(requirements);
                    t.setMajor(major);
                    t.setPeriod(period);
                    t.setSupervisor(deptLecturer);
                    t.setStatus("APPROVED");
                    t.setMaxStudents(3);
                    t.setAcademicYear("2026-2027");
                    t.setSemester(1);
                    t.setCreatedAt(OffsetDateTime.now());
                    topicRepository.save(t);
                    seededCount++;
                }
            }

            if (seededCount > 0) {
                System.out.println("[SEEDER] Đã seed thành công " + seededCount + " đề tài mẫu đa khoa vào database.");
            } else {
                System.out.println("[SEEDER] Không có đề tài mới cần seed (tất cả đã tồn tại hoặc chưa đủ điều kiện).");
            }

        } catch (Exception e) {
            System.err.println("[SEEDER-WARN] Lỗi khi seed dữ liệu đề tài: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tạo danh sách đề tài mẫu tùy theo mã khoa.
     * Trả về mảng [code, title, description, requirements].
     */
    private String[][] buildSampleTopics(String deptCode, String deptName) {
        // Đề tài CNTT
        if (deptCode.contains("CNTT") || deptCode.contains("IT") || deptCode.contains("CS")) {
            return new String[][] {
                {
                    "CNTT-SEED-01",
                    "Xây dựng hệ thống quản lý kho thông minh sử dụng IoT và Machine Learning",
                    "Nghiên cứu và phát triển hệ thống quản lý kho hàng tích hợp cảm biến IoT để theo dõi hàng tồn kho theo thời gian thực, kết hợp thuật toán dự báo nhu cầu bằng Machine Learning nhằm tối ưu hóa quy trình nhập - xuất - lưu kho.",
                    "Python / TensorFlow hoặc PyTorch cho ML module.\nRaspberry Pi hoặc ESP32 cho IoT.\nReact.js + Spring Boot cho giao diện web.\nCơ sở dữ liệu: PostgreSQL hoặc MongoDB."
                },
                {
                    "CNTT-SEED-02",
                    "Phát triển ứng dụng di động hỗ trợ học tiếng Anh theo phương pháp Spaced Repetition",
                    "Xây dựng ứng dụng mobile (Android/iOS) áp dụng thuật toán Spaced Repetition System (SRS) kết hợp xử lý ngôn ngữ tự nhiên (NLP) để cá nhân hóa lộ trình học từ vựng và ngữ pháp tiếng Anh cho người dùng.",
                    "Flutter hoặc React Native cho mobile.\nNode.js / Spring Boot cho backend API.\nThuật toán SRS (SM-2 hoặc tùy chỉnh).\nFirebase hoặc AWS cho cloud storage."
                },
                {
                    "CNTT-SEED-03",
                    "Nghiên cứu và triển khai hệ thống phát hiện xâm nhập mạng dùng Deep Learning",
                    "Phân tích và xây dựng mô hình Deep Learning (LSTM, CNN hoặc Transformer) để phát hiện các hành vi xâm nhập và tấn công mạng trong thời gian thực, áp dụng trên bộ dữ liệu chuẩn NSL-KDD, CICIDS2017.",
                    "Python: TensorFlow/Keras hoặc PyTorch.\nScapy, Wireshark để phân tích gói tin.\nELK Stack (Elasticsearch, Logstash, Kibana) để trực quan hóa.\nKiến thức về mạng máy tính và bảo mật."
                },
                {
                    "CNTT-SEED-04",
                    "Thiết kế và xây dựng nền tảng E-Learning hỗ trợ học tập cộng tác theo mô hình Microservices",
                    "Xây dựng hệ thống E-Learning đa người dùng với kiến trúc Microservices, tích hợp tính năng học nhóm theo thời gian thực (collaborative learning), theo dõi tiến độ học tập và đánh giá tự động.",
                    "Spring Boot / Spring Cloud cho Microservices.\nReact.js cho Frontend.\nWebSocket / Socket.IO cho tính năng real-time.\nDocker + Kubernetes để triển khai.\nMySQL / PostgreSQL + Redis."
                },
                {
                    "CNTT-SEED-05",
                    "Ứng dụng Computer Vision nhận dạng cảm xúc khuôn mặt trong lớp học trực tuyến",
                    "Nghiên cứu và phát triển mô hình nhận dạng cảm xúc khuôn mặt (Facial Expression Recognition) áp dụng vào hệ thống học trực tuyến để theo dõi mức độ tập trung và cảm xúc của học viên, hỗ trợ giảng viên điều chỉnh phương pháp giảng dạy.",
                    "OpenCV + MediaPipe cho xử lý hình ảnh.\nPyTorch / TensorFlow cho mô hình FER.\nFlask hoặc FastAPI cho REST API.\nReact.js cho dashboard theo dõi.\nWebRTC cho luồng video học trực tuyến."
                }
            };
        }

        // Đề tài Quản trị Kinh doanh / Kinh tế
        if (deptCode.contains("QTKD") || deptCode.contains("KT") || deptCode.contains("KINH") || deptCode.contains("BUS")) {
            return new String[][] {
                {
                    "QTKD-SEED-01",
                    "Phân tích tác động của chiến lược Digital Marketing đến hành vi mua hàng trực tuyến của thế hệ Gen Z tại Việt Nam",
                    "Nghiên cứu định lượng về ảnh hưởng của các kênh Digital Marketing (Social Media, Influencer Marketing, SEO/SEM) đến quyết định và hành vi mua hàng trực tuyến của người tiêu dùng thế hệ Gen Z (sinh năm 1997-2012) tại thị trường Việt Nam.",
                    "Phương pháp nghiên cứu định lượng + định tính.\nKhảo sát 300+ mẫu, phân tích SPSS/SmartPLS.\nMô hình TAM (Technology Acceptance Model).\nPhỏng vấn sâu 10-15 chuyên gia và người tiêu dùng."
                },
                {
                    "QTKD-SEED-02",
                    "Xây dựng mô hình đánh giá rủi ro tín dụng cho doanh nghiệp vừa và nhỏ (SME) tại ngân hàng thương mại cổ phần",
                    "Nghiên cứu và đề xuất mô hình đánh giá rủi ro tín dụng phù hợp với đặc thù của các doanh nghiệp vừa và nhỏ Việt Nam, kết hợp dữ liệu tài chính truyền thống và chỉ số phi tài chính, áp dụng các kỹ thuật phân tích thống kê và Machine Learning.",
                    "Phân tích hồi quy logistic, Random Forest, XGBoost.\nDữ liệu tài chính doanh nghiệp (báo cáo tài chính 3 năm).\nPhần mềm R / Python + SPSS.\nNghiên cứu tình huống tại 2-3 ngân hàng cổ phần."
                },
                {
                    "QTKD-SEED-03",
                    "Nghiên cứu các nhân tố ảnh hưởng đến sự hài lòng và gắn kết của nhân viên trong doanh nghiệp công nghệ tại Việt Nam",
                    "Phân tích và đánh giá các yếu tố tác động đến mức độ hài lòng (job satisfaction) và gắn kết tổ chức (organizational commitment) của nhân viên ngành công nghệ thông tin tại các doanh nghiệp Việt Nam, đề xuất chính sách nhân sự phù hợp.",
                    "Khảo sát định lượng 250+ nhân viên IT.\nMô hình Herzberg + OCP (Organizational Culture Profile).\nPhân tích nhân tố khám phá (EFA) và SEM.\nPhỏng vấn sâu HR managers tại 5+ doanh nghiệp."
                },
                {
                    "QTKD-SEED-04",
                    "Chiến lược phát triển thương hiệu xanh (Green Brand) cho doanh nghiệp sản xuất đồ uống nước giải khát Việt Nam",
                    "Đề xuất chiến lược xây dựng và phát triển thương hiệu xanh cho các doanh nghiệp sản xuất đồ uống Việt Nam trong bối cảnh người tiêu dùng ngày càng quan tâm đến bảo vệ môi trường và phát triển bền vững.",
                    "Phân tích SWOT + PEST + Porter 5 Forces.\nNghiên cứu so sánh với 3-5 thương hiệu xanh quốc tế thành công.\nKhảo sát 200+ người tiêu dùng về thái độ với sản phẩm xanh.\nPhỏng vấn ban lãnh đạo 2-3 doanh nghiệp đồ uống Việt Nam."
                },
                {
                    "QTKD-SEED-05",
                    "Ứng dụng mô hình Balanced Scorecard trong đánh giá hiệu quả hoạt động của các đơn vị hành chính sự nghiệp",
                    "Nghiên cứu khả năng ứng dụng và đề xuất khung BSC (Balanced Scorecard) được điều chỉnh phù hợp với đặc thù của các đơn vị hành chính sự nghiệp công lập tại Việt Nam, nhằm nâng cao hiệu quả quản lý và minh bạch trong hoạt động.",
                    "Nghiên cứu tài liệu và tổng quan lý thuyết BSC.\nPhân tích tình huống 2-3 đơn vị thí điểm.\nPhỏng vấn 15-20 lãnh đạo và chuyên gia quản lý công.\nPhân tích thống kê so sánh trước-sau áp dụng."
                }
            };
        }

        // Đề tài chung cho các khoa khác (Kỹ thuật, Ngoại ngữ, v.v.)
        return new String[][] {
            {
                "SEED-GEN-01-" + deptCode,
                "[" + deptName + "] Nghiên cứu ứng dụng công nghệ mới trong lĩnh vực " + deptName,
                "Đề tài nghiên cứu, khảo sát và đề xuất giải pháp ứng dụng công nghệ mới nhằm nâng cao hiệu quả hoạt động trong lĩnh vực " + deptName + " tại Việt Nam.",
                "Phương pháp nghiên cứu định tính và định lượng.\nKhảo sát, phỏng vấn chuyên gia trong ngành.\nPhân tích dữ liệu thực tế tại các tổ chức điển hình."
            },
            {
                "SEED-GEN-02-" + deptCode,
                "[" + deptName + "] Đánh giá thực trạng và đề xuất giải pháp phát triển bền vững trong ngành " + deptName,
                "Phân tích thực trạng phát triển, xác định điểm mạnh, điểm yếu và đề xuất các giải pháp phát triển bền vững cho lĩnh vực " + deptName + " trong bối cảnh hội nhập quốc tế.",
                "Thu thập và phân tích dữ liệu thứ cấp từ các báo cáo ngành.\nKhảo sát 100-200 đối tượng liên quan.\nPhân tích SWOT và đề xuất chiến lược phát triển."
            }
        };
    }
}
