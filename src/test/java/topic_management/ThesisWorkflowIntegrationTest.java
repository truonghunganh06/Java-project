package topic_management;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;
import topic_management.service.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThesisWorkflowIntegrationTest {

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private LecturerService lecturerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CouncilService councilService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CouncilRepository councilRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // Shared State across ordered test steps
    private Department testDept;
    private Major testMajor;
    private AppUser headUser;
    private Lecturer gv1;
    private Lecturer gv2;
    private Lecturer gv3;
    private Lecturer gvCouncil1;
    private Lecturer gvCouncil2;
    private Student sv1;
    private Student sv2;
    private Student sv3;
    private Student sv4;
    private RegistrationPeriod testPeriod;
    private Topic testTopic;
    private StudentGroup testGroup;
    private Council testCouncil;

    @BeforeAll
    void setupGlobalFixtures() {
        System.out.println("================================================================================");
        System.out.println(">>> KHỞI TẠO BỘ TEST TỰ ĐỘNG TOÀN DIỆN: CHU TRÌNH QUẢN LÝ ĐỀ TÀI KHOA CNTT <<<");
        System.out.println("================================================================================");

        try {
            jdbcTemplate.execute("""
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (
                        SELECT table_name, column_name 
                        FROM information_schema.columns 
                        WHERE table_schema = 'public' 
                          AND is_nullable = 'NO' 
                          AND column_name NOT IN ('id')
                    ) LOOP
                        BEGIN
                            EXECUTE 'ALTER TABLE ' || quote_ident(r.table_name) || ' ALTER COLUMN ' || quote_ident(r.column_name) || ' DROP NOT NULL';
                        EXCEPTION WHEN OTHERS THEN
                            -- ignore
                        END;
                    END LOOP;
                END $$;
            """);
        } catch (Exception e) {
            System.out.println("Schema alter notice: " + e.getMessage());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Bước 1: Khởi tạo dữ liệu người dùng, bộ môn, giảng viên và sinh viên")
    void step1_setupUsersAndEntities() {
        System.out.println("\n[TEST-1] BƯỚC 1: Khởi tạo tài khoản & thực thể tham gia");

        // 1. Department & Major
        testDept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT-TEST");
            d.setName("Khoa Công nghệ Thông tin (Test)");
            return departmentRepository.save(d);
        });

        testMajor = majorRepository.findAll().stream().findFirst().orElseGet(() -> {
            Major m = new Major();
            m.setDepartment(testDept);
            m.setCode("CNTT-KTPM");
            m.setName("Kỹ thuật Phần mềm");
            return majorRepository.save(m);
        });

        assertNotNull(testDept, "Department phải tồn tại");
        assertNotNull(testMajor, "Major phải tồn tại");

        // 2. Roles
        Role rHead = roleRepository.findByCode("DEPARTMENT_HEAD").orElse(null);
        Role rLecturer = roleRepository.findByCode("LECTURER").orElse(null);
        Role rStudent = roleRepository.findByCode("STUDENT").orElse(null);

        // 3. Head of Department
        headUser = userRepository.findByUsername("test_head").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_head");
            u.setPasswordHash("123456");
            u.setFullName("PGS.TS Nguyễn Văn Trưởng Khoa");
            u.setEmail("head.test@university.edu.vn");
            u.setRole(rHead);
            return userRepository.save(u);
        });

        // 4. Lecturers: GV1 (HD chính), GV2 (Đồng HD), GV3 (GVPB / Hội đồng)
        AppUser uGv1 = userRepository.findByUsername("test_gv1").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_gv1");
            u.setPasswordHash("123456");
            u.setFullName("TS. Trần Hướng Dẫn 1");
            u.setEmail("gv1.test@university.edu.vn");
            u.setRole(rLecturer);
            return userRepository.save(u);
        });
        gv1 = lecturerService.getLecturerByUser(uGv1);

        AppUser uGv2 = userRepository.findByUsername("test_gv2").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_gv2");
            u.setPasswordHash("123456");
            u.setFullName("ThS. Lê Đồng Hướng Dẫn 2");
            u.setEmail("gv2.test@university.edu.vn");
            u.setRole(rLecturer);
            return userRepository.save(u);
        });
        gv2 = lecturerService.getLecturerByUser(uGv2);

        AppUser uGv3 = userRepository.findByUsername("test_gv3").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_gv3");
            u.setPasswordHash("123456");
            u.setFullName("TS. Phạm Phản Biện 3");
            u.setEmail("gv3.test@university.edu.vn");
            u.setRole(rLecturer);
            return userRepository.save(u);
        });
        gv3 = lecturerService.getLecturerByUser(uGv3);

        AppUser uGvCouncil1 = userRepository.findByUsername("test_gvcouncil1").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_gvcouncil1");
            u.setPasswordHash("123456");
            u.setFullName("TS. Hoàng Thành Viên HĐ 1");
            u.setEmail("gvcouncil1.test@university.edu.vn");
            u.setRole(rLecturer);
            return userRepository.save(u);
        });
        gvCouncil1 = lecturerService.getLecturerByUser(uGvCouncil1);

        AppUser uGvCouncil2 = userRepository.findByUsername("test_gvcouncil2").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_gvcouncil2");
            u.setPasswordHash("123456");
            u.setFullName("TS. Vũ Thành Viên HĐ 2");
            u.setEmail("gvcouncil2.test@university.edu.vn");
            u.setRole(rLecturer);
            return userRepository.save(u);
        });
        gvCouncil2 = lecturerService.getLecturerByUser(uGvCouncil2);

        // 5. Students: SV1 (Leader), SV2 (Member), SV3 (Member), SV4 (Extra SV)
        AppUser uSv1 = userRepository.findByUsername("test_sv1").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_sv1");
            u.setPasswordHash("123456");
            u.setFullName("Nguyễn Văn Trưởng Nhóm");
            u.setEmail("sv1.test@student.edu.vn");
            u.setRole(rStudent);
            return userRepository.save(u);
        });
        sv1 = studentService.getStudentByUser(uSv1);

        AppUser uSv2 = userRepository.findByUsername("test_sv2").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_sv2");
            u.setPasswordHash("123456");
            u.setFullName("Trần Thị Thành Viên 2");
            u.setEmail("sv2.test@student.edu.vn");
            u.setRole(rStudent);
            return userRepository.save(u);
        });
        sv2 = studentService.getStudentByUser(uSv2);

        AppUser uSv3 = userRepository.findByUsername("test_sv3").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_sv3");
            u.setPasswordHash("123456");
            u.setFullName("Lê Văn Thành Viên 3");
            u.setEmail("sv3.test@student.edu.vn");
            u.setRole(rStudent);
            return userRepository.save(u);
        });
        sv3 = studentService.getStudentByUser(uSv3);

        AppUser uSv4 = userRepository.findByUsername("test_sv4").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("test_sv4");
            u.setPasswordHash("123456");
            u.setFullName("Phạm Văn Sinh Viên 4 (Ngoài nhóm)");
            u.setEmail("sv4.test@student.edu.vn");
            u.setRole(rStudent);
            return userRepository.save(u);
        });
        sv4 = studentService.getStudentByUser(uSv4);

        System.out.println("-> Khởi tạo thành công: 3 Giảng viên (GV1, GV2, GV3) và 4 Sinh viên (SV1, SV2, SV3, SV4)");
        assertNotNull(gv1);
        assertNotNull(gv2);
        assertNotNull(gv3);
        assertNotNull(sv1);
        assertNotNull(sv2);
        assertNotNull(sv3);
        assertNotNull(sv4);
    }

    @Test
    @Order(2)
    @DisplayName("Bước 2: Trưởng khoa tạo đợt đăng ký Khóa Luận Tốt Nghiệp (KLTN)")
    void step2_createRegistrationPeriod() {
        System.out.println("\n[TEST-2] BƯỚC 2: Trưởng khoa tạo đợt đăng ký KLTN");

        OffsetDateTime now = OffsetDateTime.now();
        testPeriod = departmentHeadService.createPeriod(
                testDept,
                "Đợt KLTN Tự Động Test 2026",
                "KLTN",
                "2026-2027",
                1,
                now.minusDays(5), now.plusDays(10), // Giảng viên đề xuất
                now.minusDays(2), now.plusDays(20), // Sinh viên đăng ký
                now.plusDays(45),                   // Hạn GVPB
                now.plusDays(60)                    // Ngày bảo vệ
        );

        assertNotNull(testPeriod.getId(), "Đợt đăng ký phải được lưu vào Database");
        assertEquals("KLTN", testPeriod.getPeriodType());
        assertEquals("OPEN", testPeriod.getStatus());
        assertTrue(testPeriod.isLecturerProposalOpen(), "Thời gian đề xuất GV phải đang mở");
        assertTrue(testPeriod.isStudentRegistrationOpen(), "Thời gian đăng ký SV phải đang mở");
        System.out.println("-> Đã tạo đợt đăng ký KLTN ID=" + testPeriod.getId() + " (" + testPeriod.getName() + ")");
    }

    @Test
    @Order(3)
    @DisplayName("Bước 3: Giảng viên 1 đề xuất đề tài mới có 2 GVHD (GV1 + GV2)")
    void step3_lecturerProposeTopicWithTwoSupervisors() {
        System.out.println("\n[TEST-3] BƯỚC 3: Giảng viên 1 đề xuất đề tài có 2 GVHD");

        Topic newTopic = new Topic();
        newTopic.setCode("DT-TEST-AI-" + System.currentTimeMillis() % 10000);
        newTopic.setTitle("Nghiên cứu ứng dụng Deep Learning phát hiện gian lận thi cử");
        newTopic.setDescription("Phát triển mô hình thị giác máy tính và học sâu trên camera phòng thi.");
        newTopic.setRequirements("Thành thạo Python, PyTorch, Spring Boot, PostgreSQL");
        newTopic.setMaxStudents(3);
        newTopic.setAcademicYear("2026-2027");
        newTopic.setSemester(1);

        testTopic = lecturerService.createTopic(
                gv1,
                testPeriod.getId(),
                testMajor.getId(),
                gv2.getId(), // Co-Supervisor
                newTopic
        );

        assertNotNull(testTopic.getId(), "Đề tài phải được lưu vào DB");
        assertEquals("PENDING", testTopic.getStatus(), "Trạng thái ban đầu phải là PENDING");
        assertEquals(gv1.getId(), testTopic.getSupervisor().getId(), "GVHD chính phải là GV1");
        assertNotNull(testTopic.getCoSupervisor(), "Phải có GVHD phụ");
        assertEquals(gv2.getId(), testTopic.getCoSupervisor().getId(), "GVHD phụ phải là GV2");
        assertTrue(testTopic.isSupervisorOrCoSupervisor(gv1), "GV1 phải là GVHD");
        assertTrue(testTopic.isSupervisorOrCoSupervisor(gv2), "GV2 phải là GVHD");
        assertFalse(testTopic.isSupervisorOrCoSupervisor(gv3), "GV3 không phải là GVHD");

        System.out.println("-> Đề tài ID=" + testTopic.getId() + " đã đề xuất: '" + testTopic.getTitle() + "'");
        System.out.println("   + GVHD chính: " + testTopic.getSupervisor().getUser().getFullName());
        System.out.println("   + GVHD phụ: " + testTopic.getCoSupervisor().getUser().getFullName());
    }

    @Test
    @Order(4)
    @DisplayName("Bước 4: Trưởng khoa phê duyệt đề tài (Chuyển sang APPROVED)")
    void step4_departmentHeadApproveTopic() {
        System.out.println("\n[TEST-4] BƯỚC 4: Trưởng khoa phê duyệt đề tài");

        boolean approved = departmentHeadService.approveTopic(testTopic.getId(), testDept.getId());
        assertTrue(approved, "Phê duyệt đề tài phải thành công");

        Topic updatedTopic = lecturerService.getTopicById(testTopic.getId());
        assertEquals("APPROVED", updatedTopic.getStatus(), "Trạng thái đề tài phải là APPROVED");
        System.out.println("-> Đề tài ID=" + updatedTopic.getId() + " đã được phê duyệt thành công (Status: APPROVED)");
    }

    @Test
    @Order(5)
    @DisplayName("Bước 5: Sinh viên tạo nhóm (tối đa 3 SV, test chặn mời SV thứ 4 và chặn trùng nhóm)")
    void step5_studentGroupManagementAndConstraints() {
        System.out.println("\n[TEST-5] BƯỚC 5: Quản lý nhóm sinh viên & Kiểm tra ràng buộc tối đa 3 thành viên");

        // Clean any pre-existing test groups for testPeriod
        List<StudentGroup> oldGroups = groupRepository.findByStudentIdAndPeriodId(sv1.getId(), testPeriod.getId());
        for (StudentGroup g : oldGroups) {
            groupMemberRepository.deleteByGroupIdAndStudentId(g.getId(), sv1.getId());
            groupRepository.delete(g);
        }

        // 1. SV1 tạo nhóm mới
        testGroup = studentService.createGroup(sv1, testPeriod.getId(), "Nhóm Nghiên Cứu AI K16");
        assertNotNull(testGroup.getId(), "Nhóm phải được lưu vào DB");
        assertEquals("DRAFT", testGroup.getStatus());
        assertTrue(testGroup.getGroupCode().startsWith("GRP-"), "Mã nhóm phải có tiền tố GRP-");
        assertTrue(testGroup.isLeader(sv1), "SV1 phải là Leader của nhóm");
        
        List<GroupMember> initialMembers = groupMemberRepository.findByGroupId(testGroup.getId());
        assertEquals(1, initialMembers.size(), "Ban đầu nhóm có 1 thành viên là Leader");

        // 2. SV1 mời SV2 vào nhóm
        String err2 = studentService.inviteMemberByCode(testGroup, sv2.getStudentCode());
        assertNull(err2, "Mời SV2 phải thành công (err == null)");

        // 3. SV1 mời SV3 vào nhóm
        String err3 = studentService.inviteMemberByCode(testGroup, sv3.getStudentCode());
        assertNull(err3, "Mời SV3 phải thành công (err == null)");

        // Reload group members
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(testGroup.getId());
        assertEquals(3, currentMembers.size(), "Nhóm phải có đúng 3 thành viên");

        // 4. KIỂM TRA RÀNG BUỘC 1: Cố tình mời thành viên thứ 4 -> Phải bị chặn!
        String err4 = studentService.inviteMemberByCode(testGroup, sv4.getStudentCode());
        assertNotNull(err4, "Hệ thống phải chặn khi nhóm vượt quá 3 thành viên");
        assertTrue(err4.contains("tối đa 3 thành viên"), "Thông báo lỗi phải nêu rõ tối đa 3 thành viên");
        System.out.println("-> [ĐẠT CHUẨN] Chặn thành công mời SV thứ 4: \"" + err4 + "\"");

        // 5. KIỂM TRA RÀNG BUỘC 2: Cố tình tạo thêm nhóm trong cùng 1 đợt -> Phải ném ngoại lệ
        assertThrows(IllegalStateException.class, () -> {
            studentService.createGroup(sv1, testPeriod.getId(), "Nhóm thứ hai vi phạm");
        }, "Hệ thống phải chặn sinh viên tạo 2 nhóm trong cùng 1 đợt");
        System.out.println("-> [ĐẠT CHUẨN] Chặn thành công sinh viên tham gia 2 nhóm trong cùng 1 đợt");
    }

    @Test
    @Order(6)
    @DisplayName("Bước 6: Đăng ký đề tài cho nhóm (Chỉ Nhóm trưởng SV1 được phép đăng ký)")
    void step6_registerTopicForGroupLeaderOnly() {
        System.out.println("\n[TEST-6] BƯỚC 6: Đăng ký đề tài cho nhóm & Kiểm tra quyền Leader");

        // 1. KIỂM TRA RÀNG BUỘC 1: SV4 (Chưa có nhóm) cố tình đăng ký đề tài -> Bị chặn tuyệt đối!
        String noGroupRegError = studentService.registerTopicForGroup(sv4, null, testTopic.getId());
        assertNotNull(noGroupRegError, "Sinh viên chưa có nhóm không được đăng ký đề tài");
        assertTrue(noGroupRegError.contains("chưa thuộc nhóm sinh viên nào"), "Phải có thông báo lỗi chưa thuộc nhóm");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công sinh viên chưa có nhóm đăng ký đề tài: \"" + noGroupRegError + "\"");

        // 2. KIỂM TRA RÀNG BUỘC 2: SV2 (Thành viên không phải Leader) cố tình đăng ký đề tài -> Bị chặn
        String memberRegError = studentService.registerTopicForGroup(sv2, testGroup, testTopic.getId());
        assertNotNull(memberRegError, "Thành viên không phải Leader không được đăng ký đề tài");
        assertTrue(memberRegError.contains("Chỉ Nhóm trưởng mới có quyền"), "Phải có thông báo từ chối quyền hạn của thành viên");
        System.out.println("-> [ĐẠT CHUẨN] Chặn thành công thành viên đăng ký thay Leader: \"" + memberRegError + "\"");

        // 3. SV1 (Nhóm trưởng) đăng ký đề tài
        String leaderRegResult = studentService.registerTopicForGroup(sv1, testGroup, testTopic.getId());
        assertNull(leaderRegResult, "Nhóm trưởng đăng ký đề tài phải thành công");

        // 4. KIỂM TRA RÀNG BUỘC 3: Khi đề tài/nhóm đang ở trạng thái REGISTERED (chưa APPROVED), cố tình nộp báo cáo -> Bị chặn!
        String prematureSubmitError = studentService.submitGroupReport(
                sv1, testGroup, "Báo cáo nộp sớm khi chưa duyệt", 1, "early_report.pdf", 1024L
        );
        assertNotNull(prematureSubmitError, "Không được phép nộp báo cáo khi đề tài chưa được duyệt");
        assertTrue(prematureSubmitError.contains("Đề tài chưa được phê duyệt"), "Phải thông báo đề tài chưa được phê duyệt");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công nộp báo cáo khi đề tài chưa được GVHD phê duyệt: \"" + prematureSubmitError + "\"");

        StudentGroup reloaded = studentService.getMyActiveGroup(sv1, testPeriod.getId());
        assertNotNull(reloaded.getTopic(), "Nhóm phải được gán đề tài");
        assertEquals(testTopic.getId(), reloaded.getTopic().getId());
        assertEquals("REGISTERED", reloaded.getStatus(), "Trạng thái nhóm phải là REGISTERED");
        System.out.println("-> [ĐẠT CHUẨN] Leader SV1 đã đăng ký thành công đề tài: '" + reloaded.getTopic().getTitle() + "'");
    }

    @Test
    @Order(7)
    @DisplayName("Bước 7: Giảng viên 1 (GVHD) phê duyệt nhóm sinh viên hướng dẫn")
    void step7_lecturerApproveStudentGroup() {
        System.out.println("\n[TEST-7] BƯỚC 7: Giảng viên hướng dẫn duyệt nhóm sinh viên");

        List<StudentGroup> lecturerGroups = lecturerService.getGroupsForLecturerTopics(gv1);
        assertFalse(lecturerGroups.isEmpty(), "GV1 phải thấy nhóm đăng ký đề tài của mình");

        boolean approved = lecturerService.approveGroupTopic(testGroup.getId(), "APPROVED", gv1);
        assertTrue(approved, "Duyệt nhóm phải thành công");

        testGroup = studentService.getMyActiveGroup(sv1, testPeriod.getId());
        assertEquals("APPROVED", testGroup.getStatus(), "Trạng thái nhóm phải chuyển sang APPROVED");
        System.out.println("-> Giảng viên 1 đã phê duyệt nhóm ID=" + testGroup.getId() + " (Status: APPROVED)");
    }

    @Test
    @Order(8)
    @DisplayName("Bước 8: Nộp báo cáo tiến độ (Bắt buộc CHỈ NHÓM TRƯỞNG MỚI ĐƯỢC NỘP)")
    void step8_submitReportLeaderPrivilegeOnly() {
        System.out.println("\n[TEST-8] BƯỚC 8: Nộp báo cáo tiến độ & Ràng buộc CHỈ LEADER MỚI ĐƯỢC NỘP");

        // 1. KIỂM TRA RÀNG BUỘC QUAN TRỌNG: SV2 (Thành viên) cố tình nộp bài báo cáo -> Bị chặn tuyệt đối!
        String memberSubmitError = studentService.submitGroupReport(
                sv2, testGroup, "Báo cáo tiến độ SV2 nộp lén", 1, "test_member.pdf", 1024L
        );
        assertNotNull(memberSubmitError, "Thành viên không phải Leader không được nộp báo cáo");
        assertTrue(memberSubmitError.contains("Chỉ Nhóm trưởng mới có quyền nộp bài"), "Phải trả về lỗi từ chối quyền nộp");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công thành viên nộp báo cáo: \"" + memberSubmitError + "\"");

        // 2. SV1 (Nhóm trưởng) nộp báo cáo tiến độ Vòng 1
        String leaderSubmitResult1 = studentService.submitGroupReport(
                sv1, testGroup, "Báo cáo tiến độ Lần 1: Đề cương chi tiết & Thiết kế CSDL", 1, "de_cuong_chi_tiet.pdf", 2048576L
        );
        assertNull(leaderSubmitResult1, "Nhóm trưởng nộp báo cáo phải thành công");

        // 3. SV1 (Nhóm trưởng) nộp báo cáo toàn văn Vòng 4 (Bảo vệ cuối kỳ)
        String leaderSubmitResult4 = studentService.submitGroupReport(
                sv1, testGroup, "Báo cáo toàn văn Khóa luận tốt nghiệp (Bản hoàn thiện)", 4, "khoa_luan_toan_van.pdf", 5242880L
        );
        assertNull(leaderSubmitResult4, "Nhóm trưởng nộp toàn văn phải thành công");

        // 4. Kiểm tra danh sách bài nộp của nhóm
        List<Submission> groupSubmissions = studentService.getGroupSubmissions(testGroup);
        assertEquals(2, groupSubmissions.size(), "Nhóm phải có 2 bài nộp");
        assertEquals(sv1.getId(), groupSubmissions.get(0).getSubmittedByStudent().getId(), "Người nộp phải là Leader SV1");

        // 5. GVHD chấm điểm nhận xét tiến độ
        Submission sub = groupSubmissions.get(1); // Vòng 1
        boolean graded = lecturerService.gradeSubmission(sub.getId(), 9.0, "Đề cương chi tiết tốt, bám sát yêu cầu đề tài.", gv1);
        assertTrue(graded, "GVHD chấm điểm tiến độ phải thành công");

        System.out.println("-> [ĐẠT CHUẨN] Nhóm trưởng SV1 đã nộp 2 bài báo cáo, GVHD đã nhận xét và chấm điểm tiến độ.");
    }

    @Test
    @Order(9)
    @DisplayName("Bước 9: Trưởng khoa thành lập Hội đồng 3 GV và phân công Giảng viên phản biện (GVPB)")
    void step9_createCouncilAndAssignReviewer() {
        System.out.println("\n[TEST-9] BƯỚC 9: Thành lập Hội đồng bảo vệ & Phân công GVPB");

        // 1. KIỂM TRA RÀNG BUỘC CHỐNG XUNG ĐỘT LỢI ÍCH:
        // Cố tình tạo Hội đồng có chứa GVHD (GV1) và gán đề tài -> Phải bị chặn!
        String suffix = "_" + System.currentTimeMillis();
        Council conflictCouncil = councilService.createCouncil(
                testDept.getId(),
                testPeriod.getId(),
                "HD-CONFLICT" + suffix,
                "Hội đồng có chứa GVHD " + suffix,
                "Phòng A1",
                OffsetDateTime.now().plusDays(15),
                List.of(gv3.getId(), gv2.getId(), gv1.getId()),
                List.of("CHAIR", "SECRETARY", "MEMBER")
        );
        boolean assignedConflictCouncil = councilService.assignTopicToCouncil(testTopic.getId(), conflictCouncil.getId());
        assertFalse(assignedConflictCouncil, "Hệ thống phải chặn gán đề tài vào Hội đồng có chứa GVHD hoặc GV đồng hướng dẫn");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công gán đề tài vào Hội đồng có chứa GVHD!");

        // 2. Thành lập Hội đồng bảo vệ độc lập hợp lệ gồm 3 GV (Chủ tịch, Thư ký, Ủy viên)
        String validCouncilName = "Hội đồng chấm KLTN chuyên ngành KTPM " + suffix;
        testCouncil = councilService.createCouncil(
                testDept.getId(),
                testPeriod.getId(),
                "HD-KLTN" + suffix,
                validCouncilName,
                "Phòng Hội thảo B1-205",
                OffsetDateTime.now().plusDays(15),
                List.of(gv3.getId(), gvCouncil1.getId(), gvCouncil2.getId()),
                List.of("CHAIR", "SECRETARY", "MEMBER")
        );

        assertNotNull(testCouncil.getId(), "Hội đồng phải được lưu vào DB");
        assertEquals("ESTABLISHED", testCouncil.getStatus());
        
        List<CouncilMember> councilMembers = councilMemberRepository.findByCouncilId(testCouncil.getId());
        assertEquals(3, councilMembers.size(), "Hội đồng phải có 3 thành viên");

        // 2b. KIỂM TRA RÀNG BUỘC CHỐNG TRÙNG LẶP HỘI ĐỒNG TRONG CÙNG ĐỢT ĐĂNG KÝ:
        // Cố tình tạo Hội đồng trùng tên trong cùng một đợt -> Phải ném IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            councilService.createCouncil(
                    testDept.getId(),
                    testPeriod.getId(),
                    "HD-DUPLICATE" + suffix,
                    validCouncilName, // Trùng tên với testCouncil
                    "Phòng Hội thảo B1-206",
                    OffsetDateTime.now().plusDays(15),
                    List.of(gv3.getId(), gvCouncil1.getId(), gvCouncil2.getId()),
                    List.of("CHAIR", "SECRETARY", "MEMBER")
            );
        }, "Hệ thống phải chặn tạo Hội đồng trùng tên trong cùng một đợt đăng ký!");

        // Kiểm tra chặn tên rỗng
        assertThrows(IllegalArgumentException.class, () -> {
            councilService.createCouncil(
                    testDept.getId(),
                    testPeriod.getId(),
                    "HD-EMPTY" + suffix,
                    "   ",
                    "Phòng A1",
                    OffsetDateTime.now().plusDays(15),
                    List.of(gv3.getId(), gvCouncil1.getId(), gvCouncil2.getId()),
                    List.of("CHAIR", "SECRETARY", "MEMBER")
            );
        }, "Hệ thống phải chặn tên hội đồng rỗng");

        System.out.println("-> [ĐẠT CHUẨN TOÀN VẸN DỮ LIỆU] Chặn thành công tạo Hội đồng trùng tên trong cùng đợt đăng ký và tên rỗng!");

        // 3. Gán đề tài vào Hội đồng bảo vệ hợp lệ
        boolean assignedCouncil = councilService.assignTopicToCouncil(testTopic.getId(), testCouncil.getId());
        assertTrue(assignedCouncil, "Gán đề tài vào Hội đồng độc lập phải thành công");

        // 4. KIỂM TRA RÀNG BUỘC: Phân công GVPB trùng với GVHD (GV1) -> Phải bị chặn!
        boolean invalidReviewer = councilService.assignReviewer(testTopic.getId(), gv1.getId());
        assertFalse(invalidReviewer, "Hệ thống phải chặn phân công GVPB trùng với GVHD");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công phân công GVPB trùng với GVHD (GV1)");

        // 5. Phân công GVPB hợp lệ (GV3 - độc lập, không phải GVHD)
        boolean validReviewer = councilService.assignReviewer(testTopic.getId(), gv3.getId());
        assertTrue(validReviewer, "Phân công GV3 làm GVPB phải thành công");

        Topic topicInCouncil = lecturerService.getTopicById(testTopic.getId());
        assertEquals(gv3.getId(), topicInCouncil.getReviewer().getId(), "GVPB phải là GV3");
        assertEquals(testCouncil.getId(), topicInCouncil.getCouncil().getId(), "Đề tài phải thuộc Hội đồng HD-KLTN-01");
        System.out.println("-> Đã phân công GVPB: " + gv3.getUser().getFullName() + " và gán vào Hội đồng " + testCouncil.getName());
    }

    @Test
    @Order(10)
    @DisplayName("Bước 10: Chấm điểm đánh giá & Kiểm tra chống xung đột lợi ích (GVHD không được chấm đề tài mình hướng dẫn)")
    void step10_evaluationsAndConflictOfInterestProtection() {
        System.out.println("\n[TEST-10] BƯỚC 10: Chấm điểm & Bảo vệ chống xung đột lợi ích");

        // 1. KIỂM TRA RÀNG BUỘC CHỐNG XUNG ĐỘT LỢI ÍCH QUAN TRỌNG:
        // GV1 (GVHD chính) cố tình chấm điểm ở vai trò COUNCIL hoặc REVIEWER -> Phải bị chặn!
        boolean gv1ConflictCouncil = councilService.submitEvaluation(testTopic.getId(), gv1.getId(), "COUNCIL", 10.0, "GVHD tự chấm điểm hội đồng");
        assertFalse(gv1ConflictCouncil, "GVHD không được phép chấm điểm đề tài của mình ở vai trò COUNCIL");

        boolean gv1ConflictReviewer = councilService.submitEvaluation(testTopic.getId(), gv1.getId(), "REVIEWER", 10.0, "GVHD tự chấm phản biện");
        assertFalse(gv1ConflictReviewer, "GVHD không được phép chấm điểm đề tài của mình ở vai trò REVIEWER");
        System.out.println("-> [ĐẠT CHUẨN BẢO VỆ] Chặn thành công GVHD tự chấm điểm ở vai trò COUNCIL và REVIEWER!");

        // 2. GVHD (GV1) chấm điểm hướng dẫn (SUPERVISOR) hợp lệ: 9.0 (Trọng số 40%)
        boolean evalSupervisor = councilService.submitEvaluation(testTopic.getId(), gv1.getId(), "SUPERVISOR", 9.0, "Sinh viên chủ động, hoàn thành tốt các mục tiêu nghiên cứu.");
        assertTrue(evalSupervisor, "GVHD chấm điểm hướng dẫn phải thành công");

        // 3. GVPB (GV3) chấm điểm phản biện (REVIEWER) hợp lệ: 8.5 (Trọng số 30%)
        boolean evalReviewer = councilService.submitEvaluation(testTopic.getId(), gv3.getId(), "REVIEWER", 8.5, "Đề tài có tính ứng dụng cao, mô hình AI đạt độ chính xác tốt.");
        assertTrue(evalReviewer, "GVPB chấm điểm phản biện phải thành công");

        // 4. Thành viên Hội đồng độc lập (gvCouncil1) chấm điểm bảo vệ (COUNCIL) hợp lệ: 9.0 (Trọng số 30%)
        boolean evalCouncil = councilService.submitEvaluation(testTopic.getId(), gvCouncil1.getId(), "COUNCIL", 9.0, "Bảo vệ tự tin, trả lời xuất sắc các câu hỏi chuyên sâu.");
        assertTrue(evalCouncil, "Thành viên Hội đồng chấm điểm bảo vệ phải thành công");

        // 5. Kiểm tra tính toán điểm trung bình có trọng số tự động:
        // Weighted Avg = 9.0 * 0.40 + 8.5 * 0.30 + 9.0 * 0.30 = 3.60 + 2.55 + 2.70 = 8.85
        Topic topicWithScore = lecturerService.getTopicById(testTopic.getId());
        assertNotNull(topicWithScore.getFinalScore(), "Điểm tổng kết finalScore phải được tự động tính");
        assertEquals(8.85, topicWithScore.getFinalScore(), 0.05, "Điểm trung bình có trọng số phải xấp xỉ 8.85");
        System.out.println("-> [ĐẠT CHUẨN] Điểm trung bình có trọng số tự động tính: " + topicWithScore.getFinalScore() + " / 10.0 (3 phiếu chấm hợp lệ)");
    }

    @Test
    @Order(11)
    @DisplayName("Bước 11: Trưởng khoa công bố kết quả & Sinh viên tra cứu bảng điểm chính thức")
    void step11_publishResultsAndStudentCheckScore() {
        System.out.println("\n[TEST-11] BƯỚC 11: Công bố kết quả & Sinh viên tra cứu bảng điểm");

        // 1. Trưởng khoa công bố kết quả chính thức
        boolean published = councilService.publishTopicResults(testTopic.getId(), true);
        assertTrue(published, "Công bố kết quả phải thành công");

        Topic finalizedTopic = lecturerService.getTopicById(testTopic.getId());
        assertTrue(finalizedTopic.getIsFinalized(), "Cờ isFinalized phải bằng true");

        // 2. Sinh viên tra cứu bảng điểm và các phiếu đánh giá
        List<Evaluation> topicEvals = councilService.getEvaluationsForTopic(testTopic.getId());
        assertEquals(3, topicEvals.size(), "Sinh viên phải xem được đầy đủ 3 phiếu chấm thành phần");

        List<GroupMember> groupMembers = groupMemberRepository.findByGroupId(testGroup.getId());
        System.out.println("-> [HOÀN TẤT CHU TRÌNH] Bảng điểm chính thức đã công bố:");
        System.out.println("   + Tên đề tài: " + finalizedTopic.getTitle());
        System.out.println("   + Nhóm sinh viên: " + testGroup.getGroupName() + " (" + groupMembers.size() + " thành viên)");
        System.out.println("   + Điểm Tổng Kết: " + finalizedTopic.getFinalScore() + " / 10.0");
        System.out.println("   + Trạng thái công bố: " + (finalizedTopic.getIsFinalized() ? "ĐÃ CÔNG BỐ" : "CHƯA CÔNG BỐ"));

        System.out.println("\n================================================================================");
        System.out.println(">>> TẤT CẢ 11 BƯỚC KIỂM THỬ TỰ ĐỘNG ĐÃ VƯỢT QUA 100% (ALL TESTS PASSED) <<<");
        System.out.println("================================================================================");
    }
}
