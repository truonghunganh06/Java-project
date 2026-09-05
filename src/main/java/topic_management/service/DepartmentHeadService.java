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
    private final DivisionRepository divisionRepository;
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
            DivisionRepository divisionRepository,
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
        this.divisionRepository = divisionRepository;
        this.majorRepository = majorRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.periodRepository = periodRepository;
        this.studentGroupRepository = studentGroupRepository;
        this.councilRepository = councilRepository;
        this.announcementRepository = announcementRepository;
    }

    public Division getDivisionForUser(AppUser user) {
        if (user != null) {
            Optional<Lecturer> lecturerOpt = lecturerRepository.findByUserId(user.getId());
            if (lecturerOpt.isPresent() && lecturerOpt.get().getDivision() != null) {
                return lecturerOpt.get().getDivision();
            }
        }
        List<Division> divisions = divisionRepository.findAll();
        if (!divisions.isEmpty()) {
            return divisions.get(0);
        }
        return null;
    }

    public Department getDepartmentForUser(AppUser user) {
        if (user != null) {
            Optional<Lecturer> lecturerOpt = lecturerRepository.findByUserId(user.getId());
            if (lecturerOpt.isPresent()) {
                Lecturer lec = lecturerOpt.get();
                if (lec.getDivision() != null && lec.getDivision().getDepartment() != null) {
                    return lec.getDivision().getDepartment();
                }
                if (lec.getDepartment() != null) {
                    return lec.getDepartment();
                }
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

    public List<Division> getDivisionsByDepartment(Long departmentId) {
        if (departmentId != null) {
            return divisionRepository.findByDepartmentId(departmentId);
        }
        return divisionRepository.findAll();
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
    public boolean updatePeriodStatus(Long periodId, String status, Long departmentId) {
        RegistrationPeriod p = getPeriodById(periodId);
        if (p == null) return false;
        if (departmentId != null && p.getDepartment() != null
                && !departmentId.equals(p.getDepartment().getId())) {
            System.err.println("[SECURITY] Cố thay đổi đợt thuộc dept=" + p.getDepartment().getId() + " nhưng user dept=" + departmentId);
            return false;
        }
        p.setStatus(status);
        periodRepository.save(p);
        System.out.println("[DB-HEAD] Cập nhật trạng thái đợt ID=" + periodId + " -> " + status);
        return true;
    }

    // ==================== MAJORS & TOPICS ====================

    public List<Major> getDepartmentMajors(Long departmentId) {
        if (departmentId != null) {
            return majorRepository.findByDepartmentId(departmentId);
        }
        return majorRepository.findAll();
    }

    public List<Major> getDivisionMajors(Long divisionId) {
        if (divisionId != null) {
            return majorRepository.findByDivisionId(divisionId);
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

        return filterTopics(topics, status, majorId, periodId, keyword);
    }

    public List<Topic> getDivisionTopics(Long divisionId, String status, Long majorId, Long periodId, String keyword) {
        List<Topic> topics;
        if (divisionId != null) {
            topics = topicRepository.findByDivisionId(divisionId);
        } else {
            topics = topicRepository.findAll();
        }

        return filterTopics(topics, status, majorId, periodId, keyword);
    }

    private List<Topic> filterTopics(List<Topic> topics, String status, Long majorId, Long periodId, String keyword) {
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

    public org.springframework.data.domain.Page<Topic> getDepartmentTopicsPaged(Long departmentId, String status, Long majorId, Long periodId, String keyword, org.springframework.data.domain.Pageable pageable) {
        return topicRepository.findDepartmentTopicsPaged(departmentId, null, status, majorId, periodId, keyword, pageable);
    }

    public org.springframework.data.domain.Page<Topic> getDivisionTopicsPaged(Long divisionId, String status, Long majorId, Long periodId, String keyword, org.springframework.data.domain.Pageable pageable) {
        return topicRepository.findDivisionTopicsPaged(divisionId, status, majorId, periodId, keyword, pageable);
    }

    public Topic getTopicById(Long topicId) {
        if (topicId == null) return null;
        return topicRepository.findById(topicId).orElse(null);
    }

    /**
     * IDOR Check: Phê duyệt đề tài theo thẩm quyền Bộ môn / Khoa
     */
    @Transactional
    public boolean approveTopic(Long topicId, Long departmentId) {
        return approveTopic(topicId, null, departmentId);
    }

    @Transactional
    public boolean approveTopic(Long topicId, Long divisionId, Long departmentId) {
        Optional<Topic> opt = topicRepository.findById(topicId);
        if (opt.isEmpty()) return false;
        Topic t = opt.get();

        if (!isTopicInScope(t, divisionId, departmentId)) {
            System.err.println("[SECURITY-IDOR] Người dùng (div=" + divisionId + ", dept=" + departmentId + ") cố duyệt đề tài ngoài phạm vi (topicId=" + topicId + ")");
            return false;
        }

        t.setStatus("APPROVED");
        t.setRejectionReason(null);
        topicRepository.save(t);
        System.out.println("[DB-HEAD] Phê duyệt đề tài ID=" + topicId + " thành công!");
        return true;
    }

    /**
     * IDOR Check: Từ chối đề tài theo thẩm quyền Bộ môn / Khoa
     */
    @Transactional
    public boolean rejectTopic(Long topicId, String rejectionReason, Long departmentId) {
        return rejectTopic(topicId, rejectionReason, null, departmentId);
    }

    @Transactional
    public boolean rejectTopic(Long topicId, String rejectionReason, Long divisionId, Long departmentId) {
        Optional<Topic> opt = topicRepository.findById(topicId);
        if (opt.isEmpty()) return false;
        Topic t = opt.get();

        if (!isTopicInScope(t, divisionId, departmentId)) {
            System.err.println("[SECURITY-IDOR] Người dùng (div=" + divisionId + ", dept=" + departmentId + ") cố từ chối đề tài ngoài phạm vi (topicId=" + topicId + ")");
            return false;
        }

        t.setStatus("REJECTED");
        t.setRejectionReason(rejectionReason);
        topicRepository.save(t);
        System.out.println("[DB-HEAD] Từ chối đề tài ID=" + topicId + " (Lý do: " + rejectionReason + ")");
        return true;
    }

    /**
     * IDOR Helper kiểm tra đề tài có thuộc Division / Department hay không
     */
    public boolean isTopicInScope(Topic topic, Long divisionId, Long departmentId) {
        if (topic == null) return false;
        if (divisionId == null && departmentId == null) return true;

        if (divisionId != null) {
            if (topic.getDivision() != null && divisionId.equals(topic.getDivision().getId())) {
                return true;
            }
            if (topic.getMajor() != null && topic.getMajor().getDivision() != null && divisionId.equals(topic.getMajor().getDivision().getId())) {
                return true;
            }
            if (topic.getSupervisor() != null && topic.getSupervisor().getDivision() != null && divisionId.equals(topic.getSupervisor().getDivision().getId())) {
                return true;
            }
            return false;
        }

        if (departmentId != null) {
            Department dept = topic.getDepartment();
            return dept != null && departmentId.equals(dept.getId());
        }

        return true;
    }

    // ==================== LECTURERS & STUDENTS ====================

    public List<Lecturer> getLecturersInDepartment(Long departmentId) {
        if (departmentId != null) {
            return lecturerRepository.findByDepartmentId(departmentId);
        }
        return lecturerRepository.findAll();
    }

    public List<Lecturer> getLecturersInDivision(Long divisionId) {
        if (divisionId != null) {
            return lecturerRepository.findByDivisionId(divisionId);
        }
        return lecturerRepository.findAll();
    }

    public List<Student> getStudentsInDepartment(Long departmentId) {
        if (departmentId != null) {
            return studentRepository.findByMajorDepartmentId(departmentId);
        }
        return studentRepository.findAll();
    }

    public List<Student> getStudentsInDivision(Long divisionId) {
        if (divisionId != null) {
            return studentRepository.findByDivisionOrMajorDivisionId(divisionId);
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

    public org.springframework.data.domain.Page<StudentGroup> getGroupsInDepartmentAndPeriodPaged(Long departmentId, Long periodId, org.springframework.data.domain.Pageable pageable) {
        if (periodId != null && departmentId != null) {
            return studentGroupRepository.findByPeriodDepartmentIdAndPeriodId(departmentId, periodId, pageable);
        } else if (periodId != null) {
            return studentGroupRepository.findByPeriodId(periodId, pageable);
        } else if (departmentId != null) {
            return studentGroupRepository.findByPeriodDepartmentId(departmentId, pageable);
        }
        return studentGroupRepository.findAll(pageable);
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

    public Map<String, Object> getDivisionStats(Long divisionId, Long departmentId) {
        List<Topic> topics = getDivisionTopics(divisionId, "ALL", null, null, null);
        List<Lecturer> lecturers = getLecturersInDivision(divisionId);
        List<Student> students = getStudentsInDivision(divisionId);
        List<Major> majors = getDivisionMajors(divisionId);
        List<RegistrationPeriod> periods = getAllPeriods(departmentId);
        List<Council> councils = councilRepository.findByDivisionId(divisionId);
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
