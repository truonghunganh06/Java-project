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
    private final FileStorageService fileStorageService;
    private final topic_management.repository.SubmissionRepository submissionRepository;
    private final GroupRegistrationService groupRegistrationService;

    public StudentController(
            StudentService studentService,
            AnnouncementService announcementService,
            FileStorageService fileStorageService,
            topic_management.repository.SubmissionRepository submissionRepository,
            GroupRegistrationService groupRegistrationService) {
        this.studentService = studentService;
        this.announcementService = announcementService;
        this.fileStorageService = fileStorageService;
        this.submissionRepository = submissionRepository;
        this.groupRegistrationService = groupRegistrationService;
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
        List<Submission> submissions = studentService.getGroupSubmissions(group);
        List<topic_management.dto.RegistrationDtos.InvitationResponseDto> myInvitations = groupRegistrationService.getMyPendingInvitations(user);

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("submissions", submissions);
        model.addAttribute("announcements", announcements.stream().limit(5).toList());
        model.addAttribute("myInvitations", myInvitations);

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
        List<topic_management.dto.RegistrationDtos.InvitationResponseDto> myInvitations = groupRegistrationService.getMyPendingInvitations(user);
        List<topic_management.dto.RegistrationDtos.InvitationResponseDto> sentInvitations = group != null ? groupRegistrationService.getGroupInvitations(user, group.getId()) : List.of();

        model.addAttribute("user", user);
        model.addAttribute("student", student);
        model.addAttribute("group", group);
        model.addAttribute("activePeriods", activePeriods);
        model.addAttribute("selectedPeriodId", periodId);
        model.addAttribute("myInvitations", myInvitations);
        model.addAttribute("sentInvitations", sentInvitations);

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

        try {
            groupRegistrationService.sendInvitation(user, group.getId(), studentCode);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi lời mời tham gia nhóm tới sinh viên " + studentCode + "! Vui lòng chờ sinh viên chấp nhận.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/invitations/{id}/accept")
    public String acceptInvitation(
            @PathVariable("id") Long invitationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.acceptInvitation(user, invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã chấp nhận lời mời và gia nhập nhóm thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/invitations/{id}/reject")
    public String rejectInvitation(
            @PathVariable("id") Long invitationId,
            @RequestParam(value = "reason", required = false) String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.rejectInvitation(user, invitationId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối lời mời vào nhóm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/invitations/{id}/cancel")
    public String cancelInvitation(
            @PathVariable("id") Long invitationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.cancelInvitation(user, invitationId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thu hồi lời mời thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/draft-leave")
    public String leaveDraftGroup(
            @RequestParam("groupId") Long groupId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.leaveGroupDraft(user, groupId, null);
            redirectAttributes.addFlashAttribute("successMessage", "Bạn đã rời nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/draft-dissolve")
    public String dissolveDraftGroup(
            @RequestParam("groupId") Long groupId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.dissolveGroupDraft(user, groupId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã giải tán nhóm thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/transfer-leadership")
    public String transferLeadership(
            @RequestParam("groupId") Long groupId,
            @RequestParam("newLeaderStudentId") Long newLeaderStudentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.transferLeadership(user, groupId, newLeaderStudentId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã chuyển giao quyền Nhóm trưởng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/leave-request")
    public String submitLeaveRequest(
            @RequestParam("groupId") Long groupId,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.requestLeaveApprovedGroup(user, groupId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu rút khỏi nhóm lên GVHD! Vui lòng chờ GVHD phê duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/student/group";
    }

    @PostMapping("/group/dissolve-request")
    public String submitDissolveRequest(
            @RequestParam("groupId") Long groupId,
            @RequestParam("reason") String reason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.requestDissolveApprovedGroup(user, groupId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hủy nhóm và đề tài lên GVHD! Vui lòng chờ GVHD phê duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
        // Xác định Khoa & Bộ môn của sinh viên để lọc đề tài theo đúng phân quyền đào tạo
        Long departmentId = student != null ? student.getDepartmentId() : null;
        Department studentDepartment = student != null ? student.getDepartment() : null;
        Division studentDivision = student != null ? student.getDivision() : null;

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
        model.addAttribute("studentDivision", studentDivision);
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

        if (group == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn chưa thuộc nhóm nào để nộp báo cáo!");
            return "redirect:/student/submissions";
        }
        if (!group.isLeader(student)) {
            redirectAttributes.addFlashAttribute("errorMessage", "QUYỀN HẠN BỊ TỪ CHỐI: Chỉ Nhóm trưởng mới được nộp báo cáo!");
            return "redirect:/student/submissions";
        }

        String err;
        if (file != null && !file.isEmpty()) {
            err = studentService.submitGroupReport(student, group, title, round, file);
        } else {
            err = studentService.submitGroupReport(student, group, title, round, "BaoCao_TienDo_Dot" + round + ".pdf", 2048576L);
        }

        if (err != null) {
            redirectAttributes.addFlashAttribute("errorMessage", err);
            return "redirect:/student/submissions";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Nhóm trưởng đã nộp file báo cáo tiến độ thành công!");
        return "redirect:/student/submissions";
    }

    @GetMapping("/submissions/{id}/download")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadSubmission(
            @PathVariable("id") Long submissionId,
            HttpSession session) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getFilePath() == null) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }

        try {
            org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(submission.getFilePath());
            String contentType = submission.getContentType() != null ? submission.getContentType() : "application/octet-stream";
            String downloadName = submission.getOriginalFileName() != null ? submission.getOriginalFileName() : "submission_" + submissionId + ".pdf";

            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
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
