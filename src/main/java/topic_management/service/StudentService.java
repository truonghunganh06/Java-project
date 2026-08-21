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
    private final TopicRegistrationRepository registrationRepository;
    private final SubmissionRepository submissionRepository;
    private final MajorRepository majorRepository;

    public StudentService(
            StudentRepository studentRepository,
            TopicRepository topicRepository,
            TopicRegistrationRepository registrationRepository,
            SubmissionRepository submissionRepository,
            MajorRepository majorRepository) {
        this.studentRepository = studentRepository;
        this.topicRepository = topicRepository;
        this.registrationRepository = registrationRepository;
        this.submissionRepository = submissionRepository;
        this.majorRepository = majorRepository;
    }

    @Transactional
    public Student getStudentByUser(AppUser user) {
        if (user == null || user.getId() == null) return null;
        Optional<Student> opt = studentRepository.findByUserId(user.getId());
        if (opt.isPresent()) {
            return opt.get();
        }

        // Tạo bản ghi Student mới trong DB nếu chưa có
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

        Student saved = studentRepository.save(newStudent);
        System.out.println("[DB-STUDENT] getStudentByUser: Đã tạo bản ghi Sinh viên mới trong DB (ID=" + saved.getId() + ", Mã=" + saved.getStudentCode() + ") cho User '" + user.getUsername() + "'");
        return saved;
    }

    public List<Topic> getAvailableTopics(Long majorId, String keyword) {
        List<Topic> list = topicRepository.findByStatus("APPROVED");
        System.out.println("[DB-STUDENT] getAvailableTopics: Lấy " + list.size() + " đề tài APPROVED từ Database");

        return list.stream()
                .filter(t -> majorId == null || (t.getMajor() != null && majorId.equals(t.getMajor().getId())))
                .filter(t -> keyword == null || keyword.trim().isEmpty() ||
                        (t.getTitle() != null && t.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                        (t.getCode() != null && t.getCode().toLowerCase().contains(keyword.toLowerCase())))
                .collect(Collectors.toList());
    }

    public Topic getTopicById(Long id) {
        if (id == null) return null;
        Optional<Topic> opt = topicRepository.findById(id);
        System.out.println("[DB-STUDENT] getTopicById(" + id + "): " + (opt.isPresent() ? "Tìm thấy đề tài: " + opt.get().getTitle() : "Không tìm thấy"));
        return opt.orElse(null);
    }

    public int getRegisteredCountForTopic(Long topicId) {
        if (topicId == null) return 0;
        List<TopicRegistration> list = registrationRepository.findByTopicId(topicId);
        int count = (int) list.stream().filter(r -> !"REJECTED".equalsIgnoreCase(r.getStatus())).count();
        System.out.println("[DB-STUDENT] getRegisteredCountForTopic(" + topicId + "): " + count + " sinh viên đã đăng ký hợp lệ");
        return count;
    }

    public TopicRegistration getMyRegistration(Student student) {
        if (student == null || student.getId() == null) return null;
        List<TopicRegistration> list = registrationRepository.findByStudentId(student.getId());
        System.out.println("[DB-STUDENT] getMyRegistration: Lấy " + list.size() + " bản ghi đăng ký của SV ID=" + student.getId());
        if (!list.isEmpty()) {
            return list.get(list.size() - 1);
        }
        return null;
    }

    @Transactional
    public String registerTopic(Student student, Long topicId) {
        TopicRegistration existing = getMyRegistration(student);
        if (existing != null && ("APPROVED".equalsIgnoreCase(existing.getStatus()) || "PENDING".equalsIgnoreCase(existing.getStatus()))) {
            return "Bạn đã có đề tài đang đăng ký (" + (existing.getTopic() != null ? existing.getTopic().getTitle() : "") + "). Không thể đăng ký thêm đề tài khác.";
        }

        Topic topic = getTopicById(topicId);
        if (topic == null) {
            return "Đề tài không tồn tại.";
        }

        int currentCount = getRegisteredCountForTopic(topicId);
        if (topic.getMaxStudents() != null && currentCount >= topic.getMaxStudents()) {
            return "Đề tài đã đủ số lượng sinh viên đăng ký (" + currentCount + "/" + topic.getMaxStudents() + ").";
        }

        TopicRegistration newReg = new TopicRegistration();
        newReg.setTopic(topic);
        newReg.setStudent(student);
        newReg.setStatus("PENDING");
        newReg.setRegisteredAt(OffsetDateTime.now());

        registrationRepository.save(newReg);
        System.out.println("[DB-STUDENT] registerTopic: ĐÃ LƯU ĐĂNG KÝ ĐỀ TÀI VÀO DB! (SV: " + student.getStudentCode() + ", Đề tài: " + topic.getTitle() + ")");
        return null; // success
    }

    public List<Submission> getMySubmissions(Student student) {
        if (student == null || student.getId() == null) return Collections.emptyList();
        List<Submission> list = submissionRepository.findByStudentId(student.getId());
        System.out.println("[DB-STUDENT] getMySubmissions: Lấy " + list.size() + " bài nộp báo cáo từ DB của SV ID=" + student.getId());
        return list.stream()
                .sorted(Comparator.comparing(Submission::getSubmittedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public Submission submitReport(Student student, Long topicId, String title, Integer round, String fileName, Long fileSize) {
        Topic topic = getTopicById(topicId);

        Submission sub = new Submission();
        sub.setTopic(topic);
        sub.setStudent(student);
        sub.setTitle(title);
        sub.setSubmissionRound(round != null ? round : 1);
        sub.setFilePath("/uploads/submissions/" + (fileName != null ? fileName : "report.pdf"));
        sub.setFileSize(fileSize != null ? fileSize : 1048576L);
        sub.setSubmittedAt(OffsetDateTime.now());

        Submission saved = submissionRepository.save(sub);
        System.out.println("[DB-STUDENT] submitReport: ĐÃ LƯU BÀI NỘP BÁO CÁO VÀO DB! (Tiêu đề: '" + title + "', Vòng: " + round + ")");
        return saved;
    }

    public List<Major> getAllMajors() {
        List<Major> list = majorRepository.findAll();
        System.out.println("[DB-STUDENT] getAllMajors: Lấy " + list.size() + " chuyên ngành từ DB");
        return list;
    }
}
