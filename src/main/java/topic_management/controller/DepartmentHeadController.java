package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.*;

import java.time.OffsetDateTime;
import java.util.*;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {

    private final DepartmentHeadService departmentHeadService;
    private final CouncilService councilService;
    private final AnnouncementService announcementService;
    private final LecturerService lecturerService;

    public DepartmentHeadController(
            DepartmentHeadService departmentHeadService,
            CouncilService councilService,
            AnnouncementService announcementService,
            LecturerService lecturerService) {
        this.departmentHeadService = departmentHeadService;
        this.councilService = councilService;
        this.announcementService = announcementService;
        this.lecturerService = lecturerService;
    }

    private AppUser getAuthenticatedDeptHead(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) return null;
        if (user.getRole() == null || (!"DEPARTMENT_HEAD".equalsIgnoreCase(user.getRole().getCode()) && !"ADMIN".equalsIgnoreCase(user.getRole().getCode()))) {
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
        Map<String, Object> stats = departmentHeadService.getDepartmentStats(dept.getId());
        List<Topic> pendingTopics = departmentHeadService.getDepartmentTopics(dept.getId(), "PENDING", null, null, null);
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());
        List<Announcement> announcements = announcementService.getAnnouncementsByDepartment(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingTopics", pendingTopics.stream().limit(5).toList());
        model.addAttribute("periods", periods);
        model.addAttribute("announcements", announcements.stream().limit(5).toList());

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

        OffsetDateTime pStart = parseDate(propStart);
        OffsetDateTime pEnd = parseDate(propEnd);
        OffsetDateTime rStart = parseDate(regStart);
        OffsetDateTime rEnd = parseDate(regEnd);
        OffsetDateTime rDead = parseDate(revDead);
        OffsetDateTime dDate = parseDate(defenseDate);

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

        departmentHeadService.updatePeriodStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đợt thành công!");
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
        List<Topic> topics = departmentHeadService.getDepartmentTopics(dept.getId(), status, majorId, periodId, keyword);
        List<Major> majors = departmentHeadService.getDepartmentMajors(dept.getId());
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());
        List<Lecturer> lecturers = departmentHeadService.getLecturersInDepartment(dept.getId());
        List<Council> councils = councilService.getCouncilsByDepartment(dept.getId());
        Map<String, Object> stats = departmentHeadService.getDepartmentStats(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);
        model.addAttribute("periods", periods);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("councils", councils);
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

        Topic topic = departmentHeadService.getTopicById(id);
        boolean success = departmentHeadService.approveTopic(id);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt thành công đề tài: '" + (topic != null ? topic.getTitle() : "") + "'!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đề tài để phê duyệt!");
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

        Topic topic = departmentHeadService.getTopicById(id);
        departmentHeadService.rejectTopic(id, rejectionReason != null ? rejectionReason.trim() : "Chưa đạt yêu cầu");

        redirectAttributes.addFlashAttribute("errorMessage", "Đã từ chối đề tài '" + (topic != null ? topic.getTitle() : "") + "'.");
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

        councilService.assignTopicToCouncil(topicId, councilId);
        redirectAttributes.addFlashAttribute("successMessage", "Phân công đề tài vào Hội đồng bảo vệ thành công!");
        return "redirect:/department-head/topics";
    }

    // ==================== COUNCILS ====================

    @GetMapping("/councils")
    public String listCouncils(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Council> councils = councilService.getCouncilsByDepartment(dept.getId());
        List<Lecturer> lecturers = departmentHeadService.getLecturersInDepartment(dept.getId());
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
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

        councilService.createCouncil(dept.getId(), periodId, code, name, location, parseDate(defenseDate), lecturerIds, roles);
        redirectAttributes.addFlashAttribute("successMessage", "Thành lập Hội đồng '" + name + "' thành công với " + lecturerIds.size() + " giảng viên!");
        return "redirect:/department-head/councils";
    }

    // ==================== RESULTS & FINALIZING ====================

    @GetMapping("/results")
    public String listResults(
            @RequestParam(value = "periodId", required = false) Long periodId,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Topic> topics = departmentHeadService.getDepartmentTopics(dept.getId(), "APPROVED", null, periodId, null);
        List<RegistrationPeriod> periods = departmentHeadService.getAllPeriods(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("topics", topics);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);

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
    public String listStudents(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Student> students = departmentHeadService.getStudentsInDepartment(dept.getId());
        List<StudentGroup> groups = departmentHeadService.getAllGroups();

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("students", students);
        model.addAttribute("groups", groups);

        return "department-head/students";
    }

    private OffsetDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            if (dateStr.length() == 10) { // yyyy-MM-dd
                return OffsetDateTime.parse(dateStr + "T00:00:00+07:00");
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
