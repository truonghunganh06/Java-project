package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.LecturerService;

import java.util.List;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    private AppUser getAuthenticatedUser(HttpSession session) {
        return (AppUser) session.getAttribute("currentUser");
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<TopicRegistration> registrations = lecturerService.getRegistrationsByLecturer(lecturer);
        List<Submission> submissions = lecturerService.getSubmissionsByLecturer(lecturer);

        long approvedCount = topics.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long pendingCount = topics.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
        long pendingRegCount = registrations.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getStatus())).count();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topics", topics);
        model.addAttribute("registrations", registrations);
        model.addAttribute("submissions", submissions);
        model.addAttribute("totalTopics", topics.size());
        model.addAttribute("approvedTopics", approvedCount);
        model.addAttribute("pendingTopics", pendingCount);
        model.addAttribute("pendingRegistrations", pendingRegCount);
        model.addAttribute("totalSubmissions", submissions.size());

        return "lecturer/dashboard";
    }

    @GetMapping("/topics")
    public String listTopics(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<Major> majors = lecturerService.getAllMajors();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);

        return "lecturer/topics";
    }

    @GetMapping("/topics/create")
    public String showCreateTopicForm(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topic", new Topic());
        model.addAttribute("majors", lecturerService.getAllMajors());

        return "lecturer/create-topic";
    }

    @PostMapping("/topics/create")
    public String handleCreateTopic(
            @ModelAttribute Topic topic,
            @RequestParam("majorId") Long majorId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        lecturerService.createTopic(lecturer, topic, majorId);

        redirectAttributes.addFlashAttribute("successMessage", "Đề xuất đề tài '" + topic.getTitle() + "' thành công và đang chờ xét duyệt!");
        return "redirect:/lecturer/topics";
    }

    @GetMapping("/registrations")
    public String listRegistrations(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<TopicRegistration> registrations = lecturerService.getRegistrationsByLecturer(lecturer);

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("registrations", registrations);

        return "lecturer/registrations";
    }

    @PostMapping("/registrations/{id}/approve")
    public String approveRegistration(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        lecturerService.updateRegistrationStatus(id, "APPROVED");
        redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt đăng ký của sinh viên thành công!");
        return "redirect:/lecturer/registrations";
    }

    @PostMapping("/registrations/{id}/reject")
    public String rejectRegistration(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        lecturerService.updateRegistrationStatus(id, "REJECTED");
        redirectAttributes.addFlashAttribute("errorMessage", "Đã từ chối đăng ký đề tài của sinh viên.");
        return "redirect:/lecturer/registrations";
    }

    @GetMapping("/submissions")
    public String listSubmissions(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Submission> submissions = lecturerService.getSubmissionsByLecturer(lecturer);

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("submissions", submissions);

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

        lecturerService.gradeSubmission(id, grade, feedback);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu kết quả đánh giá và gửi nhận xét thành công!");
        return "redirect:/lecturer/submissions";
    }
}
