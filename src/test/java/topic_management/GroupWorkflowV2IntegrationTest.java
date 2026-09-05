package topic_management;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import topic_management.dto.RegistrationDtos.*;
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
public class GroupWorkflowV2IntegrationTest {

    @Autowired
    private GroupRegistrationService groupRegistrationService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private LecturerService lecturerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private GroupInvitationRepository invitationRepository;

    @Autowired
    private GroupLeaveRequestRepository leaveRequestRepository;

    @Autowired
    private GroupDissolveRequestRepository dissolveRequestRepository;

    private Department testDept;
    private Major testMajor;
    private RegistrationPeriod testPeriod;

    private AppUser userLeader;
    private Student studentLeader;

    private AppUser userMember1;
    private Student studentMember1;

    private AppUser userMember2;
    private Student studentMember2;

    private AppUser userLecturer;
    private Lecturer lecturer;

    private Topic testTopic;

    @BeforeAll
    public void setupData() {
        Role studentRole = roleRepository.findByCode("STUDENT").orElseGet(() -> {
            Role r = new Role();
            r.setCode("STUDENT");
            r.setName("Sinh viên");
            return roleRepository.save(r);
        });

        Role lecturerRole = roleRepository.findByCode("LECTURER").orElseGet(() -> {
            Role r = new Role();
            r.setCode("LECTURER");
            r.setName("Giảng viên");
            return roleRepository.save(r);
        });

        testDept = departmentRepository.findByCode("CNTT").orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT");
            d.setName("Khoa Công nghệ Thông tin");
            return departmentRepository.save(d);
        });

        testMajor = majorRepository.findByCode("KTPM").orElseGet(() -> {
            Major m = new Major();
            m.setCode("KTPM");
            m.setName("Kỹ thuật Phần mềm");
            m.setDepartment(testDept);
            return majorRepository.save(m);
        });

        testPeriod = new RegistrationPeriod();
        testPeriod.setName("Đợt KLTN 2026-Test-V2");
        testPeriod.setPeriodType("KLTN");
        testPeriod.setAcademicYear("2026-2027");
        testPeriod.setSemester(1);
        testPeriod.setDepartment(testDept);
        testPeriod.setStatus("OPEN");
        testPeriod.setIsActive(true);
        testPeriod.setMinStudents(2);
        testPeriod.setMaxStudents(3);
        testPeriod.setStudentRegistrationStart(OffsetDateTime.now().minusDays(1));
        testPeriod.setStudentRegistrationEnd(OffsetDateTime.now().plusDays(10));
        testPeriod = periodRepository.save(testPeriod);

        // Tạo user leader
        userLeader = createOrGetUser("sv_leader_v2", "Leader V2", studentRole);
        studentLeader = createOrGetStudent(userLeader, "SV99001", testMajor);

        // Tạo user member 1
        userMember1 = createOrGetUser("sv_mem1_v2", "Member 1 V2", studentRole);
        studentMember1 = createOrGetStudent(userMember1, "SV99002", testMajor);

        // Tạo user member 2
        userMember2 = createOrGetUser("sv_mem2_v2", "Member 2 V2", studentRole);
        studentMember2 = createOrGetStudent(userMember2, "SV99003", testMajor);

        // Tạo user lecturer
        userLecturer = createOrGetUser("gv_test_v2", "Lecturer Test V2", lecturerRole);
        lecturer = createOrGetLecturer(userLecturer, "GV991", testDept);

        // Tạo đề tài
        testTopic = new Topic();
        testTopic.setCode("DT-TEST-V2-" + System.currentTimeMillis() % 10000);
        testTopic.setTitle("Hệ thống Nghiên cứu V2 Đồ án");
        testTopic.setSupervisor(lecturer);
        testTopic.setMajor(testMajor);
        testTopic.setPeriod(testPeriod);
        testTopic.setStatus("APPROVED");
        testTopic.setMaxStudents(3);
        testTopic = topicRepository.save(testTopic);
    }

    private AppUser createOrGetUser(String username, String fullName, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername(username);
            u.setPasswordHash("$2a$10$hashedpassword");
            u.setFullName(fullName);
            u.setEmail(username + "@test.edu.vn");
            u.setRole(role);
            return userRepository.save(u);
        });
    }

    private Student createOrGetStudent(AppUser u, String code, Major m) {
        return studentRepository.findByUserId(u.getId()).orElseGet(() -> {
            Student s = new Student();
            s.setUser(u);
            s.setStudentCode(code);
            s.setMajor(m);
            s.setClassName("K16-TEST");
            s.setAcademicYear("2026-2027");
            s.setGpa(3.8);
            return studentRepository.save(s);
        });
    }

    private Lecturer createOrGetLecturer(AppUser u, String code, Department d) {
        return lecturerRepository.findByUserId(u.getId()).orElseGet(() -> {
            Lecturer l = new Lecturer();
            l.setUser(u);
            l.setLecturerCode(code);
            l.setDepartment(d);
            l.setMaxTopicsQuota(5);
            return lecturerRepository.save(l);
        });
    }

    @Test
    @Order(1)
    @Transactional
    public void testTwoWayHandshakeInvitationFlow() {
        // 1. Leader tạo nhóm DRAFT
        StudentGroup groupA = studentService.createGroup(studentLeader, testPeriod.getId(), "Nhóm Alpha V2");
        assertNotNull(groupA.getId());
        assertEquals("DRAFT", groupA.getStatus());

        // 2. Leader gửi lời mời cho Member 1
        InvitationResponseDto invDto1 = groupRegistrationService.sendInvitation(userLeader, groupA.getId(), studentMember1.getStudentCode());
        assertNotNull(invDto1.getId());
        assertEquals("INVITED", invDto1.getStatus());
        assertEquals(studentMember1.getId(), invDto1.getInviteeStudentId());

        // Kiểm tra Sinh viên 1 chưa phải là member trong GroupMember
        Optional<GroupMember> gmCheck = groupMemberRepository.findByGroupIdAndStudentId(groupA.getId(), studentMember1.getId());
        assertTrue(gmCheck.isEmpty(), "Sinh viên được mời KHÔNG được vào thẳng nhóm trước khi chấp nhận!");

        // 3. Member 1 kiểm tra danh sách lời mời của mình
        List<InvitationResponseDto> myInvs = groupRegistrationService.getMyPendingInvitations(userMember1);
        assertEquals(1, myInvs.size());
        assertEquals(invDto1.getId(), myInvs.get(0).getId());

        // 4. Member 1 chấp nhận lời mời
        InvitationResponseDto accepted = groupRegistrationService.acceptInvitation(userMember1, invDto1.getId());
        assertEquals("ACCEPTED", accepted.getStatus());

        // Kiểm tra Member 1 đã vào GroupMember với role MEMBER
        Optional<GroupMember> gmAfter = groupMemberRepository.findByGroupIdAndStudentId(groupA.getId(), studentMember1.getId());
        assertTrue(gmAfter.isPresent());
        assertEquals("MEMBER", gmAfter.get().getRoleInGroup());

        // 5. Test từ chối lời mời: Leader mời Member 2 vào nhóm -> Member 2 từ chối
        InvitationResponseDto invDto2 = groupRegistrationService.sendInvitation(userLeader, groupA.getId(), studentMember2.getStudentCode());
        assertEquals("INVITED", invDto2.getStatus());

        ApiResponse<Void> rejectRes = groupRegistrationService.rejectInvitation(userMember2, invDto2.getId(), "Bận đề tài khác");
        assertTrue(rejectRes.isSuccess());

        // Bản ghi lời mời đã bị xóa để Member 2 tự do
        Optional<GroupInvitation> rejectedOpt = invitationRepository.findById(invDto2.getId());
        assertTrue(rejectedOpt.isEmpty());
    }

    @Test
    @Order(2)
    @Transactional
    public void testDraftStageLeaveTransferAndDissolve() {
        // 1. Tạo nhóm mới với 2 thành viên
        StudentGroup group = studentService.createGroup(studentLeader, testPeriod.getId(), "Nhóm Beta Draft");
        
        // Mời và thêm member 1
        InvitationResponseDto inv = groupRegistrationService.sendInvitation(userLeader, group.getId(), studentMember1.getStudentCode());
        groupRegistrationService.acceptInvitation(userMember1, inv.getId());

        assertEquals(2, groupMemberRepository.findByGroupId(group.getId()).size());

        // 2. Member 1 tự do rời nhóm trong giai đoạn DRAFT
        ApiResponse<Void> leaveRes = groupRegistrationService.leaveGroupDraft(userMember1, group.getId(), studentMember1.getId());
        assertTrue(leaveRes.isSuccess());
        assertEquals(1, groupMemberRepository.findByGroupId(group.getId()).size());

        // 3. Leader cố tự ý rời nhóm -> Bị chặn (phải nhượng quyền hoặc hủy nhóm)
        assertThrows(ResponseStatusException.class, () -> {
            groupRegistrationService.leaveGroupDraft(userLeader, group.getId(), studentLeader.getId());
        });

        // 4. Mời lại member 1 để test Transfer Leadership
        InvitationResponseDto inv2 = groupRegistrationService.sendInvitation(userLeader, group.getId(), studentMember1.getStudentCode());
        groupRegistrationService.acceptInvitation(userMember1, inv2.getId());

        ApiResponse<Void> transferRes = groupRegistrationService.transferLeadership(userLeader, group.getId(), studentMember1.getId());
        assertTrue(transferRes.isSuccess());

        StudentGroup updatedGroup = groupRepository.findById(group.getId()).get();
        assertEquals(studentMember1.getId(), updatedGroup.getLeader().getId(), "Leader mới phải là studentMember1");

        // 5. Leader mới bấm Hủy nhóm (Dissolve Group)
        ApiResponse<Void> dissolveRes = groupRegistrationService.dissolveGroupDraft(userMember1, group.getId());
        assertTrue(dissolveRes.isSuccess());
        assertTrue(groupRepository.findById(group.getId()).isEmpty(), "Nhóm DRAFT sau khi dissolve phải bị xóa hoàn toàn");
    }

    @Test
    @Order(3)
    @Transactional
    public void testEC02MemberLeaveWithWarning() {
        // Tạo nhóm có 2 thành viên và đã duyệt đề tài (APPROVED stage)
        StudentGroup group = studentService.createGroup(studentLeader, testPeriod.getId(), "Nhóm Gamma EC02");
        InvitationResponseDto inv = groupRegistrationService.sendInvitation(userLeader, group.getId(), studentMember1.getStudentCode());
        groupRegistrationService.acceptInvitation(userMember1, inv.getId());

        // Gán đề tài và duyệt đề tài
        group.setTopic(testTopic);
        group.setStatus("APPROVED");
        group = groupRepository.save(group);

        // 1. Member 1 gửi yêu cầu xin rút nhóm (EC-02)
        LeaveRequestDto reqDto = groupRegistrationService.requestLeaveApprovedGroup(userMember1, group.getId(), "Lý do cá nhân cần rút nhóm");
        assertNotNull(reqDto.getId());
        assertEquals("PENDING", reqDto.getStatus());

        // 2. GVHD xem danh sách yêu cầu
        List<LeaveRequestDto> pendingList = groupRegistrationService.getPendingLeaveRequestsForLecturer(userLecturer);
        assertTrue(pendingList.stream().anyMatch(r -> r.getId().equals(reqDto.getId())));

        // 3. GVHD duyệt yêu cầu rút nhóm -> Kích hoạt EC-02 vì sĩ số còn lại = 1 (< minStudents = 2)
        ApiResponse<LeaveRequestDto> approveRes = groupRegistrationService.approveLeaveRequest(userLecturer, reqDto.getId());
        assertTrue(approveRes.isSuccess());
        assertNotNull(approveRes.getWarning(), "Phải bắn cảnh báo EC-02 khi sĩ số < minStudents!");

        StudentGroup groupAfter = groupRepository.findById(group.getId()).get();
        assertEquals("PENDING_MEMBER", groupAfter.getStatus(), "Trạng thái nhóm phải chuyển thành PENDING_MEMBER theo EC-02!");
        assertEquals(1, groupMemberRepository.findByGroupId(group.getId()).size());
    }

    @Test
    @Order(4)
    @Transactional
    public void testEC03DissolveApprovedGroupAndFreeTopic() {
        // Tạo nhóm có đề tài APPROVED
        StudentGroup group = studentService.createGroup(studentLeader, testPeriod.getId(), "Nhóm Delta EC03");
        group.setTopic(testTopic);
        group.setStatus("APPROVED");
        group = groupRepository.save(group);

        // 1. Nhóm trưởng gửi yêu cầu hủy nhóm / đề tài (EC-03)
        DissolveRequestDto dissolveDto = groupRegistrationService.requestDissolveApprovedGroup(userLeader, group.getId(), "Nhóm không thể tiếp tục thực hiện đề tài");
        assertNotNull(dissolveDto.getId());
        assertEquals("PENDING", dissolveDto.getStatus());

        // 2. GVHD xem danh sách và duyệt yêu cầu hủy nhóm
        ApiResponse<DissolveRequestDto> approveRes = groupRegistrationService.approveDissolveRequest(userLecturer, dissolveDto.getId());
        assertTrue(approveRes.isSuccess());

        // 3. Kiểm tra Đề tài quay lại APPROVED
        Topic topicAfter = topicRepository.findById(testTopic.getId()).get();
        assertEquals("APPROVED", topicAfter.getStatus());

        // 4. Kiểm tra nhóm đã được giải tán và các thành viên được giải phóng
        StudentGroup groupAfter = groupRepository.findById(group.getId()).get();
        assertEquals("DISSOLVED", groupAfter.getStatus());
        assertNull(groupAfter.getTopic());
        assertTrue(groupMemberRepository.findByGroupId(group.getId()).isEmpty(), "Toàn bộ thành viên nhóm đã được giải phóng tự do");
    }

    @Test
    @Order(5)
    @Transactional
    public void testMergeGroupsUnderEnrolled() {
        // 1. Tạo Nhóm Nguồn (Group Source: 1 SV)
        StudentGroup sourceGroup = studentService.createGroup(studentLeader, testPeriod.getId(), "Nhóm Nguồn 1SV");
        final Long sourceId = sourceGroup.getId();
        assertEquals(1, groupMemberRepository.findByGroupId(sourceId).size());

        // 2. Tạo Nhóm Đích (Group Target: 1 SV, trạng thái PENDING_MEMBER do thiếu người)
        StudentGroup targetGroup = studentService.createGroup(studentMember1, testPeriod.getId(), "Nhóm Đích 1SV");
        targetGroup.setTopic(testTopic);
        targetGroup.setStatus("PENDING_MEMBER");
        StudentGroup savedTarget = groupRepository.save(targetGroup);
        final Long targetId = savedTarget.getId();
        assertEquals(1, groupMemberRepository.findByGroupId(targetId).size());

        // 3. GVHD lấy danh sách nhóm thiếu người
        List<UnderEnrolledGroupDto> underEnrolledList = groupRegistrationService.getUnderEnrolledGroups(userLecturer, testPeriod.getId());
        assertTrue(underEnrolledList.stream().anyMatch(g -> g.getGroupId().equals(sourceId)));
        assertTrue(underEnrolledList.stream().anyMatch(g -> g.getGroupId().equals(targetId)));

        // 4. GVHD thực hiện gộp Nhóm Nguồn vào Nhóm Đích
        ApiResponse<UnderEnrolledGroupDto> mergeRes = groupRegistrationService.mergeGroups(userLecturer, sourceId, targetId);
        assertTrue(mergeRes.isSuccess());

        // 5. Kiểm tra Nhóm Đích sau khi gộp: có 2 thành viên, trạng thái chuyển thành APPROVED
        StudentGroup targetAfter = groupRepository.findById(targetId).get();
        assertEquals("APPROVED", targetAfter.getStatus(), "Nhóm đích sau khi gộp đủ 2 thành viên phải chuyển thành APPROVED");
        assertEquals(2, groupMemberRepository.findByGroupId(targetId).size());

        // 6. Kiểm tra Nhóm Nguồn sau khi gộp: đã bị xóa / giải tán
        assertTrue(groupRepository.findById(sourceId).isEmpty(), "Nhóm nguồn sau khi gộp phải bị giải tán/xóa");
    }
}
