package topic_management.service;

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

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            MajorRepository majorRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.majorRepository = majorRepository;
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
    public boolean createUser(AppUser user, Long roleId) {
        Role role = getRoleById(roleId);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());

        AppUser saved = userRepository.save(user);
        System.out.println("[DB-ADMIN] createUser: ĐÃ TẠO TÀI KHOẢN MỚI VÀO DB! (ID=" + saved.getId() + ", Username='" + saved.getUsername() + "')");
        return true;
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
            user.setPasswordHash(newPassword);
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            System.out.println("[DB-ADMIN] resetUserPassword(" + id + "): Đã đổi mật khẩu cho user '" + user.getUsername() + "' vào DB");
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
        List<AppUser> users = getAllUsers();
        List<Department> depts = getAllDepartments();
        List<Major> majors = getAllMajors();

        long activeUsers = users.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long adminCount = users.stream().filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getCode())).count();
        long deptHeadCount = users.stream().filter(u -> u.getRole() != null && "DEPARTMENT_HEAD".equalsIgnoreCase(u.getRole().getCode())).count();
        long lecturerCount = users.stream().filter(u -> u.getRole() != null && "LECTURER".equalsIgnoreCase(u.getRole().getCode())).count();
        long studentCount = users.stream().filter(u -> u.getRole() != null && "STUDENT".equalsIgnoreCase(u.getRole().getCode())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", users.size());
        stats.put("activeUsers", activeUsers);
        stats.put("totalDepartments", depts.size());
        stats.put("totalMajors", majors.size());
        stats.put("adminCount", adminCount);
        stats.put("deptHeadCount", deptHeadCount);
        stats.put("lecturerCount", lecturerCount);
        stats.put("studentCount", studentCount);

        System.out.println("[DB-ADMIN] getSystemStats: Thống kê DB -> Tổng user: " + users.size() + ", Khoa: " + depts.size() + ", Ngành: " + majors.size());
        return stats;
    }
}
