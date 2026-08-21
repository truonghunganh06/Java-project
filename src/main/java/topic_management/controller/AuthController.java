package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import topic_management.entity.AppUser;
import topic_management.service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}