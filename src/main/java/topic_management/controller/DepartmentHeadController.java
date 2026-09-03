package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.*;

import topic_management.repository.StudentGroupRepository;
import topic_management.repository.SubmissionRepository;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping({"/department-head", "/division-head"})
public class DepartmentHeadController {

    private final DepartmentHeadService departmentHeadService;
    private final CouncilService councilService;
    private final AnnouncementService announcementService;
    private final LecturerService lecturerService;
    private final StudentGroupRepository studentGroupRepository;
    private final SubmissionRepository submissionRepository;

    public DepartmentHeadController(
            DepartmentHeadService departmentHeadService,
            CouncilService councilService,
            AnnouncementService announcementService,
            LecturerService lecturerService,
            StudentGroupRepository studentGroupRepository,
            SubmissionRepository submissionRepository) {
        this.departmentHeadService = departmentHeadService;
        this.councilService = councilService;
        this.announcementService = announcementService;
        this.lecturerService = lecturerService;
        this.studentGroupRepository = studentGroupRepository;
        this.submissionRepository = submissionRepository;
    }

    private AppUser getAuthenticatedDeptHead(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) return null;
        if (user.getRole() == null || (!"DEPARTMENT_HEAD".equalsIgnoreCase(user.getRole().getCode())
                && !"DIVISION_HEAD".equalsIgnoreCase(user.getRole().getCode())
                && !"ADMIN".equalsIgnoreCase(user.getRole().getCode()))) {
            return null;
        }
        return user;
    }

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);
        Map<String, Object> stats = (division != null)
                ? departmentHeadService.getDivisionStats(division.getId(), dept.getId())
                : departmentHeadService.getDepartmentStats(dept.getId());

        List<Topic> pendingTopics = (division != null)
                ? departmentHeadService.getDivisionTopics(division.getId(), "PENDING", null, null, null)
                : departmentHeadService.getDepartmentTopics(dept.getId(), "PENDING", null, null, null);

        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());
        List<Announcement> announcements = announcementService.getAnnouncementsByDepartment(dept.getId());
        List<Council> councils = (division != null)
                ? councilService.getCouncilsByDivision(division.getId())
                : councilService.getCouncilsByDepartment(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("division", division);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingTopics", pendingTopics.stream().limit(8).toList());
        model.addAttribute("totalPendingTopics", pendingTopics.size());
        model.addAttribute("periods", periods.stream().limit(4).toList());
        model.addAttribute("allPeriods", periods);
        model.addAttribute("councils", councils.stream().limit(4).toList());
        model.addAttribute("announcements", announcements.stream().limit(3).toList());

        return "department-head/dashboard";
    }

    // ==================== REGISTRATION PERIODS ====================

    @GetMapping("/periods")
    public String listPeriods(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("periods", periods);

        return "department-head/periods";
    }

    @PostMapping("/periods/create")
    public String createPeriod(
            @RequestParam("name") String name,
            @RequestParam("periodType") String periodType,
            @RequestParam("academicYear") String academicYear,
            @RequestParam("semester") Integer semester,
            @RequestParam(value = "propStart", required = false) String propStart,
            @RequestParam(value = "propEnd", required = false) String propEnd,
            @RequestParam(value = "regStart", required = false) String regStart,
            @RequestParam(value = "regEnd", required = false) String regEnd,
            @RequestParam(value = "revDead", required = false) String revDead,
            @RequestParam(value = "defenseDate", required = false) String defenseDate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);

        OffsetDateTime pStart = parseDate(propStart, false);
        OffsetDateTime pEnd = parseDate(propEnd, true);
        OffsetDateTime rStart = parseDate(regStart, false);
        OffsetDateTime rEnd = parseDate(regEnd, true);
        OffsetDateTime rDead = parseDate(revDead, true);
        OffsetDateTime dDate = parseDate(defenseDate, false);

        departmentHeadService.createPeriod(dept, name, periodType, academicYear, semester, pStart, pEnd, rStart, rEnd, rDead, dDate);

        redirectAttributes.addFlashAttribute("successMessage", "Tạo đợt đăng ký '" + name + "' thành công!");
        return "redirect:/department-head/periods";
    }

    @PostMapping("/periods/{id}/status")
    public String updatePeriodStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        // BUG-06 FIX: Truyền departmentId để kiểm tra cross-department
        Department dept = departmentHeadService.getDepartmentForUser(user);
        boolean success = departmentHeadService.updatePeriodStatus(id, status, dept.getId());
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đợt thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái đợt này!");
        }
        return "redirect:/department-head/periods";
    }

    // ==================== TOPICS MANAGEMENT ====================

    @GetMapping("/topics")
    public String listTopics(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);

        List<Topic> topics = (division != null)
                ? departmentHeadService.getDivisionTopics(division.getId(), status, majorId, periodId, keyword)
                : departmentHeadService.getDepartmentTopics(dept.getId(), status, majorId, periodId, keyword);

        List<Major> majors = (division != null)
                ? departmentHeadService.getDivisionMajors(division.getId())
                : departmentHeadService.getDepartmentMajors(dept.getId());

        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());
        List<Lecturer> lecturers = (division != null)
                ? departmentHeadService.getLecturersInDivision(division.getId())
                : departmentHeadService.getLecturersInDepartment(dept.getId());

        List<Council> councils = (division != null)
                ? councilService.getCouncilsByDivision(division.getId())
                : councilService.getCouncilsByDepartment(dept.getId());

        Map<String, Object> stats = (division != null)
                ? departmentHeadService.getDivisionStats(division.getId(), dept.getId())
                : departmentHeadService.getDepartmentStats(dept.getId());

        // Lập bản đồ thành viên từng hội đồng để tự động lọc bỏ hội đồng xung đột với GVHD
        Map<Long, Set<Long>> councilLecturerIdsMap = new HashMap<>();
        for (Council c : councils) {
            List<CouncilMember> members = councilService.getCouncilMembers(c.getId());
            Set<Long> lIds = members.stream()
                    .filter(m -> m.getLecturer() != null)
                    .map(m -> m.getLecturer().getId())
                    .collect(Collectors.toSet());
            councilLecturerIdsMap.put(c.getId(), lIds);
        }

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("division", division);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);
        model.addAttribute("periods", periods);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("councils", councils);
        model.addAttribute("councilLecturerIdsMap", councilLecturerIdsMap);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMajorId", majorId);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", stats.get("pendingTopics"));

        return "department-head/topics";
    }

    @PostMapping("/topics/{id}/approve")
    public String approveTopic(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);
        Topic topic = departmentHeadService.getTopicById(id);
        Long divId = division != null ? division.getId() : null;
        Long deptId = dept != null ? dept.getId() : null;
        boolean success = departmentHeadService.approveTopic(id, divId, deptId);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt thành công đề tài: '" + (topic != null ? topic.getTitle() : "") + "'!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể phê duyệt đề tài (không tìm thấy hoặc không thuộc bộ môn của bạn)!");
        }

        return "redirect:/department-head/topics";
    }

    @PostMapping("/topics/{id}/reject")
    public String rejectTopic(
            @PathVariable("id") Long id,
            @RequestParam("rejectionReason") String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);
        Topic topic = departmentHeadService.getTopicById(id);
        Long divId = division != null ? division.getId() : null;
        Long deptId = dept != null ? dept.getId() : null;
        boolean success = departmentHeadService.rejectTopic(id, rejectionReason != null ? rejectionReason.trim() : "Chưa đạt yêu cầu", divId, deptId);

        if (success) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã từ chối đề tài '" + (topic != null ? topic.getTitle() : "") + "'.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể từ chối đề tài (không tìm thấy hoặc không thuộc bộ môn của bạn)!");
        }
        return "redirect:/department-head/topics";
    }

    @PostMapping("/topics/{id}/assign-reviewer")
    public String assignReviewer(
            @PathVariable("id") Long topicId,
            @RequestParam("reviewerId") Long reviewerId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        boolean ok = councilService.assignReviewer(topicId, reviewerId);
        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "Phân công Giảng viên phản biện thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể phân công GVPB trùng với Giảng viên hướng dẫn!");
        }
        return "redirect:/department-head/topics";
    }

    @PostMapping("/topics/{id}/assign-council")
    public String assignCouncil(
            @PathVariable("id") Long topicId,
            @RequestParam(value = "councilId", required = false) Long councilId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        boolean ok = councilService.assignTopicToCouncil(topicId, councilId);
        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "Phân công đề tài vào Hội đồng bảo vệ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Xung đột lợi ích: Không thể gán đề tài vào Hội đồng có chứa Giảng viên hướng dẫn!");
        }
        return "redirect:/department-head/topics";
    }

    // ==================== COUNCILS ====================

    @GetMapping("/councils")
    public String listCouncils(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);

        List<Council> councils = (division != null)
                ? councilService.getCouncilsByDivision(division.getId())
                : councilService.getCouncilsByDepartment(dept.getId());

        List<Lecturer> lecturers = (division != null)
                ? departmentHeadService.getLecturersInDivision(division.getId())
                : departmentHeadService.getLecturersInDepartment(dept.getId());

        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("division", division);
        model.addAttribute("councils", councils);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("periods", periods);

        return "department-head/councils";
    }

    @PostMapping("/councils/create")
    public String createCouncil(
            @RequestParam("name") String name,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "defenseDate", required = false) String defenseDate,
            @RequestParam("presidentId") Long presidentId,
            @RequestParam("secretaryId") Long secretaryId,
            @RequestParam(value = "member1Id", required = false) Long member1Id,
            @RequestParam(value = "member2Id", required = false) Long member2Id,
            @RequestParam(value = "member3Id", required = false) Long member3Id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Division division = departmentHeadService.getDivisionForUser(user);

        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên hội đồng không được để trống!");
            return "redirect:/department-head/councils";
        }

        List<Long> lecturerIds = new ArrayList<>();
        List<String> roles = new ArrayList<>();

        if (presidentId != null) {
            lecturerIds.add(presidentId);
            roles.add("PRESIDENT");
        }
        if (secretaryId != null) {
            lecturerIds.add(secretaryId);
            roles.add("SECRETARY");
        }
        if (member1Id != null) {
            lecturerIds.add(member1Id);
            roles.add("MEMBER");
        }
        if (member2Id != null) {
            lecturerIds.add(member2Id);
            roles.add("MEMBER");
        }
        if (member3Id != null) {
            lecturerIds.add(member3Id);
            roles.add("MEMBER");
        }

        if (lecturerIds.size() < 3) {
            redirectAttributes.addFlashAttribute("errorMessage", "Hội đồng phải có ít nhất 3 thành viên (Chủ tịch, Thư ký và ít nhất 1 Ủy viên)!");
            return "redirect:/department-head/councils";
        }

        Set<Long> uniqueLids = new HashSet<>();
        for (Long lid : lecturerIds) {
            if (!uniqueLids.add(lid)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Một giảng viên không thể đảm nhiệm nhiều vị trí trong cùng một Hội đồng!");
                return "redirect:/department-head/councils";
            }
        }

        try {
            Long divId = division != null ? division.getId() : null;
            councilService.createCouncil(dept.getId(), divId, periodId, code, name, location, parseDate(defenseDate), lecturerIds, roles);
            redirectAttributes.addFlashAttribute("successMessage", "Thành lập Hội đồng '" + name.trim() + "' thành công với " + lecturerIds.size() + " giảng viên!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi tạo hội đồng: " + ex.getMessage());
        }

        return "redirect:/department-head/councils";
    }

    // ==================== RESULTS & FINALIZING ====================

    @GetMapping("/results")
    public String listResults(
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Topic> allTopics = departmentHeadService.getDepartmentTopics(dept.getId(), "APPROVED", null, periodId, null);
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        // Pagination: Limit to 10 topics per page
        int pageSize = 10;
        int totalTopics = allTopics.size();
        int totalPages = totalTopics > 0 ? (int) Math.ceil((double) totalTopics / pageSize) : 1;
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalTopics);

        List<Topic> pagedTopics = (fromIndex < totalTopics) ? allTopics.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<Long, List<Evaluation>> topicEvaluationsMap = councilService.getEvaluationsMapForTopics(pagedTopics);
        Map<Long, StudentGroup> topicGroupsMap = councilService.getGroupsMapForTopics(pagedTopics);

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("topics", pagedTopics);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("topicEvaluationsMap", topicEvaluationsMap);
        model.addAttribute("topicGroupsMap", topicGroupsMap);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalTopics", totalTopics);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("hasPrevious", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);

        return "department-head/results";
    }

    @PostMapping("/results/{id}/publish")
    public String togglePublishResults(
            @PathVariable("id") Long topicId,
            @RequestParam("isFinalized") Boolean isFinalized,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        councilService.publishTopicResults(topicId, isFinalized);
        redirectAttributes.addFlashAttribute("successMessage", Boolean.TRUE.equals(isFinalized) ? "Đã công bố kết quả điểm số cho sinh viên!" : "Đã chuyển về trạng thái dự thảo điểm.");
        return "redirect:/department-head/results";
    }

    @PostMapping("/results/publish-all")
    public String publishAllResults(
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "isFinalized", defaultValue = "true") Boolean isFinalized,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        int count = councilService.publishAllResults(dept.getId(), periodId, isFinalized);
        redirectAttributes.addFlashAttribute("successMessage",
                Boolean.TRUE.equals(isFinalized)
                        ? "Đã công bố kết quả điểm chính thức cho toàn bộ " + count + " đề tài trong đợt!"
                        : "Đã chuyển " + count + " đề tài về trạng thái dự thảo điểm.");
        return "redirect:/department-head/results" + (periodId != null ? "?periodId=" + periodId : "");
    }

    // ==================== ANNOUNCEMENTS ====================

    @GetMapping("/announcements")
    public String listAnnouncements(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Announcement> announcements = announcementService.getAnnouncementsByDepartment(dept.getId());
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("announcements", announcements);
        model.addAttribute("periods", periods);

        return "department-head/announcements";
    }

    @PostMapping("/announcements/create")
    public String createAnnouncement(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "isPinned", required = false) Boolean isPinned,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        announcementService.createAnnouncement(user, dept, periodId, title, content, category, isPinned, null);

        redirectAttributes.addFlashAttribute("successMessage", "Đăng thông báo mới thành công!");
        return "redirect:/department-head/announcements";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        announcementService.deleteAnnouncement(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thông báo!");
        return "redirect:/department-head/announcements";
    }

    // ==================== LECTURERS & STUDENTS ====================

    @GetMapping("/lecturers")
    public String listLecturers(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Lecturer> lecturers = departmentHeadService.getLecturersInDepartment(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("lecturers", lecturers);

        return "department-head/lecturers";
    }

    @GetMapping("/students")
    public String listStudents(
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        // Default to first period if none selected
        Long effectivePeriodId = periodId;
        if (effectivePeriodId == null && !periods.isEmpty()) {
            effectivePeriodId = periods.get(0).getId();
        }

        List<StudentGroup> allGroups = departmentHeadService.getGroupsInDepartmentAndPeriod(dept.getId(), effectivePeriodId);

        // Pagination: Limit to 8 groups per page
        int pageSize = 8;
        int totalGroups = allGroups.size();
        int totalPages = totalGroups > 0 ? (int) Math.ceil((double) totalGroups / pageSize) : 1;
        int currentPage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalGroups);

        List<StudentGroup> pagedGroups = (fromIndex < totalGroups) ? allGroups.subList(fromIndex, toIndex) : Collections.emptyList();

        Map<Long, List<Submission>> groupSubmissionsMap = new HashMap<>();
        List<Topic> pagedTopics = new ArrayList<>();
        for (StudentGroup g : pagedGroups) {
            groupSubmissionsMap.put(g.getId(), submissionRepository.findByGroupId(g.getId()));
            if (g.getTopic() != null) {
                pagedTopics.add(g.getTopic());
            }
        }
        Map<Long, List<Evaluation>> topicEvaluationsMap = councilService.getEvaluationsMapForTopics(pagedTopics);

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("groups", pagedGroups);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", effectivePeriodId);
        model.addAttribute("groupSubmissionsMap", groupSubmissionsMap);
        model.addAttribute("topicEvaluationsMap", topicEvaluationsMap);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalGroups", totalGroups);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("hasPrevious", currentPage > 1);
        model.addAttribute("hasNext", currentPage < totalPages);

        return "department-head/students";
    }

    @PostMapping("/students/groups/{id}/unassign-topic")
    public String unassignTopicFromGroup(
            @PathVariable("id") Long groupId,
            @RequestParam(value = "periodId", required = false) Long periodId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        StudentGroup group = studentGroupRepository.findById(groupId).orElse(null);
        if (group != null) {
            group.setTopic(null);
            group.setStatus("DRAFT");
            studentGroupRepository.save(group);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy gán đề tài cho nhóm " + group.getGroupName() + "!");
        }
        return "redirect:/department-head/students" + (periodId != null ? "?periodId=" + periodId : "");
    }

    private OffsetDateTime parseDate(String dateStr) {
        return parseDate(dateStr, false);
    }

    private OffsetDateTime parseDate(String dateStr, boolean isEndOfDay) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            if (dateStr.length() == 10) { // yyyy-MM-dd
                String timePart = isEndOfDay ? "T23:59:59+07:00" : "T00:00:00+07:00";
                return OffsetDateTime.parse(dateStr + timePart);
            }
            if (dateStr.length() == 16) { // yyyy-MM-ddTHH:mm
                return OffsetDateTime.parse(dateStr + ":00+07:00");
            }
            return OffsetDateTime.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}
