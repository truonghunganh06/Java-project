package topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topicmanagement.entity.Role;
import topicmanagement.entity.User;
import topicmanagement.repository.RoleRepository;
import topicmanagement.repository.UserRepository;

import java.util.Optional;

/**
 * DataSeederService — Kiểm tra các vai trò hệ thống cơ bản (System Roles).
 * Toàn bộ dữ liệu nghiệp vụ (Đề tài, Đợt đăng ký, Khoa, Giảng viên, Sinh viên)
 * được lưu trữ và quản lý trực tiếp trên Neon PostgreSQL Database.
 */
@Service
@DependsOn("databaseMigrationService")
@Order(5)
public class DataSeederService {

    private static final Logger log = LoggerFactory.getLogger(DataSeederService.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeederService(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureBasicSystemRoles() {
        try {
            // Đảm bảo các roles hệ thống cơ bản tồn tại trong database
            createRoleIfMissing("ADMIN", "Quản trị viên hệ thống");
            createRoleIfMissing("DEPARTMENT_HEAD", "Trưởng khoa / Trưởng bộ môn");
            createRoleIfMissing("DIVISION_HEAD", "Trưởng bộ môn");
            createRoleIfMissing("LECTURER", "Giảng viên");
            createRoleIfMissing("STUDENT", "Sinh viên");

            // Đảm bảo tài khoản admin khởi tạo nếu database hoàn toàn mới
            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
                if (adminRole != null) {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPasswordHash(passwordEncoder.encode("123456"));
                    admin.setFullName("Quản Trị Viên Hệ Thống");
                    admin.setEmail("admin@topicmanagement.edu.vn");
                    admin.setRole(adminRole);
                    admin.setIsActive(true);
                    userRepository.save(admin);
                    log.info("[DATA-SEEDER] Đã khởi tạo tài khoản admin mặc định.");
                }
            }
            log.info("[DATA-SEEDER] Hoàn tất kiểm tra hệ thống. Dữ liệu được quản lý 100% trên Neon PostgreSQL.");
        } catch (Exception e) {
            log.warn("[DATA-SEEDER-WARN] Lỗi kiểm tra role khởi tạo: {}", e.getMessage());
        }
    }

    private void createRoleIfMissing(String code, String name) {
        if (roleRepository.findByCode(code).isEmpty()) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            roleRepository.save(role);
            log.info("[DATA-SEEDER] Đã khởi tạo vai trò: {}", code);
        }
    }
}
