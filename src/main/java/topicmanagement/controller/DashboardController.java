package topicmanagement.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import topicmanagement.entity.AppUser;

@Controller
public class DashboardController {

    @GetMapping({"/", "/dashboard"})
    public String showDashboard(HttpSession session, Model model) {
        AppUser currentUser = (AppUser) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

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
}
