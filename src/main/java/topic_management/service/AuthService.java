package topic_management.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import topic_management.entity.AppUser;
import topic_management.repository.UserRepository;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AppUser authenticate(String username, String password) {
        String cleanUsername = username != null ? username.trim() : "";
        String cleanPassword = password != null ? password.trim() : "";

        System.out.println("==================================================");
        System.out.println("[AUTH DEBUG] Bắt đầu xác thực đăng nhập từ Database:");
        System.out.println("  -> Username nhập vào: '" + cleanUsername + "'");
        System.out.println("  -> Password nhập vào: '" + cleanPassword + "'");

        // 1. Tìm kiếm trực tiếp trong Database
        Optional<AppUser> userOpt = userRepository.findByUsernameIgnoreCase(cleanUsername);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(cleanUsername);
        }

        if (userOpt.isEmpty()) {
            System.out.println("  -> [AUTH FAILED] KHÔNG tìm thấy tài khoản với username: '" + cleanUsername + "' trong Database");
            System.out.println("==================================================");
            return null;
        }

        AppUser foundUser = userOpt.get();
        System.out.println("  -> [DB] Tìm thấy User: ID=" + foundUser.getId() + ", Username='" + foundUser.getUsername() + "', FullName='" + foundUser.getFullName() + "', Role=" + (foundUser.getRole() != null ? foundUser.getRole().getCode() : "null"));

        // 2. Kiểm tra trạng thái hoạt động của tài khoản
        if (foundUser.getIsActive() != null && !foundUser.getIsActive()) {
            System.out.println("  -> [AUTH FAILED] Tài khoản đã bị KHÓA (isActive = false)");
            System.out.println("==================================================");
            return null;
        }

        // 3. Kiểm tra mật khẩu (hỗ trợ cả Plain Text và BCrypt)
        String storedHash = foundUser.getPasswordHash();
        System.out.println("  -> Mật khẩu lưu trữ trong Database: '" + storedHash + "'");

        boolean isMatch = verifyPassword(cleanPassword, storedHash);
        System.out.println("  -> Kết quả đối soát mật khẩu: " + (isMatch ? "CHÍNH XÁC (MATCH)" : "SAI (MISMATCH)"));

        if (isMatch) {
            // BUG-12 FIX: Auto-upgrade plaintext passwords to BCrypt
            upgradePasswordIfNeeded(foundUser, cleanPassword);
            System.out.println("  -> [AUTH SUCCESS] Đăng nhập THÀNH CÔNG cho user: " + foundUser.getUsername() + " (Vai trò: " + (foundUser.getRole() != null ? foundUser.getRole().getCode() : "Chưa có") + ")");
            System.out.println("==================================================");
            return foundUser;
        }

        System.out.println("  -> [AUTH FAILED] Mật khẩu không trùng khớp.");
        System.out.println("==================================================");
        return null;
    }

    /**
     * BUG-12 FIX: Xác minh mật khẩu hỗ trợ cả plaintext (backward compat) và BCrypt.
     * Đã xóa backdoor dev password bypass ("123456", "admin123").
     */
    private boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        // Ưu tiên kiểm tra BCrypt hash trước
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return passwordEncoder.matches(rawPassword, storedHash);
            } catch (Exception e) {
                System.out.println("  -> [BCrypt Error] " + e.getMessage());
                return false;
            }
        }

        // Backward compatibility: So sánh plaintext (cho dữ liệu cũ chưa hash)
        // Nếu match, caller sẽ gọi upgradePasswordIfNeeded() để hash lại
        if (rawPassword.equals(storedHash)) {
            return true;
        }

        return false;
    }

    /**
     * BUG-12 FIX: Tự động nâng cấp mật khẩu plaintext lên BCrypt hash sau khi đăng nhập thành công.
     */
    private void upgradePasswordIfNeeded(AppUser user, String rawPassword) {
        String storedHash = user.getPasswordHash();
        // Nếu mật khẩu lưu dạng plaintext (không phải BCrypt), hash lại
        if (storedHash != null
                && !storedHash.startsWith("$2a$")
                && !storedHash.startsWith("$2b$")
                && !storedHash.startsWith("$2y$")) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
            System.out.println("  -> [AUTO-UPGRADE] Đã nâng cấp mật khẩu plaintext -> BCrypt cho user: " + user.getUsername());
        }
    }
}
