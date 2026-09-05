package topic_management;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import topic_management.entity.*;
import topic_management.repository.*;
import topic_management.service.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ TOÀN DIỆN: HỘI ĐỒNG BẢO VỆ, ĐÁNH GIÁ ĐIỂM SỐ & CHỐNG XUNG ĐỘT LỢI ÍCH
 * Theo đúng yêu cầu nghiệp vụ:
 * 1. Hội đồng gồm 3 - 5 GV (gồm 1 Chủ tịch, 1 Thư ký và các Ủy viên).
 * 2. Một GV có thể tham gia chấm nhiều đề tài khác nhau.
 * 3. GV KHÔNG ĐƯỢC CHẤM ĐỀ TÀI MÌNH HƯỚNG DẪN (Chống xung đột lợi ích).
 * 4. Điểm cuối cùng của đề tài được tính bằng trung bình cộng các điểm thành phần.
 * 5. Công bố kết quả đánh giá và sinh viên tra cứu điểm số minh bạch.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CouncilAndDefenseComprehensiveTest {

    @Autowired
    private CouncilService councilService;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private LecturerService lecturerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private Department department;
    private Major major;
    private RegistrationPeriod kltnPeriod;
    private Lecturer gvChair;
    private Lecturer gvSecretary;
    private Lecturer gvMember1;
    private Lecturer gvMember2;
    private Lecturer gvMember3;
    private Lecturer gvSupervisorOnly;
    private Topic topicA;
    private Topic topicB;
    private Council council;
    private Student studentLeader;
    private StudentGroup studentGroup;

    @BeforeAll
    void setupFixtures() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT-DEF");
            d.setName("Khoa CNTT - Hội Đồng");
            return departmentRepository.save(d);
        });

        major = majorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Major m = new Major();
            m.setDepartment(department);
            m.setCode("HTTT");
            m.setName("Hệ thống Thông tin");
            return majorRepository.save(m);
        });

        // 1. Tạo 5 Giảng viên cho Hội đồng và 1 GVHD độc lập
        gvChair = createLecturerFixture("gv_chair", "PGS.TS Chủ tịch Hội Đồng");
        gvSecretary = createLecturerFixture("gv_sec", "TS. Thư ký Hội Đồng");
        gvMember1 = createLecturerFixture("gv_mem1", "ThS. Ủy viên 1");
        gvMember2 = createLecturerFixture("gv_mem2", "TS. Ủy viên 2");
        gvMember3 = createLecturerFixture("gv_mem3", "PGS.TS Ủy viên 3");
        gvSupervisorOnly = createLecturerFixture("gv_sup", "TS. Hướng dẫn độc lập");

        // 2. Tạo đợt KLTN
        OffsetDateTime now = OffsetDateTime.now();
        kltnPeriod = departmentHeadService.createPeriod(
                department,
                "Đợt KLTN Hội Đồng Đánh Giá",
                "KLTN",
                "2026-2027",
                2,
                now.minusDays(10), now.plusDays(5),
                now.minusDays(5), now.plusDays(15),
                now.plusDays(30),
                now.plusDays(45)
        );

        // 3. Tạo đề tài Topic A do gvChair làm GVHD chính và gvMember1 làm GVHD phụ
        Topic tA = new Topic();
        tA.setCode("TOPIC-A-" + System.nanoTime());
        tA.setTitle("Hệ thống phân tích dữ liệu lớn trên nền tảng Cloud");
        tA.setDescription("Nghiên cứu các thuật toán tối ưu hóa xử lý truy vấn");
        tA.setRequirements("Java, Spark, Cloud Computing");
        tA.setAcademicYear("2026-2027");
        tA.setSemester(2);
        topicA = lecturerService.createTopic(gvChair, kltnPeriod.getId(), major.getId(), gvMember1.getId(), tA);
        departmentHeadService.approveTopic(topicA.getId(), department.getId());

        // 4. Tạo đề tài Topic B do gvSupervisorOnly làm GVHD
        Topic tB = new Topic();
        tB.setCode("TOPIC-B-" + System.nanoTime());
        tB.setTitle("Xây dựng giải pháp Blockchain cho chuỗi cung ứng");
        tB.setDescription("Hợp đồng thông minh và cơ chế đồng thuận");
        tB.setRequirements("Solidity, Web3, Spring Boot");
        tB.setAcademicYear("2026-2027");
        tB.setSemester(2);
        topicB = lecturerService.createTopic(gvSupervisorOnly, kltnPeriod.getId(), major.getId(), null, tB);
        departmentHeadService.approveTopic(topicB.getId(), department.getId());

        // 5. Tạo nhóm sinh viên đăng ký Topic B
        studentLeader = createStudentFixture("sv_defense_lead", "Trần Văn Leader");
        studentGroup = studentService.createGroup(studentLeader, kltnPeriod.getId(), "Nhóm Blockchain KLTN");
        studentService.registerTopicForGroup(studentLeader, studentGroup, topicB.getId());
        lecturerService.approveGroupTopic(studentGroup.getId(), "APPROVED", gvSupervisorOnly);
    }

    @Test
    @Order(1)
    @DisplayName("1. Thành lập Hội đồng từ 3 đến 5 Giảng viên (gồm Chủ tịch, Thư ký và Ủy viên)")
    void testCreateCouncilWithStandardRoles() {
        List<Long> memberIds = List.of(
                gvChair.getId(),
                gvSecretary.getId(),
                gvMember1.getId(),
                gvMember2.getId(),
                gvMember3.getId()
        );
        List<String> roles = List.of("PRESIDENT", "SECRETARY", "MEMBER", "MEMBER", "MEMBER");

        council = councilService.createCouncil(
                department.getId(),
                kltnPeriod.getId(),
                "HĐ-CHUYÊN-NGÀNH-01",
                "Hội đồng đánh giá KLTN Hệ thống thông tin 01",
                "Phòng Lab A3-302",
                OffsetDateTime.now().plusDays(20),
                memberIds,
                roles
        );

        assertNotNull(council.getId(), "Hội đồng phải được khởi tạo thành công");
        assertEquals("ESTABLISHED", council.getStatus());
        assertEquals("HĐ-CHUYÊN-NGÀNH-01", council.getCode());

        Council loaded = councilService.getCouncilById(council.getId());
        assertNotNull(loaded);
        assertEquals(5, loaded.getMembers().size(), "Hội đồng phải có đúng 5 thành viên");

        // Kiểm tra chức danh Chủ tịch và Thư ký
        boolean hasPresident = loaded.getMembers().stream()
                .anyMatch(m -> "PRESIDENT".equalsIgnoreCase(m.getCouncilRole()) && m.getLecturer().getId().equals(gvChair.getId()));
        boolean hasSecretary = loaded.getMembers().stream()
                .anyMatch(m -> "SECRETARY".equalsIgnoreCase(m.getCouncilRole()) && m.getLecturer().getId().equals(gvSecretary.getId()));

        assertTrue(hasPresident, "Hội đồng phải có Chủ tịch được phân công chính xác");
        assertTrue(hasSecretary, "Hội đồng phải có Thư ký được phân công chính xác");
    }

    @Test
    @Order(2)
    @DisplayName("2. Phân công đề tài vào Hội đồng và kiểm tra tính hợp lệ của Giảng viên phản biện (GVPB)")
    void testAssignTopicAndReviewer() {
        // Gán đề tài B vào Hội đồng
        boolean assigned = councilService.assignTopicToCouncil(topicB.getId(), council.getId());
        assertTrue(assigned, "Gán đề tài vào hội đồng phải thành công");

        // Thử phân công GVHD của Topic B làm GVPB -> Phải bị từ chối
        boolean conflictReviewer = councilService.assignReviewer(topicB.getId(), gvSupervisorOnly.getId());
        assertFalse(conflictReviewer, "Hệ thống phải ngăn cấm phân công GVPB trùng với GVHD");

        // Phân công GVPB độc lập (gvMember2) -> Thành công
        boolean validReviewer = councilService.assignReviewer(topicB.getId(), gvMember2.getId());
        assertTrue(validReviewer, "Phân công GVPB độc lập phải thành công");

        Topic reloaded = lecturerService.getTopicById(topicB.getId());
        assertEquals(gvMember2.getId(), reloaded.getReviewer().getId());
        assertEquals(council.getId(), reloaded.getCouncil().getId());
    }

    @Test
    @Order(3)
    @DisplayName("3. Ràng buộc chống xung đột lợi ích: GVHD KHÔNG ĐƯỢC CHẤM đề tài mình hướng dẫn")
    void testConflictOfInterestSupervisorCannotGradeOwnTopicInCouncilOrReview() {
        // Topic A có gvChair là GVHD chính, gvMember1 là GVHD phụ
        // Cố tình để gvChair chấm COUNCIL cho Topic A -> Phải bị chặn
        boolean chairGradeFail = councilService.submitEvaluation(
                topicA.getId(), gvChair.getId(), "COUNCIL", 10.0, "GVHD chính tự chấm hội đồng"
        );
        assertFalse(chairGradeFail, "GVHD chính không được chấm điểm Hội đồng cho đề tài mình hướng dẫn");

        // Cố tình để gvMember1 (đồng HD) chấm REVIEWER cho Topic A -> Phải bị chặn
        boolean coSupGradeFail = councilService.submitEvaluation(
                topicA.getId(), gvMember1.getId(), "REVIEWER", 9.5, "GVHD phụ tự chấm phản biện"
        );
        assertFalse(coSupGradeFail, "GVHD phụ không được chấm điểm Phản biện cho đề tài mình đồng hướng dẫn");

        // Tuy nhiên, GVHD chấm điểm hướng dẫn (SUPERVISOR) là hợp lệ
        boolean supervisorGradeOk = councilService.submitEvaluation(
                topicA.getId(), gvChair.getId(), "SUPERVISOR", 9.0, "Đánh giá quá trình hướng dẫn tốt"
        );
        assertTrue(supervisorGradeOk, "GVHD được phép chấm điểm ở vai trò SUPERVISOR");
    }

    @Test
    @Order(4)
    @DisplayName("4. Một Giảng viên có thể tham gia chấm nhiều đề tài khác nhau trong Hội đồng")
    void testLecturerCanGradeMultipleDifferentTopics() {
        // gvMember2 là GVPB của Topic B
        boolean gradeB = councilService.submitEvaluation(
                topicB.getId(), gvMember2.getId(), "REVIEWER", 8.5, "Phản biện tốt cho Topic B"
        );
        assertTrue(gradeB, "gvMember2 chấm Topic B thành công");

        // gvMember2 cũng là thành viên Hội đồng chấm Topic A (vì gvMember2 không hướng dẫn Topic A)
        boolean gradeA = councilService.submitEvaluation(
                topicA.getId(), gvMember2.getId(), "COUNCIL", 8.0, "Hội đồng chấm Topic A tốt"
        );
        assertTrue(gradeA, "gvMember2 chấm Topic A thành công vì không phải GVHD của Topic A");
    }

    @Test
    @Order(5)
    @DisplayName("5. Tính toán điểm trung bình cộng chính xác từ các thành phần (GVHD, GVPB, Hội đồng)")
    void testFinalScoreCalculationAverageOfComponents() {
        // Đề tài Topic B có 3 phiếu điểm:
        // 1. GVHD (gvSupervisorOnly): 9.0
        // 2. GVPB (gvMember2): 8.5
        // 3. Chủ tịch Hội đồng (gvChair - độc lập với Topic B): 9.5
        // 4. Thư ký Hội đồng (gvSecretary - độc lập với Topic B): 8.0

        councilService.submitEvaluation(topicB.getId(), gvSupervisorOnly.getId(), "SUPERVISOR", 9.0, "Thực hiện đúng tiến độ");
        councilService.submitEvaluation(topicB.getId(), gvMember2.getId(), "REVIEWER", 8.5, "Báo cáo đầy đủ, rõ ràng");
        councilService.submitEvaluation(topicB.getId(), gvChair.getId(), "COUNCIL", 9.5, "Bảo vệ thuyết phục");
        councilService.submitEvaluation(topicB.getId(), gvSecretary.getId(), "COUNCIL", 8.0, "Trả lời câu hỏi tốt");

        // Trung bình cộng: (9.0 + 8.5 + 9.5 + 8.0) / 4 = 35.0 / 4 = 8.75
        Topic updatedB = lecturerService.getTopicById(topicB.getId());
        assertNotNull(updatedB.getFinalScore(), "Điểm tổng kết không được null");
        assertEquals(8.75, updatedB.getFinalScore(), 0.01, "Điểm tổng kết phải đúng bằng trung bình cộng 8.75");
    }

    @Test
    @Order(6)
    @DisplayName("6. Công bố kết quả và Sinh viên tra cứu điểm số của đề tài mình thực hiện")
    void testPublishResultsAndStudentScoreView() {
        // 1. Trưởng khoa công bố kết quả
        boolean published = councilService.publishTopicResults(topicB.getId(), true);
        assertTrue(published, "Công bố kết quả thành công");

        Topic finalTopic = lecturerService.getTopicById(topicB.getId());
        assertTrue(finalTopic.getIsFinalized(), "Trạng thái công bố phải là true");

        // 2. Sinh viên tra cứu kết quả đánh giá của nhóm mình
        List<Evaluation> evals = studentService.getGroupEvaluations(studentGroup);
        assertNotNull(evals);
        assertEquals(4, evals.size(), "Sinh viên phải xem được đầy đủ 4 phiếu đánh giá thành phần");

        for (Evaluation eval : evals) {
            assertNotNull(eval.getScore(), "Mỗi phiếu đánh giá phải có điểm");
            assertNotNull(eval.getLecturer(), "Mỗi phiếu đánh giá phải có thông tin GV chấm");
            assertNotNull(eval.getComments(), "Mỗi phiếu đánh giá phải có nhận xét");
        }
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
