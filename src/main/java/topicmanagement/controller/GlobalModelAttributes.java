package topicmanagement.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import topicmanagement.entity.AppUser;
import topicmanagement.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Cung cấp dữ liệu tài khoản kiểm thử thực tế từ cơ sở dữ liệu Neon DB cho toàn bộ giao diện Thymeleaf.
 */
@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    private static final List<String> TEST_USERNAMES = List.of(
            "admin",
            "truongkhoa",
            "giangvien01",
            "giangvien02",
            "sinhvien01",
            "sinhvien02",
            "sinhvien03"
    );

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Danh sách tài khoản kiểm thử được query trực tiếp từ Neon PostgreSQL.
     */
    @ModelAttribute("testAccounts")
    public List<AppUser> getTestAccountsFromNeon() {
        List<AppUser> accounts = new ArrayList<>();
        for (String uname : TEST_USERNAMES) {
            userRepository.findByUsernameIgnoreCase(uname).ifPresent(accounts::add);
        }
        return accounts;
    }
}
