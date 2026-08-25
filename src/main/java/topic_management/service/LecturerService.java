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

        if (periodId != null) {
            topic.setPeriod(periodRepository.findById(periodId).orElse(null));
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

    public List<StudentGroup> getGroupsForLecturerTopics(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return studentGroupRepository.findByLecturerTopics(lecturer.getId());
    }

    @Transactional
    public boolean approveGroupTopic(Long groupId, String status) {
        StudentGroup group = studentGroupRepository.findById(groupId).orElse(null);
        if (group != null) {
            group.setStatus(status);
            studentGroupRepository.save(group);
            System.out.println("[DB-LECTURER] Cập nhật trạng thái nhóm ID=" + groupId + " -> " + status);
            return true;
        }
        return false;
    }

    public List<Submission> getSubmissionsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        return submissionRepository.findBySupervisorOrCoSupervisorId(lecturer.getId());
    }

    @Transactional
    public boolean gradeSubmission(Long submissionId, Double grade, String feedback) {
        if (submissionId == null) return false;
        Optional<Submission> opt = submissionRepository.findById(submissionId);
        if (opt.isPresent()) {
            Submission sub = opt.get();
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
