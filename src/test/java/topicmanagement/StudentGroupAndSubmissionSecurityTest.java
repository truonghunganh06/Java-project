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
 * BỘ KIỂM THỬ: QUẢN LÝ NHÓM SINH VIÊN, PHÂN QUYỀN VÀ NỘP BÁO CÁO TIẾN ĐỘ
 * Theo đặc tả:
 * 1. Sinh viên tham gia theo hình thức nhóm (tối đa 3 SV, có 1 nhóm trưởng).
 * 2. Mỗi SV chỉ tham gia duy nhất 1 nhóm trong suốt quá trình.
 * 3. Mỗi nhóm chỉ đăng ký duy nhất 1 đề tài từ danh sách đã công bố.
 * 4. Nộp báo cáo chỉ được thực hiện bởi nhóm trưởng.
 * 5. GVHD duyệt nhóm và chấm điểm/nhận xét báo cáo tiến độ.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StudentGroupAndSubmissionSecurityTest {

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    private Department department;
    private Major major;
    private RegistrationPeriod period;
    private Lecturer supervisor;
    private Student leader;
    private Student member1;
    private Student member2;
    private Student memberExtra;
    private Topic topic1;
    private Topic topic2;
    private StudentGroup activeGroup;

    @BeforeAll
    void setupFixtures() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT-GRP");
            d.setName("Khoa CNTT - Quản lý nhóm");
            return departmentRepository.save(d);
        });

        major = majorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Major m = new Major();
            m.setDepartment(department);
            m.setCode("CNTT-KTPM-GRP");
            m.setName("Kỹ thuật Phần mềm");
            return majorRepository.save(m);
        });

        supervisor = createLecturerFixture("gv_grp_sup", "TS. Hướng Dẫn Nhóm");

        leader = createStudentFixture("sv_lead_1", "Nguyễn Nhóm Trưởng");
        member1 = createStudentFixture("sv_mem_1", "Lê Thành Viên 1");
        member2 = createStudentFixture("sv_mem_2", "Phạm Thành Viên 2");
        memberExtra = createStudentFixture("sv_mem_extra", "Hoàng Sinh Viên Dư");

        OffsetDateTime now = OffsetDateTime.now();
        period = departmentHeadService.createPeriod(
                department,
                "Đợt kiểm thử nhóm và nộp bài",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(10),
                now.minusDays(3), now.plusDays(20),
                now.plusDays(40), now.plusDays(60)
        );

        // Tạo 2 đề tài đã được duyệt
        Topic t1 = new Topic();
        t1.setCode("DT-GRP-01-" + System.nanoTime());
        t1.setTitle("Ứng dụng AI phân tích dữ liệu thị trường");
        t1.setDescription("Phát triển nền tảng dữ liệu");
        t1.setRequirements("Python, AI");
        t1.setAcademicYear("2026-2027");
        t1.setSemester(1);
        topic1 = lecturerService.createTopic(supervisor, period.getId(), major.getId(), null, t1);
        departmentHeadService.approveTopic(topic1.getId(), department.getId());

        Topic t2 = new Topic();
        t2.setCode("DT-GRP-02-" + System.nanoTime());
        t2.setTitle("Xây dựng sàn thương mại điện tử Microservices");
        t2.setDescription("Hệ thống phân tán");
        t2.setRequirements("Spring Boot, Docker");
        t2.setAcademicYear("2026-2027");
        t2.setSemester(1);
        topic2 = lecturerService.createTopic(supervisor, period.getId(), major.getId(), null, t2);
        departmentHeadService.approveTopic(topic2.getId(), department.getId());
    }

    @Test
    @Order(1)
    @DisplayName("1. Tạo nhóm sinh viên và kiểm tra giới hạn tối đa 3 thành viên")
    void testGroupCreationAndMaxThreeMembers() {
        activeGroup = studentService.createGroup(leader, period.getId(), "Nhóm Công Nghệ Tiên Tiến");
        assertNotNull(activeGroup.getId());
        assertTrue(activeGroup.isLeader(leader), "leader phải là nhóm trưởng");
        
        List<GroupMember> initialMembers = groupMemberRepository.findByGroupId(activeGroup.getId());
        assertEquals(1, initialMembers.size(), "Ban đầu nhóm phải có đúng 1 thành viên là Leader");

        // Mời thành viên 1 -> Thành công (2 thành viên)
        String res1 = studentService.inviteMemberByCode(activeGroup, member1.getStudentCode());
        assertNull(res1, "Mời member1 thành công");

        // Mời thành viên 2 -> Thành công (3 thành viên)
        String res2 = studentService.inviteMemberByCode(activeGroup, member2.getStudentCode());
        assertNull(res2, "Mời member2 thành công");

        List<GroupMember> fullMembers = groupMemberRepository.findByGroupId(activeGroup.getId());
        assertEquals(3, fullMembers.size(), "Nhóm có đủ 3 thành viên");

        // Cố tình mời thành viên thứ 4 -> Bị chặn
        String resExtra = studentService.inviteMemberByCode(activeGroup, memberExtra.getStudentCode());
        assertNotNull(resExtra, "Hệ thống phải chặn mời thành viên thứ 4");
        assertTrue(resExtra.contains("tối đa 3 thành viên"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Mỗi sinh viên chỉ được tham gia duy nhất 1 nhóm trong suốt quá trình đợt")
    void testStudentCanOnlyJoinOneGroupPerPeriod() {
        // leader cố tình tạo nhóm thứ hai trong cùng đợt -> Ném ngoại lệ
        assertThrows(IllegalStateException.class, () -> {
            studentService.createGroup(leader, period.getId(), "Nhóm Trùng Của Leader");
        }, "Leader không được tạo thêm nhóm trong cùng đợt");

        // leader cố tình tham gia nhóm khác trong cùng đợt -> Bị chặn
        Student otherLeader = createStudentFixture("sv_other_lead", "Sinh Viên Nhóm Khác");
        StudentGroup otherGroup = studentService.createGroup(otherLeader, period.getId(), "Nhóm Độc Lập");

        String joinErr = studentService.joinGroupByCode(leader, otherGroup.getGroupCode());
        assertNotNull(joinErr, "Sinh viên đã có nhóm không được tham gia nhóm khác");
        assertTrue(joinErr.contains("đã thuộc một nhóm khác"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Ràng buộc: Mỗi nhóm chỉ được đăng ký DUY NHẤT 1 đề tài và CHỈ NHÓM TRƯỞNG mới được đăng ký")
    void testOnlyLeaderCanRegisterTopicAndOneTopicPerGroup() {
        // 1. Thành viên thường (member1) cố tình đăng ký đề tài -> Bị từ chối
        String memRegErr = studentService.registerTopicForGroup(member1, activeGroup, topic1.getId());
        assertNotNull(memRegErr, "Thành viên thường không được phép đăng ký đề tài thay nhóm trưởng");
        assertTrue(memRegErr.contains("Chỉ Nhóm trưởng mới có quyền"));

        // 2. Nhóm trưởng (leader) đăng ký Topic 1 -> Thành công
        String leaderRegOk = studentService.registerTopicForGroup(leader, activeGroup, topic1.getId());
        assertNull(leaderRegOk, "Nhóm trưởng đăng ký đề tài thành công");

        // 3. Nhóm trưởng cố tình đăng ký thêm Topic 2 cho nhóm -> Bị từ chối (Mỗi nhóm chỉ 1 đề tài)
        String duplicateTopicErr = studentService.registerTopicForGroup(leader, activeGroup, topic2.getId());
        assertNotNull(duplicateTopicErr, "Nhóm không được đăng ký thêm đề tài thứ hai");
        assertTrue(duplicateTopicErr.contains("đã đăng ký đề tài"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Giảng viên hướng dẫn duyệt nhóm đề tài")
    void testSupervisorApproveGroup() {
        List<StudentGroup> supervisorGroups = lecturerService.getGroupsForLecturerTopics(supervisor);
        assertFalse(supervisorGroups.isEmpty());

        boolean approved = lecturerService.approveGroupTopic(activeGroup.getId(), "APPROVED", supervisor);
        assertTrue(approved, "GVHD duyệt nhóm thành công");

        StudentGroup reloaded = studentService.getMyActiveGroup(leader, period.getId());
        assertEquals("APPROVED", reloaded.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("5. BẮT BUỘC: Việc nộp báo cáo chỉ được thực hiện bởi NHÓM TRƯỞNG")
    void testOnlyLeaderCanSubmitReportsAndSupervisorGrades() {
        // 1. Thành viên thường (member2) cố tình nộp báo cáo -> Bị chặn tuyệt đối!
        String memberSubmitErr = studentService.submitGroupReport(
                member2, activeGroup, "Báo cáo do thành viên nộp", 1, "test_member.pdf", 1024L
        );
        assertNotNull(memberSubmitErr, "Thành viên thường không được phép nộp báo cáo");
        assertTrue(memberSubmitErr.contains("Chỉ Nhóm trưởng mới có quyền nộp bài"));

        // 2. Nhóm trưởng (leader) nộp báo cáo tiến độ Đợt 1 -> Thành công
        String leaderSubmitOk = studentService.submitGroupReport(
                leader, activeGroup, "Báo cáo tiến độ Lần 1: Kiến trúc hệ thống", 1, "tien_do_lan_1.pdf", 2048576L
        );
        assertNull(leaderSubmitOk, "Nhóm trưởng nộp báo cáo thành công");

        // 3. Kiểm tra danh sách bài nộp
        List<Submission> submissions = studentService.getGroupSubmissions(activeGroup);
        assertFalse(submissions.isEmpty());
        Submission sub1 = submissions.get(0);
        assertEquals(leader.getId(), sub1.getSubmittedByStudent().getId(), "Người nộp phải là Leader");

        // 4. Giảng viên hướng dẫn nhận xét và chấm điểm bài nộp tiến độ
        boolean graded = lecturerService.gradeSubmission(sub1.getId(), 9.5, "Báo cáo tiến độ rất tốt, hoàn thành đúng hạn.", supervisor);
        assertTrue(graded, "GVHD chấm điểm bài nộp thành công");
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
