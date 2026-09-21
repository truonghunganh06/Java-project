package topicmanagement.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topicmanagement.entity.*;
import topicmanagement.service.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/lecturer")
public class LecturerController {

    private final LecturerService lecturerService;
    private final CouncilService councilService;
    private final AnnouncementService announcementService;
    private final FileStorageService fileStorageService;
    private final topicmanagement.repository.SubmissionRepository submissionRepository;
    private final GroupRegistrationService groupRegistrationService;

    public LecturerController(
            LecturerService lecturerService,
            CouncilService councilService,
            AnnouncementService announcementService,
            FileStorageService fileStorageService,
            topicmanagement.repository.SubmissionRepository submissionRepository,
            GroupRegistrationService groupRegistrationService) {
        this.lecturerService = lecturerService;
        this.councilService = councilService;
        this.announcementService = announcementService;
        this.fileStorageService = fileStorageService;
        this.submissionRepository = submissionRepository;
        this.groupRegistrationService = groupRegistrationService;
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
                && !"DIVISION_HEAD".equalsIgnoreCase(roleCode)
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
        List<topicmanagement.dto.RegistrationDtos.LeaveRequestDto> pendingLeaveRequests = lecturer != null ? groupRegistrationService.getPendingLeaveRequestsForLecturer(user) : List.of();
        List<topicmanagement.dto.RegistrationDtos.DissolveRequestDto> pendingDissolveRequests = lecturer != null ? groupRegistrationService.getPendingDissolveRequestsForLecturer(user) : List.of();

        long approvedCount = topics.stream().filter(t -> "APPROVED".equalsIgnoreCase(t.getStatus())).count();
        long pendingCount = topics.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
        List<Announcement> announcements = announcementService.getAllAnnouncements();

        List<StudentGroup> pendingGroups = groups.stream()
                .filter(g -> "DRAFT".equalsIgnoreCase(g.getStatus()) || "REGISTERED".equalsIgnoreCase(g.getStatus()) || "PENDING".equalsIgnoreCase(g.getStatus()))
                .toList();
        List<Submission> ungradedSubmissions = submissions.stream()
                .filter(s -> s.getGrade() == null)
                .toList();
        List<Topic> rejectedTopics = topics.stream()
                .filter(t -> "REJECTED".equalsIgnoreCase(t.getStatus()))
                .toList();

        List<RegistrationPeriod> openProposalPeriods = lecturerService.getOpenProposalPeriods(lecturer);
        List<LecturerService.ProposalProgressDto> proposalProgressList = lecturerService.getProposalProgressList(lecturer);

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("openProposalPeriods", openProposalPeriods);
        model.addAttribute("proposalProgressList", proposalProgressList);
        model.addAttribute("topics", topics);
        model.addAttribute("reviewerTopics", reviewerTopics);
        model.addAttribute("councils", councils);
        model.addAttribute("groups", groups);
        model.addAttribute("studentGroups", groups);
        model.addAttribute("pendingGroups", pendingGroups);
        model.addAttribute("ungradedSubmissions", ungradedSubmissions);
        model.addAttribute("rejectedTopics", rejectedTopics);
        model.addAttribute("submissions", submissions);
        model.addAttribute("totalSubmissions", submissions.size());
        model.addAttribute("announcements", announcements.stream().limit(5).toList());
        model.addAttribute("totalTopics", topics.size());
        model.addAttribute("approvedTopics", approvedCount);
        model.addAttribute("pendingTopics", pendingCount);
        model.addAttribute("reviewerCount", reviewerTopics.size());
        model.addAttribute("councilCount", councils.size());
        model.addAttribute("pendingLeaveRequests", pendingLeaveRequests);
        model.addAttribute("pendingDissolveRequests", pendingDissolveRequests);

        return "lecturer/dashboard";
    }

    // ==================== FULL SCHEDULE / CALENDAR ====================

    @GetMapping("/schedule")
    public String showSchedule(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<Topic> reviewerTopics = lecturerService.getReviewerTopics(lecturer);
        List<Council> councils = lecturerService.getCouncilsForLecturer(lecturer);
        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();
        List<Submission> submissions = lecturerService.getSubmissionsByLecturer(lecturer);
        List<Announcement> announcements = announcementService.getAllAnnouncements();

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topics", topics);
        model.addAttribute("reviewerTopics", reviewerTopics);
        model.addAttribute("councils", councils);
        model.addAttribute("periods", periods);
        model.addAttribute("submissions", submissions);
        model.addAttribute("announcements", announcements.stream().limit(5).toList());

        return "lecturer/schedule";
    }

    // ==================== TOPICS ====================

