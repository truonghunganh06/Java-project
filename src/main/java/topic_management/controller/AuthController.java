package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import topic_management.entity.AppUser;
import topic_management.service.AuthService;
import topic_management.service.GoogleAuthService;

import java.util.UUID;

@Controller
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        String trimmedUser = username != null ? username.trim() : "";
        String trimmedPass = password != null ? password.trim() : "";

        AppUser user = authService.authenticate(trimmedUser, trimmedPass);
        if (user != null) {
            session.setAttribute("currentUser", user);
            return "redirect:/dashboard";
        }

        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không chính xác!");
        model.addAttribute("username", trimmedUser);
        return "auth/login";
    }

    @GetMapping("/auth/google")
    public String initiateGoogleLogin(HttpSession session, RedirectAttributes redirectAttributes) {
        if (!googleAuthService.isConfigured()) {
            redirectAttributes.addFlashAttribute("error", 
                "Chưa cấu hình Google Client ID / Secret trong hệ thống. Vui lòng cập nhật application.properties hoặc sử dụng phương thức đăng nhập tài khoản.");
            return "redirect:/login?google_unconfigured=true";
        }

        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);
        String googleAuthUrl = googleAuthService.buildAuthorizationUrl(state);
        return "redirect:" + googleAuthUrl;
    }

    @GetMapping("/auth/google/callback")
    public String handleGoogleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "state", required = false) String state,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (error != null) {
            redirectAttributes.addFlashAttribute("error", "Đăng nhập Google không thành công: " + error);
            return "redirect:/login";
        }

        if (code == null || code.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không nhận được mã xác thực từ Google.");
            return "redirect:/login";
        }

        try {
            AppUser user = googleAuthService.processGoogleCallback(code);
            session.setAttribute("currentUser", user);
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/auth/switch-role")
    public String switchRole(
            @RequestParam("username") String username,
            @RequestParam(value = "redirect", required = false) String redirectUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        AppUser user = authService.findUserByUsername(username);
        if (user != null) {
            session.setAttribute("currentUser", user);
            System.out.println("[ROLE-SWITCHER] Đã chuyển đổi nhanh sang tài khoản: " + user.getUsername() + " (" + (user.getRole() != null ? user.getRole().getCode() : "NO_ROLE") + ")");
            if (redirectUrl != null && !redirectUrl.trim().isEmpty() && redirectUrl.startsWith("/")) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/dashboard";
        }

        redirectAttributes.addFlashAttribute("error", "Không tìm thấy tài khoản kiểm thử: " + username);
        return "redirect:/login";
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}