package topic_management;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import topic_management.entity.*;
import topic_management.repository.*;
import topic_management.service.*;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ: QUẢN TRỊ NGƯỜI DÙNG, BẢO MẬT XÁC THỰC VÀ THÔNG BÁO CHÍNH THỨC
 * Theo đặc tả:
 * 1. Hỗ trợ quản lý tài khoản người dùng và giải pháp phân quyền (ADMIN, DEPARTMENT_HEAD, LECTURER, STUDENT).
 * 2. Đăng tải và quản lý các thông báo chính thức của nhà trường và khoa.
 * 3. Bảo mật dữ liệu và khóa tài khoản khi cần thiết.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserAccountAndAnnouncementSecurityTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AuthService authService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Department department;
    private Role roleLecturer;
    private Role roleStudent;
    private Role roleAdmin;
    private Role roleDeptHead;

    @BeforeAll
    void setupFixtures() {
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setCode("CNTT-ADMIN");
            d.setName("Khoa CNTT - Admin Test");
            return departmentRepository.save(d);
        });

        roleAdmin = roleRepository.findByCode("ADMIN").orElse(null);
        roleDeptHead = roleRepository.findByCode("DEPARTMENT_HEAD").orElse(null);
        roleLecturer = roleRepository.findByCode("LECTURER").orElse(null);
        roleStudent = roleRepository.findByCode("STUDENT").orElse(null);

        assertNotNull(roleAdmin);
        assertNotNull(roleDeptHead);
        assertNotNull(roleLecturer);
        assertNotNull(roleStudent);
    }

    @Test
    @Order(1)
    @DisplayName("1. Quản trị tạo mới tài khoản người dùng với phân quyền chuẩn và xác thực đăng nhập")
    void testUserCreationAndAuthentication() {
        String unique = "user_sec_" + System.nanoTime();
        AppUser newUser = new AppUser();
        newUser.setUsername(unique);
        newUser.setPasswordHash("Password@123");
        newUser.setFullName("Giảng Viên Bảo Mật Test");
        newUser.setEmail(unique + "@faculty.edu.vn");

        String created = adminService.createUser(newUser, roleLecturer.getId());
        assertNull(created, "Tạo user mới thành công");

        // 1. Đăng nhập với thông tin đúng -> Thành công
        AppUser authenticated = authService.authenticate(unique, "Password@123");
        assertNotNull(authenticated, "Xác thực phải thành công với mật khẩu đúng");
        assertEquals("LECTURER", authenticated.getRole().getCode());

        // 2. Đăng nhập với mật khẩu sai -> Trả về null
        AppUser wrongPass = authService.authenticate(unique, "WrongPassword");
        assertNull(wrongPass, "Xác thực phải từ chối khi nhập sai mật khẩu");

        // 3. Đăng nhập với username không tồn tại -> Trả về null
        AppUser nonExistent = authService.authenticate("non_existent_user_9999", "123456");
        assertNull(nonExistent, "Xác thực phải từ chối khi username không tồn tại");
    }

    @Test
    @Order(2)
    @DisplayName("2. Khóa tài khoản và kiểm tra bảo mật (Tài khoản bị khóa không được phép đăng nhập)")
    void testAccountLockSecurity() {
        String unique = "user_lock_" + System.nanoTime();
        AppUser newUser = new AppUser();
        newUser.setUsername(unique);
        newUser.setPasswordHash("Secret@123");
        newUser.setFullName("Sinh Viên Thử Nghiệm Khóa");
        newUser.setEmail(unique + "@student.edu.vn");

        String created2 = adminService.createUser(newUser, roleStudent.getId());
        assertNull(created2);

        AppUser createdUser = userRepository.findByUsername(unique).orElse(null);
        assertNotNull(createdUser);

        // Khóa tài khoản
        adminService.toggleUserActive(createdUser.getId());
        AppUser lockedUser = adminService.getUserById(createdUser.getId());
        assertFalse(lockedUser.getIsActive(), "Tài khoản phải ở trạng thái không hoạt động (isActive = false)");

        // Thử đăng nhập với tài khoản bị khóa -> Phải bị từ chối
        AppUser authLocked = authService.authenticate(unique, "Secret@123");
        assertNull(authLocked, "Tài khoản bị khóa phải không thể đăng nhập thành công");

        // Mở khóa lại
        adminService.toggleUserActive(createdUser.getId());
        AppUser unlockedUser = authService.authenticate(unique, "Secret@123");
        assertNotNull(unlockedUser, "Sau khi mở khóa, tài khoản đăng nhập lại bình thường");
    }

    @Test
    @Order(3)
    @DisplayName("3. Đổi mật khẩu tài khoản người dùng và xác thực với mật khẩu mới")
    void testResetUserPassword() {
        String unique = "user_reset_" + System.nanoTime();
        AppUser newUser = new AppUser();
        newUser.setUsername(unique);
        newUser.setPasswordHash("OldPass123");
        newUser.setFullName("Người Dùng Đổi Mật Khẩu");
        newUser.setEmail(unique + "@uni.edu.vn");

        String created3 = adminService.createUser(newUser, roleDeptHead.getId());
        assertNull(created3);
        AppUser createdUser = userRepository.findByUsername(unique).orElse(null);
        assertNotNull(createdUser);

        // Reset mật khẩu mới
        boolean resetOk = adminService.resetUserPassword(createdUser.getId(), "NewSecurePass456");
        assertTrue(resetOk);

        // Đăng nhập mật khẩu cũ -> Thất bại
        assertNull(authService.authenticate(unique, "OldPass123"));

        // Đăng nhập mật khẩu mới -> Thành công
        assertNotNull(authService.authenticate(unique, "NewSecurePass456"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Đăng tải, phân loại, ghim và tra cứu thông báo chính thức của Khoa/Trường")
    void testOfficialAnnouncementsManagement() {
        AppUser author = userRepository.findByUsername("admin").orElseGet(() -> {
            AppUser u = new AppUser();
            u.setUsername("admin_ann");
            u.setPasswordHash("123456");
            u.setFullName("Ban Chủ Nhiệm Khoa");
            u.setEmail("admin_ann@uni.edu.vn");
            u.setRole(roleAdmin);
            u.setIsActive(true);
            return userRepository.save(u);
        });

        // 1. Tạo thông báo quan trọng (Được GHIM lên đầu)
        Announcement pinnedNotice = announcementService.createAnnouncement(
                author,
                department,
                null,
                "LỊCH NỘP BÁO CÁO TOÀN VĂN KHÓA LUẬN TỐT NGHIỆP 2026",
                "Tất cả nhóm trưởng khẩn trương hoàn thiện và nộp bản toàn văn trước ngày 15/06/2026.",
                "IMPORTANT",
                true,
                "/files/lich_nop_kltn_2026.pdf"
        );

        assertNotNull(pinnedNotice.getId());
        assertTrue(pinnedNotice.getIsPinned(), "Thông báo phải có trạng thái ghim");
        assertEquals("IMPORTANT", pinnedNotice.getCategory());

        // 2. Tra cứu danh sách thông báo của khoa
        List<Announcement> announcements = announcementService.getAnnouncementsByDepartment(department.getId());
        assertFalse(announcements.isEmpty());
        assertTrue(announcements.stream().anyMatch(a -> a.getId().equals(pinnedNotice.getId())));

        // 3. Xóa thông báo
        boolean deleted = announcementService.deleteAnnouncement(pinnedNotice.getId());
        assertTrue(deleted);
        assertNull(announcementService.getAnnouncementById(pinnedNotice.getId()));
    }
}
