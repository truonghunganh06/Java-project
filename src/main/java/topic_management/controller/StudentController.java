package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.StudentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
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

        Student student = studentService.getStudentByUser(user);
        TopicRegistration myReg = studentService.getMyRegistration(student);
        List<Submission> submissions = studentService.getMySubmissions(student);
        List<Topic> availableTopics = studentService.getAvailableTopics(null, null);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("myReg", myReg);
        model.addAttribute("submissions", submissions);
        model.addAttribute("availableTopicsCount", availableTopics.size());
        model.addAttribute("submissionsCount", submissions.size());

        return "student/dashboard";
    }

    @GetMapping("/topics")
    public String listTopics(
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUser(user);
        TopicRegistration myReg = studentService.getMyRegistration(student);
        List<Topic> topics = studentService.getAvailableTopics(majorId, keyword);
        List<Major> majors = studentService.getAllMajors();

        // Build map of current registered count per topic
        Map<Long, Integer> registeredCounts = new HashMap<>();
        for (Topic t : topics) {
            registeredCounts.put(t.getId(), studentService.getRegisteredCountForTopic(t.getId()));
        }

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);
        model.addAttribute("selectedMajorId", majorId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("myReg", myReg);
        model.addAttribute("registeredCounts", registeredCounts);

        return "student/topics";
    }

    @PostMapping("/topics/{id}/register")
    public String handleRegisterTopic(
            @PathVariable("id") Long topicId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUser(user);
        String error = studentService.registerTopic(student, topicId);

        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
            return "redirect:/student/topics";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Gửi yêu cầu đăng ký đề tài thành công! Vui lòng chờ Giảng viên hướng dẫn duyệt.");
        return "redirect:/student/my-topic";
    }

    @GetMapping("/my-topic")
    public String showMyTopic(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUser(user);
        TopicRegistration myReg = studentService.getMyRegistration(student);
        List<Submission> submissions = studentService.getMySubmissions(student);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("myReg", myReg);
        model.addAttribute("submissions", submissions);

        return "student/my-topic";
    }

    @GetMapping("/submissions")
    public String listSubmissions(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUser(user);
        TopicRegistration myReg = studentService.getMyRegistration(student);
        List<Submission> submissions = studentService.getMySubmissions(student);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("myReg", myReg);
        model.addAttribute("submissions", submissions);

        return "student/submissions";
    }

    @PostMapping("/submissions/upload")
    public String handleUploadReport(
            @RequestParam("title") String title,
            @RequestParam("round") Integer round,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Student student = studentService.getStudentByUser(user);
        TopicRegistration myReg = studentService.getMyRegistration(student);

        if (myReg == null || !"APPROVED".equalsIgnoreCase(myReg.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần được Giảng viên phê duyệt đề tài trước khi nộp báo cáo tiến độ.");
            return "redirect:/student/submissions";
        }

        String fileName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : "BaoCao_TienDo_Dot" + round + ".pdf";
        Long fileSize = (file != null && !file.isEmpty()) ? file.getSize() : 2048576L;

        studentService.submitReport(student, myReg.getTopic().getId(), title, round, fileName, fileSize);

        redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo tiến độ thành công! Giảng viên sẽ sớm nhận xét và chấm điểm.");
        return "redirect:/student/submissions";
    }
}
