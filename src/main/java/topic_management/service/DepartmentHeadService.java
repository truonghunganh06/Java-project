package topic_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentHeadService {

    private final TopicRepository topicRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final TopicRegistrationRepository registrationRepository;

    public DepartmentHeadService(
            TopicRepository topicRepository,
            DepartmentRepository departmentRepository,
            MajorRepository majorRepository,
            LecturerRepository lecturerRepository,
            StudentRepository studentRepository,
            TopicRegistrationRepository registrationRepository) {
        this.topicRepository = topicRepository;
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
    }

    public Department getDepartmentForUser(AppUser user) {
        if (user != null) {
            Optional<Lecturer> lecturerOpt = lecturerRepository.findByUserId(user.getId());
            if (lecturerOpt.isPresent() && lecturerOpt.get().getDepartment() != null) {
                System.out.println("[DB-HEAD] getDepartmentForUser: Tìm thấy khoa của Trưởng khoa '" + user.getUsername() + "' -> " + lecturerOpt.get().getDepartment().getName());
                return lecturerOpt.get().getDepartment();
            }
        }

        List<Department> departments = departmentRepository.findAll();
        System.out.println("[DB-HEAD] getDepartmentForUser: Lấy danh sách khoa từ DB (Tổng: " + departments.size() + " khoa)");
        if (!departments.isEmpty()) {
            return departments.get(0);
        }

        Department defaultDept = new Department();
        defaultDept.setId(1L);
        defaultDept.setCode("CNTT");
        defaultDept.setName("Khoa Công nghệ Thông tin");
        return defaultDept;
    }

    public List<Major> getDepartmentMajors(Long departmentId) {
        List<Major> majors;
        if (departmentId != null) {
            majors = majorRepository.findByDepartmentId(departmentId);
        } else {
            majors = majorRepository.findAll();
        }
        System.out.println("[DB-HEAD] getDepartmentMajors: Lấy " + majors.size() + " chuyên ngành từ DB cho DepartmentId=" + departmentId);
        return majors;
    }

    public List<Topic> getDepartmentTopics(Long departmentId, String status, Long majorId, String keyword) {
        List<Topic> topics;
        if (departmentId != null) {
            topics = topicRepository.findByMajorDepartmentId(departmentId);
        } else {
            topics = topicRepository.findAll();
        }

        System.out.println("[DB-HEAD] getDepartmentTopics: Truy vấn từ DB được " + topics.size() + " đề tài (DepartmentId=" + departmentId + ")");

        return topics.stream()
                .filter(t -> {
                    if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(t.getStatus())) return false;
                    }
                    if (majorId != null) {
                        if (t.getMajor() == null || !majorId.equals(t.getMajor().getId())) return false;
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchTitle = t.getTitle() != null && t.getTitle().toLowerCase().contains(kw);
                        boolean matchCode = t.getCode() != null && t.getCode().toLowerCase().contains(kw);
                        boolean matchSupervisor = t.getSupervisor() != null && t.getSupervisor().getUser() != null &&
                                t.getSupervisor().getUser().getFullName() != null &&
                                t.getSupervisor().getUser().getFullName().toLowerCase().contains(kw);
                        return matchTitle || matchCode || matchSupervisor;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Topic::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Topic getTopicById(Long topicId) {
        if (topicId == null) return null;
        Optional<Topic> opt = topicRepository.findById(topicId);
        System.out.println("[DB-HEAD] getTopicById(" + topicId + "): " + (opt.isPresent() ? "Tìm thấy đề tài: " + opt.get().getTitle() : "Không tìm thấy"));
        return opt.orElse(null);
    }

    @Transactional
    public boolean approveTopic(Long topicId) {
        Optional<Topic> opt = topicRepository.findById(topicId);
        if (opt.isPresent()) {
            Topic t = opt.get();
            t.setStatus("APPROVED");
            t.setRejectionReason(null);
            topicRepository.save(t);
            System.out.println("[DB-HEAD] approveTopic(" + topicId + "): Đã cập nhật trạng thái APPROVED vào DB thành công!");
            return true;
        }
        System.out.println("[DB-HEAD] approveTopic(" + topicId + "): Thất bại, không tìm thấy đề tài trong DB.");
        return false;
    }

    @Transactional
    public boolean rejectTopic(Long topicId, String rejectionReason) {
        Optional<Topic> opt = topicRepository.findById(topicId);
        if (opt.isPresent()) {
            Topic t = opt.get();
            t.setStatus("REJECTED");
            t.setRejectionReason(rejectionReason);
            topicRepository.save(t);
            System.out.println("[DB-HEAD] rejectTopic(" + topicId + "): Đã cập nhật trạng thái REJECTED (Lý do: " + rejectionReason + ") vào DB thành công!");
            return true;
        }
        System.out.println("[DB-HEAD] rejectTopic(" + topicId + "): Thất bại, không tìm thấy đề tài trong DB.");
        return false;
    }

    public List<Lecturer> getLecturersInDepartment(Long departmentId) {
        List<Lecturer> list;
        if (departmentId != null) {
            list = lecturerRepository.findByDepartmentId(departmentId);
        } else {
            list = lecturerRepository.findAll();
        }
        System.out.println("[DB-HEAD] getLecturersInDepartment: Lấy " + list.size() + " giảng viên từ DB (DepartmentId=" + departmentId + ")");
        return list;
    }

    public List<Student> getStudentsInDepartment(Long departmentId) {
        List<Student> list;
        if (departmentId != null) {
            list = studentRepository.findByMajorDepartmentId(departmentId);
        } else {
            list = studentRepository.findAll();
        }
        System.out.println("[DB-HEAD] getStudentsInDepartment: Lấy " + list.size() + " sinh viên từ DB (DepartmentId=" + departmentId + ")");
        return list;
    }

    public Map<Long, TopicRegistration> getStudentRegistrationsMap() {
        Map<Long, TopicRegistration> map = new HashMap<>();
        List<TopicRegistration> list = registrationRepository.findAll();
        for (TopicRegistration r : list) {
            if (r.getStudent() != null) {
                map.put(r.getStudent().getId(), r);
            }
        }
        System.out.println("[DB-HEAD] getStudentRegistrationsMap: Lấy " + list.size() + " bản ghi đăng ký đề tài từ DB");
        return map;
    }

    public Map<String, Object> getDepartmentStats(Long departmentId) {
        List<Topic> topics = getDepartmentTopics(departmentId, "ALL", null, null);
        List<Lecturer> lecturers = getLecturersInDepartment(departmentId);
        List<Student> students = getStudentsInDepartment(departmentId);
        List<Major> majors = getDepartmentMajors(departmentId);

        long pendingCount = topics.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
        long approvedCount = topics.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long rejectedCount = topics.stream().filter(t -> "REJECTED".equalsIgnoreCase(t.getStatus())).count();

        Map<String, Long> majorTopicMap = new HashMap<>();
        for (Major m : majors) {
            long count = topics.stream().filter(t -> t.getMajor() != null && m.getId().equals(t.getMajor().getId())).count();
            majorTopicMap.put(m.getName(), count);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTopics", topics.size());
        stats.put("pendingTopics", pendingCount);
        stats.put("approvedTopics", approvedCount);
        stats.put("rejectedTopics", rejectedCount);
        stats.put("totalLecturers", lecturers.size());
        stats.put("totalStudents", students.size());
        stats.put("majorTopicMap", majorTopicMap);

        System.out.println("[DB-HEAD] getDepartmentStats: Thống kê từ DB -> Tổng đề tài: " + topics.size() + ", Chờ duyệt: " + pendingCount + ", Đã duyệt: " + approvedCount + ", Từ chối: " + rejectedCount);
        return stats;
    }
}
