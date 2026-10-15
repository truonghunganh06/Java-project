package topicmanagement.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import topicmanagement.entity.AppUser;

@Controller
public class DashboardController {

    private final topicmanagement.repository.UserRepository userRepository;

    public DashboardController(topicmanagement.repository.UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(HttpSession session, Model model) {
        AppUser currentUser = (session != null) ? (AppUser) session.getAttribute("currentUser") : null;
        if (currentUser == null || currentUser.getId() == null) {
            return "redirect:/login";
        }

        var dbUserOpt = userRepository.findById(currentUser.getId());
        if (dbUserOpt.isEmpty() || !Boolean.TRUE.equals(dbUserOpt.get().getIsActive())) {
            if (session != null) session.invalidate();
            return "redirect:/login";
        }
        currentUser = dbUserOpt.get();
        if (session != null) session.setAttribute("currentUser", currentUser);

        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode)) {
                return "redirect:/admin/dashboard";
            }
            if ("DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/dashboard";
            }
            if ("LECTURER".equalsIgnoreCase(roleCode)) {
                return "redirect:/lecturer/dashboard";
            }
            if ("STUDENT".equalsIgnoreCase(roleCode)) {
                return "redirect:/student/dashboard";
            }
        }

        return "redirect:/login";
    }

    @GetMapping("/schedule")
    public String redirectSchedule(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode) || "DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/schedule";
            }
            if ("LECTURER".equalsIgnoreCase(roleCode)) {
                return "redirect:/lecturer/schedule";
            }
            if ("STUDENT".equalsIgnoreCase(roleCode)) {
                return "redirect:/student/schedule";
            }
        }

        return "redirect:/login";
    }

    @GetMapping("/councils")
    public String redirectCouncils(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";
        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode) || "DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/councils";
            }
            if ("LECTURER".equalsIgnoreCase(roleCode)) {
                return "redirect:/lecturer/councils";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/topics")
    public String redirectTopics(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";
        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode) || "DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/topics";
            }
            if ("LECTURER".equalsIgnoreCase(roleCode)) {
                return "redirect:/lecturer/topics";
            }
            if ("STUDENT".equalsIgnoreCase(roleCode)) {
                return "redirect:/student/topics";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/results")
    public String redirectResults(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";
        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode) || "DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/results";
            }
            if ("LECTURER".equalsIgnoreCase(roleCode)) {
                return "redirect:/lecturer/dashboard";
            }
            if ("STUDENT".equalsIgnoreCase(roleCode)) {
                return "redirect:/student/results";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/announcements")
    public String redirectAnnouncements(HttpSession session) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) return "redirect:/login";
        if (currentUser.getRole() != null) {
            String roleCode = currentUser.getRole().getCode();
            if ("ADMIN".equalsIgnoreCase(roleCode) || "DEPARTMENT_HEAD".equalsIgnoreCase(roleCode) || "DIVISION_HEAD".equalsIgnoreCase(roleCode)) {
                return "redirect:/department-head/announcements";
            }
        }
        return "redirect:/login";
    }
}
