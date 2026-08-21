package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.DepartmentHeadService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/department-head")
public class DepartmentHeadController {

    private final DepartmentHeadService departmentHeadService;

    public DepartmentHeadController(DepartmentHeadService departmentHeadService) {
        this.departmentHeadService = departmentHeadService;
    }

    private AppUser getAuthenticatedDeptHead(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) {
            return null;
        }
        if (user.getRole() == null || !"DEPARTMENT_HEAD".equalsIgnoreCase(user.getRole().getCode())) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) {
            return "redirect:/login";
        }

        Department dept = departmentHeadService.getDepartmentForUser(user);
        Map<String, Object> stats = departmentHeadService.getDepartmentStats(dept.getId());
        List<Topic> pendingTopics = departmentHeadService.getDepartmentTopics(dept.getId(), "PENDING", null, null);
        List<Major> majors = departmentHeadService.getDepartmentMajors(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("stats", stats);
        model.addAttribute("pendingTopics", pendingTopics.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("majors", majors);

        return "department-head/dashboard";
    }

    @GetMapping("/topics")
    public String listTopics(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) {
            return "redirect:/login";
        }

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Topic> topics = departmentHeadService.getDepartmentTopics(dept.getId(), status, majorId, keyword);
        List<Major> majors = departmentHeadService.getDepartmentMajors(dept.getId());
        Map<String, Object> stats = departmentHeadService.getDepartmentStats(dept.getId());

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("topics", topics);
        model.addAttribute("majors", majors);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedMajorId", majorId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pendingCount", stats.get("pendingTopics"));

        return "department-head/topics";
    }

    @PostMapping("/topics/{id}/approve")
    public String approveTopic(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) return "redirect:/login";

        Topic topic = departmentHeadService.getTopicById(id);
        boolean success = departmentHeadService.approveTopic(id);

        if (success) {
            String topicName = topic != null ? topic.getTitle() : "Đề tài";
            redirectAttributes.addFlashAttribute("successMessage", "Phê duyệt thành công đề tài: '" + topicName + "'!");
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

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập lý do từ chối đề tài!");
            return "redirect:/department-head/topics";
        }

        Topic topic = departmentHeadService.getTopicById(id);
        boolean success = departmentHeadService.rejectTopic(id, rejectionReason.trim());

        if (success) {
            String topicName = topic != null ? topic.getTitle() : "Đề tài";
            redirectAttributes.addFlashAttribute("errorMessage", "Đã từ chối đề tài '" + topicName + "'. Giảng viên sẽ nhận được phản hồi.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy đề tài để từ chối!");
        }

        return "redirect:/department-head/topics";
    }

    @GetMapping("/lecturers")
    public String listLecturers(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) {
            return "redirect:/login";
        }

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Lecturer> lecturers = departmentHeadService.getLecturersInDepartment(dept.getId());
        List<Topic> allTopics = departmentHeadService.getDepartmentTopics(dept.getId(), "ALL", null, null);

        // Count topics per lecturer
        Map<Long, Integer> topicCounts = new HashMap<>();
        Map<Long, Integer> approvedTopicCounts = new HashMap<>();
        for (Lecturer l : lecturers) {
            int total = (int) allTopics.stream().filter(t -> t.getSupervisor() != null && l.getId().equals(t.getSupervisor().getId())).count();
            int approved = (int) allTopics.stream().filter(t -> t.getSupervisor() != null && l.getId().equals(t.getSupervisor().getId()) && "APPROVED".equalsIgnoreCase(t.getStatus())).count();
            topicCounts.put(l.getId(), total);
            approvedTopicCounts.put(l.getId(), approved);
        }

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("lecturers", lecturers);
        model.addAttribute("topicCounts", topicCounts);
        model.addAttribute("approvedTopicCounts", approvedTopicCounts);

        return "department-head/lecturers";
    }

    @GetMapping("/students")
    public String listStudents(HttpSession session, Model model) {
        AppUser user = getAuthenticatedDeptHead(session);
        if (user == null) {
            return "redirect:/login";
        }

        Department dept = departmentHeadService.getDepartmentForUser(user);
        List<Student> students = departmentHeadService.getStudentsInDepartment(dept.getId());
        Map<Long, TopicRegistration> registrationsMap = departmentHeadService.getStudentRegistrationsMap();

        model.addAttribute("user", user);
        model.addAttribute("department", dept);
        model.addAttribute("students", students);
        model.addAttribute("registrationsMap", registrationsMap);

        return "department-head/students";
    }
}
