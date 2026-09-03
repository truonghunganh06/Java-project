package topic_management.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * DataSeederService — Khởi tạo tự động bộ dữ liệu mẫu (Seed Data) chuẩn V2.0
 * bao gồm: Roles (có DIVISION_HEAD), Khoa (Departments), Bộ môn (Divisions),
 * Chuyên ngành (Majors), Đợt đăng ký, Tài khoản các vai trò
 * (Admin, Trưởng bộ môn, Giảng viên theo bộ môn, Sinh viên theo bộ môn),
 * và các đề tài mẫu liên kết trực tiếp với Giảng viên & Bộ môn.
 */
@Service
@DependsOn("databaseMigrationService")
@Order(5)
public class DataSeederService {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DivisionRepository divisionRepository;
    private final MajorRepository majorRepository;
    private final UserRepository userRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataSeederService(
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            DivisionRepository divisionRepository,
            MajorRepository majorRepository,
            UserRepository userRepository,
            LecturerRepository lecturerRepository,
            StudentRepository studentRepository,
            RegistrationPeriodRepository periodRepository,
            TopicRepository topicRepository) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.divisionRepository = divisionRepository;
        this.majorRepository = majorRepository;
        this.userRepository = userRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.periodRepository = periodRepository;
        this.topicRepository = topicRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedAllTestData() {
        try {
            System.out.println("\n================================================================================");
            System.out.println("  [DATA-SEEDER] KHỞI TẠO BỘ DỮ LIỆU VÀ TÀI KHOẢN TEST ĐẦY ĐỦ CÁC VAI TRÒ (V2.0)");
            System.out.println("================================================================================");

            // 1. Khởi tạo / Kiểm tra Roles
            Map<String, Role> roleMap = seedRoles();

            // 2. Khởi tạo / Kiểm tra Khoa (Departments)
            Map<String, Department> deptMap = seedDepartments();

            // 3. Khởi tạo / Kiểm tra Bộ môn (Divisions)
            Map<String, Division> divMap = seedDivisions(deptMap);

            // 4. Khởi tạo / Kiểm tra Chuyên ngành (Majors)
            Map<String, Major> majorMap = seedMajors(deptMap, divMap);

            // 5. Khởi tạo / Kiểm tra Đợt đăng ký (Registration Periods) cho từng khoa
            Map<String, RegistrationPeriod> periodMap = seedPeriods(deptMap);

            // 6. Khởi tạo Tài khoản Quản trị viên (ADMIN)
            seedAdminUser(roleMap.get("ADMIN"));

            // 7. Khởi tạo Tài khoản Trưởng bộ môn / Trưởng khoa (DIVISION_HEAD & DEPARTMENT_HEAD)
            Role headRole = roleMap.get("DIVISION_HEAD") != null ? roleMap.get("DIVISION_HEAD") : roleMap.get("DEPARTMENT_HEAD");
            seedDepartmentHeads(headRole, deptMap, divMap);

            // 8. Khởi tạo Tài khoản Giảng viên (LECTURER) theo từng Bộ môn
            Map<String, Lecturer> lecturerMap = seedLecturers(roleMap.get("LECTURER"), deptMap, divMap);

            // 9. Khởi tạo Tài khoản Sinh viên (STUDENT) theo từng Bộ môn & Chuyên ngành
            seedStudents(roleMap.get("STUDENT"), majorMap, divMap);

            // 10. Khởi tạo Đề tài mẫu gán cho Giảng viên, Bộ môn và Đợt
            seedSampleTopics(deptMap, divMap, majorMap, periodMap, lecturerMap);

            // 11. In bảng danh sách tài khoản test ra console
            printTestAccountsSummary();

        } catch (Exception e) {
            System.err.println("[DATA-SEEDER-ERROR] Đã xảy ra lỗi khi khởi tạo dữ liệu mẫu: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    // 1. ROLES
    // =========================================================================
    private Map<String, Role> seedRoles() {
        Map<String, Role> map = new HashMap<>();
        String[][] rolesData = {
                {"ADMIN", "Quản trị viên hệ thống"},
                {"DEPARTMENT_HEAD", "Trưởng khoa / Trưởng bộ môn"},
                {"DIVISION_HEAD", "Trưởng bộ môn"},
                {"LECTURER", "Giảng viên"},
                {"STUDENT", "Sinh viên"}
        };

        for (String[] r : rolesData) {
            String code = r[0];
            String name = r[1];
            Role role = roleRepository.findByCode(code).orElseGet(() -> {
                Role newRole = new Role();
                newRole.setCode(code);
                newRole.setName(name);
                return roleRepository.save(newRole);
            });
            map.put(code, role);
        }
        return map;
    }

    // =========================================================================
    // 2. DEPARTMENTS
    // =========================================================================
    private Map<String, Department> seedDepartments() {
        Map<String, Department> map = new HashMap<>();
        String[][] deptsData = {
                {"CNTT", "Khoa Công nghệ Thông tin", "Khoa đào tạo các chuyên ngành Công nghệ Thông tin, Kỹ thuật Phần mềm, Khoa học Máy tính."},
                {"QTKD", "Khoa Quản trị Kinh doanh", "Khoa đào tạo các chuyên ngành Quản trị Kinh doanh, Marketing, Kinh doanh Quốc tế."}
        };

        for (String[] d : deptsData) {
            String code = d[0];
            String name = d[1];
            String desc = d[2];
            Department dept = departmentRepository.findByCode(code).orElseGet(() -> {
                Department newDept = new Department();
                newDept.setCode(code);
                newDept.setName(name);
                newDept.setDescription(desc);
                newDept.setIsActive(true);
                return departmentRepository.save(newDept);
            });
            map.put(code, dept);
        }

        for (Department existing : departmentRepository.findAll()) {
            if (existing.getCode() != null && !map.containsKey(existing.getCode())) {
                map.put(existing.getCode(), existing);
            }
        }
        return map;
    }

    // =========================================================================
    // 3. DIVISIONS (BỘ MÔN)
    // =========================================================================
    private Map<String, Division> seedDivisions(Map<String, Department> deptMap) {
        Map<String, Division> map = new HashMap<>();

        Department cnttDept = deptMap.get("CNTT");
        if (cnttDept != null) {
            createDivisionIfNotExist(map, "KTPM", "Bộ môn Kỹ thuật Phần mềm", "Bộ môn phụ trách đào tạo và nghiên cứu chuyên sâu về Kỹ thuật Phần mềm và Hệ thống.", cnttDept);
            createDivisionIfNotExist(map, "KHMT_HTTT", "Bộ môn Khoa học Máy tính & HTTT", "Bộ môn phụ trách đào tạo về AI, Khoa học dữ liệu, Hệ thống Thông tin và Mạng.", cnttDept);
        }

        Department qtkdDept = deptMap.get("QTKD");
        if (qtkdDept != null) {
            createDivisionIfNotExist(map, "QTKD_DN", "Bộ môn Quản trị Doanh nghiệp", "Bộ môn phụ trách đào tạo Quản trị Kinh doanh và Tài chính Doanh nghiệp.", qtkdDept);
            createDivisionIfNotExist(map, "MKT_TMDT", "Bộ môn Marketing & TMĐT", "Bộ môn phụ trách Marketing số và Thương mại Điện tử.", qtkdDept);
        }

        for (Division existing : divisionRepository.findAll()) {
            if (existing.getCode() != null && !map.containsKey(existing.getCode())) {
                map.put(existing.getCode(), existing);
            }
        }
        return map;
    }

    private void createDivisionIfNotExist(Map<String, Division> map, String code, String name, String desc, Department dept) {
        Division div = divisionRepository.findByCode(code).orElseGet(() -> {
            Division d = new Division();
            d.setCode(code);
            d.setName(name);
            d.setDescription(desc);
            d.setDepartment(dept);
            d.setIsActive(true);
            return divisionRepository.save(d);
        });
        if (div.getDepartment() == null && dept != null) {
            div.setDepartment(dept);
            div = divisionRepository.save(div);
        }
        map.put(code, div);
    }

    // =========================================================================
    // 4. MAJORS
    // =========================================================================
    private Map<String, Major> seedMajors(Map<String, Department> deptMap, Map<String, Division> divMap) {
        Map<String, Major> map = new HashMap<>();

        Department cnttDept = deptMap.get("CNTT");
        Division divKtpm = divMap.get("KTPM");
        Division divKhmt = divMap.get("KHMT_HTTT");

        if (cnttDept != null) {
            createMajorIfNotExist(map, "KTPM", "Kỹ thuật Phần mềm", cnttDept, divKtpm);
            createMajorIfNotExist(map, "CNTT", "Công nghệ Thông tin", cnttDept, divKhmt);
        }

        Department qtkdDept = deptMap.get("QTKD");
        Division divQtkd = divMap.get("QTKD_DN");
        Division divMkt = divMap.get("MKT_TMDT");

        if (qtkdDept != null) {
            createMajorIfNotExist(map, "QTKD", "Quản trị Kinh doanh", qtkdDept, divQtkd);
            createMajorIfNotExist(map, "MKT", "Marketing & Truyền thông", qtkdDept, divMkt);
        }

        for (Major existing : majorRepository.findAll()) {
            if (existing.getCode() != null && !map.containsKey(existing.getCode())) {
                map.put(existing.getCode(), existing);
            }
        }
        return map;
    }

    private void createMajorIfNotExist(Map<String, Major> map, String code, String name, Department dept, Division div) {
        Major major = majorRepository.findByCode(code).orElseGet(() -> {
            Major m = new Major();
            m.setCode(code);
            m.setName(name);
            m.setDepartment(dept);
            m.setDivision(div);
            m.setIsActive(true);
            return majorRepository.save(m);
        });
        if (major.getDivision() == null && div != null) {
            major.setDivision(div);
            major = majorRepository.save(major);
        }
        map.put(code, major);
    }

    // =========================================================================
    // 5. REGISTRATION PERIODS
    // =========================================================================
    private Map<String, RegistrationPeriod> seedPeriods(Map<String, Department> deptMap) {
        Map<String, RegistrationPeriod> map = new HashMap<>();
        OffsetDateTime now = OffsetDateTime.now();

        Department cnttDept = deptMap.get("CNTT");
        if (cnttDept != null) {
            RegistrationPeriod pCntt = findOrCreatePeriod(
                    "Đợt Khóa luận Tốt nghiệp CNTT 2026-2027",
                    "KLTN",
                    cnttDept,
                    now.minusDays(15),
                    now.plusDays(30),
                    now.minusDays(10),
                    now.plusDays(30)
            );
            map.put("CNTT_KLTN", pCntt);
        }

        Department qtkdDept = deptMap.get("QTKD");
        if (qtkdDept != null) {
            RegistrationPeriod pQtkd = findOrCreatePeriod(
                    "Đợt Khóa luận Tốt nghiệp QTKD 2026-2027",
                    "KLTN",
                    qtkdDept,
                    now.minusDays(15),
                    now.plusDays(30),
                    now.minusDays(10),
                    now.plusDays(30)
            );
            map.put("QTKD_KLTN", pQtkd);
        }

        return map;
    }

    private RegistrationPeriod findOrCreatePeriod(
            String name, String type, Department dept,
            OffsetDateTime propStart, OffsetDateTime propEnd,
            OffsetDateTime regStart, OffsetDateTime regEnd) {

        List<RegistrationPeriod> existing = periodRepository.findByDepartmentId(dept.getId());
        for (RegistrationPeriod p : existing) {
            if (type.equalsIgnoreCase(p.getPeriodType()) && Boolean.TRUE.equals(p.getIsActive())) {
                p.setStatus("OPEN");
                p.setLecturerProposalStart(propStart);
                p.setLecturerProposalEnd(propEnd);
                p.setStudentRegistrationStart(regStart);
                p.setStudentRegistrationEnd(regEnd);
                return periodRepository.save(p);
            }
        }

        RegistrationPeriod p = new RegistrationPeriod();
        p.setName(name);
        p.setPeriodType(type);
        p.setDepartment(dept);
        p.setAcademicYear("2026-2027");
        p.setSemester(1);
        p.setStatus("OPEN");
        p.setIsActive(true);
        p.setLecturerProposalStart(propStart);
        p.setLecturerProposalEnd(propEnd);
        p.setStudentRegistrationStart(regStart);
        p.setStudentRegistrationEnd(regEnd);
        p.setReviewerDeadline(regEnd.plusDays(10));
        p.setCouncilDefenseDate(regEnd.plusDays(20));
        p.setCreatedAt(OffsetDateTime.now());
        return periodRepository.save(p);
    }

    // =========================================================================
    // 6. ADMIN USER
    // =========================================================================
    private void seedAdminUser(Role adminRole) {
        if (adminRole == null) return;
        findOrCreateUser(
                "admin",
                "admin123",
                "admin@system.edu.vn",
                "Quản Trị Viên Hệ Thống",
                "0901234567",
                adminRole
        );
    }

    // =========================================================================
    // 7. DIVISION / DEPARTMENT HEADS (CNTT & QTKD)
    // =========================================================================
    private void seedDepartmentHeads(Role headRole, Map<String, Department> deptMap, Map<String, Division> divMap) {
        if (headRole == null) return;

        Department cnttDept = deptMap.get("CNTT");
        Division divKtpm = divMap.get("KTPM");
        if (cnttDept != null) {
            AppUser headCnttUser = findOrCreateUser(
                    "head_cntt",
                    "123456",
                    "head.cntt@university.edu.vn",
                    "TS. Nguyễn Văn Thắng",
                    "0912345601",
                    headRole
            );
            Lecturer headLecturer = findOrCreateLecturer(headCnttUser, cnttDept, divKtpm, "GV_HEAD_CNTT", "TS", "Tiến sĩ");
            if (divKtpm != null && divKtpm.getHeadLecturer() == null) {
                divKtpm.setHeadLecturer(headLecturer);
                divisionRepository.save(divKtpm);
            }
        }

        Department qtkdDept = deptMap.get("QTKD");
        Division divQtkd = divMap.get("QTKD_DN");
        if (qtkdDept != null) {
            AppUser headQtkdUser = findOrCreateUser(
                    "head_qtkd",
                    "123456",
                    "head.qtkd@university.edu.vn",
                    "PGS.TS. Trần Thị Mai",
                    "0912345602",
                    headRole
            );
            Lecturer headLecturer = findOrCreateLecturer(headQtkdUser, qtkdDept, divQtkd, "GV_HEAD_QTKD", "PGS.TS", "Tiến sĩ");
            if (divQtkd != null && divQtkd.getHeadLecturer() == null) {
                divQtkd.setHeadLecturer(headLecturer);
                divisionRepository.save(divQtkd);
            }
        }
    }

    // =========================================================================
    // 8. LECTURERS (CNTT & QTKD)
    // =========================================================================
    private Map<String, Lecturer> seedLecturers(Role lecturerRole, Map<String, Department> deptMap, Map<String, Division> divMap) {
        Map<String, Lecturer> map = new HashMap<>();
        if (lecturerRole == null) return map;

        Department cnttDept = deptMap.get("CNTT");
        Division divKtpm = divMap.get("KTPM");
        Division divKhmt = divMap.get("KHMT_HTTT");

        if (cnttDept != null) {
            // Giảng viên CNTT 1 (Bộ môn KTPM)
            AppUser gv1User = findOrCreateUser(
                    "gv_cntt01",
                    "123456",
                    "long.lh@university.edu.vn",
                    "ThS. Lê Hoàng Long",
                    "0912345611",
                    lecturerRole
            );
            Lecturer lec1 = findOrCreateLecturer(gv1User, cnttDept, divKtpm, "GV_IT_01", "ThS", "Thạc sĩ");
            map.put("gv_cntt01", lec1);

            // Giảng viên CNTT 2 (Bộ môn KHMT & HTTT)
            AppUser gv2User = findOrCreateUser(
                    "gv_cntt02",
                    "123456",
                    "tuan.pm@university.edu.vn",
                    "TS. Phạm Minh Tuấn",
                    "0912345612",
                    lecturerRole
            );
            Lecturer lec2 = findOrCreateLecturer(gv2User, cnttDept, divKhmt, "GV_IT_02", "TS", "Tiến sĩ");
            map.put("gv_cntt02", lec2);
        }

        Department qtkdDept = deptMap.get("QTKD");
        Division divQtkd = divMap.get("QTKD_DN");
        if (qtkdDept != null) {
            // Giảng viên QTKD (Bộ môn QTKD Doanh nghiệp)
            AppUser gvQtkdUser = findOrCreateUser(
                    "gv_qtkd01",
                    "123456",
                    "thao.ht@university.edu.vn",
                    "ThS. Hoàng Thu Thảo",
                    "0912345621",
                    lecturerRole
            );
            Lecturer lecQtkd = findOrCreateLecturer(gvQtkdUser, qtkdDept, divQtkd, "GV_BA_01", "ThS", "Thạc sĩ");
            map.put("gv_qtkd01", lecQtkd);
        }

        return map;
    }

    // =========================================================================
    // 9. STUDENTS (CNTT & QTKD)
    // =========================================================================
    private void seedStudents(Role studentRole, Map<String, Major> majorMap, Map<String, Division> divMap) {
        if (studentRole == null) return;

        Major cnttMajor = majorMap.get("KTPM") != null ? majorMap.get("KTPM") : majorMap.get("CNTT");
        Division divKtpm = divMap.get("KTPM");

        if (cnttMajor != null) {
            // Sinh viên CNTT 1
            AppUser sv1User = findOrCreateUser(
                    "sv_cntt01",
                    "123456",
                    "sv.cntt01@student.edu.vn",
                    "Nguyễn Văn An",
                    "0987654301",
                    studentRole
            );
            findOrCreateStudent(sv1User, cnttMajor, divKtpm, "20110001", "20T1-CNTT", "2021-2025", 3.65);

            // Sinh viên CNTT 2
            AppUser sv2User = findOrCreateUser(
                    "sv_cntt02",
                    "123456",
                    "sv.cntt02@student.edu.vn",
                    "Trần Thị Bình",
                    "0987654302",
                    studentRole
            );
            findOrCreateStudent(sv2User, cnttMajor, divKtpm, "20110002", "20T1-CNTT", "2021-2025", 3.42);
        }

        Major qtkdMajor = majorMap.get("QTKD");
        Division divQtkd = divMap.get("QTKD_DN");
        if (qtkdMajor != null) {
            // Sinh viên QTKD
            AppUser svQtkdUser = findOrCreateUser(
                    "sv_qtkd01",
                    "123456",
                    "sv.qtkd01@student.edu.vn",
                    "Phạm Quốc Cường",
                    "0987654321",
                    studentRole
            );
            findOrCreateStudent(svQtkdUser, qtkdMajor, divQtkd, "20120001", "20KD1", "2021-2025", 3.50);
        }
    }

    // =========================================================================
    // 10. TOPICS (Gán trực tiếp cho từng Giảng viên & Bộ môn)
    // =========================================================================
    private void seedSampleTopics(
            Map<String, Department> deptMap,
            Map<String, Division> divMap,
            Map<String, Major> majorMap,
            Map<String, RegistrationPeriod> periodMap,
            Map<String, Lecturer> lecturerMap) {

        Major cnttMajor = majorMap.get("KTPM") != null ? majorMap.get("KTPM") : majorMap.get("CNTT");
        RegistrationPeriod cnttPeriod = periodMap.get("CNTT_KLTN");
        Division divKtpm = divMap.get("KTPM");
        Division divKhmt = divMap.get("KHMT_HTTT");
        Lecturer gvCntt1 = lecturerMap.get("gv_cntt01");
        Lecturer gvCntt2 = lecturerMap.get("gv_cntt02");

        if (cnttPeriod != null) {
            if (gvCntt1 != null) {
                createTopicIfNotExist(
                        "CNTT-SEED-01",
                        "Xây dựng hệ thống quản lý kho thông minh sử dụng IoT và Machine Learning",
                        "Nghiên cứu và phát triển hệ thống quản lý kho hàng tích hợp cảm biến IoT để theo dõi hàng tồn kho theo thời gian thực, kết hợp thuật toán dự báo nhu cầu bằng Machine Learning.",
                        "Python / TensorFlow cho ML module.\nESP32 cho IoT.\nReact.js + Spring Boot.\nCơ sở dữ liệu PostgreSQL.",
                        cnttMajor, divKtpm, cnttPeriod, gvCntt1, 3
                );
                createTopicIfNotExist(
                        "CNTT-SEED-02",
                        "Phát triển ứng dụng di động hỗ trợ học tiếng Anh theo phương pháp Spaced Repetition",
                        "Xây dựng ứng dụng mobile áp dụng thuật toán Spaced Repetition System (SRS) kết hợp NLP để cá nhân hóa lộ trình học từ vựng cho người dùng.",
                        "Flutter / React Native cho mobile.\nSpring Boot API backend.\nPostgreSQL Database.",
                        cnttMajor, divKtpm, cnttPeriod, gvCntt1, 2
                );
            }

            if (gvCntt2 != null) {
                createTopicIfNotExist(
                        "CNTT-SEED-03",
                        "Nghiên cứu và triển khai hệ thống phát hiện xâm nhập mạng dùng Deep Learning",
                        "Phân tích và xây dựng mô hình Deep Learning để phát hiện các hành vi xâm nhập và tấn công mạng trong thời gian thực trên tập dữ liệu CICIDS2017.",
                        "Python: PyTorch / TensorFlow.\nWireshark / Scapy phân tích gói tin.\nSpring Boot REST Dashboard.",
                        cnttMajor, divKhmt, cnttPeriod, gvCntt2, 2
                );
                createTopicIfNotExist(
                        "CNTT-SEED-04",
                        "Thiết kế nền tảng E-Learning hỗ trợ học tập cộng tác theo kiến trúc Microservices",
                        "Xây dựng hệ thống E-Learning đa người dùng hỗ trợ phòng học cộng tác real-time với WebSocket và Microservices.",
                        "Spring Boot / Cloud Microservices.\nReact.js Frontend.\nDocker + Kubernetes.",
                        cnttMajor, divKhmt, cnttPeriod, gvCntt2, 3
                );
            }
        }

        Major qtkdMajor = majorMap.get("QTKD");
        RegistrationPeriod qtkdPeriod = periodMap.get("QTKD_KLTN");
        Division divQtkd = divMap.get("QTKD_DN");
        Lecturer gvQtkd1 = lecturerMap.get("gv_qtkd01");

        if (qtkdPeriod != null && gvQtkd1 != null) {
            createTopicIfNotExist(
                    "QTKD-SEED-01",
                    "Phân tích tác động của Digital Marketing đến hành vi mua sắm trực tuyến của Gen Z",
                    "Nghiên cứu định lượng về ảnh hưởng của Social Media và KOL Marketing đến quyết định mua hàng trên sàn TMĐT của giới trẻ tại Việt Nam.",
                    "Phương pháp nghiên cứu định lượng (SPSS, SmartPLS).\nKhảo sát 300+ đối tượng Gen Z.",
                    qtkdMajor, divQtkd, qtkdPeriod, gvQtkd1, 3
            );
            createTopicIfNotExist(
                    "QTKD-SEED-02",
                    "Xây dựng mô hình đánh giá rủi ro tín dụng cho doanh nghiệp SME tại Ngân hàng Thương mại",
                    "Ứng dụng phân tích dữ liệu tài chính và mô hình Logistic Regression để tối ưu hóa phê duyệt tín dụng doanh nghiệp vừa và nhỏ.",
                    "Phân tích tài chính doanh nghiệp.\nPhần mềm R / Python + SPSS.",
                    qtkdMajor, divQtkd, qtkdPeriod, gvQtkd1, 2
            );
        }
    }

    private void createTopicIfNotExist(
            String code, String title, String desc, String reqs,
            Major major, Division division, RegistrationPeriod period, Lecturer supervisor, int maxStudents) {

        if (topicRepository.existsByCode(code)) {
            Topic existing = topicRepository.findAll().stream()
                    .filter(t -> code.equalsIgnoreCase(t.getCode()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                boolean changed = false;
                if (existing.getDivision() == null && division != null) {
                    existing.setDivision(division);
                    changed = true;
                }
                if (existing.getSupervisor() == null && supervisor != null) {
                    existing.setSupervisor(supervisor);
                    changed = true;
                }
                if (existing.getPeriod() == null && period != null) {
                    existing.setPeriod(period);
                    changed = true;
                }
                if (changed) {
                    topicRepository.save(existing);
                }
            }
            return;
        }

        Topic t = new Topic();
        t.setCode(code);
        t.setTitle(title);
        t.setDescription(desc);
        t.setRequirements(reqs);
        t.setMajor(major);
        t.setDivision(division);
        t.setPeriod(period);
        t.setSupervisor(supervisor);
        t.setStatus("APPROVED");
        t.setMaxStudents(maxStudents);
        t.setAcademicYear("2026-2027");
        t.setSemester(1);
        t.setCreatedAt(OffsetDateTime.now());
        topicRepository.save(t);
    }

    // =========================================================================
    // HELPER CREATORS
    // =========================================================================
    private AppUser findOrCreateUser(
            String username, String password, String email,
            String fullName, String phone, Role role) {

        Optional<AppUser> userOpt = userRepository.findByUsernameIgnoreCase(username);
        AppUser user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            user.setRole(role);
            user.setIsActive(true);
            user.setFullName(fullName);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setUpdatedAt(OffsetDateTime.now());
            return userRepository.save(user);
        }

        user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepository.save(user);
    }

    private Lecturer findOrCreateLecturer(
            AppUser user, Department dept, Division div, String code, String title, String degree) {

        Optional<Lecturer> opt = lecturerRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            Lecturer lec = opt.get();
            lec.setDepartment(dept);
            lec.setDivision(div);
            lec.setLecturerCode(code);
            lec.setAcademicTitle(title);
            lec.setDegree(degree);
            return lecturerRepository.save(lec);
        }

        Lecturer lec = new Lecturer();
        lec.setUser(user);
        lec.setDepartment(dept);
        lec.setDivision(div);
        lec.setLecturerCode(code);
        lec.setAcademicTitle(title);
        lec.setDegree(degree);
        lec.setMaxTopicsQuota(5);
        return lecturerRepository.save(lec);
    }

    private Student findOrCreateStudent(
            AppUser user, Major major, Division div, String code, String className, String academicYear, Double gpa) {

        Optional<Student> opt = studentRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            Student s = opt.get();
            s.setMajor(major);
            s.setDivision(div);
            s.setStudentCode(code);
            s.setClassName(className);
            s.setAcademicYear(academicYear);
            s.setGpa(gpa);
            return studentRepository.save(s);
        }

        Student s = new Student();
        s.setUser(user);
        s.setMajor(major);
        s.setDivision(div);
        s.setStudentCode(code);
        s.setClassName(className);
        s.setAcademicYear(academicYear);
        s.setGpa(gpa);
        return studentRepository.save(s);
    }

    // =========================================================================
    // 11. IN DANH SÁCH TÀI KHOẢN TEST RA CONSOLE
    // =========================================================================
    private void printTestAccountsSummary() {
        System.out.println("\n+-------------------------------------------------------------------------------------------------------------------------------+");
        System.out.println("|                                           BẢNG TÀI KHOẢN KIỂM THỬ HỆ THỐNG (V2.0 DIVISION SCOPING)                             |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+");
        System.out.println("| VAI TRÒ (ROLE)     | TÀI KHOẢN     | MẬT KHẨU         | HỌ VÀ TÊN                    | BỘ MÔN / KHOA                          |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+");
        System.out.println("| QUẢN TRỊ VIÊN      | admin         | admin123         | Quản Trị Viên Hệ Thống       | Quản trị toàn trường                   |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+");
        System.out.println("| TRƯỞNG BỘ MÔN KTPM | head_cntt     | 123456           | TS. Nguyễn Văn Thắng         | Bộ môn Kỹ thuật Phần mềm (Khoa CNTT)   |");
        System.out.println("| TRƯỞNG BỘ MÔN QTKD | head_qtkd     | 123456           | PGS.TS. Trần Thị Mai         | Bộ môn QTKD Doanh nghiệp (Khoa QTKD)   |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+");
        System.out.println("| GIẢNG VIÊN (KTPM)  | gv_cntt01     | 123456           | ThS. Lê Hoàng Long           | BM Kỹ thuật Phần mềm (Khoa CNTT)       |");
        System.out.println("| GIẢNG VIÊN (KHMT)  | gv_cntt02     | 123456           | TS. Phạm Minh Tuấn           | BM KHMT & HTTT (Khoa CNTT)             |");
        System.out.println("| GIẢNG VIÊN (QTKD)  | gv_qtkd01     | 123456           | ThS. Hoàng Thu Thảo          | BM QTKD Doanh nghiệp (Khoa QTKD)       |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+");
        System.out.println("| SINH VIÊN (KTPM)   | sv_cntt01     | 123456           | Nguyễn Văn An (20110001)     | BM Kỹ thuật Phần mềm (Khoa CNTT)       |");
        System.out.println("| SINH VIÊN (KTPM)   | sv_cntt02     | 123456           | Trần Thị Bình (20110002)     | BM Kỹ thuật Phần mềm (Khoa CNTT)       |");
        System.out.println("| SINH VIÊN (QTKD)   | sv_qtkd01     | 123456           | Phạm Quốc Cường (20120001)   | BM QTKD Doanh nghiệp (Khoa QTKD)       |");
        System.out.println("+--------------------+---------------+------------------+------------------------------+----------------------------------------+\n");
    }
}
