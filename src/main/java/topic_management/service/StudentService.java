package topic_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final TopicRepository topicRepository;
    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SubmissionRepository submissionRepository;
    private final MajorRepository majorRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final EvaluationRepository evaluationRepository;

    public StudentService(
            StudentRepository studentRepository,
            TopicRepository topicRepository,
            StudentGroupRepository groupRepository,
            GroupMemberRepository groupMemberRepository,
            SubmissionRepository submissionRepository,
            MajorRepository majorRepository,
            RegistrationPeriodRepository periodRepository,
            EvaluationRepository evaluationRepository) {
        this.studentRepository = studentRepository;
        this.topicRepository = topicRepository;
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.submissionRepository = submissionRepository;
        this.majorRepository = majorRepository;
        this.periodRepository = periodRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public Student getStudentByUser(AppUser user) {
        if (user == null || user.getId() == null) return null;
        Optional<Student> opt = studentRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            return opt.get();
        }

        Student newStudent = new Student();
        newStudent.setUser(user);
        newStudent.setStudentCode(user.getUsername() != null ? user.getUsername() : "SV" + user.getId());
        newStudent.setClassName("CNTT-K16");
        newStudent.setAcademicYear("2021-2025");
        newStudent.setGpa(3.50);

        List<Major> majors = majorRepository.findAll();
        if (!majors.isEmpty()) {
            newStudent.setMajor(majors.get(0));
        }

        return studentRepository.save(newStudent);
    }

    public Student getStudentByCode(String studentCode) {
        if (studentCode == null || studentCode.trim().isEmpty()) return null;
        return studentRepository.findByStudentCode(studentCode.trim()).orElse(null);
    }

    // ==================== GROUP MANAGEMENT (MAX 3 SV) ====================

    public StudentGroup getMyActiveGroup(Student student, Long periodId) {
        if (student == null) return null;
        List<StudentGroup> list = groupRepository.findByStudentIdAndPeriodId(student.getId(), periodId);
        if (!list.isEmpty()) {
            return list.get(0);
        }
        // If periodId is null, get latest group
        List<StudentGroup> allMyGroups = groupRepository.findByStudentId(student.getId());
        if (!allMyGroups.isEmpty()) {
            return allMyGroups.get(allMyGroups.size() - 1);
        }
        return null;
    }

    @Transactional
    public StudentGroup createGroup(Student leader, Long periodId, String groupName) {
        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;
        if (period == null) {
            List<RegistrationPeriod> activePeriods = periodRepository.findByIsActiveTrueOrderByIdDesc();
            if (!activePeriods.isEmpty()) period = activePeriods.get(0);
        }

        // Check if student already in a group in this period
        if (period != null) {
            List<StudentGroup> existing = groupRepository.findByStudentIdAndPeriodId(leader.getId(), period.getId());
            if (!existing.isEmpty()) {
                throw new IllegalStateException("Bạn đã tham gia một nhóm trong đợt này rồi (" + existing.get(0).getGroupName() + ").");
            }
        }

        StudentGroup group = new StudentGroup();
        group.setPeriod(period);
        group.setLeader(leader);
        group.setGroupName((groupName != null && !groupName.trim().isEmpty()) ? groupName.trim() : "Nhóm của " + leader.getUser().getFullName());
        group.setGroupCode("GRP-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        group.setStatus("DRAFT");
        group.setCreatedAt(OffsetDateTime.now());

        StudentGroup saved = groupRepository.save(group);

        // Add leader as first member
        GroupMember gm = new GroupMember();
        gm.setGroup(saved);
        gm.setStudent(leader);
        gm.setRoleInGroup("LEADER");
        gm.setJoinedAt(OffsetDateTime.now());
        groupMemberRepository.save(gm);

        System.out.println("[DB-STUDENT] Đã tạo nhóm mới: '" + saved.getGroupName() + "' (Mã: " + saved.getGroupCode() + ") bởi Leader " + leader.getStudentCode());
        return saved;
    }

    @Transactional
    public String inviteMemberByCode(StudentGroup group, String studentCode) {
        if (group == null) return "Không tìm thấy nhóm.";
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());
        
        // BUG-03 FIX: Kiểm tra giới hạn thành viên theo topic.maxStudents nếu nhóm đã có đề tài
        int maxAllowed = 3;
        if (group.getTopic() != null && group.getTopic().getMaxStudents() != null && group.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, group.getTopic().getMaxStudents());
        }
        if (currentMembers.size() >= maxAllowed) {
            return "Nhóm đã đạt số lượng tối đa " + maxAllowed + " thành viên" + (group.getTopic() != null ? " theo quy định đề tài!" : "!");
        }

        Student invitee = getStudentByCode(studentCode);
        if (invitee == null) {
            return "Không tìm thấy sinh viên có mã: " + studentCode;
        }

        Long periodId = group.getPeriod() != null ? group.getPeriod().getId() : null;
        List<StudentGroup> existing = groupRepository.findByStudentIdAndPeriodId(invitee.getId(), periodId);
        if (!existing.isEmpty()) {
            return "Sinh viên " + invitee.getUser().getFullName() + " (" + studentCode + ") đã tham gia một nhóm khác trong đợt này!";
        }

        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(invitee);
        gm.setRoleInGroup("MEMBER");
        gm.setJoinedAt(OffsetDateTime.now());
        groupMemberRepository.save(gm);

        System.out.println("[DB-STUDENT] Đã thêm thành viên " + invitee.getStudentCode() + " vào nhóm " + group.getGroupCode());
        return null; // success
    }

    @Transactional
    public String joinGroupByCode(Student student, String groupCode) {
        if (student == null) return "Vui lòng đăng nhập.";
        if (groupCode == null || groupCode.trim().isEmpty()) return "Vui lòng nhập mã nhóm.";

        Optional<StudentGroup> opt = groupRepository.findByGroupCode(groupCode.trim().toUpperCase());
        if (opt.isEmpty()) {
            return "Mã nhóm không tồn tại!";
        }

        StudentGroup group = opt.get();
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());

        // BUG-03 FIX: Kiểm tra giới hạn thành viên theo topic.maxStudents nếu nhóm đã có đề tài
        int maxAllowed = 3;
        if (group.getTopic() != null && group.getTopic().getMaxStudents() != null && group.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, group.getTopic().getMaxStudents());
        }
        if (currentMembers.size() >= maxAllowed) {
            return "Nhóm " + group.getGroupName() + " đã đủ " + maxAllowed + " thành viên" + (group.getTopic() != null ? " theo quy định đề tài!" : "!");
        }

        Long periodId = group.getPeriod() != null ? group.getPeriod().getId() : null;
        List<StudentGroup> existing = groupRepository.findByStudentIdAndPeriodId(student.getId(), periodId);
        if (!existing.isEmpty()) {
            return "Bạn đã thuộc một nhóm khác trong đợt này rồi!";
        }

        GroupMember gm = new GroupMember();
        gm.setGroup(group);
        gm.setStudent(student);
        gm.setRoleInGroup("MEMBER");
        gm.setJoinedAt(OffsetDateTime.now());
        groupMemberRepository.save(gm);

        System.out.println("[DB-STUDENT] Sinh viên " + student.getStudentCode() + " đã gia nhập nhóm " + group.getGroupCode());
        return null; // success
    }

    @Transactional
    public boolean removeMember(StudentGroup group, Long memberStudentId) {
        if (group == null || memberStudentId == null) return false;
        // Cannot remove leader
        if (group.getLeader() != null && group.getLeader().getId().equals(memberStudentId)) {
            return false;
        }
        groupMemberRepository.deleteByGroupIdAndStudentId(group.getId(), memberStudentId);
        return true;
    }

    // ==================== TOPIC REGISTRATION ====================

    public List<Topic> getAvailableTopics(Long periodId, Long majorId, String keyword) {
        List<Topic> list = (periodId != null)
                ? topicRepository.findApprovedByPeriodId(periodId)
                : topicRepository.findByStatus("APPROVED");
        return list.stream()
                .filter(t -> majorId == null || (t.getMajor() != null && majorId.equals(t.getMajor().getId())))
                // BUG-02 FIX: Chỉ hiển thị đề tài trong đợt đang mở đăng ký cho sinh viên
                .filter(t -> t.getPeriod() == null || t.getPeriod().isStudentRegistrationOpen())
                .filter(t -> keyword == null || keyword.trim().isEmpty() ||
                        (t.getTitle() != null && t.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                        (t.getCode() != null && t.getCode().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Topic getTopicById(Long id) {
        if (id == null) return null;
        return topicRepository.findById(id).orElse(null);
    }

    @Transactional
    public String registerTopicForGroup(Student student, StudentGroup group, Long topicId) {
        if (student == null) {
            return "Vui lòng đăng nhập lại!";
        }

        Topic topic = getTopicById(topicId);
        if (topic == null || !"APPROVED".equalsIgnoreCase(topic.getStatus())) {
            return "Đề tài không tồn tại hoặc chưa được duyệt!";
        }

        // LỖI 1 FIX: Bắt buộc sinh viên phải thuộc một nhóm hợp lệ và là nhóm trưởng
        if (group == null) {
            return "Bạn chưa thuộc nhóm sinh viên nào! Vui lòng tạo nhóm hoặc tham gia nhóm trước khi đăng ký đề tài.";
        }

        // Kiểm tra quyền Trưởng nhóm: Chỉ Trưởng nhóm mới có quyền chọn và đăng ký đề tài
        if (!group.isLeader(student)) {
            return "Chỉ Nhóm trưởng mới có quyền chọn và đăng ký đề tài cho nhóm!";
        }

        // Kiểm tra nhóm đã đăng ký đề tài nào khác trong đợt này chưa
        if (group.getTopic() != null) {
            return "Nhóm của bạn đã đăng ký đề tài: '" + group.getTopic().getTitle() + "'. Mỗi nhóm chỉ được đăng ký 1 đề tài trong một đợt!";
        }

        // Kiểm tra thời hạn đợt đăng ký
        if (topic.getPeriod() != null && !topic.getPeriod().isStudentRegistrationOpen()) {
            return "Đợt đăng ký đề tài này hiện không trong thời gian mở đăng ký cho sinh viên!";
        }

        // BUG-03 FIX: Kiểm tra số lượng thành viên nhóm không được vượt quá maxStudents của đề tài
        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());
        int memberCount = (currentMembers != null && !currentMembers.isEmpty()) ? currentMembers.size() : 1;
        int maxStudentsAllowed = (topic.getMaxStudents() != null && topic.getMaxStudents() > 0) ? topic.getMaxStudents() : 3;
        if (memberCount > maxStudentsAllowed) {
            return "Số lượng thành viên của nhóm (" + memberCount + " SV) vượt quá số lượng tối đa cho phép của đề tài này (" + maxStudentsAllowed + " SV)!";
        }

        // Kiểm tra chỉ tiêu đăng ký (mỗi đề tài chỉ 1 nhóm thực hiện)
        List<StudentGroup> existingGroupsOnTopic = groupRepository.findByTopicId(topicId);
        if (!existingGroupsOnTopic.isEmpty()) {
            return "Đề tài này đã được một nhóm khác đăng ký!";
        }

        group.setTopic(topic);
        group.setStatus("REGISTERED");
        groupRepository.save(group);

        System.out.println("[DB-STUDENT] Nhóm '" + group.getGroupName() + "' đã đăng ký đề tài '" + topic.getTitle() + "'");
        return null; // success
    }

    // ==================== SUBMISSIONS (LEADER ONLY) ====================

    public List<Submission> getGroupSubmissions(StudentGroup group) {
        if (group == null) return Collections.emptyList();
        List<Submission> list = submissionRepository.findByGroupId(group.getId());
        return list.stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public String submitGroupReport(Student student, StudentGroup group, String title, Integer round, String fileName, Long fileSize) {
        if (group == null) {
            return "Bạn chưa thuộc nhóm nào!";
        }

        if (group.getId() != null) {
            group = groupRepository.findById(group.getId()).orElse(group);
        }

        // BẮT BUỘC: CHỈ NHÓM TRƯỞNG MỚI ĐƯỢC NỘP BÁO CÁO ĐỀ TÀI
        if (!group.isLeader(student)) {
            return "QUYỀN HẠN BỊ TỪ CHỐI: Chỉ Nhóm trưởng mới có quyền nộp bài báo cáo đề tài!";
        }

        if (group.getTopic() == null) {
            return "Nhóm của bạn chưa đăng ký đề tài nào!";
        }

        // LỖI 2 FIX: Chỉ cho phép nộp báo cáo khi đề tài và đăng ký nhóm đã được GVHD phê duyệt (APPROVED)
        if (!"APPROVED".equalsIgnoreCase(group.getStatus()) || !"APPROVED".equalsIgnoreCase(group.getTopic().getStatus())) {
            return "Đề tài chưa được phê duyệt, không thể nộp báo cáo!";
        }

        Submission sub = new Submission();
        sub.setTopic(group.getTopic());
        sub.setGroup(group);
        sub.setSubmittedByStudent(student);
        sub.setStudent(student);
        sub.setTitle(title);
        sub.setSubmissionRound(round != null ? round : 1);
        sub.setFilePath("/uploads/submissions/" + (fileName != null ? fileName : "report.pdf"));
        sub.setFileSize(fileSize != null ? fileSize : 1048576L);
        sub.setSubmittedAt(OffsetDateTime.now());

        submissionRepository.save(sub);
        System.out.println("[DB-STUDENT] Leader " + student.getStudentCode() + " đã nộp báo cáo cho nhóm " + group.getGroupName());
        return null; // success
    }

    // ==================== RESULTS & EVALUATIONS ====================

    /**
     * BUG-10 FIX: Chỉ trả về evaluations khi đề tài đã được công bố chính thức (isFinalized = true)
     */
    public List<Evaluation> getGroupEvaluations(StudentGroup group) {
        if (group == null || group.getTopic() == null) return Collections.emptyList();
        // Guard: Ẩn điểm cho SV khi chưa được công bố
        if (!Boolean.TRUE.equals(group.getTopic().getIsFinalized())) {
            return Collections.emptyList();
        }
        return evaluationRepository.findByTopicId(group.getTopic().getId());
    }

    public List<RegistrationPeriod> getAllActivePeriods() {
        return periodRepository.findByIsActiveTrueOrderByIdDesc();
    }

    public List<Major> getAllMajors() {
        return majorRepository.findAll();
    }
}
