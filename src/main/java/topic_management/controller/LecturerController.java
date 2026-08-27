package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final LecturerService lecturerService;
    private final CouncilService councilService;
    private final AnnouncementService announcementService;

    public LecturerController(
            LecturerService lecturerService,
            CouncilService councilService,
            AnnouncementService announcementService) {
        this.lecturerService = lecturerService;
        this.councilService = councilService;
        this.announcementService = announcementService;
    }

    /**
     * BUG-04 FIX: Kiểm tra role LECTURER / DEPARTMENT_HEAD / ADMIN
     * (Trưởng khoa và Admin cũng có thể là giảng viên nên được phép truy cập)
     */
    private AppUser getAuthenticatedUser(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) return null;
        if (user.getRole() == null) return null;
        String roleCode = user.getRole().getCode();
        if (!"LECTURER".equalsIgnoreCase(roleCode)
                && !"DEPARTMENT_HEAD".equalsIgnoreCase(roleCode)
                && !"ADMIN".equalsIgnoreCase(roleCode)) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<Topic> reviewerTopics = lecturerService.getReviewerTopics(lecturer);
        List<Council> councils = lecturerService.getCouncilsForLecturer(lecturer);
        List<StudentGroup> groups = lecturerService.getGroupsForLecturerTopics(lecturer);
        List<Submission> submissions = lecturerService.getSubmissionsByLecturer(lecturer);
        List<Announcement> announcements = announcementService.getAllAnnouncements();

        long approvedCount = topics.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long pendingCount = topics.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topics", topics);
        model.addAttribute("reviewerTopics", reviewerTopics);
        model.addAttribute("councils", councils);
        model.addAttribute("groups", groups);
        model.addAttribute("submissions", submissions);
        model.addAttribute("announcements", announcements.stream().limit(5).toList());
        model.addAttribute("totalTopics", topics.size());
        model.addAttribute("approvedTopics", approvedCount);
        model.addAttribute("pendingTopics", pendingCount);
        model.addAttribute("reviewerCount", reviewerTopics.size());
        model.addAttribute("councilCount", councils.size());

        return "lecturer/dashboard";
    }

    // ==================== TOPICS ====================

    @GetMapping("/topics")
    public String listTopics(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<Major> majors = lecturerService.getAllMajors();
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);
        model.addAttribute("periods", periods);

        return "lecturer/topics";
    }

    @GetMapping("/topics/create")
    public String showCreateTopicForm(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<RegistrationPeriod> openPeriods = lecturerService.getOpenProposalPeriods();
        List<Lecturer> coSupervisors = lecturerService.getOtherLecturers(lecturer.getId());
        List<Major> majors = lecturerService.getAllMajors();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topic", new Topic());
        model.addAttribute("openPeriods", openPeriods);
        model.addAttribute("coSupervisors", coSupervisors);
        model.addAttribute("majors", majors);

        return "lecturer/create-topic";
    }

    @PostMapping("/topics/create")
    public String handleCreateTopic(
            @ModelAttribute Topic topic,
            @RequestParam("majorId") Long majorId,
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "coSupervisorId", required = false) Long coSupervisorId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        try {
            lecturerService.createTopic(lecturer, periodId, majorId, coSupervisorId, topic);
            redirectAttributes.addFlashAttribute("successMessage", "Đề xuất đề tài '" + topic.getTitle() + "' thành công và đang chờ Trưởng khoa xét duyệt!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    // ==================== GROUPS & APPROVAL ====================

    @GetMapping("/registrations")
    public String listGroups(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<StudentGroup> groups = lecturerService.getGroupsForLecturerTopics(lecturer);
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("groups", groups);
        model.addAttribute("periods", periods);

        return "lecturer/registrations";
    }

    @PostMapping("/registrations/{id}/approve")
    public String approveGroup(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        // BUG-07 FIX: Truyền lecturer để kiểm tra ownership
        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean success = lecturerService.approveGroupTopic(id, "APPROVED", lecturer);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt nhóm sinh viên hướng dẫn thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể phê duyệt nhóm này (không tìm thấy hoặc không phải đề tài bạn hướng dẫn)!");
        }
        return "redirect:/lecturer/registrations";
    }

    @PostMapping("/registrations/{id}/reject")
    public String rejectGroup(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        // BUG-07 FIX: Truyền lecturer để kiểm tra ownership
        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean success = lecturerService.approveGroupTopic(id, "REJECTED", lecturer);
        if (success) {
            redirectAttributes.addFlashAttribute("errorMessage", "Đã từ chối nhóm đăng ký.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể từ chối nhóm này!");
        }
        return "redirect:/lecturer/registrations";
    }

    // ==================== REVIEWS (GVPB) ====================

    @GetMapping("/reviews")
    public String listReviewTopics(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> reviewerTopics = lecturerService.getReviewerTopics(lecturer);
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("reviewerTopics", reviewerTopics);
        model.addAttribute("periods", periods);

        return "lecturer/reviews";
    }

    @PostMapping("/reviews/{topicId}/grade")
    public String gradeReviewer(
            @PathVariable("topicId") Long topicId,
            @RequestParam("score") Double score,
            @RequestParam("comments") String comments,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean ok = councilService.submitEvaluation(topicId, lecturer.getId(), "REVIEWER", score, comments);

        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã nộp điểm và nhận xét phản biện thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chấm phản biện cho đề tài này (xung đột lợi ích)!");
        }

        return "redirect:/lecturer/reviews";
    }

    // ==================== COUNCILS ====================

    @GetMapping("/councils")
    public String listCouncils(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Council> councils = lecturerService.getCouncilsForLecturer(lecturer);
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();

        Map<Long, List<Topic>> councilTopicsMap = new HashMap<>();
        for (Council c : councils) {
            councilTopicsMap.put(c.getId(), councilService.getTopicsInCouncil(c.getId()));
        }

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("councils", councils);
        model.addAttribute("councilTopicsMap", councilTopicsMap);
        model.addAttribute("periods", periods);

        return "lecturer/councils";
    }

    @PostMapping("/councils/{topicId}/grade")
    public String gradeCouncil(
            @PathVariable("topicId") Long topicId,
            @RequestParam("score") Double score,
            @RequestParam("comments") String comments,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean ok = councilService.submitEvaluation(topicId, lecturer.getId(), "COUNCIL", score, comments);

        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã nộp phiếu chấm điểm Hội đồng thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "LỖI XUNG ĐỘT: Giảng viên không được chấm điểm đề tài mình đang hướng dẫn!");
        }

        return "redirect:/lecturer/councils";
    }

    // ==================== SUBMISSIONS ====================

    @GetMapping("/submissions")
    public String listSubmissions(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Submission> submissions = lecturerService.getSubmissionsByLecturer(lecturer);
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("submissions", submissions);
        model.addAttribute("periods", periods);

        return "lecturer/submissions";
    }

    @PostMapping("/submissions/{id}/grade")
    public String gradeSubmission(
            @PathVariable("id") Long id,
            @RequestParam("grade") Double grade,
            @RequestParam("feedback") String feedback,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        // BUG-08 FIX: Truyền lecturer để kiểm tra ownership
        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean success = lecturerService.gradeSubmission(id, grade, feedback, lecturer);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đánh giá bài nộp báo cáo thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể chấm bài nộp này (không tìm thấy hoặc không phải đề tài bạn hướng dẫn)!");
        }
        return "redirect:/lecturer/submissions";
    }
}
