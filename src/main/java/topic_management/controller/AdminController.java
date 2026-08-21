package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.*;
import topic_management.service.AdminService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private AppUser getAuthenticatedAdmin(HttpSession session) {
        AppUser user = (AppUser) session.getAttribute("currentUser");
        if (user == null) {
            return null;
        }
        if (user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getCode())) {
            return null;
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        AppUser user = getAuthenticatedAdmin(session);
        if (user == null) {
            return "redirect:/login";
        }

        Map<String, Object> stats = adminService.getSystemStats();
        List<AppUser> users = adminService.getAllUsers();
        List<Department> departments = adminService.getAllDepartments();
        List<Major> majors = adminService.getAllMajors();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("recentUsers", users.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("departments", departments);
        model.addAttribute("majors", majors);

        return "admin/dashboard";
    }

    // ==================== USER MANAGEMENT ====================

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(value = "role", required = false) String roleCode,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpSession session,
            Model model) {

        AppUser user = getAuthenticatedAdmin(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<AppUser> allUsers = adminService.getAllUsers();
        List<AppUser> filteredUsers = allUsers.stream()
                .filter(u -> {
                    if (roleCode != null && !roleCode.isEmpty() && !"ALL".equalsIgnoreCase(roleCode)) {
                        if (u.getRole() == null || !roleCode.equalsIgnoreCase(u.getRole().getCode())) {
                            return false;
                        }
                    }
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        String kw = keyword.trim().toLowerCase();
                        boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(kw);
                        boolean matchUser = u.getUsername() != null && u.getUsername().toLowerCase().contains(kw);
                        boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(kw);
                        return matchName || matchUser || matchEmail;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<Role> roles = adminService.getAllRoles();

        model.addAttribute("user", user);
        model.addAttribute("users", filteredUsers);
        model.addAttribute("roles", roles);
        model.addAttribute("selectedRole", roleCode);
        model.addAttribute("keyword", keyword);
        model.addAttribute("totalCount", allUsers.size());
        model.addAttribute("newUser", new AppUser());

        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(
            @ModelAttribute AppUser newUser,
            @RequestParam("roleId") Long roleId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        if (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty()) {
            newUser.setPasswordHash("123456"); // Default password
        }

        boolean success = adminService.createUser(newUser, roleId);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới tài khoản '" + newUser.getUsername() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo tài khoản, vui lòng thử lại!");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/edit")
    public String editUser(
            @PathVariable("id") Long id,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam("roleId") Long roleId,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.updateUser(id, fullName, email, phone, roleId, isActive);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Cập nhật tài khoản thất bại!");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        if (admin.getId() != null && admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tự khóa tài khoản quản trị đang đăng nhập!");
            return "redirect:/admin/users";
        }

        boolean success = adminService.toggleUserActive(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái kích hoạt tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(
            @PathVariable("id") Long id,
            @RequestParam(value = "newPassword", defaultValue = "123456") String newPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.resetUserPassword(id, newPassword);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Mật khẩu mới là: " + newPassword);
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt lại mật khẩu!");
        }

        return "redirect:/admin/users";
    }

    // ==================== DEPARTMENTS & MAJORS ====================

    @GetMapping("/departments")
    public String listDepartments(HttpSession session, Model model) {
        AppUser user = getAuthenticatedAdmin(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<Department> departments = adminService.getAllDepartments();
        List<Major> majors = adminService.getAllMajors();

        model.addAttribute("user", user);
        model.addAttribute("departments", departments);
        model.addAttribute("majors", majors);
        model.addAttribute("newDepartment", new Department());
        model.addAttribute("newMajor", new Major());

        return "admin/departments";
    }

    @PostMapping("/departments/create")
    public String createDepartment(
            @ModelAttribute Department department,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.createDepartment(department);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Khoa/Bộ môn '" + department.getName() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo Khoa/Bộ môn!");
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/edit")
    public String editDepartment(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.updateDepartment(id, name, code, description, isActive);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Khoa thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Khoa!");
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/toggle-status")
    public String toggleDepartmentStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.toggleDepartmentActive(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái Khoa thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Khoa!");
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/majors/create")
    public String createMajor(
            @ModelAttribute Major major,
            @RequestParam("departmentId") Long departmentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.createMajor(major, departmentId);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Chuyên ngành '" + major.getName() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo Chuyên ngành!");
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/majors/{id}/edit")
    public String editMajor(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam(value = "isActive", defaultValue = "true") Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.updateMajor(id, name, code, departmentId, isActive);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Chuyên ngành thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Chuyên ngành!");
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/majors/{id}/toggle-status")
    public String toggleMajorStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.toggleMajorActive(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái Chuyên ngành thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Chuyên ngành!");
        }

        return "redirect:/admin/departments";
    }
}
