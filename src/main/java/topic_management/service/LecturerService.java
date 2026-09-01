package topic_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class LecturerService {

    private final LecturerRepository lecturerRepository;
    private final TopicRepository topicRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final SubmissionRepository submissionRepository;
    private final MajorRepository majorRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final CouncilRepository councilRepository;
    private final EvaluationRepository evaluationRepository;

    public LecturerService(
            LecturerRepository lecturerRepository,
            TopicRepository topicRepository,
            TopicRegistrationRepository registrationRepository,
            SubmissionRepository submissionRepository,
            MajorRepository majorRepository,
            DepartmentRepository departmentRepository,
            RegistrationPeriodRepository periodRepository,
            StudentGroupRepository studentGroupRepository,
            CouncilRepository councilRepository,
            EvaluationRepository evaluationRepository) {
        this.lecturerRepository = lecturerRepository;
        this.topicRepository = topicRepository;
        this.registrationRepository = registrationRepository;
        this.submissionRepository = submissionRepository;
        this.majorRepository = majorRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.councilRepository = councilRepository;
        this.evaluationRepository = evaluationRepository;
    }

    @Transactional
    public Lecturer getLecturerByUser(AppUser user) {
        if (user == null || user.getId() == null) return null;
        Optional<Lecturer> opt = lecturerRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            return opt.get();
        }

        Lecturer newLecturer = new Lecturer();
        newLecturer.setUser(user);
        newLecturer.setLecturerCode("GV" + String.format("%03d", user.getId()));
        newLecturer.setAcademicTitle("TS");
        newLecturer.setDegree("Tiến sĩ");
        newLecturer.setMaxTopicsQuota(5);

        List<Department> depts = departmentRepository.findAll();
        if (!depts.isEmpty()) {
            newLecturer.setDepartment(depts.get(0));
        }

        return lecturerRepository.save(newLecturer);
    }

    public List<Lecturer> getAllLecturers() {
        return lecturerRepository.findAll();
    }

    public List<Lecturer> getOtherLecturers(Long currentLecturerId) {
        return lecturerRepository.findAll().stream()
                .filter(l -> !l.getId().equals(currentLecturerId))
                .toList();
    }

    public List<Topic> getTopicsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return topicRepository.findBySupervisorOrCoSupervisorId(lecturer.getId());
    }

    public List<Topic> getReviewerTopics(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return topicRepository.findByReviewerId(lecturer.getId());
    }

    public List<Council> getCouncilsForLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return councilRepository.findByLecturerId(lecturer.getId());
    }

    public Topic getTopicById(Long id) {
        if (id == null) return null;
        return topicRepository.findById(id).orElse(null);
    }

    @Transactional
    public Topic createTopic(Lecturer supervisor, Long periodId, Long majorId, Long coSupervisorId, Topic topic) {
        Major major = majorRepository.findById(majorId).orElse(null);
        topic.setMajor(major);
        topic.setSupervisor(supervisor);

        // BUG-01 FIX: Kiểm tra đợt đăng ký có đang mở cho giảng viên đề xuất không
        if (periodId != null) {
            RegistrationPeriod period = periodRepository.findById(periodId).orElse(null);
            if (period != null && !period.isLecturerProposalOpen()) {
                throw new IllegalStateException("Đợt '" + period.getName()
                        + "' hiện không trong giai đoạn nhận đề xuất đề tài từ giảng viên!");
            }
            topic.setPeriod(period);
        }

        if (coSupervisorId != null && !coSupervisorId.equals(supervisor.getId())) {
            topic.setCoSupervisor(lecturerRepository.findById(coSupervisorId).orElse(null));
        }

        if (topic.getCode() == null || topic.getCode().trim().isEmpty()) {
            topic.setCode("DT-" + System.currentTimeMillis() % 100000);
        }

        topic.setStatus("PENDING");
        topic.setCreatedAt(OffsetDateTime.now());

        Topic saved = topicRepository.save(topic);
        System.out.println("[DB-LECTURER] Đã lưu đề tài đề xuất mới: '" + saved.getTitle() + "' (ID=" + saved.getId() + ")");
        return saved;
    }

    @Transactional
    public Topic updateTopic(Lecturer currentLecturer, Long topicId, Long periodId, Long majorId, Long coSupervisorId, Topic topicData) {
        if (topicId == null || currentLecturer == null) {
            throw new IllegalArgumentException("Thông tin không hợp lệ.");
        }
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) {
            throw new IllegalArgumentException("Không tìm thấy đề tài ID=" + topicId);
        }

        // Security check: Only supervisor or co-supervisor can edit
        if (!topic.isSupervisorOrCoSupervisor(currentLecturer)) {
            throw new IllegalStateException("Bạn không có quyền chỉnh sửa đề tài này (không phải GVHD)!");
        }

        // Check if student group already assigned and active
        List<StudentGroup> groups = studentGroupRepository.findByTopicId(topicId);
        boolean hasActiveGroup = groups.stream().anyMatch(g -> g.getStatus() != null && !"REJECTED".equalsIgnoreCase(g.getStatus()) && !"DRAFT".equalsIgnoreCase(g.getStatus()));
        
        if (hasActiveGroup && "APPROVED".equalsIgnoreCase(topic.getStatus())) {
            // If already approved with active group, allow updating description and requirements
            topic.setDescription(topicData.getDescription());
            topic.setRequirements(topicData.getRequirements());
        } else {
            // Fully updateable
            if (topicData.getTitle() != null && !topicData.getTitle().trim().isEmpty()) {
                topic.setTitle(topicData.getTitle().trim());
            }
            if (topicData.getCode() != null && !topicData.getCode().trim().isEmpty()) {
                topic.setCode(topicData.getCode().trim());
            }
            if (majorId != null) {
                Major major = majorRepository.findById(majorId).orElse(null);
                topic.setMajor(major);
            }
            if (periodId != null) {
                RegistrationPeriod period = periodRepository.findById(periodId).orElse(null);
                topic.setPeriod(period);
            }
            if (coSupervisorId != null && !coSupervisorId.equals(currentLecturer.getId())) {
                topic.setCoSupervisor(lecturerRepository.findById(coSupervisorId).orElse(null));
            } else if (coSupervisorId == null) {
                topic.setCoSupervisor(null);
            }
            if (topicData.getMaxStudents() != null && topicData.getMaxStudents() > 0) {
                topic.setMaxStudents(Math.min(3, topicData.getMaxStudents()));
            }
            topic.setDescription(topicData.getDescription());
            topic.setRequirements(topicData.getRequirements());

            // If it was REJECTED, resubmitting sets it back to PENDING and clears rejection reason
            if ("REJECTED".equalsIgnoreCase(topic.getStatus())) {
                topic.setStatus("PENDING");
                topic.setRejectionReason(null);
            }
        }

        Topic saved = topicRepository.save(topic);
        System.out.println("[DB-LECTURER] Đã cập nhật đề tài ID=" + saved.getId() + " (Status=" + saved.getStatus() + ")");
        return saved;
    }

    @Transactional
    public boolean deleteTopic(Long topicId, Lecturer currentLecturer) {
        if (topicId == null || currentLecturer == null) return false;
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;

        if (!topic.isSupervisorOrCoSupervisor(currentLecturer)) {
            System.err.println("[SECURITY] GV ID=" + currentLecturer.getId() + " cố xóa đề tài không phải của mình!");
            return false;
        }

        List<StudentGroup> groups = studentGroupRepository.findByTopicId(topicId);
        if (!groups.isEmpty()) {
            throw new IllegalStateException("Không thể xóa đề tài đã có nhóm sinh viên đăng ký!");
        }

        topicRepository.delete(topic);
        System.out.println("[DB-LECTURER] Đã xóa đề tài ID=" + topicId);
        return true;
    }

    public List<StudentGroup> getGroupsForLecturerTopics(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        List<Topic> myTopics = getTopicsByLecturer(lecturer);
        List<StudentGroup> groups = new ArrayList<>();
        for (Topic t : myTopics) {
            groups.addAll(studentGroupRepository.findByTopicId(t.getId()));
        }
        return groups;
    }

    /**
     * BUG-07 FIX: Kiểm tra nhóm SV thuộc đề tài của GV hiện tại trước khi duyệt/từ chối
     */
    @Transactional
    public boolean approveGroupTopic(Long groupId, String status, Lecturer currentLecturer) {
        StudentGroup group = studentGroupRepository.findById(groupId).orElse(null);
        if (group == null) return false;
        // Guard: Chỉ GVHD/GVHD phụ mới được duyệt
        if (group.getTopic() != null && currentLecturer != null
                && !group.getTopic().isSupervisorOrCoSupervisor(currentLecturer)) {
            System.err.println("[SECURITY] GV ID=" + currentLecturer.getId() + " cố duyệt nhóm của đề tài không phải mình hướng dẫn!");
            return false;
        }
        group.setStatus(status);
        studentGroupRepository.save(group);
        System.out.println("[DB-LECTURER] Cập nhật trạng thái nhóm ID=" + groupId + " -> " + status);
        return true;
    }

    public List<Submission> getSubmissionsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return submissionRepository.findBySupervisorOrCoSupervisorId(lecturer.getId());
    }

    /**
     * BUG-08 FIX: Kiểm tra bài nộp thuộc đề tài của GV hiện tại trước khi chấm điểm
     */
    @Transactional
    public boolean gradeSubmission(Long submissionId, Double grade, String feedback, Lecturer currentLecturer) {
        if (submissionId == null) return false;
        Optional<Submission> opt = submissionRepository.findById(submissionId);
        if (opt.isPresent()) {
            Submission sub = opt.get();
            // Guard: Chỉ GVHD/GVHD phụ mới được chấm bài nộp
            if (sub.getTopic() != null && currentLecturer != null
                    && !sub.getTopic().isSupervisorOrCoSupervisor(currentLecturer)) {
                System.err.println("[SECURITY] GV ID=" + currentLecturer.getId() + " cố chấm bài nộp của đề tài không phải mình hướng dẫn!");
                return false;
            }
            sub.setGrade(grade);
            sub.setFeedback(feedback);
            submissionRepository.save(sub);
            System.out.println("[DB-LECTURER] Đã lưu nhận xét và điểm bài nộp ID=" + submissionId);
            return true;
        }
        return false;
    }

    public List<RegistrationPeriod> getOpenProposalPeriods() {
        return periodRepository.findByIsActiveTrueOrderByIdDesc().stream()
                .filter(RegistrationPeriod::isLecturerProposalOpen)
                .toList();
    }

    public List<Major> getAllMajors() {
        return majorRepository.findAll();
    }

    public List<RegistrationPeriod> getAllPeriods() {
        return periodRepository.findAll();
    }
}
