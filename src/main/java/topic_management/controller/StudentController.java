package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.*;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final AnnouncementService announcementService;

    public StudentController(
            StudentService studentService,
            AnnouncementService announcementService) {
        this.studentService = studentService;
        this.announcementService = announcementService;
    }

    /**
     * BUG-05 FIX: Kiểm tra role STUDENT trước khi cho phép truy cập
     */
    private AppUser getAuthenticatedUser(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) return null;
        if (user.getRole() == null || !"STUDENT".equalsIgnoreCase(user.getRole().getCode())) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);
        List<Announcement> announcements = announcementService.getAllAnnouncements();
        List<RegistrationPeriod> activePeriods = studentService.getAllActivePeriods();
        List<Submission> submissions = studentService.getGroupSubmissions(group);
        List<Evaluation> evaluations = studentService.getGroupEvaluations(group);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("submissions", submissions);
        model.addAttribute("evaluations", evaluations);
        model.addAttribute("announcements", announcements.stream().limit(3).toList());
        model.addAttribute("activePeriods", activePeriods);

        return "student/dashboard";
    }

    // ==================== GROUP MANAGEMENT ====================

    @GetMapping("/group")
    public String showGroupPage(
            @RequestParam(value = "periodId", required = false) Long periodId,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, periodId);
        List<RegistrationPeriod> activePeriods = studentService.getAllActivePeriods();

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("activePeriods", activePeriods);
        model.addAttribute("selectedPeriodId", periodId);

        return "student/group";
    }

    @PostMapping("/group/create")
    public String createGroup(
            @RequestParam("groupName") String groupName,
            @RequestParam(value = "periodId", required = false) Long periodId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        try {
            StudentGroup g = studentService.createGroup(student, periodId, groupName);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo nhóm '" + g.getGroupName() + "' thành công! Mã nhóm: " + g.getGroupCode());
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            redirectAttributes.addFlashAttribute("errorMessage", rse.getReason() != null ? rse.getReason() : rse.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/invite")
    public String inviteMember(
            @RequestParam("studentCode") String studentCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);

        if (group == null || !group.isLeader(student)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ Nhóm trưởng mới có quyền mời thành viên!");
            return "redirect:/student/group";
        }

        String err = studentService.inviteMemberByCode(group, studentCode);
        if (err != null) {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sinh viên " + studentCode + " vào nhóm thành công!");
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/join")
    public String joinGroup(
            @RequestParam("groupCode") String groupCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        String err = studentService.joinGroupByCode(student, groupCode);

        if (err != null) {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Gia nhập nhóm thành công!");
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/remove-member")
    public String removeMember(
            @RequestParam("studentId") Long studentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);

        if (group == null || !group.isLeader(student)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chỉ Nhóm trưởng mới có quyền xóa thành viên!");
            return "redirect:/student/group";
        }

        studentService.removeMember(group, studentId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa thành viên khỏi nhóm.");
        return "redirect:/student/group";
    }

    // ==================== TOPICS & REGISTRATION ====================

    @GetMapping("/topics")
    public String listTopics(
            @RequestParam(value = "periodId", required = false) Long periodId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        // Xác định Khoa của sinh viên để lọc đề tài theo đúng phân quyền đào tạo
        Long departmentId = student != null ? student.getDepartmentId() : null;
        Department studentDepartment = student != null ? student.getDepartment() : null;

        StudentGroup group = studentService.getMyActiveGroup(student, periodId);
        // Lọc đề tài theo Khoa của sinh viên (departmentId) + periodId + majorId + keyword
        List<Topic> topics = studentService.getAvailableTopics(periodId, majorId, keyword, departmentId);
        // Chỉ lấy chuyên ngành thuộc Khoa của sinh viên cho dropdown filter
        List<Major> majors = studentService.getMajorsByDepartment(departmentId);
        List<RegistrationPeriod> periods = studentService.getAllActivePeriods();

        // Lấy danh sách ID đề tài đã có nhóm đăng ký (để làm mờ/disable nút trên UI)
        java.util.Set<Long> takenTopicIds = studentService.getTakenTopicIds(topics);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("studentDepartment", studentDepartment);
        model.addAttribute("group", group);
        model.addAttribute("topics", topics);
        model.addAttribute("takenTopicIds", takenTopicIds);
        model.addAttribute("majors", majors);
        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("selectedMajorId", majorId);
        model.addAttribute("keyword", keyword);

        return "student/topics";
    }

    @PostMapping("/topics/{id}/register")
    public String registerTopic(
            @PathVariable("id") Long topicId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);

        String err = studentService.registerTopicForGroup(student, group, topicId);
        if (err != null) {
            redirectAttributes.addFlashAttribute("errorMessage", err);
            return "redirect:/student/topics";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký đề tài cho nhóm thành công! Vui lòng chờ Giảng viên hướng dẫn duyệt.");
        return "redirect:/student/my-topic";
    }

    // ==================== MY TOPIC ====================

    @GetMapping("/my-topic")
    public String showMyTopic(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);
        List<Submission> submissions = studentService.getGroupSubmissions(group);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("submissions", submissions);

        return "student/my-topic";
    }

    // ==================== SUBMISSIONS (LEADER ONLY) ====================

    @GetMapping("/submissions")
    public String listSubmissions(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);
        List<Submission> submissions = studentService.getGroupSubmissions(group);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
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
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);

        // BUG-11 FIX: Defense-in-depth - Kiểm tra quyền Trưởng nhóm ngay tại Controller layer
        if (group == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa thuộc nhóm nào để nộp báo cáo!");
            return "redirect:/student/submissions";
        }
        if (!group.isLeader(student)) {
            redirectAttributes.addFlashAttribute("errorMessage", "QUYỀN HẠN BỊ TỪ CHỐI: Chỉ Nhóm trưởng mới được nộp báo cáo!");
            return "redirect:/student/submissions";
        }

        String fileName = (file != null && !file.isEmpty()) ? file.getOriginalFilename() : "BaoCao_TienDo_Dot" + round + ".pdf";
        Long fileSize = (file != null && !file.isEmpty()) ? file.getSize() : 2048576L;

        String err = studentService.submitGroupReport(student, group, title, round, fileName, fileSize);
        if (err != null) {
            redirectAttributes.addFlashAttribute("errorMessage", err);
            return "redirect:/student/submissions";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Nhóm trưởng đã nộp báo cáo tiến độ thành công!");
        return "redirect:/student/submissions";
    }

    // ==================== RESULTS ====================

    @GetMapping("/results")
    public String showResults(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Student student = studentService.getStudentByUser(user);
        StudentGroup group = studentService.getMyActiveGroup(student, null);
        List<Evaluation> evaluations = studentService.getGroupEvaluations(group);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("evaluations", evaluations);

        return "student/results";
    }
}
