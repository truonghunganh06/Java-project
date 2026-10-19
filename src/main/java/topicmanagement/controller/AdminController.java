package topicmanagement.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topicmanagement.entity.*;
import topicmanagement.service.AdminService;

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
        AppUser user = (session != null) ? (AppUser) session.getAttribute("currentUser") : null;
        if (user == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                user = adminService.getUserByUsername(auth.getName());
                if (user != null && session != null) {
                    session.setAttribute("currentUser", user);
                }
            }
        }
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
        List<Role> roles = adminService.getAllRoles();

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("recentUsers", users.stream().limit(5).collect(Collectors.toList()));
        model.addAttribute("departments", departments);
        model.addAttribute("majors", majors);
        model.addAttribute("roles", roles);

        return "admin/dashboard";
    }

    @GetMapping("/schedule")
    public String showSchedule() {
        return "redirect:/department-head/schedule";
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
        model.addAttribute("studentMap", adminService.getStudentMap());
        model.addAttribute("lecturerMap", adminService.getLecturerMap());
        model.addAttribute("departments", adminService.getAllDepartments());
        model.addAttribute("divisions", adminService.getAllDivisions());
        model.addAttribute("majors", adminService.getAllMajors());

        return "admin/users";
    }

    @PostMapping("/users/create")
    public String createUser(
            @ModelAttribute AppUser newUser,
            @RequestParam(value = "roleId", required = false) Long roleId,
            @RequestParam(value = "roleCode", required = false) String roleCode,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        if (password != null && !password.trim().isEmpty() && (newUser.getPasswordHash() == null || newUser.getPasswordHash().trim().isEmpty())) {
            newUser.setPasswordHash(password.trim());
        }

        if (roleId == null && roleCode != null && !roleCode.trim().isEmpty()) {
            List<Role> allRoles = adminService.getAllRoles();
            for (Role r : allRoles) {
                if (roleCode.equalsIgnoreCase(r.getCode())) {
                    roleId = r.getId();
                    break;
                }
            }
        }

        String err = adminService.createUser(newUser, roleId);
        if (err == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới tài khoản '" + newUser.getUsername() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        }

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
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
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "studentCode", required = false) String studentCode,
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "gpa", required = false) Double gpa,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "lecturerCode", required = false) String lecturerCode,
            @RequestParam(value = "academicTitle", required = false) String academicTitle,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "maxTopicsQuota", required = false) Integer maxTopicsQuota,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "divisionId", required = false) Long divisionId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean activeStatus = Boolean.TRUE.equals(isActive);
        String err = adminService.updateUser(id, fullName, email, phone, roleId, activeStatus);
        if (err == null) {
            Role r = adminService.getRoleById(roleId);
            if (r != null && "STUDENT".equalsIgnoreCase(r.getCode())) {
                adminService.updateStudentProfile(id, studentCode, className, academicYear, gpa, majorId);
            } else if (r != null && ("LECTURER".equalsIgnoreCase(r.getCode()) || "DEPARTMENT_HEAD".equalsIgnoreCase(r.getCode()) || "DIVISION_HEAD".equalsIgnoreCase(r.getCode()))) {
                adminService.updateLecturerProfile(id, lecturerCode, academicTitle, degree, maxTopicsQuota, departmentId, divisionId);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin tài khoản và hồ sơ thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        }

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/student-profile")
    public String updateStudentProfile(
            @PathVariable("id") Long id,
            @RequestParam("studentCode") String studentCode,
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "gpa", required = false) Double gpa,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        String err = adminService.updateStudentProfile(id, studentCode, className, academicYear, gpa, majorId);
        if (err == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ học vụ sinh viên thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        }

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/lecturer-profile")
    public String updateLecturerProfile(
            @PathVariable("id") Long id,
            @RequestParam("lecturerCode") String lecturerCode,
            @RequestParam(value = "academicTitle", required = false) String academicTitle,
            @RequestParam(value = "degree", required = false) String degree,
            @RequestParam(value = "maxTopicsQuota", required = false) Integer maxTopicsQuota,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "divisionId", required = false) Long divisionId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        String err = adminService.updateLecturerProfile(id, lecturerCode, academicTitle, degree, maxTopicsQuota, departmentId, divisionId);
        if (err == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ chuyên môn giảng viên thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", err);
        }

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(
            @PathVariable("id") Long id,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        if (admin.getId() != null && admin.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tự khóa tài khoản quản trị đang đăng nhập!");
            if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/admin/users";
        }

        boolean success = adminService.toggleUserActive(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái kích hoạt tài khoản thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng!");
        }

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(
            @PathVariable("id") Long id,
            @RequestParam(value = "newPassword", defaultValue = "123456") String newPassword,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
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

        if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/admin")) {
            return "redirect:" + redirectUrl;
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
        List<Division> divisions = adminService.getAllDivisions();

        model.addAttribute("user", user);
        model.addAttribute("departments", departments);
        model.addAttribute("majors", majors);
        model.addAttribute("divisions", divisions);
        model.addAttribute("newDepartment", new Department());
        model.addAttribute("newMajor", new Major());
        model.addAttribute("newDivision", new Division());

        return "admin/departments";
    }

    @PostMapping("/departments/create")
    public String createDepartment(
            @ModelAttribute Department department,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        String error = adminService.createDepartment(department);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Khoa/Bộ môn '" + department.getName() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/departments/{id}/edit")
    public String editDepartment(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean activeStatus = Boolean.TRUE.equals(isActive);
        String error = adminService.updateDepartment(id, name, code, description, activeStatus);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Khoa thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
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

        String error = adminService.createMajor(major, departmentId);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Chuyên ngành '" + major.getName() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/majors/{id}/edit")
    public String editMajor(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean activeStatus = Boolean.TRUE.equals(isActive);
        String error = adminService.updateMajor(id, name, code, departmentId, activeStatus);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Chuyên ngành thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
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

    // ==================== DIVISIONS ====================

    @PostMapping("/divisions/create")
    public String createDivision(
            @ModelAttribute Division division,
            @RequestParam("departmentId") Long departmentId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        String error = adminService.createDivision(division, departmentId);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Thêm mới Bộ môn '" + division.getName() + "' thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/divisions/{id}/edit")
    public String editDivision(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("code") String code,
            @RequestParam("departmentId") Long departmentId,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean activeStatus = Boolean.TRUE.equals(isActive);
        String error = adminService.updateDivision(id, name, code, departmentId, activeStatus);
        if (error == null) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật Bộ môn thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        }

        return "redirect:/admin/departments";
    }

    @PostMapping("/divisions/{id}/toggle-status")
    public String toggleDivisionStatus(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser admin = getAuthenticatedAdmin(session);
        if (admin == null) return "redirect:/login";

        boolean success = adminService.toggleDivisionActive(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái Bộ môn thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy Bộ môn!");
        }

        return "redirect:/admin/departments";
    }
}
