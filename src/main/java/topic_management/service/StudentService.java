package topic_management.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

    /**
     * GLOBAL VALIDATION (Áp dụng cho 100% sinh viên, không ngoại lệ):
     * 1. Mỗi Đợt đăng ký (registration_period) thuộc về một Khoa cụ thể (department_id).
     * 2. Bất kỳ sinh viên nào (student) được mời vào nhóm hoặc tham gia trong đợt đó,
     *    hệ thống BẮT BUỘC phải check: student.department_id PHẢI TRÙNG KHỚP với period.department_id.
     * 3. Nếu khác khoa, tuyệt đối từ chối và chặn đứng ở tầng Backend, ném ra lỗi 400
     *    với thông báo: "Sinh viên không thuộc khoa quản lý đợt đăng ký này!".
     */
    public void validateStudentDepartmentInPeriod(Student student, RegistrationPeriod period) {
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin sinh viên!");
        }
        if (period == null) {
            return;
        }

        // Lấy Khoa của đợt đăng ký
        Department periodDept = period.getDepartment();
        if (periodDept == null && period.getId() != null) {
            RegistrationPeriod reloaded = periodRepository.findById(period.getId()).orElse(null);
            if (reloaded != null) {
                periodDept = reloaded.getDepartment();
            }
        }
        if (periodDept == null || periodDept.getId() == null) {
            return; // Đợt dùng chung toàn trường không gán khoa
        }

        // Lấy Khoa của sinh viên thông qua Chuyên ngành (Major)
        Department studentDept = student.getDepartment();
        if (studentDept == null && student.getMajor() != null && student.getMajor().getId() != null) {
            Major m = majorRepository.findById(student.getMajor().getId()).orElse(null);
            if (m != null) {
                studentDept = m.getDepartment();
            }
        }

        if (studentDept == null || !Objects.equals(studentDept.getId(), periodDept.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sinh viên không thuộc khoa quản lý đợt đăng ký này!");
        }
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

    /**
     * BUG-H FIX: Khi periodId = null, uu tien tra ve nhom trong dot dang hoat
     * dong nhat (isActive + status OPEN/IN_PROGRESS) thay vi nhom cuoi cung theo insert order.
     */
    public StudentGroup getMyActiveGroup(Student student, Long periodId) {
        if (student == null) return null;

        // Case 1: period cu the duoc chi dinh
        if (periodId != null) {
            List<StudentGroup> list = groupRepository.findByStudentIdAndPeriodId(student.getId(), periodId);
            if (!list.isEmpty()) return list.get(0);
        }

        // Case 2: Khong co periodId — tim nhom trong dot dang hoat dong nhat
        List<StudentGroup> allMyGroups = groupRepository.findByStudentId(student.getId());
        if (allMyGroups.isEmpty()) return null;

        // Uu tien: nhom trong dot dang mo dang ky (isActive + OPEN/IN_PROGRESS)
        return allMyGroups.stream()
                .filter(g -> g.getPeriod() != null
                        && Boolean.TRUE.equals(g.getPeriod().getIsActive())
                        && ("OPEN".equalsIgnoreCase(g.getPeriod().getStatus())
                            || "IN_PROGRESS".equalsIgnoreCase(g.getPeriod().getStatus())))
                .max(Comparator.comparing(g -> g.getPeriod().getId()))
                // Fallback: nhom gan nhat (theo created time)
                .orElseGet(() -> allMyGroups.stream()
                        .max(Comparator.comparing(StudentGroup::getCreatedAt,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .orElse(null));
    }

    @Transactional
    public StudentGroup createGroup(Student leader, Long periodId, String groupName) {
        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;
        if (period == null) {
            List<RegistrationPeriod> activePeriods = periodRepository.findByIsActiveTrueOrderByIdDesc();
            if (!activePeriods.isEmpty()) period = activePeriods.get(0);
        }

        // GLOBAL VALIDATION: Kiểm tra tính đồng bộ Khoa cho Trưởng nhóm
        if (period != null) {
            validateStudentDepartmentInPeriod(leader, period);
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

        // GLOBAL VALIDATION: Bắt buộc sinh viên được mời phải cùng Khoa với Đợt đăng ký
        RegistrationPeriod period = group.getPeriod();
        if (period != null) {
            try {
                validateStudentDepartmentInPeriod(invitee, period);
            } catch (ResponseStatusException ex) {
                return ex.getReason() != null ? ex.getReason() : "Sinh viên không thuộc khoa quản lý đợt đăng ký này!";
            }
        }

        Long periodId = period != null ? period.getId() : null;
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
        RegistrationPeriod period = group.getPeriod();

        // GLOBAL VALIDATION: Bắt buộc sinh viên gia nhập phải cùng Khoa với Đợt đăng ký
        if (period != null) {
            try {
                validateStudentDepartmentInPeriod(student, period);
            } catch (ResponseStatusException ex) {
                return ex.getReason() != null ? ex.getReason() : "Sinh viên không thuộc khoa quản lý đợt đăng ký này!";
            }
        }

        List<GroupMember> currentMembers = groupMemberRepository.findByGroupId(group.getId());

        // BUG-03 FIX: Kiểm tra giới hạn thành viên theo topic.maxStudents nếu nhóm đã có đề tài
        int maxAllowed = 3;
        if (group.getTopic() != null && group.getTopic().getMaxStudents() != null && group.getTopic().getMaxStudents() > 0) {
            maxAllowed = Math.min(3, group.getTopic().getMaxStudents());
        }
        if (currentMembers.size() >= maxAllowed) {
            return "Nhóm " + group.getGroupName() + " đã đủ " + maxAllowed + " thành viên" + (group.getTopic() != null ? " theo quy định đề tài!" : "!");
        }

        Long periodId = period != null ? period.getId() : null;
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

    /**
     * Trả về tập hợp các topicId đã có nhóm đăng ký (trạng thái != DRAFT, != REJECTED).
     * Dùng để UI hiển thị trực quan đề tài nào còn trống / đã kín chỗ.
     */
    public java.util.Set<Long> getTakenTopicIds(List<Topic> topics) {
        if (topics == null || topics.isEmpty()) return java.util.Collections.emptySet();
        List<Long> topicIds = topics.stream().map(Topic::getId).collect(Collectors.toList());
        List<StudentGroup> groups = groupRepository.findByTopicIdIn(topicIds);
        java.util.Set<Long> takenIds = new java.util.HashSet<>();
        for (StudentGroup g : groups) {
            // Chỉ tính nhóm đang ACTIVE (không phải DRAFT hay REJECTED)
            if (g.getTopic() != null && g.getStatus() != null
                    && !"DRAFT".equalsIgnoreCase(g.getStatus())
                    && !"REJECTED".equalsIgnoreCase(g.getStatus())) {
                takenIds.add(g.getTopic().getId());
            }
        }
        return takenIds;
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
            return "Đề tài này đã được một nhóm khác đăng ký rồi!";
        }

        // BUG-B FIX: Kiểm tra hạn mức hướng dẫn (maxTopicsQuota) của GVHD chính
        // Quota tính theo đợt: đếm số nhóm REGISTERED/APPROVED mà GVHD đang nhận trong đợt này
        List<String> excludedStatuses = List.of("DRAFT", "REJECTED");
        Long topicPeriodId = (topic.getPeriod() != null) ? topic.getPeriod().getId() : null;

        if (topic.getSupervisor() != null) {
            Integer quota = topic.getSupervisor().getMaxTopicsQuota();
            if (quota != null && quota > 0) {
                long currentCount = groupRepository.countActiveGroupsBySupervisorInPeriod(
                        topic.getSupervisor().getId(), topicPeriodId, excludedStatuses);
                if (currentCount >= quota) {
                    String supervisorName = topic.getSupervisor().getUser() != null
                            ? topic.getSupervisor().getUser().getFullName()
                            : "GV#" + topic.getSupervisor().getId();
                    return "Giảng viên hướng dẫn '" + supervisorName
                            + "' đã đạt hạn mức hướng dẫn tối đa (" + quota
                            + " nhóm/đợt). Vui lòng chọn đề tài khác!";
                }
            }
        }

        // BUG-B FIX: Kiểm tra hạn mức của GVHD phụ (nếu có)
        if (topic.getCoSupervisor() != null) {
            Integer coQuota = topic.getCoSupervisor().getMaxTopicsQuota();
            if (coQuota != null && coQuota > 0) {
                long coCount = groupRepository.countActiveGroupsByCoSupervisorInPeriod(
                        topic.getCoSupervisor().getId(), topicPeriodId, excludedStatuses);
                if (coCount >= coQuota) {
                    String coName = topic.getCoSupervisor().getUser() != null
                            ? topic.getCoSupervisor().getUser().getFullName()
                            : "GV#" + topic.getCoSupervisor().getId();
                    return "Giảng viên đồng hướng dẫn '" + coName
                            + "' đã đạt hạn mức hướng dẫn tối đa (" + coQuota
                            + " nhóm/đợt). Vui lòng chọn đề tài khác!";
                }
            }
        }

        group.setTopic(topic);
        group.setStatus("REGISTERED");
        groupRepository.save(group);

        System.out.println("[DB-STUDENT] Nhom '" + group.getGroupName() + "' da dang ky de tai '" + topic.getTitle() + "'");
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
