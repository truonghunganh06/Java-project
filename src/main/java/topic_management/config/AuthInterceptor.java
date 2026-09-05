package topic_management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import topic_management.entity.AppUser;

import java.io.IOException;

/**
 * Centralized Authentication & Role-Based Access Control (RBAC) Interceptor.
 * Protects MVC endpoints and REST API routes based on user role.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        HttpSession session = request.getSession(false);
        AppUser currentUser = (session != null) ? (AppUser) session.getAttribute("currentUser") : null;

        // 1. If unauthenticated
        if (currentUser == null) {
            if (isAjaxOrApiRequest(request, path)) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục!");
                return false;
            }
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        // 2. Check if user is active
        if (Boolean.FALSE.equals(currentUser.getIsActive())) {
            if (session != null) session.invalidate();
            if (isAjaxOrApiRequest(request, path)) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Tài khoản của bạn đã bị khóa!");
                return false;
            }
            response.sendRedirect(request.getContextPath() + "/login?error=account_locked");
            return false;
        }

        String roleCode = (currentUser.getRole() != null && currentUser.getRole().getCode() != null)
                ? currentUser.getRole().getCode().toUpperCase()
                : "";

        // 3. Admin can access everything
        if ("ADMIN".equals(roleCode)) {
            return true;
        }

        // 4. Role-based URL Authorization
        if (path.startsWith("/admin")) {
            return handleForbidden(request, response, path);
        }

        if (path.startsWith("/department-head") || path.startsWith("/division-head")) {
            if (!"DEPARTMENT_HEAD".equals(roleCode) && !"DIVISION_HEAD".equals(roleCode)) {
                return handleForbidden(request, response, path);
            }
        }

        if (path.startsWith("/lecturer")) {
            if (!"LECTURER".equals(roleCode) && !"DEPARTMENT_HEAD".equals(roleCode) && !"DIVISION_HEAD".equals(roleCode)) {
                return handleForbidden(request, response, path);
            }
        }

        if (path.startsWith("/student")) {
            if (!"STUDENT".equals(roleCode)) {
                return handleForbidden(request, response, path);
            }
        }

        return true;
    }

    private boolean handleForbidden(HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
        if (isAjaxOrApiRequest(request, path)) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền truy cập chức năng này!");
            return false;
        }
        response.sendRedirect(request.getContextPath() + "/login?error=access_denied");
        return false;
    }

    private boolean isAjaxOrApiRequest(HttpServletRequest request, String path) {
        if (path.startsWith("/api/")) return true;
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)) return true;
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + escapeJson(message) + "\"}");
        response.getWriter().flush();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
