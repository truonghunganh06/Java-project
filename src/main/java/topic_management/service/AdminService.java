package topic_management.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final MajorRepository majorRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            MajorRepository majorRepository,
            StudentRepository studentRepository,
            LecturerRepository lecturerRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
    }

    // ==================== USER MANAGEMENT ====================

    public List<AppUser> getAllUsers() {
        List<AppUser> list = userRepository.findAll();
        System.out.println("[DB-ADMIN] getAllUsers: Lấy " + list.size() + " người dùng từ Database");
        return list;
    }

    public AppUser getUserById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public String createUser(AppUser user, Long roleId) {
        if (user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return "Tên đăng nhập không được để trống!";
        }
        String cleanUsername = user.getUsername().trim();

        if (userRepository.findByUsernameIgnoreCase(cleanUsername).isPresent()) {
            return "Tên đăng nhập '" + cleanUsername + "' đã tồn tại trên hệ thống. Vui lòng chọn tên khác!";
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            String cleanEmail = user.getEmail().trim().toLowerCase();
            if (userRepository.findByEmailIgnoreCase(cleanEmail).isPresent()) {
                return "Email '" + cleanEmail + "' đã được sử dụng bởi tài khoản khác!";
            }
            user.setEmail(cleanEmail);
        } else {
            user.setEmail(cleanUsername.toLowerCase() + "@topicmanagement.edu.vn");
        }

        Role role = getRoleById(roleId);
        if (role == null) {
            return "Vui lòng chọn vai trò hợp lệ cho người dùng!";
        }

        user.setUsername(cleanUsername);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        String rawPassword = user.getPasswordHash();
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            rawPassword = "123456";
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword.trim()));

        AppUser saved = userRepository.save(user);

        // Khởi tạo hồ sơ thực thể con (Student / Lecturer) để người dùng đăng nhập sử dụng ngay
        if ("STUDENT".equalsIgnoreCase(role.getCode())) {
            if (studentRepository.findByUserId(saved.getId()).isEmpty()) {
                Student student = new Student();
                student.setUser(saved);
                student.setStudentCode(saved.getUsername());
                student.setClassName("CNTT-K16");
                student.setAcademicYear("2026-2027");
                student.setGpa(3.50);
                List<Major> majors = majorRepository.findAll();
                if (!majors.isEmpty()) {
                    student.setMajor(majors.get(0));
                }
                studentRepository.save(student);
            }
        } else if ("LECTURER".equalsIgnoreCase(role.getCode()) || "DEPARTMENT_HEAD".equalsIgnoreCase(role.getCode())) {
            if (lecturerRepository.findByUserId(saved.getId()).isEmpty()) {
                Lecturer lecturer = new Lecturer();
                lecturer.setUser(saved);
                lecturer.setLecturerCode(saved.getUsername());
                lecturer.setAcademicTitle("TS");
                lecturer.setDegree("Tiến sĩ");
                lecturer.setMaxTopicsQuota(5);
                List<Department> depts = departmentRepository.findAll();
                if (!depts.isEmpty()) {
                    lecturer.setDepartment(depts.get(0));
                }
                lecturerRepository.save(lecturer);
            }
        }

        System.out.println("[DB-ADMIN] createUser: ĐÃ TẠO TÀI KHOẢN MỚI VÀO DB! (ID=" + saved.getId() + ", Username='" + saved.getUsername() + "', Role=" + role.getCode() + ")");
        return null; // success
    }

    @Transactional
    public boolean updateUser(Long id, String fullName, String email, String phone, Long roleId, Boolean isActive) {
        if (id == null) return false;
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            AppUser user = opt.get();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            if (roleId != null) {
                user.setRole(getRoleById(roleId));
            }
            if (isActive != null) {
                user.setIsActive(isActive);
            }
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            System.out.println("[DB-ADMIN] updateUser(" + id + "): Đã cập nhật thông tin user '" + user.getUsername() + "' vào DB thành công!");
            return true;
        }
        System.out.println("[DB-ADMIN] updateUser: Không tìm thấy User ID=" + id);
        return false;
    }

    @Transactional
    public boolean toggleUserActive(Long id) {
        if (id == null) return false;
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            AppUser user = opt.get();
            user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            System.out.println("[DB-ADMIN] toggleUserActive(" + id + "): Trạng thái mới isActive=" + user.getIsActive());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean resetUserPassword(Long id, String newPassword) {
        if (id == null) return false;
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            AppUser user = opt.get();
            // BUG-12 FIX: Hash mật khẩu mới bằng BCrypt
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            System.out.println("[DB-ADMIN] resetUserPassword(" + id + "): Đã đổi mật khẩu (BCrypt) cho user '" + user.getUsername() + "' vào DB");
            return true;
        }
        return false;
    }

    // ==================== ROLE MANAGEMENT ====================

    public List<Role> getAllRoles() {
        List<Role> list = roleRepository.findAll();
        System.out.println("[DB-ADMIN] getAllRoles: Lấy " + list.size() + " vai trò từ DB");
        return list;
    }

    public Role getRoleById(Long roleId) {
        if (roleId == null) return null;
        return roleRepository.findById(roleId).orElse(null);
    }

    // ==================== DEPARTMENT MANAGEMENT ====================

    public List<Department> getAllDepartments() {
        List<Department> list = departmentRepository.findAll();
        System.out.println("[DB-ADMIN] getAllDepartments: Lấy " + list.size() + " khoa từ DB");
        return list;
    }

    public Department getDepartmentById(Long id) {
        if (id == null) return null;
        return departmentRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean createDepartment(Department department) {
        if (department.getIsActive() == null) {
            department.setIsActive(true);
        }
        Department saved = departmentRepository.save(department);
        System.out.println("[DB-ADMIN] createDepartment: ĐÃ TẠO KHOA MỚI VÀO DB! (Mã: " + saved.getCode() + ", Tên: " + saved.getName() + ")");
        return true;
    }

    @Transactional
    public boolean updateDepartment(Long id, String name, String code, String description, Boolean isActive) {
        if (id == null) return false;
        Optional<Department> opt = departmentRepository.findById(id);
        if (opt.isPresent()) {
            Department d = opt.get();
            d.setName(name);
            d.setCode(code);
            d.setDescription(description);
            if (isActive != null) d.setIsActive(isActive);
            departmentRepository.save(d);
            System.out.println("[DB-ADMIN] updateDepartment(" + id + "): Đã cập nhật khoa '" + code + "' vào DB");
            return true;
        }
        return false;
    }

    @Transactional
    public boolean toggleDepartmentActive(Long id) {
        if (id == null) return false;
        Optional<Department> opt = departmentRepository.findById(id);
        if (opt.isPresent()) {
            Department d = opt.get();
            d.setIsActive(!Boolean.TRUE.equals(d.getIsActive()));
            departmentRepository.save(d);
            System.out.println("[DB-ADMIN] toggleDepartmentActive(" + id + "): Trạng thái mới isActive=" + d.getIsActive());
            return true;
        }
        return false;
    }

    // ==================== MAJOR MANAGEMENT ====================

    public List<Major> getAllMajors() {
        List<Major> list = majorRepository.findAll();
        System.out.println("[DB-ADMIN] getAllMajors: Lấy " + list.size() + " chuyên ngành từ DB");
        return list;
    }

    public Major getMajorById(Long id) {
        if (id == null) return null;
        return majorRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean createMajor(Major major, Long departmentId) {
        Department dept = getDepartmentById(departmentId);
        major.setDepartment(dept);
        if (major.getIsActive() == null) {
            major.setIsActive(true);
        }
        Major saved = majorRepository.save(major);
        System.out.println("[DB-ADMIN] createMajor: ĐÃ TẠO CHUYÊN NGÀNH MỚI VÀO DB! (Mã: " + saved.getCode() + ", Tên: " + saved.getName() + ")");
        return true;
    }

    @Transactional
    public boolean updateMajor(Long id, String name, String code, Long departmentId, Boolean isActive) {
        if (id == null) return false;
        Department dept = getDepartmentById(departmentId);
        Optional<Major> opt = majorRepository.findById(id);
        if (opt.isPresent()) {
            Major m = opt.get();
            m.setName(name);
            m.setCode(code);
            m.setDepartment(dept);
            if (isActive != null) m.setIsActive(isActive);
            majorRepository.save(m);
            System.out.println("[DB-ADMIN] updateMajor(" + id + "): Đã cập nhật chuyên ngành '" + code + "' vào DB");
            return true;
        }
        return false;
    }

    @Transactional
    public boolean toggleMajorActive(Long id) {
        if (id == null) return false;
        Optional<Major> opt = majorRepository.findById(id);
        if (opt.isPresent()) {
            Major m = opt.get();
            m.setIsActive(!Boolean.TRUE.equals(m.getIsActive()));
            majorRepository.save(m);
            System.out.println("[DB-ADMIN] toggleMajorActive(" + id + "): Trạng thái mới isActive=" + m.getIsActive());
            return true;
        }
        return false;
    }

    // ==================== SYSTEM STATS ====================

    public Map<String, Object> getSystemStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long adminCount = userRepository.countByRoleCode("ADMIN");
        long deptHeadCount = userRepository.countByRoleCode("DEPARTMENT_HEAD");
        long lecturerCount = userRepository.countByRoleCode("LECTURER");
        long studentCount = userRepository.countByRoleCode("STUDENT");
        
        List<Department> depts = getAllDepartments();
        List<Major> majors = getAllMajors();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalDepartments", depts.size());
        stats.put("totalMajors", majors.size());
        stats.put("adminCount", adminCount);
        stats.put("deptHeadCount", deptHeadCount);
        stats.put("lecturerCount", lecturerCount);
        stats.put("studentCount", studentCount);

        System.out.println("[DB-ADMIN] getSystemStats: Thống kê DB -> Tổng user: " + totalUsers + ", Active: " + activeUsers + ", Khoa: " + depts.size() + ", Ngành: " + majors.size());
        return stats;
    }
}
