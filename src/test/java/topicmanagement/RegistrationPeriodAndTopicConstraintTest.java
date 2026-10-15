package topicmanagement;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import topicmanagement.entity.*;
import topicmanagement.repository.*;
import topicmanagement.service.*;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ RÀNG BUỘC ĐỢT ĐĂNG KÝ VÀ QUẢN LÝ ĐỀ TÀI THEO BỘ MÔN
 * Quy chuẩn:
 * 1. Đợt đăng ký do Trưởng khoa tạo với các loại: MON_HOC, NCKH, TLCN, KLTN.
 * 2. Trường reviewerDeadline chỉ thiết lập cho TLCN / KLTN; councilDefenseDate chỉ cho KLTN.
 * 3. Tổ chức theo 2 giai đoạn:
 *    - Giai đoạn 1: Giảng viên đề xuất và Trưởng khoa phê duyệt, công bố danh sách.
 *    - Giai đoạn 2: Sinh viên đăng ký đề tài từ danh sách đã công bố.
 * 4. Đề tài thuộc bộ môn quản lý, hướng dẫn bởi ít nhất 1 GV và tối đa 2 GV.
 * 5. Chặn đăng ký khi thời gian đăng ký đóng hoặc đề tài chưa được duyệt.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RegistrationPeriodAndTopicConstraintTest {

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private LecturerService lecturerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Department department;
    private Major majorSoftware;
    private Major majorNetwork;
    private Lecturer supervisor1;
    private Lecturer supervisor2;
    private Student studentLeader;

    @BeforeAll
    void setupFixtures() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT-PERIOD");
            d.setName("Khoa CNTT - Quản lý đợt");
            return departmentRepository.save(d);
        });

        majorSoftware = majorRepository.findAll().stream()
                .filter(m -> "CNTT-KTPM".equalsIgnoreCase(m.getCode()))
                .findFirst().orElseGet(() -> {
                    Major m = new Major();
                    m.setDepartment(department);
                    m.setCode("CNTT-KTPM");
                    m.setName("Kỹ thuật Phần mềm");
                    return majorRepository.save(m);
                });

        majorNetwork = majorRepository.findAll().stream()
                .filter(m -> "CNTT-MMT".equalsIgnoreCase(m.getCode()))
                .findFirst().orElseGet(() -> {
                    Major m = new Major();
                    m.setDepartment(department);
                    m.setCode("CNTT-MMT");
                    m.setName("Mạng máy tính & Truyền thông");
                    return majorRepository.save(m);
                });

        supervisor1 = createLecturerFixture("gv_reg_sup1", "TS. Hoàng Hướng Dẫn 1");
        supervisor2 = createLecturerFixture("gv_reg_sup2", "ThS. Ngô Đồng Hướng Dẫn 2");
        studentLeader = createStudentFixture("sv_reg_leader", "Vũ Trưởng Nhóm");
    }

    @Test
    @Order(1)
    @DisplayName("1. Tạo các đợt đăng ký theo 4 phân loại (Môn học, NCKH, TLCN, KLTN) với các mốc thời gian đặc thù")
    void testCreatePeriodTypesAndDates() {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Đợt Đồ án môn học
        RegistrationPeriod pMonHoc = departmentHeadService.createPeriod(
                department,
                "Đợt Đồ Án Môn Học Học Kỳ 1",
                "MON_HOC",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(10),
                now.plusDays(11), now.plusDays(25),
                null, null
        );
        assertNotNull(pMonHoc.getId());
        assertEquals("Đồ án Môn học", pMonHoc.getPeriodTypeName());
        assertEquals("MON_HOC", pMonHoc.getPeriodType());

        // 2. Đợt Nghiên cứu khoa học (NCKH)
        RegistrationPeriod pNckh = departmentHeadService.createPeriod(
                department,
                "Đợt Nghiên Cứu Khoa Học Sinh Viên 2026",
                "NCKH",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(15),
                now.plusDays(16), now.plusDays(30),
                null, null
        );
        assertNotNull(pNckh.getId());
        assertEquals("Nghiên cứu Khoa học", pNckh.getPeriodTypeName());

        // 3. Đợt Tiểu luận chuyên ngành (TLCN) - có reviewerDeadline được thiết lập
        OffsetDateTime revDeadTlcn = now.plusDays(45);
        RegistrationPeriod pTlcn = departmentHeadService.createPeriod(
                department,
                "Đợt Tiểu Luận Chuyên Ngành K16",
                "TLCN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(15),
                now.plusDays(16), now.plusDays(30),
                revDeadTlcn, null
        );
        assertNotNull(pTlcn.getId());
        assertEquals("Tiểu luận Chuyên ngành", pTlcn.getPeriodTypeName());
        assertNotNull(pTlcn.getReviewerDeadline(), "Đợt TLCN phải có hạn chót GVPB nộp điểm");

        // 4. Đợt Khóa luận tốt nghiệp (KLTN) - có cả reviewerDeadline và councilDefenseDate
        OffsetDateTime revDeadKltn = now.plusDays(50);
        OffsetDateTime defenseDateKltn = now.plusDays(65);
        RegistrationPeriod pKltn = departmentHeadService.createPeriod(
                department,
                "Đợt Khóa Luận Tốt Nghiệp 2026",
                "KLTN",
                "2026-2027",
                2,
                now.minusDays(5), now.plusDays(15),
                now.plusDays(16), now.plusDays(30),
                revDeadKltn, defenseDateKltn
        );
        assertNotNull(pKltn.getId());
        assertEquals("Khóa luận Tốt nghiệp", pKltn.getPeriodTypeName());
        assertNotNull(pKltn.getReviewerDeadline(), "Đợt KLTN phải có hạn GVPB");
        assertNotNull(pKltn.getCouncilDefenseDate(), "Đợt KLTN phải có ngày báo cáo Hội đồng");
    }

    @Test
    @Order(2)
    @DisplayName("2. Giai đoạn 1: Giảng viên đề xuất đề tài thuộc Bộ môn và Trưởng khoa phê duyệt công bố danh sách")
    void testPhase1TopicProposalAndDepartmentHeadApproval() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod period = departmentHeadService.createPeriod(
                department,
                "Đợt KLTN Giai Đoạn 1 Test",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(2), now.plusDays(10), // GV đề xuất đang mở
                now.plusDays(11), now.plusDays(25), // SV đăng ký chưa mở
                now.plusDays(45), now.plusDays(60)
        );

        assertTrue(period.isLecturerProposalOpen(), "Thời gian đề xuất của GV phải đang mở");
        assertFalse(period.isStudentRegistrationOpen(), "Thời gian đăng ký của SV chưa mở");

        // GV1 đề xuất đề tài thuộc chuyên ngành Kỹ thuật phần mềm với 2 GVHD
        Topic proposal = new Topic();
        proposal.setCode("DT-KTPM-" + System.nanoTime());
        proposal.setTitle("Kiến trúc Microservices xử lý dữ liệu thời gian thực");
        proposal.setDescription("Xây dựng giải pháp với Apache Kafka và Spring Cloud");
        proposal.setRequirements("Hiểu biết sâu về Spring Cloud và Docker");
        proposal.setAcademicYear("2026-2027");
        proposal.setSemester(1);

        Topic createdTopic = lecturerService.createTopic(
                supervisor1, period.getId(), majorSoftware.getId(), supervisor2.getId(), proposal
        );

        assertNotNull(createdTopic.getId());
        assertEquals("PENDING", createdTopic.getStatus(), "Trạng thái đề xuất ban đầu là PENDING");
        assertEquals(supervisor1.getId(), createdTopic.getSupervisor().getId(), "GVHD chính");
        assertEquals(supervisor2.getId(), createdTopic.getCoSupervisor().getId(), "GVHD đồng hướng dẫn");
        assertEquals(majorSoftware.getId(), createdTopic.getMajor().getId(), "Đề tài thuộc đúng chuyên ngành KTPM");

        // Trưởng khoa duyệt đề tài -> Chuyển thành APPROVED để công bố
            departmentHeadService.approveTopic(createdTopic.getId(), department.getId());
        Topic approvedTopic = lecturerService.getTopicById(createdTopic.getId());
        assertEquals("APPROVED", approvedTopic.getStatus(), "Đề tài đã được công bố danh sách");
    }

    @Test
    @Order(3)
    @DisplayName("3. Giai đoạn 2: Ngăn chặn sinh viên đăng ký đề tài khi đợt chưa đến thời gian đăng ký")
    void testPhase2BlockRegistrationBeforeStudentWindowOpens() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod earlyPeriod = departmentHeadService.createPeriod(
                department,
                "Đợt chưa mở đăng ký cho SV",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(5), // GV proposal đang mở để tạo đề tài
                now.plusDays(10), now.plusDays(25),   // SV registration trong tương lai
                now.plusDays(40), now.plusDays(55)
        );

        Topic topic = new Topic();
        topic.setCode("DT-EARLY-" + System.nanoTime());
        topic.setTitle("Nghiên cứu an toàn mạng máy tính");
        topic.setDescription("Đề tài mạng");
        topic.setRequirements("CCNA");
        topic.setAcademicYear("2026-2027");
        topic.setSemester(1);

        Topic saved = lecturerService.createTopic(supervisor1, earlyPeriod.getId(), majorNetwork.getId(), null, topic);
            departmentHeadService.approveTopic(saved.getId(), department.getId());

        StudentGroup group = studentService.createGroup(studentLeader, earlyPeriod.getId(), "Nhóm Thử Nghiệm");
        String result = studentService.registerTopicForGroup(studentLeader, group, saved.getId());

        assertNotNull(result, "Hệ thống phải từ chối đăng ký khi chưa đến thời gian mở");
        assertTrue(result.contains("không trong thời gian mở"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Ngăn chặn sinh viên đăng ký đề tài bị TỪ CHỐI (REJECTED) hoặc đang CHỜ DUYỆT (PENDING)")
    void testBlockRegistrationForUnapprovedTopics() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod openPeriod = departmentHeadService.createPeriod(
                department,
                "Đợt Đang Mở Đăng Ký",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(10), now.plusDays(5),
                now.minusDays(2), now.plusDays(10),
                now.plusDays(30), now.plusDays(45)
        );

        // 1. Đề tài đang PENDING
        Topic pendingTopic = new Topic();
        pendingTopic.setCode("DT-PEND-" + System.nanoTime());
        pendingTopic.setTitle("Đề tài chưa duyệt");
        pendingTopic.setDescription("Chờ xem xét");
        pendingTopic.setRequirements("Yêu cầu");
        pendingTopic.setAcademicYear("2026-2027");
        pendingTopic.setSemester(1);
        Topic savedPending = lecturerService.createTopic(supervisor1, openPeriod.getId(), majorSoftware.getId(), null, pendingTopic);

        Student student = createStudentFixture("sv_unapproved_test", "Sinh Viên Test Đề Tài Chưa Duyệt");
        StudentGroup group = studentService.createGroup(student, openPeriod.getId(), "Nhóm Test Đề Tài Chưa Duyệt");

        String regPendingResult = studentService.registerTopicForGroup(student, group, savedPending.getId());
        assertNotNull(regPendingResult, "Không được cho đăng ký đề tài PENDING");
        assertTrue(regPendingResult.contains("chưa được duyệt") || regPendingResult.contains("không tồn tại"));

        // 2. Đề tài bị REJECTED bởi Trưởng khoa
            departmentHeadService.rejectTopic(savedPending.getId(), "Đề tài không phù hợp chuyên ngành", department.getId());
        String regRejectedResult = studentService.registerTopicForGroup(student, group, savedPending.getId());
        assertNotNull(regRejectedResult, "Không được cho đăng ký đề tài REJECTED");
    }

    @Test
    @Order(5)
    @DisplayName("5. Đề tài chỉ được hướng dẫn bởi tối thiểu 1 GV và tối đa 2 GV")
    void testTopicSupervisorsCountConstraint() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod period = departmentHeadService.createPeriod(
                department,
                "Đợt kiểm tra GVHD",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(5),
                now.minusDays(2), now.plusDays(10),
                now.plusDays(30), now.plusDays(45)
        );

        // Đề tài có 1 GVHD
        Topic singleSup = new Topic();
        singleSup.setCode("DT-1GV-" + System.nanoTime());
        singleSup.setTitle("Đề tài 1 GVHD");
        singleSup.setDescription("Chỉ có GVHD chính");
        singleSup.setRequirements("Tốt");
        singleSup.setAcademicYear("2026-2027");
        singleSup.setSemester(1);

        Topic saved1 = lecturerService.createTopic(supervisor1, period.getId(), majorSoftware.getId(), null, singleSup);
        assertNotNull(saved1.getSupervisor());
        assertNull(saved1.getCoSupervisor());

        // Đề tài có 2 GVHD (GVHD chính + đồng hướng dẫn)
        Topic doubleSup = new Topic();
        doubleSup.setCode("DT-2GV-" + System.nanoTime());
        doubleSup.setTitle("Đề tài 2 GVHD");
        doubleSup.setDescription("Có cả GVHD chính và phụ");
        doubleSup.setRequirements("Tốt");
        doubleSup.setAcademicYear("2026-2027");
        doubleSup.setSemester(1);

        Topic saved2 = lecturerService.createTopic(supervisor1, period.getId(), majorSoftware.getId(), supervisor2.getId(), doubleSup);
        assertNotNull(saved2.getSupervisor());
        assertNotNull(saved2.getCoSupervisor());
        assertNotEquals(saved2.getSupervisor().getId(), saved2.getCoSupervisor().getId(), "Hai GVHD không được trùng nhau");
    }

    @Test
    @Order(6)
    @DisplayName("6. Kiểm tra theo dõi tiến trình đề xuất (Quota), thanh tiến độ và cơ chế Ân hạn sửa đề tài Rejected")
    void testProposalQuotaTrackingAndGracePeriodResubmission() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod period = departmentHeadService.createPeriod(
                department,
                "Đợt kiểm tra Quota & Grace Period",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(5),
                now.plusDays(10), now.plusDays(25),
                now.plusDays(40), now.plusDays(55),
                3 // Chỉ tiêu tối thiểu = 3 đề tài / GV
        );

        assertEquals(3, period.getMinTopicsPerLecturer(), "Chỉ tiêu tối thiểu phải là 3 đề tài");

        // GV đề xuất 1 đề tài
        Topic t1 = new Topic();
        t1.setCode("DT-QUOTA-1-" + System.nanoTime());
        t1.setTitle("Hệ thống nhận diện biển số xe");
        t1.setDescription("Mô tả AI");
        t1.setRequirements("OpenCV, Python");
        Topic savedT1 = lecturerService.createTopic(supervisor1, period.getId(), majorSoftware.getId(), null, t1);

        // Kiểm tra tiến độ qua Service
        List<LecturerService.ProposalProgressDto> progressList = lecturerService.getProposalProgressList(supervisor1);
        assertFalse(progressList.isEmpty());
        LecturerService.ProposalProgressDto currentProg = progressList.stream()
                .filter(p -> p.getPeriod().getId().equals(period.getId()))
                .findFirst().orElse(null);

        assertNotNull(currentProg);
        assertEquals(1, currentProg.getProposedCount(), "Đã nộp 1 đề tài");
        assertEquals(3, currentProg.getMinRequired(), "Chỉ tiêu tối thiểu là 3");
        assertFalse(currentProg.isTargetReached(), "Chưa đạt chỉ tiêu vì mới có 1/3");
        assertEquals(2, currentProg.getRemainingNeeded(), "Còn thiếu 2 đề tài");
        assertEquals(33, currentProg.getProgressPercent(), "Tiến độ 33%");

        // Trưởng khoa Từ chối đề tài T1
        departmentHeadService.rejectTopic(savedT1.getId(), "Yêu cầu thu hẹp phạm vi đề tài", department.getId());
        Topic rejectedT1 = lecturerService.getTopicById(savedT1.getId());
        assertEquals("REJECTED", rejectedT1.getStatus());
        assertEquals("Yêu cầu thu hẹp phạm vi đề tài", rejectedT1.getRejectionReason());

        // GV thực hiện Ân hạn sửa chữa & Gửi lại (Grace Period Resubmit)
        Topic editData = new Topic();
        editData.setTitle("Hệ thống nhận diện biển số xe máy tại bãi đỗ");
        editData.setDescription("Thu hẹp phạm vi trong bãi đỗ nội bộ");
        editData.setRequirements("OpenCV, Python, FastAPI");
        Topic resubmitted = lecturerService.updateTopic(supervisor1, savedT1.getId(), period.getId(), majorSoftware.getId(), null, editData);

        assertEquals("PENDING", resubmitted.getStatus(), "Sau khi sửa và nộp lại, trạng thái phải chuyển về PENDING");
        assertNull(resubmitted.getRejectionReason(), "Lý do từ chối cũ phải được xóa sạch");
        assertEquals("Hệ thống nhận diện biển số xe máy tại bãi đỗ", resubmitted.getTitle());
    }

    @Test
    @Order(7)
    @DisplayName("7. Kiểm tra Trưởng khoa Hiệu chỉnh nhanh & Phê duyệt trực tiếp (Quick Edit & Approve)")
    void testDepartmentHeadQuickEditAndApprove() {
        OffsetDateTime now = OffsetDateTime.now();
        RegistrationPeriod period = departmentHeadService.createPeriod(
                department,
                "Đợt kiểm tra Quick Edit Approve",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(5),
                now.plusDays(10), now.plusDays(25),
                now.plusDays(40), now.plusDays(55),
                3
        );

        // GV nộp đề tài tên chưa chuẩn
        Topic t = new Topic();
        t.setCode("DT-ROUGH-" + System.nanoTime());
        t.setTitle("lam web ban hang");
        t.setDescription("web ban quan ao");
        t.setRequirements("biet code");
        Topic saved = lecturerService.createTopic(supervisor1, period.getId(), majorSoftware.getId(), null, t);
        assertEquals("PENDING", saved.getStatus());

        // Trưởng khoa dùng tính năng Sửa nhanh & Duyệt luôn (để kịp công bố cho SV)
        boolean quickApproved = departmentHeadService.quickEditAndApproveTopic(
                saved.getId(),
                "Xây dựng Website Thương mại Điện tử Đa nền tảng",
                "Nghiên cứu kiến trúc Microservices và thanh toán trực tuyến",
                "Hiểu biết về Spring Boot, React, MySQL",
                null, department.getId()
        );

        assertTrue(quickApproved, "Trưởng khoa phải sửa nhanh và duyệt thành công");
        Topic approvedTopic = lecturerService.getTopicById(saved.getId());
        assertEquals("APPROVED", approvedTopic.getStatus(), "Trạng thái đề tài phải là APPROVED");
        assertEquals("Xây dựng Website Thương mại Điện tử Đa nền tảng", approvedTopic.getTitle(), "Tiêu đề đã được chuẩn hóa");
        assertNull(approvedTopic.getRejectionReason());
    }

    private Lecturer createLecturerFixture(String username, String fullName) {
        Role role = roleRepository.findByCode("LECTURER").orElse(null);
        String unique = username + "_" + System.nanoTime();
        AppUser u = new AppUser();
        u.setUsername(unique);
        u.setPasswordHash("123456");
        u.setFullName(fullName);
        u.setEmail(unique + "@uni.edu.vn");
        u.setRole(role);
        u.setIsActive(true);
        AppUser savedUser = userRepository.save(u);
        return lecturerService.getLecturerByUser(savedUser);
    }

    private Student createStudentFixture(String username, String fullName) {
        Role role = roleRepository.findByCode("STUDENT").orElse(null);
        String unique = username + "_" + System.nanoTime();
        AppUser u = new AppUser();
        u.setUsername(unique);
        u.setPasswordHash("123456");
        u.setFullName(fullName);
        u.setEmail(unique + "@student.edu.vn");
        u.setRole(role);
        u.setIsActive(true);
        AppUser savedUser = userRepository.save(u);
        return studentService.getStudentByUser(savedUser);
    }
}