    @GetMapping("/topics")
    public String listTopics(HttpSession session, Model model) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        List<Topic> topics = lecturerService.getTopicsByLecturer(lecturer);
        List<Major> majors = lecturerService.getMajorsForLecturer(lecturer);
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
        List<RegistrationPeriod> openPeriods = lecturerService.getOpenProposalPeriods(lecturer);
        List<LecturerService.ProposalProgressDto> proposalProgressList = lecturerService.getProposalProgressList(lecturer);
        List<Lecturer> coSupervisors = lecturerService.getOtherLecturers(lecturer.getId());
        List<Major> majors = lecturerService.getMajorsForLecturer(lecturer);

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topic", new Topic());
        model.addAttribute("openPeriods", openPeriods);
        model.addAttribute("proposalProgressList", proposalProgressList);
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
            Topic created = lecturerService.createTopic(lecturer, periodId, majorId, coSupervisorId, topic);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đề xuất đề tài '" + created.getTitle() + "' thành công! Vui lòng chờ Trưởng khoa/Bộ môn xét duyệt.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi đề xuất đề tài: " + e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    @GetMapping("/topics/{id}/edit")
    public String showEditTopicForm(@PathVariable("id") Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        Topic topic = lecturerService.getTopicById(id);
        if (topic == null || !topic.isSupervisorOrCoSupervisor(lecturer)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đề tài hoặc bạn không có quyền chỉnh sửa!");
            return "redirect:/lecturer/topics";
        }

        List<RegistrationPeriod> periods = lecturerService.getAllPeriods();
        List<Lecturer> coSupervisors = lecturerService.getOtherLecturers(lecturer.getId());
        List<Major> majors = lecturerService.getMajorsForLecturer(lecturer);

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("topic", topic);
        model.addAttribute("periods", periods);
        model.addAttribute("coSupervisors", coSupervisors);
        model.addAttribute("majors", majors);

        return "lecturer/edit-topic";
    }

    @PostMapping("/topics/{id}/edit")
    public String handleEditTopic(
            @PathVariable("id") Long id,
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
            Topic updated = lecturerService.updateTopic(lecturer, id, periodId, majorId, coSupervisorId, topic);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đề tài '" + updated.getTitle() + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật đề tài: " + e.getMessage());
        }
        return "redirect:/lecturer/topics";
    }

    @PostMapping("/topics/{id}/delete")
    public String handleDeleteTopic(@PathVariable("id") Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        try {
            boolean success = lecturerService.deleteTopic(id, lecturer);
            if (success) {
                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đề tài thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa đề tài này!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa: " + e.getMessage());
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

    @PostMapping("/topics/{topicId}/grade-advisor")
    public String gradeAdvisor(
            @PathVariable("topicId") Long topicId,
            @RequestParam("score") Double score,
            @RequestParam(value = "comments", required = false) String comments,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        Lecturer lecturer = lecturerService.getLecturerByUser(user);
        boolean ok = councilService.submitEvaluation(topicId, lecturer.getId(), "SUPERVISOR", score, comments);
        if (ok) {
            redirectAttributes.addFlashAttribute("successMessage", "Đã nộp điểm và nhận xét hướng dẫn (quá trình) thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể nộp điểm hướng dẫn (bạn không phải GVHD hoặc đề tài chưa có nhóm sinh viên)!");
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

        Map<Long, Evaluation> existingEvaluationsMap = new HashMap<>();
        for (Topic t : reviewerTopics) {
            councilService.getEvaluation(t.getId(), lecturer.getId(), "REVIEWER")
                    .ifPresent(e -> existingEvaluationsMap.put(t.getId(), e));
        }

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("reviewerTopics", reviewerTopics);
        model.addAttribute("existingEvaluationsMap", existingEvaluationsMap);
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
        Map<Long, Evaluation> existingEvaluationsMap = new HashMap<>();
        for (Council c : councils) {
            List<Topic> topics = councilService.getTopicsInCouncil(c.getId());
            councilTopicsMap.put(c.getId(), topics);
            for (Topic t : topics) {
                councilService.getEvaluation(t.getId(), lecturer.getId(), "COUNCIL")
                        .ifPresent(e -> existingEvaluationsMap.put(t.getId(), e));
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("lecturer", lecturer);
        model.addAttribute("councils", councils);
        model.addAttribute("councilTopicsMap", councilTopicsMap);
        model.addAttribute("existingEvaluationsMap", existingEvaluationsMap);
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

    // ==================== V2 GROUP REQUEST APPROVALS ====================

    @PostMapping("/leave-requests/{id}/approve")
    public String approveLeaveRequest(
            @PathVariable("id") Long requestId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            topicmanagement.dto.RegistrationDtos.ApiResponse<?> res = groupRegistrationService.approveLeaveRequest(user, requestId);
            redirectAttributes.addFlashAttribute("successMessage", res.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/lecturer/dashboard";
    }

    @PostMapping("/leave-requests/{id}/reject")
    public String rejectLeaveRequest(
            @PathVariable("id") Long requestId,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.rejectLeaveRequest(user, requestId, rejectionReason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu rút nhóm.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/lecturer/dashboard";
    }

    @PostMapping("/dissolve-requests/{id}/approve")
    public String approveDissolveRequest(
            @PathVariable("id") Long requestId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            topicmanagement.dto.RegistrationDtos.ApiResponse<?> res = groupRegistrationService.approveDissolveRequest(user, requestId);
            redirectAttributes.addFlashAttribute("successMessage", res.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/lecturer/dashboard";
    }

    @PostMapping("/dissolve-requests/{id}/reject")
    public String rejectDissolveRequest(
            @PathVariable("id") Long requestId,
            @RequestParam(value = "rejectionReason", required = false) String rejectionReason,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedUser(session);
        if (user == null) return "redirect:/login";

        try {
            groupRegistrationService.rejectDissolveRequest(user, requestId, rejectionReason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu hủy nhóm và đề tài.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/lecturer/dashboard";
    }
}
