package topic_management.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import topic_management.dto.RegistrationDtos.*;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupRegistrationService {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupLeaveRequestRepository groupLeaveRequestRepository;
    private final GroupDissolveRequestRepository groupDissolveRequestRepository;
    private final TopicRepository topicRepository;
    private final TopicRegistrationRepository topicRegistrationRepository;
    private final StudentService studentService;

    public GroupRegistrationService(
            StudentRepository studentRepository,
            LecturerRepository lecturerRepository,
            StudentGroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            GroupInvitationRepository groupInvitationRepository,
            GroupLeaveRequestRepository groupLeaveRequestRepository,
            GroupDissolveRequestRepository groupDissolveRequestRepository,
            TopicRepository topicRepository,
            TopicRegistrationRepository topicRegistrationRepository,
            StudentService studentService) {
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupInvitationRepository = groupInvitationRepository;
        this.groupLeaveRequestRepository = groupLeaveRequestRepository;
        this.groupDissolveRequestRepository = groupDissolveRequestRepository;
        this.topicRepository = topicRepository;
        this.topicRegistrationRepository = topicRegistrationRepository;
        this.studentService = studentService;
    }

    // ==================== HELPER AUTH METHODS ====================

    private Student getStudentFromUser(AppUser user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }
        return studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ sinh viên!"));
    }

    private Lecturer getLecturerFromUser(AppUser user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }
        return lecturerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Không tìm thấy hồ sơ giảng viên!"));
    }

    private StudentGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm sinh viên ID=" + groupId));
    }

    private boolean isGroupInDraftStage(StudentGroup group) {
        if (group == null) return true;
        // Group is in DRAFT stage if no topic or status is DRAFT/REJECTED/null
        if (group.getTopic() == null) return true;
        String status = group.getStatus();
        return status == null || "DRAFT".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status);
    }

    // ==================== 1. LUỒNG BẮT TAY 2 CHIỀU (INVITATIONS) ====================

    @Transactional(rollbackFor = Exception.class)
    public InvitationResponseDto sendInvitation(AppUser user, Long groupId, String studentCode) {
        Student leader = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        // Kiểm tra quyền Trưởng nhóm
        if (!group.isLeader(leader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng mới có quyền mời thành viên vào nhóm!");
        }

        // Kiểm tra số lượng thành viên tối đa
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());
        int maxAllowed = 3;
        if (group.getTopic() != null && group.getTopic().getMaxStudents() != null && group.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, group.getTopic().getMaxStudents());
        }
        if (currentMembers.size() >= maxAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm đã đạt số lượng tối đa " + maxAllowed + " thành viên!");
        }

        // Tìm sinh viên được mời
        if (studentCode == null || studentCode.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã sinh viên không được để trống!");
        }
        Student invitee = studentRepository.findByStudentCode(studentCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên có mã: " + studentCode));

        // Không được tự mời chính mình
        if (invitee.getId().equals(leader.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn là nhóm trưởng, không thể tự gửi lời mời cho chính mình!");
        }

        // GLOBAL VALIDATION: Kiểm tra tính đồng bộ Khoa cho sinh viên được mời
        RegistrationPeriod period = group.getPeriod();
        if (period != null) {
            studentService.validateStudentDepartmentInPeriod(invitee, period);
        }

        // Kiểm tra sinh viên đã ở trong nhóm nào trong đợt này chưa
        Long periodId = period != null ? period.getId() : null;
        List<StudentGroup> existing = groupRepository.findByStudentIdAndPeriodId(invitee.getId(), periodId);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sinh viên " + invitee.getUser().getFullName() + " (" + studentCode + ") đã tham gia một nhóm khác trong đợt này!");
        }

        // Kiểm tra đã có lời mời INVITED đang chờ từ nhóm này chưa
        Optional<GroupInvitation> pendingOpt = groupInvitationRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), invitee.getId(), "INVITED");
        if (pendingOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đã gửi lời mời đến sinh viên này rồi và đang chờ phản hồi!");
        }

        // Tạo lời mời mới ở trạng thái INVITED
        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setStudent(invitee);
        invitation.setInvitedBy(leader);
        invitation.setStatus("INVITED");
        invitation.setCreatedAt(OffsetDateTime.now());

        GroupInvitation saved = groupInvitationRepository.save(invitation);
        System.out.println("[INVITATION-V2] Trưởng nhóm " + leader.getStudentCode() + " đã gửi lời mời tham gia nhóm " + group.getGroupCode() + " tới SV " + invitee.getStudentCode());

        return mapToInvitationDto(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public InvitationResponseDto acceptInvitation(AppUser user, Long invitationId) {
        Student student = getStudentFromUser(user);

        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lời mời ID=" + invitationId));

        // Bảo mật IDOR: Chỉ đúng sinh viên nhận lời mời mới được chấp nhận
        if (!invitation.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền phản hồi lời mời này!");
        }

        if (!"INVITED".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lời mời này không còn ở trạng thái chờ phản hồi (Status=" + invitation.getStatus() + ")!");
        }

        StudentGroup group = invitation.getGroup();
        RegistrationPeriod period = group.getPeriod();
        Long periodId = period != null ? period.getId() : null;

        // Kiểm tra sinh viên có bị trùng nhóm khác không
        List<StudentGroup> existing = groupRepository.findByStudentIdAndPeriodId(student.getId(), periodId);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã là thành viên của một nhóm khác trong đợt này rồi!");
        }

        // Kiểm tra sĩ số nhóm hiện tại
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());
        int maxAllowed = 3;
        if (group.getTopic() != null && group.getTopic().getMaxStudents() != null && group.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, group.getTopic().getMaxStudents());
        }
        if (currentMembers.size() >= maxAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm đã đủ số lượng thành viên tối đa (" + maxAllowed + " SV)!");
        }

        // 1. Cập nhật lời mời thành ACCEPTED
        invitation.setStatus("ACCEPTED");
        invitation.setRespondedAt(OffsetDateTime.now());
        groupInvitationRepository.save(invitation);

        // 2. Thêm sinh viên vào nhóm làm MEMBER chính thức
        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(student);
        gm.setRoleInGroup("MEMBER");
        gm.setJoinedAt(OffsetDateTime.now());
        groupMemberRepository.save(gm);

        // 3. Tự động xóa/hủy tất cả các lời mời INVITED khác gửi tới sinh viên này để khóa quyền tham gia nhóm khác
        groupInvitationRepository.deletePendingInvitationsForStudent(student.getId());

        System.out.println("[INVITATION-V2] Sinh viên " + student.getStudentCode() + " đã chấp nhận lời mời vào nhóm " + group.getGroupCode() + ". Đã xóa toàn bộ lời mời khác.");

        return mapToInvitationDto(invitation);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> rejectInvitation(AppUser user, Long invitationId, String reason) {
        Student student = getStudentFromUser(user);

        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lời mời ID=" + invitationId));

        // Bảo mật IDOR
        if (!invitation.getStudent().getId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền từ chối lời mời này!");
        }

        if (!"INVITED".equalsIgnoreCase(invitation.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lời mời này không ở trạng thái chờ!");
        }

        // Xóa bản ghi lời mời theo đặc tả để sinh viên tự do nhận lời mời khác hoặc tự tạo nhóm
        groupInvitationRepository.delete(invitation);

        System.out.println("[INVITATION-V2] Sinh viên " + student.getStudentCode() + " đã từ chối và xóa lời mời ID=" + invitationId);
        return ApiResponse.ok("Đã từ chối lời mời tham gia nhóm thành công!", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> cancelInvitation(AppUser user, Long invitationId) {
        Student leader = getStudentFromUser(user);

        GroupInvitation invitation = groupInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lời mời ID=" + invitationId));

        if (!invitation.getGroup().isLeader(leader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng mới có quyền hủy lời mời!");
        }

        groupInvitationRepository.delete(invitation);
        System.out.println("[INVITATION-V2] Nhóm trưởng " + leader.getStudentCode() + " đã hủy lời mời ID=" + invitationId);
        return ApiResponse.ok("Đã hủy lời mời thành công!", null);
    }

    public List<InvitationResponseDto> getMyPendingInvitations(AppUser user) {
        Student student = getStudentFromUser(user);
        return groupInvitationRepository.findByStudentIdAndStatus(student.getId(), "INVITED").stream()
                .map(this::mapToInvitationDto)
                .collect(Collectors.toList());
    }

    public List<InvitationResponseDto> getGroupInvitations(AppUser user, Long groupId) {
        Student student = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        // Phải là thành viên nhóm mới được xem lời mời
        boolean isMember = groupMemberRepository.findByGroupIdAndStudentId(group.getId(), student.getId()).isPresent();
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc nhóm này!");
        }

        return groupInvitationRepository.findByGroupId(group.getId()).stream()
                .map(this::mapToInvitationDto)
                .collect(Collectors.toList());
    }

    // ==================== 2. GIAI ĐOẠN DRAFT (OUT NHÓM & HỦY NHÓM TỰ DO) ====================

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> leaveGroupDraft(AppUser user, Long groupId, Long targetStudentId) {
        Student currentStudent = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        Long studentIdToRemove = (targetStudentId != null) ? targetStudentId : currentStudent.getId();

        // Kiểm tra xem nhóm có đang ở giai đoạn DRAFT không
        if (!isGroupInDraftStage(group)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đề tài của nhóm đã được đăng ký/phê duyệt. Thành viên muốn rút khỏi nhóm phải gửi yêu cầu xin rút lên Giảng viên hướng dẫn duyệt!");
        }

        // Quyền thực hiện: Tự rời nhóm HOẶC Nhóm trưởng đuổi thành viên
        boolean isSelf = currentStudent.getId().equals(studentIdToRemove);
        boolean isLeader = group.isLeader(currentStudent);

        if (!isSelf && !isLeader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa thành viên này khỏi nhóm!");
        }

        // Nếu người muốn rời là LEADER -> Chặn lại
        if (group.isLeader(currentStudent) && isSelf) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhóm trưởng không được rời nhóm trừ khi nhượng quyền (Transfer Leadership) hoặc bấm nút Hủy nhóm (Dissolve Group)!");
        }

        groupMemberRepository.deleteByGroupIdAndStudentId(group.getId(), studentIdToRemove);
        System.out.println("[DRAFT-LEAVE] Đã xóa sinh viên ID=" + studentIdToRemove + " khỏi nhóm " + group.getGroupCode() + " (Giai đoạn DRAFT)");

        return ApiResponse.ok("Đã rời khỏi nhóm thành công!", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> transferLeadership(AppUser user, Long groupId, Long newLeaderStudentId) {
        Student currentLeader = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        if (!group.isLeader(currentLeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng hiện tại mới có quyền nhượng quyền trưởng nhóm!");
        }

        if (newLeaderStudentId == null || newLeaderStudentId.equals(currentLeader.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn một thành viên khác trong nhóm để nhượng quyền!");
        }

        Student newLeader = studentRepository.findById(newLeaderStudentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sinh viên được chỉ định làm Leader mới!"));

        GroupMember newLeaderMember = groupMemberRepository.findByGroupIdAndStudentId(group.getId(), newLeader.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên được chỉ định không phải là thành viên trong nhóm!"));

        GroupMember oldLeaderMember = groupMemberRepository.findByGroupIdAndStudentId(group.getId(), currentLeader.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin nhóm trưởng hiện tại trong nhóm!"));

        // Đổi vai trò
        oldLeaderMember.setRoleInGroup("MEMBER");
        newLeaderMember.setRoleInGroup("LEADER");
        groupMemberRepository.save(oldLeaderMember);
        groupMemberRepository.save(newLeaderMember);

        group.setLeader(newLeader);
        groupRepository.save(group);

        System.out.println("[LEADER-TRANSFER] Nhóm " + group.getGroupCode() + " đã chuyển quyền Trưởng nhóm từ " + currentLeader.getStudentCode() + " sang " + newLeader.getStudentCode());

        return ApiResponse.ok("Nhượng quyền Trưởng nhóm thành công cho " + newLeader.getUser().getFullName(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> dissolveGroupDraft(AppUser user, Long groupId) {
        Student leader = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        if (!group.isLeader(leader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng mới có quyền giải tán / hủy nhóm!");
        }

        if (!isGroupInDraftStage(group)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đề tài của nhóm đã được đăng ký/phê duyệt. Nhóm trưởng muốn hủy nhóm/hủy đề tài phải gửi yêu cầu lên Giảng viên hướng dẫn duyệt (EC-03)!");
        }

        // Xóa tất cả lời mời của nhóm
        groupInvitationRepository.deleteByGroupId(group.getId());

        // Xóa tất cả thành viên của nhóm
        groupMemberRepository.findByGroupId(group.getId()).forEach(groupMemberRepository::delete);

        // Xóa đăng ký đề tài nếu có
        topicRegistrationRepository.deleteByGroupId(group.getId());

        // Xóa nhóm
        groupRepository.delete(group);

        System.out.println("[DISSOLVE-DRAFT] Nhóm trưởng " + leader.getStudentCode() + " đã giải tán nhóm " + group.getGroupCode() + ". Mọi thành viên đã tự do.");
        return ApiResponse.ok("Đã giải tán nhóm thành công! Tất cả thành viên đã được giải phóng.", null);
    }

    // ==================== 3. GIAI ĐOẠN ĐANG THỰC HIỆN: EC-02 RÚT NHÓM ====================

    @Transactional(rollbackFor = Exception.class)
    public LeaveRequestDto requestLeaveApprovedGroup(AppUser user, Long groupId, String reason) {
        Student student = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        // Phải là thành viên trong nhóm
        GroupMember member = groupMemberRepository.findByGroupIdAndStudentId(group.getId(), student.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không phải thành viên nhóm này!"));

        // Nếu là Leader, phải dùng luồng Dissolve Request (EC-03) hoặc transfer leadership trước
        if (group.isLeader(student)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhóm trưởng không thể tạo yêu cầu rút cá nhân. Vui lòng nhượng quyền Trưởng nhóm trước hoặc tạo yêu cầu Hủy nhóm/đề tài (EC-03)!");
        }

        if (isGroupInDraftStage(group)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhóm đang ở giai đoạn DRAFT, bạn có thể tự do rời nhóm trực tiếp mà không cần GVHD duyệt.");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do xin rút khỏi nhóm!");
        }

        // Kiểm tra xem đã có request PENDING chưa
        Optional<GroupLeaveRequest> pendingOpt = groupLeaveRequestRepository.findByGroupIdAndStudentIdAndStatus(group.getId(), student.getId(), "PENDING");
        if (pendingOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã gửi yêu cầu rút nhóm rồi và đang chờ GVHD phê duyệt!");
        }

        GroupLeaveRequest req = new GroupLeaveRequest();
        req.setGroup(group);
        req.setStudent(student);
        req.setReason(reason.trim());
        req.setStatus("PENDING");
        req.setCreatedAt(OffsetDateTime.now());

        GroupLeaveRequest saved = groupLeaveRequestRepository.save(req);
        System.out.println("[EC-02-REQUEST] Sinh viên " + student.getStudentCode() + " đã gửi yêu cầu rút khỏi nhóm " + group.getGroupCode() + " tới GVHD");

        return mapToLeaveRequestDto(saved);
    }

    public List<LeaveRequestDto> getPendingLeaveRequestsForLecturer(AppUser user) {
        if (user == null || user.getId() == null) return Collections.emptyList();
        Lecturer lecturer = lecturerRepository.findByUserId(user.getId()).orElse(null);
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return groupLeaveRequestRepository.findBySupervisorIdAndStatus(lecturer.getId(), "PENDING").stream()
                .map(this::mapToLeaveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<LeaveRequestDto> approveLeaveRequest(AppUser user, Long requestId) {
        Lecturer lecturer = getLecturerFromUser(user);

        GroupLeaveRequest req = groupLeaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu rút nhóm ID=" + requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu này đã được xử lý trước đó!");
        }

        StudentGroup group = req.getGroup();
        Topic topic = group.getTopic();

        // Kiểm tra quyền: Chỉ GVHD hoặc GVHD phụ của đề tài mới được duyệt
        if (topic == null || !topic.isSupervisorOrCoSupervisor(lecturer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải GVHD của nhóm/đề tài này!");
        }

        // 1. Phê duyệt yêu cầu
        req.setStatus("APPROVED");
        req.setResolvedBy(lecturer);
        req.setResolvedAt(OffsetDateTime.now());
        groupLeaveRequestRepository.save(req);

        // 2. Xóa thành viên khỏi nhóm
        groupMemberRepository.deleteByGroupIdAndStudentId(group.getId(), req.getStudent().getId());

        // 3. KÍCH HOẠT EC-02: Kiểm tra sĩ số còn lại với minStudents của Đợt
        List<GroupMember> remainingMembers = groupMemberRepository.findByGroupId(group.getId());
        int remainingCount = remainingMembers.size();
        int minStudents = 2;
        if (group.getPeriod() != null && group.getPeriod().getMinStudents() != null && group.getPeriod().getMinStudents() > 0) {
            minStudents = group.getPeriod().getMinStudents();
        }

        String warningMessage = null;
        if (remainingCount < minStudents) {
            group.setStatus("PENDING_MEMBER");
            groupRepository.save(group);
            warningMessage = "CẢNH BÁO EC-02: Sau khi thành viên rút nhóm, sĩ số còn lại (" + remainingCount
                    + " SV) nhỏ hơn quy định tối thiểu của Đợt (" + minStudents
                    + " SV). Trạng thái nhóm đã được chuyển thành 'PENDING_MEMBER'. Nhóm cần bổ sung thêm thành viên!";
            System.out.println("[EC-02-TRIGGERED] " + warningMessage);
        } else {
            System.out.println("[EC-02-APPROVED] Đã duyệt cho sinh viên " + req.getStudent().getStudentCode() + " rút khỏi nhóm " + group.getGroupCode() + ". Sĩ số còn lại: " + remainingCount);
        }

        LeaveRequestDto dto = mapToLeaveRequestDto(req);
        if (warningMessage != null) {
            return ApiResponse.okWithWarning("Đã phê duyệt yêu cầu rút nhóm thành công!", dto, warningMessage);
        }
        return ApiResponse.ok("Đã phê duyệt yêu cầu rút nhóm thành công!", dto);
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<LeaveRequestDto> rejectLeaveRequest(AppUser user, Long requestId, String rejectionReason) {
        Lecturer lecturer = getLecturerFromUser(user);

        GroupLeaveRequest req = groupLeaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu rút nhóm ID=" + requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu này đã được xử lý trước đó!");
        }

        StudentGroup group = req.getGroup();
        Topic topic = group.getTopic();

        if (topic == null || !topic.isSupervisorOrCoSupervisor(lecturer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải GVHD của nhóm/đề tài này!");
        }

        req.setStatus("REJECTED");
        req.setRejectionReason(rejectionReason != null ? rejectionReason.trim() : "GVHD từ chối yêu cầu rút nhóm");
        req.setResolvedBy(lecturer);
        req.setResolvedAt(OffsetDateTime.now());
        groupLeaveRequestRepository.save(req);

        System.out.println("[EC-02-REJECTED] GVHD " + lecturer.getLecturerCode() + " đã từ chối yêu cầu rút nhóm của SV " + req.getStudent().getStudentCode());
        return ApiResponse.ok("Đã từ chối yêu cầu rút nhóm.", mapToLeaveRequestDto(req));
    }

    // ==================== 4. GIAI ĐOẠN ĐANG THỰC HIỆN: EC-03 HỦY NHÓM / ĐỀ TÀI ====================

    @Transactional(rollbackFor = Exception.class)
    public DissolveRequestDto requestDissolveApprovedGroup(AppUser user, Long groupId, String reason) {
        Student leader = getStudentFromUser(user);
        StudentGroup group = getGroupById(groupId);

        if (!group.isLeader(leader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Nhóm trưởng mới có quyền gửi yêu cầu hủy nhóm / đề tài!");
        }

        if (isGroupInDraftStage(group)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhóm đang ở giai đoạn DRAFT, bạn có thể tự do bấm nút Hủy nhóm trực tiếp mà không cần GVHD duyệt.");
        }

        if (group.getTopic() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm chưa được phân công đề tài!");
        }

        if (reason == null || reason.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do yêu cầu hủy nhóm / đề tài!");
        }

        Optional<GroupDissolveRequest> pendingOpt = groupDissolveRequestRepository.findByGroupIdAndRequestedByIdAndStatus(group.getId(), leader.getId(), "PENDING");
        if (pendingOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn đã gửi yêu cầu hủy nhóm rồi và đang chờ GVHD phê duyệt!");
        }

        GroupDissolveRequest req = new GroupDissolveRequest();
        req.setGroup(group);
        req.setRequestedBy(leader);
        req.setTopic(group.getTopic());
        req.setReason(reason.trim());
        req.setStatus("PENDING");
        req.setCreatedAt(OffsetDateTime.now());

        GroupDissolveRequest saved = groupDissolveRequestRepository.save(req);
        System.out.println("[EC-03-REQUEST] Nhóm trưởng " + leader.getStudentCode() + " đã gửi yêu cầu hủy nhóm & đề tài " + group.getTopic().getCode() + " tới GVHD");

        return mapToDissolveRequestDto(saved);
    }

    public List<DissolveRequestDto> getPendingDissolveRequestsForLecturer(AppUser user) {
        if (user == null || user.getId() == null) return Collections.emptyList();
        Lecturer lecturer = lecturerRepository.findByUserId(user.getId()).orElse(null);
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return groupDissolveRequestRepository.findBySupervisorIdAndStatus(lecturer.getId(), "PENDING").stream()
                .map(this::mapToDissolveRequestDto)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<DissolveRequestDto> approveDissolveRequest(AppUser user, Long requestId) {
        Lecturer lecturer = getLecturerFromUser(user);

        GroupDissolveRequest req = groupDissolveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy nhóm ID=" + requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu này đã được xử lý trước đó!");
        }

        Topic topic = req.getTopic();
        StudentGroup group = req.getGroup();

        if (topic == null || !topic.isSupervisorOrCoSupervisor(lecturer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải GVHD của đề tài này!");
        }

        // 1. Phê duyệt yêu cầu hủy nhóm
        req.setStatus("APPROVED");
        req.setResolvedBy(lecturer);
        req.setResolvedAt(OffsetDateTime.now());
        groupDissolveRequestRepository.save(req);

        // 2. KÍCH HOẠT EC-03:
        // a) Chuyển trạng thái đề tài (Topic) quay lại APPROVED để nhóm khác có thể đăng ký
        topic.setStatus("APPROVED");
        topic.setAdvisorScore(null);
        topic.setReviewerScore(null);
        topic.setCouncilScore(null);
        topic.setFinalScore(null);
        topic.setIsFinalized(false);
        topicRepository.save(topic);

        // b) Xóa đăng ký đề tài liên quan
        topicRegistrationRepository.deleteByGroupId(group.getId());

        // c) Giải tán nhóm & giải phóng mọi thành viên tự do
        groupInvitationRepository.deleteByGroupId(group.getId());
        groupMemberRepository.findByGroupId(group.getId()).forEach(groupMemberRepository::delete);

        group.setTopic(null);
        group.setStatus("DISSOLVED");
        groupRepository.save(group);

        System.out.println("[EC-03-TRIGGERED] GVHD " + lecturer.getLecturerCode() + " đã duyệt yêu cầu hủy nhóm " + group.getGroupCode()
                + ". Đề tài '" + topic.getTitle() + "' đã quay lại trạng thái APPROVED để nhóm khác đăng ký.");

        return ApiResponse.ok("Đã phê duyệt hủy nhóm và đề tài thành công! Đề tài '" + topic.getTitle() + "' đã được giải phóng quay lại trạng thái APPROVED.", mapToDissolveRequestDto(req));
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<DissolveRequestDto> rejectDissolveRequest(AppUser user, Long requestId, String rejectionReason) {
        Lecturer lecturer = getLecturerFromUser(user);

        GroupDissolveRequest req = groupDissolveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy nhóm ID=" + requestId));

        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Yêu cầu này đã được xử lý trước đó!");
        }

        Topic topic = req.getTopic();
        if (topic == null || !topic.isSupervisorOrCoSupervisor(lecturer)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải GVHD của đề tài này!");
        }

        req.setStatus("REJECTED");
        req.setRejectionReason(rejectionReason != null ? rejectionReason.trim() : "GVHD từ chối yêu cầu hủy nhóm");
        req.setResolvedBy(lecturer);
        req.setResolvedAt(OffsetDateTime.now());
        groupDissolveRequestRepository.save(req);

        System.out.println("[EC-03-REJECTED] GVHD " + lecturer.getLecturerCode() + " đã từ chối yêu cầu hủy nhóm ID=" + requestId);
        return ApiResponse.ok("Đã từ chối yêu cầu hủy nhóm.", mapToDissolveRequestDto(req));
    }

    // ==================== MAPPER METHODS ====================

    private InvitationResponseDto mapToInvitationDto(GroupInvitation i) {
        InvitationResponseDto dto = new InvitationResponseDto();
        dto.setId(i.getId());
        if (i.getGroup() != null) {
            dto.setGroupId(i.getGroup().getId());
            dto.setGroupCode(i.getGroup().getGroupCode());
            dto.setGroupName(i.getGroup().getGroupName());
        }
        if (i.getStudent() != null) {
            dto.setInviteeStudentId(i.getStudent().getId());
            dto.setInviteeCode(i.getStudent().getStudentCode());
            dto.setInviteeName(i.getStudent().getUser() != null ? i.getStudent().getUser().getFullName() : "");
        }
        if (i.getInvitedBy() != null) {
            dto.setInvitedByStudentId(i.getInvitedBy().getId());
            dto.setInvitedByCode(i.getInvitedBy().getStudentCode());
            dto.setInvitedByName(i.getInvitedBy().getUser() != null ? i.getInvitedBy().getUser().getFullName() : "");
        }
        dto.setStatus(i.getStatus());
        dto.setRejectionReason(i.getRejectionReason());
        dto.setCreatedAt(i.getCreatedAt());
        dto.setRespondedAt(i.getRespondedAt());
        return dto;
    }

    // ==================== 5. GỘP NHÓM ĐIỀU PHỐI (ADMIN / DEPARTMENT HEAD / LECTURER MERGE) ====================

    public List<UnderEnrolledGroupDto> getUnderEnrolledGroups(AppUser user, Long periodId) {
        if (user == null || user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }
        String role = user.getRole().getCode();
        if (!"ADMIN".equalsIgnoreCase(role) && !"DEPARTMENT_HEAD".equalsIgnoreCase(role) && !"LECTURER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên, Trưởng bộ môn hoặc Giảng viên mới có quyền xem danh sách nhóm cần gộp!");
        }

        List<StudentGroup> allGroups = (periodId != null) 
                ? groupRepository.findByPeriodId(periodId) 
                : groupRepository.findAll();

        return allGroups.stream()
                .filter(g -> {
                    String status = g.getStatus() != null ? g.getStatus() : "";
                    if ("DISSOLVED".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status) || "DEFENDING".equalsIgnoreCase(status)) {
                        return false;
                    }
                    int memberCount = g.getMembers() != null ? g.getMembers().size() : 0;
                    int minStudents = (g.getPeriod() != null && g.getPeriod().getMinStudents() != null) ? g.getPeriod().getMinStudents() : 2;
                    return "PENDING_MEMBER".equalsIgnoreCase(status) || "DRAFT".equalsIgnoreCase(status) || memberCount < minStudents;
                })
                .map(this::mapToUnderEnrolledDto)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<UnderEnrolledGroupDto> mergeGroups(AppUser user, Long sourceGroupId, Long targetGroupId) {
        if (user == null || user.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập!");
        }
        String role = user.getRole().getCode();
        if (!"ADMIN".equalsIgnoreCase(role) && !"DEPARTMENT_HEAD".equalsIgnoreCase(role) && !"LECTURER".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Quản trị viên, Trưởng bộ môn hoặc Giảng viên mới có quyền thực hiện gộp nhóm!");
        }

        if (sourceGroupId == null || targetGroupId == null || sourceGroupId.equals(targetGroupId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm nguồn và nhóm đích phải khác nhau và không được để trống!");
        }

        StudentGroup sourceGroup = getGroupById(sourceGroupId);
        StudentGroup targetGroup = getGroupById(targetGroupId);

        // Kiểm tra quyền nếu là Giảng viên: phải là GVHD của một trong 2 nhóm
        if ("LECTURER".equalsIgnoreCase(role)) {
            Lecturer lec = getLecturerFromUser(user);
            boolean isSourceSupervisor = sourceGroup.getTopic() != null && sourceGroup.getTopic().isSupervisorOrCoSupervisor(lec);
            boolean isTargetSupervisor = targetGroup.getTopic() != null && targetGroup.getTopic().isSupervisorOrCoSupervisor(lec);
            if (!isSourceSupervisor && !isTargetSupervisor) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải Giảng viên hướng dẫn của nhóm nguồn hoặc nhóm đích!");
            }
        }

        // Kiểm tra đợt đăng ký phải cùng một đợt
        if (sourceGroup.getPeriod() != null && targetGroup.getPeriod() != null) {
            if (!sourceGroup.getPeriod().getId().equals(targetGroup.getPeriod().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể gộp hai nhóm thuộc hai Đợt đăng ký khác nhau!");
            }
        }

        // 1. Kiểm tra sĩ số hợp nhất
        List<GroupMember> sourceMembers = groupMemberRepository.findByGroupId(sourceGroup.getId());
        List<GroupMember> targetMembers = groupMemberRepository.findByGroupId(targetGroup.getId());

        int totalMembers = sourceMembers.size() + targetMembers.size();
        int maxAllowed = 3;
        if (targetGroup.getTopic() != null && targetGroup.getTopic().getMaxStudents() != null && targetGroup.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, targetGroup.getTopic().getMaxStudents());
        }

        if (totalMembers > maxAllowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể gộp nhóm: Tổng sĩ số (" + totalMembers + " SV) vượt quá giới hạn tối đa cho phép (" + maxAllowed + " SV) của nhóm đích!");
        }

        // 2. Chuyển toàn bộ thành viên từ nhóm nguồn sang nhóm đích làm MEMBER
        for (GroupMember sm : sourceMembers) {
            sm.setGroup(targetGroup);
            sm.setRoleInGroup("MEMBER"); // Nhóm trưởng cũ của nguồn chuyển thành MEMBER ở nhóm đích
            groupMemberRepository.save(sm);
        }

        // 3. Xóa các lời mời và yêu cầu đang chờ của nhóm nguồn
        groupInvitationRepository.deleteByGroupId(sourceGroup.getId());
        groupLeaveRequestRepository.deleteByGroupId(sourceGroup.getId());
        groupDissolveRequestRepository.deleteByGroupId(sourceGroup.getId());

        // 4. Giải phóng đề tài của nhóm nguồn (nếu có) quay về APPROVED
        if (sourceGroup.getTopic() != null) {
            Topic sourceTopic = sourceGroup.getTopic();
            // Nếu đề tài nhóm nguồn khác đề tài nhóm đích thì trả lại APPROVED để nhóm khác đăng ký
            if (targetGroup.getTopic() == null || !sourceTopic.getId().equals(targetGroup.getTopic().getId())) {
                sourceTopic.setStatus("APPROVED");
                sourceTopic.setAdvisorScore(null);
                sourceTopic.setReviewerScore(null);
                sourceTopic.setCouncilScore(null);
                sourceTopic.setFinalScore(null);
                sourceTopic.setIsFinalized(false);
                topicRepository.save(sourceTopic);
                System.out.println("[MERGE-GROUPS] Đề tài '" + sourceTopic.getTitle() + "' của nhóm nguồn đã được giải phóng quay lại APPROVED.");
            }
            topicRegistrationRepository.deleteByGroupId(sourceGroup.getId());
        }

        // 5. Cập nhật trạng thái nhóm đích sau khi đã đủ người
        int newTargetCount = groupMemberRepository.findByGroupId(targetGroup.getId()).size();
        int minStudents = (targetGroup.getPeriod() != null && targetGroup.getPeriod().getMinStudents() != null) ? targetGroup.getPeriod().getMinStudents() : 2;

        if (newTargetCount >= minStudents) {
            if (targetGroup.getTopic() != null) {
                targetGroup.setStatus("APPROVED");
            } else {
                targetGroup.setStatus("DRAFT");
            }
        }
        groupRepository.save(targetGroup);

        // 6. Xóa nhóm nguồn hoàn tất
        sourceGroup.setTopic(null);
        sourceGroup.setStatus("DISSOLVED");
        groupRepository.delete(sourceGroup);

        System.out.println("[MERGE-GROUPS] Đã gộp thành công nhóm '" + sourceGroup.getGroupName() + "' vào nhóm '" + targetGroup.getGroupName()
                + "'. Sĩ số nhóm đích hiện tại: " + newTargetCount + " SV.");

        return ApiResponse.ok("Gộp nhóm thành công! Nhóm '" + targetGroup.getGroupName() + "' hiện có " + newTargetCount + " thành viên.", mapToUnderEnrolledDto(targetGroup));
    }

    private UnderEnrolledGroupDto mapToUnderEnrolledDto(StudentGroup g) {
        UnderEnrolledGroupDto dto = new UnderEnrolledGroupDto();
        dto.setGroupId(g.getId());
        dto.setGroupCode(g.getGroupCode());
        dto.setGroupName(g.getGroupName());
        dto.setStatus(g.getStatus());
        if (g.getPeriod() != null) {
            dto.setPeriodId(g.getPeriod().getId());
            dto.setPeriodName(g.getPeriod().getName());
            dto.setMinStudents(g.getPeriod().getMinStudents());
            dto.setMaxStudents(g.getPeriod().getMaxStudents());
        }
        List<GroupMember> members = groupMemberRepository.findByGroupId(g.getId());
        dto.setCurrentMembers(members.size());
        dto.setMemberNames(members.stream()
                .map(m -> m.getStudent().getUser() != null ? m.getStudent().getUser().getFullName() + " (" + m.getStudent().getStudentCode() + ")" : m.getStudent().getStudentCode())
                .collect(Collectors.toList()));
        if (g.getLeader() != null) {
            dto.setLeaderCode(g.getLeader().getStudentCode());
            dto.setLeaderName(g.getLeader().getUser() != null ? g.getLeader().getUser().getFullName() : "");
        }
        if (g.getTopic() != null) {
            dto.setTopicId(g.getTopic().getId());
            dto.setTopicCode(g.getTopic().getCode());
            dto.setTopicTitle(g.getTopic().getTitle());
        }
        return dto;
    }

    private LeaveRequestDto mapToLeaveRequestDto(GroupLeaveRequest r) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(r.getId());
        if (r.getGroup() != null) {
            dto.setGroupId(r.getGroup().getId());
            dto.setGroupCode(r.getGroup().getGroupCode());
            dto.setGroupName(r.getGroup().getGroupName());
        }
        if (r.getStudent() != null) {
            dto.setStudentId(r.getStudent().getId());
            dto.setStudentCode(r.getStudent().getStudentCode());
            dto.setStudentName(r.getStudent().getUser() != null ? r.getStudent().getUser().getFullName() : "");
        }
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        if (r.getResolvedBy() != null) {
            dto.setSupervisorId(r.getResolvedBy().getId());
            dto.setSupervisorName(r.getResolvedBy().getUser() != null ? r.getResolvedBy().getUser().getFullName() : "");
        }
        dto.setRejectionReason(r.getRejectionReason());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setResolvedAt(r.getResolvedAt());
        return dto;
    }

    private DissolveRequestDto mapToDissolveRequestDto(GroupDissolveRequest r) {
        DissolveRequestDto dto = new DissolveRequestDto();
        dto.setId(r.getId());
        if (r.getGroup() != null) {
            dto.setGroupId(r.getGroup().getId());
            dto.setGroupCode(r.getGroup().getGroupCode());
            dto.setGroupName(r.getGroup().getGroupName());
        }
        if (r.getTopic() != null) {
            dto.setTopicId(r.getTopic().getId());
            dto.setTopicCode(r.getTopic().getCode());
            dto.setTopicTitle(r.getTopic().getTitle());
        }
        if (r.getRequestedBy() != null) {
            dto.setRequestedByStudentId(r.getRequestedBy().getId());
            dto.setRequestedByCode(r.getRequestedBy().getStudentCode());
            dto.setRequestedByName(r.getRequestedBy().getUser() != null ? r.getRequestedBy().getUser().getFullName() : "");
        }
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        if (r.getResolvedBy() != null) {
            dto.setSupervisorId(r.getResolvedBy().getId());
            dto.setSupervisorName(r.getResolvedBy().getUser() != null ? r.getResolvedBy().getUser().getFullName() : "");
        }
        dto.setRejectionReason(r.getRejectionReason());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setResolvedAt(r.getResolvedAt());
        return dto;
    }
}
