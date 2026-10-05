package topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topicmanagement.entity.Role;
import topicmanagement.entity.User;
import topicmanagement.repository.RoleRepository;
import topicmanagement.repository.UserRepository;

import java.util.Optional;

/**
 * DataSeederService — Kiểm tra các vai trò hệ thống cơ bản (System Roles).
 * Toàn bộ dữ liệu nghiệp vụ (Đề tài, Đợt đăng ký, Khoa, Giảng viên, Sinh viên)
 * được lưu trữ và quản lý trực tiếp trên Neon PostgreSQL Database.
 */
@Service
@DependsOn("databaseMigrationService")
@Order(5)
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeederService(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureBasicSystemRoles() {
        try {
            // Đảm bảo các roles hệ thống cơ bản tồn tại trong database
            createRoleIfMissing("ADMIN", "Quản trị viên hệ thống");
            createRoleIfMissing("DEPARTMENT_HEAD", "Trưởng khoa / Trưởng bộ môn");
            createRoleIfMissing("DIVISION_HEAD", "Trưởng bộ môn");
            createRoleIfMissing("LECTURER", "Giảng viên");
            createRoleIfMissing("STUDENT", "Sinh viên");

            // Đảm bảo tài khoản admin khởi tạo nếu database hoàn toàn mới
            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
                if (adminRole != null) {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPasswordHash(passwordEncoder.encode("123456"));
                    admin.setFullName("Quản Trị Viên Hệ Thống");
                    admin.setEmail("admin@topicmanagement.edu.vn");
                    admin.setRole(adminRole);
                    admin.setIsActive(true);
                    userRepository.save(admin);
                    log.info("[DATA-SEEDER] Đã khởi tạo tài khoản admin mặc định.");
                }
            }
            log.info("[DATA-SEEDER] Hoàn tất kiểm tra hệ thống. Dữ liệu được quản lý 100% trên Neon PostgreSQL.");
        } catch (Exception e) {
            log.warn("[DATA-SEEDER-WARN] Lỗi kiểm tra role khởi tạo: {}", e.getMessage());
        }
    }

    private void createRoleIfMissing(String code, String name) {
        if (roleRepository.findByCode(code).isEmpty()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            roleRepository.save(role);
            log.info("[DATA-SEEDER] Đã khởi tạo vai trò: {}", code);
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
            // 1. KLTN CNTT
            RegistrationPeriod pKltn = findOrCreatePeriod(
                    "Đợt Khóa luận Tốt nghiệp CNTT 2026-2027",
                    "KLTN",
                    cnttDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("CNTT_KLTN", pKltn);

            // 2. NCKH CNTT
            RegistrationPeriod pNckh = findOrCreatePeriod(
                    "Đợt Nghiên cứu Khoa học Sinh viên CNTT 2026",
                    "NCKH",
                    cnttDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("CNTT_NCKH", pNckh);

            // 3. TLCN CNTT
            RegistrationPeriod pTlcn = findOrCreatePeriod(
                    "Đợt Tiểu luận Chuyên ngành CNTT HK1 2026",
                    "TLCN",
                    cnttDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("CNTT_TLCN", pTlcn);

            // 4. MON_HOC CNTT
            RegistrationPeriod pMonHoc = findOrCreatePeriod(
                    "Đợt Đồ án Chuyên ngành CNTT HK1 2026",
                    "MON_HOC",
                    cnttDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("CNTT_MON_HOC", pMonHoc);
        }

        Department qtkdDept = deptMap.get("QTKD");
        if (qtkdDept != null) {
            // 1. KLTN QTKD
            RegistrationPeriod pKltnQtkd = findOrCreatePeriod(
                    "Đợt Khóa luận Tốt nghiệp QTKD 2026-2027",
                    "KLTN",
                    qtkdDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("QTKD_KLTN", pKltnQtkd);

            // 2. NCKH QTKD
            RegistrationPeriod pNckhQtkd = findOrCreatePeriod(
                    "Đợt Nghiên cứu Khoa học Sinh viên QTKD 2026",
                    "NCKH",
                    qtkdDept,
                    now.minusDays(20),
                    now.minusDays(5),
                    now.minusDays(4),
                    now.plusDays(30)
            );
            map.put("QTKD_NCKH", pNckhQtkd);
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
                p.setName(name);
                p.setLecturerProposalStart(propStart);
                p.setLecturerProposalEnd(propEnd);
                p.setStudentRegistrationStart(regStart);
                p.setStudentRegistrationEnd(regEnd);
                p.setReviewerDeadline(regEnd.plusDays(10));
                p.setCouncilDefenseDate(regEnd.plusDays(20));
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

            // Giảng viên CNTT 3 (Bộ môn KTPM)
            AppUser gv3User = findOrCreateUser(
                    "gv_cntt03",
                    "123456",
                    "nam.tv@university.edu.vn",
                    "TS. Trần Văn Nam",
                    "0912345613",
                    lecturerRole
            );
            Lecturer lec3 = findOrCreateLecturer(gv3User, cnttDept, divKtpm, "GV_IT_03", "TS", "Tiến sĩ");
            map.put("gv_cntt03", lec3);

            // Giảng viên CNTT 4 (Bộ môn KTPM)
            AppUser gv4User = findOrCreateUser(
                    "gv_cntt04",
                    "123456",
                    "ha.ntt@university.edu.vn",
                    "ThS. Nguyễn Thị Thu Hà",
                    "0912345614",
                    lecturerRole
            );
            Lecturer lec4 = findOrCreateLecturer(gv4User, cnttDept, divKtpm, "GV_IT_04", "ThS", "Thạc sĩ");
            map.put("gv_cntt04", lec4);

            // Giảng viên CNTT 5 (Bộ môn KHMT & HTTT)
            AppUser gv5User = findOrCreateUser(
                    "gv_cntt05",
                    "123456",
                    "hung.dq@university.edu.vn",
                    "PGS.TS. Đỗ Quốc Hưng",
                    "0912345615",
                    lecturerRole
            );
            Lecturer lec5 = findOrCreateLecturer(gv5User, cnttDept, divKhmt, "GV_IT_05", "PGS.TS", "Phó Giáo sư");
            map.put("gv_cntt05", lec5);
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

        Major ktpmMajor = majorMap.get("KTPM") != null ? majorMap.get("KTPM") : majorMap.get("CNTT");
        Division divKtpm = divMap.get("KTPM");

        if (ktpmMajor != null) {
            // Sinh viên CNTT 1
            AppUser sv1User = findOrCreateUser(
                    "sv_cntt01",
                    "123456",
                    "sv.cntt01@student.edu.vn",
                    "Nguyễn Văn An",
                    "0987654301",
                    studentRole
            );
            findOrCreateStudent(sv1User, ktpmMajor, divKtpm, "20110001", "20T1-CNTT", "2021-2025", 3.65);

            // Sinh viên CNTT 2
            AppUser sv2User = findOrCreateUser(
                    "sv_cntt02",
                    "123456",
                    "sv.cntt02@student.edu.vn",
                    "Trần Thị Bình",
                    "0987654302",
                    studentRole
            );
            findOrCreateStudent(sv2User, ktpmMajor, divKtpm, "20110002", "20T1-CNTT", "2021-2025", 3.42);
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
    // 10. TOPICS (Gán trực tiếp cho từng Giảng viên & Bộ môn & Đợt)
    // =========================================================================
    private void seedSampleTopics(
            Map<String, Department> deptMap,
            Map<String, Division> divMap,
            Map<String, Major> majorMap,
            Map<String, RegistrationPeriod> periodMap,
            Map<String, Lecturer> lecturerMap) {

        Major ktpmMajor = majorMap.get("KTPM") != null ? majorMap.get("KTPM") : majorMap.get("CNTT");
        Major cnttMajor = majorMap.get("CNTT") != null ? majorMap.get("CNTT") : ktpmMajor;
        Division divKtpm = divMap.get("KTPM");
        Division divKhmt = divMap.get("KHMT_HTTT");

        Lecturer gv1 = lecturerMap.get("gv_cntt01"); // ThS. Lê Hoàng Long
        Lecturer gv2 = lecturerMap.get("gv_cntt02"); // TS. Phạm Minh Tuấn
        Lecturer gv3 = lecturerMap.get("gv_cntt03"); // TS. Trần Văn Nam
        Lecturer gv4 = lecturerMap.get("gv_cntt04"); // ThS. Nguyễn Thị Thu Hà
        Lecturer gv5 = lecturerMap.get("gv_cntt05"); // PGS.TS. Đỗ Quốc Hưng

        // 1. Đợt Khóa Luận Tốt Nghiệp CNTT
        RegistrationPeriod cnttKltn = periodMap.get("CNTT_KLTN");
        if (cnttKltn != null) {
            if (gv1 != null) {
                createTopicIfNotExist(
                        "CNTT-KLTN-01",
                        "Xây dựng hệ thống quản lý kho thông minh sử dụng IoT và Machine Learning",
                        "Nghiên cứu và phát triển hệ thống quản lý kho hàng tích hợp cảm biến IoT để theo dõi hàng tồn kho theo thời gian thực, kết hợp thuật toán dự báo nhu cầu bằng Machine Learning.",
                        "Python / TensorFlow cho ML module.\nESP32 cho IoT.\nReact.js + Spring Boot.\nCơ sở dữ liệu PostgreSQL.",
                        ktpmMajor, divKtpm, cnttKltn, gv1, 3
                );
                createTopicIfNotExist(
                        "CNTT-KLTN-02",
                        "Phát triển ứng dụng di động hỗ trợ học tiếng Anh theo phương pháp Spaced Repetition",
                        "Xây dựng ứng dụng mobile áp dụng thuật toán Spaced Repetition System (SRS) kết hợp NLP để cá nhân hóa lộ trình học từ vựng cho người dùng.",
                        "Flutter / React Native cho mobile.\nSpring Boot API backend.\nPostgreSQL Database.",
                        ktpmMajor, divKtpm, cnttKltn, gv1, 2
                );
            }
            if (gv2 != null) {
                createTopicIfNotExist(
                        "CNTT-KLTN-03",
                        "Nghiên cứu và triển khai hệ thống phát hiện xâm nhập mạng dùng Deep Learning",
                        "Phân tích và xây dựng mô hình Deep Learning để phát hiện các hành vi xâm nhập và tấn công mạng trong thời gian thực trên tập dữ liệu CICIDS2017.",
                        "Python: PyTorch / TensorFlow.\nWireshark / Scapy phân tích gói tin.\nSpring Boot REST Dashboard.",
                        cnttMajor, divKhmt, cnttKltn, gv2, 2
                );
                createTopicIfNotExist(
                        "CNTT-KLTN-04",
                        "Thiết kế nền tảng E-Learning hỗ trợ học tập cộng tác theo kiến trúc Microservices",
                        "Xây dựng hệ thống E-Learning đa người dùng hỗ trợ phòng học cộng tác real-time với WebSocket và Microservices.",
                        "Spring Boot / Cloud Microservices.\nReact.js Frontend.\nDocker + Kubernetes.",
                        cnttMajor, divKhmt, cnttKltn, gv2, 3
                );
            }
            if (gv3 != null) {
                createTopicIfNotExist(
                        "CNTT-KLTN-05",
                        "Xây dựng nền tảng sàn giao dịch đồ cũ sinh viên tích hợp AI kiểm duyệt nội dung",
                        "Phát triển sàn TMĐT C2C cho sinh viên với tính năng xác thực thẻ sinh viên, định giá tự động bằng AI và tích hợp cổng thanh toán trực tuyến.",
                        "Spring Boot + React.js.\nFastAPI kiểm duyệt hình ảnh và nội dung.\nPostgreSQL + Redis.",
                        ktpmMajor, divKtpm, cnttKltn, gv3, 3
                );
            }
        }

        // 2. Đợt Nghiên Cứu Khoa Học CNTT (NCKH)
        RegistrationPeriod cnttNckh = periodMap.get("CNTT_NCKH");
        if (cnttNckh != null) {
            if (gv5 != null) {
                createTopicIfNotExist(
                        "CNTT-NCKH-01",
                        "Nghiên cứu mô hình RAG (Retrieval-Augmented Generation) cho hệ thống tra cứu văn bản pháp luật",
                        "Xây dựng pipeline RAG kết hợp Vector Database (Milvus/ChromaDB) và LLM nguồn mở để tra cứu và giải đáp tự động quy định pháp luật tiếng Việt với độ chính xác cao.",
                        "Python / LangChain / LlamaIndex.\nVector DB: ChromaDB / Milvus.\nLLM: PhoGPT / Vistral.",
                        cnttMajor, divKhmt, cnttNckh, gv5, 3
                );
            }
            if (gv2 != null) {
                createTopicIfNotExist(
                        "CNTT-NCKH-02",
                        "Ứng dụng Computer Vision trong giám sát tự động vi phạm an toàn lao động tại công trường",
                        "Huấn luyện mô hình YOLOv8 phát hiện người không đội mũ bảo hộ, không mặc áo phản quang từ luồng camera giám sát thời gian thực.",
                        "Python / OpenCV / YOLOv8 / PyTorch.\nStreaming RTSP / FastAPI.\nDashboard cảnh báo thời gian thực.",
                        cnttMajor, divKhmt, cnttNckh, gv2, 3
                );
            }
            if (gv1 != null) {
                createTopicIfNotExist(
                        "CNTT-NCKH-03",
                        "Phân tích và tối ưu hóa giải thuật định tuyến xe giao hàng (VRP) với Genetic Algorithm",
                        "Nghiên cứu bài toán Vehicle Routing Problem với ràng buộc thời gian (VRPTW) và ứng dụng thuật toán di truyền kết hợp bản đồ OpenStreetMap để tối ưu lộ trình xe.",
                        "Java / Python / Google OR-Tools.\nOpenStreetMap / Leaflet.js.\nSpring Boot REST API.",
                        ktpmMajor, divKtpm, cnttNckh, gv1, 2
                );
            }
            if (gv4 != null) {
                createTopicIfNotExist(
                        "CNTT-NCKH-04",
                        "Nghiên cứu giải pháp bảo mật dữ liệu y tế điện tử trên nền tảng Blockchain",
                        "Thiết kế kiến trúc lưu trữ hồ sơ bệnh án điện tử phân tán sử dụng Ethereum Smart Contracts kết hợp IPFS đảm bảo tính toàn vẹn và bảo mật dữ liệu.",
                        "Solidity / Hardhat / Web3.js.\nIPFS Decentralized Storage.\nReact.js + Node.js / Java.",
                        ktpmMajor, divKtpm, cnttNckh, gv4, 3
                );
            }
        }

        // 3. Đợt Tiểu Luận Chuyên Ngành CNTT (TLCN)
        RegistrationPeriod cnttTlcn = periodMap.get("CNTT_TLCN");
        if (cnttTlcn != null) {
            if (gv5 != null) {
                createTopicIfNotExist(
                        "CNTT-TLCN-01",
                        "Khảo sát và thực nghiệm các kỹ thuật Prompt Engineering nâng cao cho LLMs",
                        "Nghiên cứu so sánh hiệu năng của Chain-of-Thought (CoT), Tree-of-Thoughts (ToT) và ReAct trong các bài toán lập luận logic phức tạp.",
                        "Python / OpenAI API / HuggingFace Transformers.\nĐánh giá trên tập benchmark tiếng Việt.",
                        cnttMajor, divKhmt, cnttTlcn, gv5, 2
                );
            }
            if (gv3 != null) {
                createTopicIfNotExist(
                        "CNTT-TLCN-02",
                        "Tìm hiểu kiến trúc Event-Driven với Apache Kafka và ứng dụng xử lý luồng dữ liệu",
                        "Xây dựng hệ thống phân tích nhật ký giao dịch người dùng thời gian thực sử dụng kiến trúc Event-Driven với Apache Kafka và Spring Cloud Stream.",
                        "Apache Kafka / Zookeeper / Kafka Streams.\nSpring Boot + Docker.\nElasticsearch + Kibana Dashboard.",
                        ktpmMajor, divKtpm, cnttTlcn, gv3, 2
                );
            }
            if (gv4 != null) {
                createTopicIfNotExist(
                        "CNTT-TLCN-03",
                        "Nghiên cứu phương pháp tối ưu hóa hiệu năng cơ sở dữ liệu phân tán NoSQL MongoDB",
                        "Thực nghiệm đo lường hiệu năng sharding, indexing và replica set trên cụm MongoDB xử lý 10 triệu bản ghi dữ liệu mạng xã hội.",
                        "MongoDB Cluster / Docker Swarm.\nJMeter Load Testing.\nPython / NodeJS đo kiểm benchmark.",
                        ktpmMajor, divKtpm, cnttTlcn, gv4, 2
                );
            }
        }

        // 4. Đợt Đồ Án Môn Học CNTT (MON_HOC)
        RegistrationPeriod cnttMonHoc = periodMap.get("CNTT_MON_HOC");
        if (cnttMonHoc != null) {
            if (gv1 != null) {
                createTopicIfNotExist(
                        "CNTT-MH-01",
                        "Xây dựng Website bán sách trực tuyến với Spring Boot và React",
                        "Phát triển website bán sách hoàn chỉnh có giỏ hàng, thanh toán VNPAY, gửi email xác nhận đơn hàng và trang quản trị doanh thu.",
                        "Spring Boot 3 / Spring Security / JWT.\nReact.js / Tailwind CSS.\nPostgreSQL Database.",
                        ktpmMajor, divKtpm, cnttMonHoc, gv1, 3
                );
            }
            if (gv4 != null) {
                createTopicIfNotExist(
                        "CNTT-MH-02",
                        "Xây dựng ứng dụng quản lý công việc nhóm thời gian thực với WebSocket",
                        "Thiết kế ứng dụng tương tự Trello hỗ trợ kéo thả thẻ công việc, gán hạn chót, chat nhóm và cập nhật thay đổi tức thì qua WebSocket.",
                        "Spring Boot WebSocket / STOMP.\nReact.js / HTML5 Drag and Drop.\nMySQL Database.",
                        ktpmMajor, divKtpm, cnttMonHoc, gv4, 3
                );
            }
            if (gv3 != null) {
                createTopicIfNotExist(
                        "CNTT-MH-03",
                        "Phát triển trò chơi 2D vượt chướng ngại vật trên nền tảng Unity",
                        "Thiết kế game 2D Platformer với hệ thống cấp độ, thu thập vật phẩm, hiệu ứng âm thanh sống động và bảng xếp hạng điểm số cao.",
                        "Unity Engine / C# scripting.\nPixel Art Assets / Photoshop.\nSQLite Local Database.",
                        ktpmMajor, divKtpm, cnttMonHoc, gv3, 2
                );
            }
        }

        // 5. Đợt Khóa Luận Tốt Nghiệp QTKD
        Major qtkdMajor = majorMap.get("QTKD");
        Division divQtkd = divMap.get("QTKD_DN");
        Lecturer gvQtkd1 = lecturerMap.get("gv_qtkd01");

        RegistrationPeriod qtkdKltn = periodMap.get("QTKD_KLTN");
        if (qtkdKltn != null && gvQtkd1 != null) {
            createTopicIfNotExist(
                    "QTKD-KLTN-01",
                    "Phân tích tác động của Digital Marketing đến hành vi mua sắm trực tuyến của Gen Z",
                    "Nghiên cứu định lượng về ảnh hưởng của Social Media và KOL Marketing đến quyết định mua hàng trên sàn TMĐT của giới trẻ tại Việt Nam.",
                    "Phương pháp nghiên cứu định lượng (SPSS, SmartPLS).\nKhảo sát 300+ đối tượng Gen Z.",
                    qtkdMajor, divQtkd, qtkdKltn, gvQtkd1, 3
            );
            createTopicIfNotExist(
                    "QTKD-KLTN-02",
                    "Xây dựng mô hình đánh giá rủi ro tín dụng cho doanh nghiệp SME tại Ngân hàng Thương mại",
                    "Ứng dụng phân tích dữ liệu tài chính và mô hình Logistic Regression để tối ưu hóa phê duyệt tín dụng doanh nghiệp vừa và nhỏ.",
                    "Phân tích tài chính doanh nghiệp.\nPhần mềm R / Python + SPSS.",
                    qtkdMajor, divQtkd, qtkdKltn, gvQtkd1, 2
            );
        }

        // 6. Đợt Nghiên Cứu Khoa Học QTKD
        RegistrationPeriod qtkdNckh = periodMap.get("QTKD_NCKH");
        if (qtkdNckh != null && gvQtkd1 != null) {
            createTopicIfNotExist(
                    "QTKD-NCKH-01",
                    "Nghiên cứu các yếu tố ảnh hưởng đến ý định khởi nghiệp đổi mới sáng tạo của sinh viên",
                    "Khảo sát các yếu tố về môi trường đại học, nguồn vốn và chính sách hỗ trợ tác động đến tinh thần khởi nghiệp của sinh viên các trường đại học.",
                    "Mô hình SEM / SmartPLS.\nKhảo sát 400 sinh viên tại các trường Đại học.",
                    qtkdMajor, divQtkd, qtkdNckh, gvQtkd1, 3
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

    }
}
