package topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topicmanagement.entity.*;
import topicmanagement.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final DivisionRepository divisionRepository;
    private final MajorRepository majorRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            DivisionRepository divisionRepository,
            MajorRepository majorRepository,
            StudentRepository studentRepository,
            LecturerRepository lecturerRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.divisionRepository = divisionRepository;
        this.majorRepository = majorRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.passwordEncoder = passwordEncoder;
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
                    if (majors.get(0).getDivision() != null) {
                        student.setDivision(majors.get(0).getDivision());
                    }
                }
                studentRepository.save(student);
            }
        } else if ("LECTURER".equalsIgnoreCase(role.getCode()) || "DEPARTMENT_HEAD".equalsIgnoreCase(role.getCode()) || "DIVISION_HEAD".equalsIgnoreCase(role.getCode())) {
            if (lecturerRepository.findByUserId(saved.getId()).isEmpty()) {
                Lecturer lecturer = new Lecturer();
                lecturer.setUser(saved);
                lecturer.setLecturerCode(saved.getUsername());
                lecturer.setAcademicTitle("TS");
                lecturer.setDegree("Tiến sĩ");
                lecturer.setMaxTopicsQuota(5);
                List<Division> divs = divisionRepository.findAll();
                if (!divs.isEmpty()) {
                    lecturer.setDivision(divs.get(0));
                    lecturer.setDepartment(divs.get(0).getDepartment());
                } else {
                    List<Department> depts = departmentRepository.findAll();
                    if (!depts.isEmpty()) {
                        lecturer.setDepartment(depts.get(0));
                    }
                }
                lecturerRepository.save(lecturer);
            }
        }

        System.out.println("[DB-ADMIN] createUser: ĐÃ TẠO TÀI KHOẢN MỚI VÀO DB! (ID=" + saved.getId() + ", Username='" + saved.getUsername() + "', Role=" + role.getCode() + ")");
        return null; // success
    }

    @Transactional
    public String updateUser(Long id, String fullName, String email, String phone, Long roleId, Boolean isActive) {
        if (id == null) return "Mã định danh người dùng không hợp lệ!";
        Optional<AppUser> opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            System.out.println("[DB-ADMIN] updateUser: Không tìm thấy User ID=" + id);
            return "Không tìm thấy người dùng với ID: " + id;
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            return "Họ và tên không được để trống!";
        }
        if (email == null || email.trim().isEmpty()) {
            return "Email không được để trống!";
        }

        String cleanEmail = email.trim().toLowerCase();
        Optional<AppUser> existingEmailUser = userRepository.findByEmailIgnoreCase(cleanEmail);
        if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(id)) {
            return "Email '" + cleanEmail + "' đã được sử dụng bởi tài khoản khác (" + existingEmailUser.get().getUsername() + ")!";
        }

        AppUser user = opt.get();
        user.setFullName(fullName.trim());
        user.setEmail(cleanEmail);
        user.setPhone(phone != null && !phone.trim().isEmpty() ? phone.trim() : null);

        Role newRole = null;
        if (roleId != null) {
            newRole = getRoleById(roleId);
            if (newRole != null) {
                user.setRole(newRole);
            }
        }
        if (isActive != null) {
            user.setIsActive(isActive);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        AppUser saved = userRepository.save(user);

        // Khởi tạo/đồng bộ hồ sơ thực thể con (Student / Lecturer) nếu người dùng được đổi sang vai trò mới
        if (newRole != null) {
            if ("STUDENT".equalsIgnoreCase(newRole.getCode())) {
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
                        if (majors.get(0).getDivision() != null) {
                            student.setDivision(majors.get(0).getDivision());
                        }
                    }
                    studentRepository.save(student);
                }
            } else if ("LECTURER".equalsIgnoreCase(newRole.getCode()) || "DEPARTMENT_HEAD".equalsIgnoreCase(newRole.getCode()) || "DIVISION_HEAD".equalsIgnoreCase(newRole.getCode()) || "DEAN".equalsIgnoreCase(newRole.getCode())) {
                if (lecturerRepository.findByUserId(saved.getId()).isEmpty()) {
                    Lecturer lecturer = new Lecturer();
                    lecturer.setUser(saved);
                    lecturer.setLecturerCode(saved.getUsername());
                    lecturer.setAcademicTitle("TS");
                    lecturer.setDegree("Tiến sĩ");
                    lecturer.setMaxTopicsQuota(5);
                    List<Division> divs = divisionRepository.findAll();
                    if (!divs.isEmpty()) {
                        lecturer.setDivision(divs.get(0));
                        lecturer.setDepartment(divs.get(0).getDepartment());
                    } else {
                        List<Department> depts = departmentRepository.findAll();
                        if (!depts.isEmpty()) {
                            lecturer.setDepartment(depts.get(0));
                        }
                    }
                    lecturerRepository.save(lecturer);
                }
            }
        }

        System.out.println("[DB-ADMIN] updateUser(" + id + "): Đã cập nhật thông tin user '" + user.getUsername() + "' vào DB thành công!");
        return null; // Thành công
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
    public String createDepartment(Department department) {
        if (department == null || department.getCode() == null || department.getCode().trim().isEmpty()) {
            return "Mã khoa không được để trống!";
        }
        if (department.getName() == null || department.getName().trim().isEmpty()) {
            return "Tên khoa không được để trống!";
        }
        String cleanCode = department.getCode().trim().toUpperCase();
        if (departmentRepository.findByCodeIgnoreCase(cleanCode).isPresent()) {
            return "Mã khoa '" + cleanCode + "' đã tồn tại trên hệ thống!";
        }

        department.setCode(cleanCode);
        department.setName(department.getName().trim());
        if (department.getIsActive() == null) {
            department.setIsActive(true);
        }
        Department saved = departmentRepository.save(department);
        System.out.println("[DB-ADMIN] createDepartment: ĐÃ TẠO KHOA MỚI VÀO DB! (Mã: " + saved.getCode() + ", Tên: " + saved.getName() + ")");
        return null;
    }

    @Transactional
    public String updateDepartment(Long id, String name, String code, String description, Boolean isActive) {
        if (id == null) return "Mã định danh khoa không hợp lệ!";
        Optional<Department> opt = departmentRepository.findById(id);
        if (opt.isEmpty()) {
            return "Không tìm thấy Khoa với ID: " + id;
        }
        if (code == null || code.trim().isEmpty()) {
            return "Mã khoa không được để trống!";
        }
        if (name == null || name.trim().isEmpty()) {
            return "Tên khoa không được để trống!";
        }

        String cleanCode = code.trim().toUpperCase();
        Optional<Department> existingCodeDept = departmentRepository.findByCodeIgnoreCase(cleanCode);
        if (existingCodeDept.isPresent() && !existingCodeDept.get().getId().equals(id)) {
            return "Mã khoa '" + cleanCode + "' đã được sử dụng bởi khoa khác (" + existingCodeDept.get().getName() + ")!";
        }

        Department d = opt.get();
        d.setName(name.trim());
        d.setCode(cleanCode);
        d.setDescription(description != null ? description.trim() : null);
        if (isActive != null) d.setIsActive(isActive);
        departmentRepository.save(d);
        System.out.println("[DB-ADMIN] updateDepartment(" + id + "): Đã cập nhật khoa '" + cleanCode + "' vào DB");
        return null;
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
    public String createMajor(Major major, Long departmentId) {
        if (major == null || major.getCode() == null || major.getCode().trim().isEmpty()) {
            return "Mã chuyên ngành không được để trống!";
        }
        if (major.getName() == null || major.getName().trim().isEmpty()) {
            return "Tên chuyên ngành không được để trống!";
        }
        if (departmentId == null) {
            return "Vui lòng chọn Khoa trực thuộc!";
        }
        Department dept = getDepartmentById(departmentId);
        if (dept == null) {
            return "Khoa trực thuộc không tồn tại!";
        }

        String cleanCode = major.getCode().trim().toUpperCase();
        if (majorRepository.findByCodeIgnoreCase(cleanCode).isPresent()) {
            return "Mã chuyên ngành '" + cleanCode + "' đã tồn tại trên hệ thống!";
        }

        major.setCode(cleanCode);
        major.setName(major.getName().trim());
        major.setDepartment(dept);
        if (major.getIsActive() == null) {
            major.setIsActive(true);
        }
        Major saved = majorRepository.save(major);
        System.out.println("[DB-ADMIN] createMajor: ĐÃ TẠO CHUYÊN NGÀNH MỚI VÀO DB! (Mã: " + saved.getCode() + ", Tên: " + saved.getName() + ")");
        return null;
    }

    @Transactional
    public String updateMajor(Long id, String name, String code, Long departmentId, Boolean isActive) {
        if (id == null) return "Mã định danh chuyên ngành không hợp lệ!";
        Optional<Major> opt = majorRepository.findById(id);
        if (opt.isEmpty()) {
            return "Không tìm thấy Chuyên ngành với ID: " + id;
        }
        if (code == null || code.trim().isEmpty()) {
            return "Mã chuyên ngành không được để trống!";
        }
        if (name == null || name.trim().isEmpty()) {
            return "Tên chuyên ngành không được để trống!";
        }
        if (departmentId == null) {
            return "Vui lòng chọn Khoa trực thuộc!";
        }
        Department dept = getDepartmentById(departmentId);
        if (dept == null) {
            return "Khoa trực thuộc không tồn tại!";
        }

        String cleanCode = code.trim().toUpperCase();
        Optional<Major> existingCodeMajor = majorRepository.findByCodeIgnoreCase(cleanCode);
        if (existingCodeMajor.isPresent() && !existingCodeMajor.get().getId().equals(id)) {
            return "Mã chuyên ngành '" + cleanCode + "' đã được sử dụng bởi chuyên ngành khác (" + existingCodeMajor.get().getName() + ")!";
        }

        Major m = opt.get();
        m.setName(name.trim());
        m.setCode(cleanCode);
        m.setDepartment(dept);
        if (isActive != null) m.setIsActive(isActive);
        majorRepository.save(m);
        System.out.println("[DB-ADMIN] updateMajor(" + id + "): Đã cập nhật chuyên ngành '" + cleanCode + "' vào DB");
        return null;
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
