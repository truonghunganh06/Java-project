package topic_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
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
    private final RegistrationPeriodRepository periodRepository;
    private final StudentGroupRepository studentGroupRepository;
    private final CouncilRepository councilRepository;
    private final AnnouncementRepository announcementRepository;

    public DepartmentHeadService(
            TopicRepository topicRepository,
            DepartmentRepository departmentRepository,
            MajorRepository majorRepository,
            LecturerRepository lecturerRepository,
            StudentRepository studentRepository,
            TopicRegistrationRepository registrationRepository,
            RegistrationPeriodRepository periodRepository,
            StudentGroupRepository studentGroupRepository,
            CouncilRepository councilRepository,
            AnnouncementRepository announcementRepository) {
        this.topicRepository = topicRepository;
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.periodRepository = periodRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.councilRepository = councilRepository;
        this.announcementRepository = announcementRepository;
    }

    public Department getDepartmentForUser(AppUser user) {
        if (user != null) {
            Optional<Lecturer> lecturerOpt = lecturerRepository.findByUserId(user.getId());
            if (lecturerOpt.isPresent() && lecturerOpt.get().getDepartment() != null) {
                return lecturerOpt.get().getDepartment();
            }
        }

        List<Department> departments = departmentRepository.findAll();
        if (!departments.isEmpty()) {
            return departments.get(0);
        }

        Department defaultDept = new Department();
        defaultDept.setId(1L);
        defaultDept.setCode("CNTT");
        defaultDept.setName("Khoa Công nghệ Thông tin");
        return defaultDept;
    }

    // ==================== REGISTRATION PERIODS ====================

    public List<RegistrationPeriod> getAllPeriods(Long departmentId) {
        if (departmentId != null) {
            List<RegistrationPeriod> list = periodRepository.findByDepartmentId(departmentId);
            if (!list.isEmpty()) return list;
        }
        return periodRepository.findByIsActiveTrueOrderByIdDesc();
    }

    public RegistrationPeriod getPeriodById(Long id) {
        if (id == null) return null;
        return periodRepository.findById(id).orElse(null);
    }

    @Transactional
    public RegistrationPeriod createPeriod(Department dept, String name, String periodType, String academicYear, Integer semester,
                                           OffsetDateTime propStart, OffsetDateTime propEnd,
                                           OffsetDateTime regStart, OffsetDateTime regEnd,
                                           OffsetDateTime revDead, OffsetDateTime defenseDate) {
        RegistrationPeriod p = new RegistrationPeriod();
        p.setDepartment(dept);
        p.setName(name);
        p.setPeriodType(periodType != null ? periodType : "KLTN");
        p.setAcademicYear(academicYear != null ? academicYear : "2026-2027");
        p.setSemester(semester != null ? semester : 1);
        p.setLecturerProposalStart(propStart != null ? propStart : OffsetDateTime.now());
        p.setLecturerProposalEnd(propEnd != null ? propEnd : OffsetDateTime.now().plusDays(15));
        p.setStudentRegistrationStart(regStart != null ? regStart : OffsetDateTime.now().plusDays(16));
        p.setStudentRegistrationEnd(regEnd != null ? regEnd : OffsetDateTime.now().plusDays(30));
        p.setReviewerDeadline(revDead != null ? revDead : OffsetDateTime.now().plusDays(60));
        p.setCouncilDefenseDate(defenseDate != null ? defenseDate : OffsetDateTime.now().plusDays(75));
        p.setStatus("OPEN");
        p.setIsActive(true);
        p.setCreatedAt(OffsetDateTime.now());

        RegistrationPeriod saved = periodRepository.save(p);
        System.out.println("[DB-HEAD] Đã tạo đợt đăng ký mới: '" + name + "' (ID=" + saved.getId() + ")");
        return saved;
    }

    @Transactional
    public boolean updatePeriodStatus(Long periodId, String status) {
        RegistrationPeriod p = getPeriodById(periodId);
        if (p != null) {
            p.setStatus(status);
            periodRepository.save(p);
            System.out.println("[DB-HEAD] Cập nhật trạng thái đợt ID=" + periodId + " -> " + status);
            return true;
        }
        return false;
    }

    // ==================== MAJORS & TOPICS ====================

    public List<Major> getDepartmentMajors(Long departmentId) {
        if (departmentId != null) {
            return majorRepository.findByDepartmentId(departmentId);
        }
        return majorRepository.findAll();
    }

    public List<Topic> getDepartmentTopics(Long departmentId, String status, Long majorId, Long periodId, String keyword) {
        List<Topic> topics;
        if (departmentId != null) {
            topics = topicRepository.findByMajorDepartmentId(departmentId);
        } else {
            topics = topicRepository.findAll();
        }

        return topics.stream()
                .filter(t -> {
                    if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(t.getStatus())) return false;
                    }
                    if (majorId != null) {
                        if (t.getMajor() == null || !majorId.equals(t.getMajor().getId())) return false;
                    }
                    if (periodId != null) {
                        if (t.getPeriod() == null || !periodId.equals(t.getPeriod().getId())) return false;
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
        return topicRepository.findById(topicId).orElse(null);
    }

    @Transactional
    public boolean approveTopic(Long topicId) {
        Optional<Topic> opt = topicRepository.findById(topicId);
        if (opt.isPresent()) {
            Topic t = opt.get();
            t.setStatus("APPROVED");
            t.setRejectionReason(null);
            topicRepository.save(t);
            System.out.println("[DB-HEAD] Phê duyệt đề tài ID=" + topicId + " thành công!");
            return true;
        }
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
            System.out.println("[DB-HEAD] Từ chối đề tài ID=" + topicId + " (Lý do: " + rejectionReason + ")");
            return true;
        }
        return false;
    }

    // ==================== LECTURERS & STUDENTS ====================

    public List<Lecturer> getLecturersInDepartment(Long departmentId) {
        if (departmentId != null) {
            return lecturerRepository.findByDepartmentId(departmentId);
        }
        return lecturerRepository.findAll();
    }

    public List<Student> getStudentsInDepartment(Long departmentId) {
        if (departmentId != null) {
            return studentRepository.findByMajorDepartmentId(departmentId);
        }
        return studentRepository.findAll();
    }

    public List<StudentGroup> getAllGroups() {
        return studentGroupRepository.findAll();
    }

    public List<StudentGroup> getGroupsInDepartmentAndPeriod(Long departmentId, Long periodId) {
        if (periodId != null) {
            return studentGroupRepository.findByPeriodId(periodId);
        }
        if (departmentId != null) {
            List<StudentGroup> list = studentGroupRepository.findByPeriodDepartmentId(departmentId);
            if (!list.isEmpty()) return list;
        }
        return studentGroupRepository.findAll();
    }

    public Map<String, Object> getDepartmentStats(Long departmentId) {
        List<Topic> topics = getDepartmentTopics(departmentId, "ALL", null, null, null);
        List<Lecturer> lecturers = getLecturersInDepartment(departmentId);
        List<Student> students = getStudentsInDepartment(departmentId);
        List<Major> majors = getDepartmentMajors(departmentId);
        List<RegistrationPeriod> periods = getAllPeriods(departmentId);
        List<Council> councils = councilRepository.findByDepartmentId(departmentId);
        List<StudentGroup> groups = studentGroupRepository.findAll();

        long pendingCount = topics.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
        long approvedCount = topics.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long rejectedCount = topics.stream().filter(t -> "REJECTED".equalsIgnoreCase(t.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTopics", topics.size());
        stats.put("pendingTopics", pendingCount);
        stats.put("approvedTopics", approvedCount);
        stats.put("rejectedTopics", rejectedCount);
        stats.put("totalLecturers", lecturers.size());
        stats.put("totalStudents", students.size());
        stats.put("totalPeriods", periods.size());
        stats.put("totalCouncils", councils.size());
        stats.put("totalGroups", groups.size());

        return stats;
    }
}
