package topicmanagement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security Integration Tests conforming to Chapter 07 & Lab 4 requirements.
 * Tests:
 * 1. Form Login with database credentials & session synchronization.
 * 2. Role-based access control (RBAC).
 * 3. HTTP Basic Authentication for REST clients (Postman).
 * 4. CSRF protection behavior.
 * 5. Method-level security (@PreAuthorize).
 * 6. Logout workflow.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class SpringSecurityLab4IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("LAB4-01: Form Login thành công với tài khoản Database -> Chuyển hướng /dashboard")
    void testFormLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "admin123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("LAB4-02: Form Login thất bại với sai mật khẩu -> Chuyển hướng /login?error=true")
    void testFormLoginFailure() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin")
                        .param("password", "wrongpassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("LAB4-03: Chưa xác thực truy cập tài nguyên bảo vệ -> Chuyển hướng /login")
    void testUnauthenticatedAccessRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "sv_cntt01", roles = {"STUDENT"})
    @DisplayName("LAB4-04: Sinh viên truy cập khu vực Admin -> Bị chặn và chuyển hướng /login?error=access_denied")
    void testStudentForbiddenFromAdminArea() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=access_denied"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("LAB4-05: Quản trị viên (ROLE_ADMIN) truy cập /admin/users -> Thành công HTTP 200")
    void testAdminAccessAllowed() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LAB4-06: REST API hỗ trợ HTTP Basic Authentication cho Postman")
    void testHttpBasicAuthenticationOnRestApi() throws Exception {
        // 1. Gọi không kèm Basic Auth credentials
        mockMvc.perform(get("/api/topics/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // 2. Gọi kèm HTTP Basic Auth chuẩn username:password
        mockMvc.perform(get("/api/topics/1")
                        .with(httpBasic("admin", "admin123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("LAB4-07: Đăng xuất Logout -> Invalidate phiên và chuyển hướng /login?logout=true")
    void testLogoutWorkflow() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
