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

    public LecturerService(
            LecturerRepository lecturerRepository,
            TopicRepository topicRepository,
            TopicRegistrationRepository registrationRepository,
            SubmissionRepository submissionRepository,
            MajorRepository majorRepository,
            DepartmentRepository departmentRepository) {
        this.lecturerRepository = lecturerRepository;
        this.topicRepository = topicRepository;
        this.registrationRepository = registrationRepository;
        this.submissionRepository = submissionRepository;
        this.majorRepository = majorRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public Lecturer getLecturerByUser(AppUser user) {
        if (user == null || user.getId() == null) return null;
        Optional<Lecturer> opt = lecturerRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            return opt.get();
        }

        // Tạo bản ghi Lecturer mới trong DB nếu chưa có
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

        Lecturer saved = lecturerRepository.save(newLecturer);
        System.out.println("[DB-LECTURER] getLecturerByUser: Đã tạo bản ghi Giảng viên mới trong DB (ID=" + saved.getId() + ", Mã=" + saved.getLecturerCode() + ") cho User '" + user.getUsername() + "'");
        return saved;
    }

    public List<Topic> getTopicsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        List<Topic> topics = topicRepository.findBySupervisorId(lecturer.getId());
        System.out.println("[DB-LECTURER] getTopicsByLecturer: Lấy " + topics.size() + " đề tài từ DB của GV ID=" + lecturer.getId() + " (" + (lecturer.getUser() != null ? lecturer.getUser().getFullName() : "") + ")");
        return topics;
    }

    public Topic getTopicById(Long id) {
        if (id == null) return null;
        Optional<Topic> opt = topicRepository.findById(id);
        System.out.println("[DB-LECTURER] getTopicById(" + id + "): " + (opt.isPresent() ? "Tìm thấy đề tài: " + opt.get().getTitle() : "Không tìm thấy"));
        return opt.orElse(null);
    }

    @Transactional
    public Topic createTopic(Lecturer lecturer, Topic topic, Long majorId) {
        Major major = getMajorById(majorId);
        topic.setMajor(major);
        topic.setSupervisor(lecturer);
        topic.setStatus("PENDING");
        topic.setCreatedAt(OffsetDateTime.now());

        Topic saved = topicRepository.save(topic);
        System.out.println("[DB-LECTURER] createTopic: Đã LƯU ĐỀ TÀI MỚI vào Database thành công! ID=" + saved.getId() + ", Mã=" + saved.getCode() + ", Tiêu đề='" + saved.getTitle() + "'");
        return saved;
    }

    public List<TopicRegistration> getRegistrationsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        List<TopicRegistration> list = registrationRepository.findByTopicSupervisorId(lecturer.getId());
        System.out.println("[DB-LECTURER] getRegistrationsByLecturer: Lấy " + list.size() + " bản ghi đăng ký đề tài từ DB");
        return list;
    }

    @Transactional
    public boolean updateRegistrationStatus(Long registrationId, String status) {
        if (registrationId == null) return false;
        Optional<TopicRegistration> opt = registrationRepository.findById(registrationId);
        if (opt.isPresent()) {
            TopicRegistration reg = opt.get();
            reg.setStatus(status);
            if ("APPROVED".equalsIgnoreCase(status)) {
                reg.setApprovedAt(OffsetDateTime.now());
            }
            registrationRepository.save(reg);
            System.out.println("[DB-LECTURER] updateRegistrationStatus(" + registrationId + ", '" + status + "'): Đã cập nhật vào DB thành công!");
            return true;
        }
        System.out.println("[DB-LECTURER] updateRegistrationStatus: Không tìm thấy bản ghi đăng ký ID=" + registrationId);
        return false;
    }

    public List<Submission> getSubmissionsByLecturer(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return Collections.emptyList();
        List<Submission> list = submissionRepository.findByTopicSupervisorId(lecturer.getId());
        System.out.println("[DB-LECTURER] getSubmissionsByLecturer: Lấy " + list.size() + " bài nộp báo cáo từ DB");
        return list;
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
            System.out.println("[DB-LECTURER] gradeSubmission(" + submissionId + "): Đã lưu điểm (" + grade + ") và nhận xét vào DB thành công!");
            return true;
        }
        System.out.println("[DB-LECTURER] gradeSubmission: Không tìm thấy bài nộp ID=" + submissionId);
        return false;
    }

    public List<Major> getAllMajors() {
        List<Major> list = majorRepository.findAll();
        System.out.println("[DB-LECTURER] getAllMajors: Lấy " + list.size() + " chuyên ngành từ DB");
        return list;
    }

    public Major getMajorById(Long majorId) {
        if (majorId == null) return null;
        return majorRepository.findById(majorId).orElse(null);
    }
}
