package topic_management;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import topic_management.entity.*;
import topic_management.repository.*;
import topic_management.service.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExtendedBusinessRequirementTest {

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private LecturerService lecturerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Test
    @Order(1)
    void shouldAllowTopicWithOneOrTwoSupervisors() {
        Department department = getOrCreateDepartment();
        Major major = getOrCreateMajor(department);
        Lecturer supervisor = createLecturer("ext_gv_1");
        Lecturer coSupervisor = createLecturer("ext_gv_2");
        RegistrationPeriod period = createOpenPeriod("Đợt mở rộng kiểm thử 1", "KLTN");

        Topic singleSupervisorTopic = new Topic();
        singleSupervisorTopic.setCode("EXT-ONE-" + System.nanoTime());
        singleSupervisorTopic.setTitle("Đề tài sinh viên cần một GVHD");
        singleSupervisorTopic.setDescription("Kiểm tra đề tài chỉ có GVHD chính");
        singleSupervisorTopic.setRequirements("Thiết kế hợp lý");
        singleSupervisorTopic.setAcademicYear("2026-2027");
        singleSupervisorTopic.setSemester(1);

        Topic savedSingle = lecturerService.createTopic(supervisor, period.getId(), major.getId(), null,
                singleSupervisorTopic);
        assertNotNull(savedSingle.getId());
        assertEquals(supervisor.getId(), savedSingle.getSupervisor().getId());
        assertNull(savedSingle.getCoSupervisor());

        Topic twoSupervisorTopic = new Topic();
        twoSupervisorTopic.setCode("EXT-TWO-" + System.nanoTime());
        twoSupervisorTopic.setTitle("Đề tài cần hai GVHD");
        twoSupervisorTopic.setDescription("Kiểm tra đề tài có đồng hướng dẫn");
        twoSupervisorTopic.setRequirements("Nghiên cứu và triển khai");
        twoSupervisorTopic.setAcademicYear("2026-2027");
        twoSupervisorTopic.setSemester(1);

        Topic savedTwo = lecturerService.createTopic(supervisor, period.getId(), major.getId(), coSupervisor.getId(),
                twoSupervisorTopic);
        assertNotNull(savedTwo.getId());
        assertEquals(supervisor.getId(), savedTwo.getSupervisor().getId());
        assertNotNull(savedTwo.getCoSupervisor());
        assertEquals(coSupervisor.getId(), savedTwo.getCoSupervisor().getId());
    }

    @Test
    @Order(2)
    void shouldRejectRegistrationWhenStudentWindowIsClosed() {
        Department department = getOrCreateDepartment();
        Major major = getOrCreateMajor(department);
        Lecturer supervisor = createLecturer("ext_gv_3");
        RegistrationPeriod closedPeriod = departmentHeadService.createPeriod(
                department,
                "Đợt đóng kiểm thử đăng ký SV",
                "TLCN",
                "2026-2027",
                1,
                OffsetDateTime.now().minusDays(20),
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().plusDays(30),
                OffsetDateTime.now().plusDays(45),
                OffsetDateTime.now().plusDays(60),
                OffsetDateTime.now().plusDays(70));

        assertFalse(closedPeriod.isStudentRegistrationOpen(), "Đợt kiểm thử phải ở ngoài thời gian đăng ký sinh viên");

        Topic topic = new Topic();
        topic.setCode("EXT-CLOSED-" + System.nanoTime());
        topic.setTitle("Đề tài kiểm thử đóng cửa");
        topic.setDescription("Bài kiểm thử cho trạng thái đăng ký ngoài khung thời gian");
        topic.setRequirements("Yêu cầu kiểm thử");
        topic.setAcademicYear("2026-2027");
        topic.setSemester(1);

        Topic createdTopic = lecturerService.createTopic(supervisor, closedPeriod.getId(), major.getId(), null, topic);
        departmentHeadService.approveTopic(createdTopic.getId(), department.getId());

        Student leader = createStudent("ext_sv_closed");
        StudentGroup group = studentService.createGroup(leader, closedPeriod.getId(), "Nhóm đóng cửa");

        String result = studentService.registerTopicForGroup(leader, group, createdTopic.getId());
        assertNotNull(result, "Hệ thống phải từ chối đăng ký khi đợt đang đóng");
        assertTrue(result.toLowerCase(Locale.ROOT).contains("không trong thời gian mở"),
                "Thông báo lỗi phải mô tả việc đợt đăng ký đang đóng");
    }

    @Test
    @Order(3)
    void shouldAllowOnlyOneGroupPerStudentPerPeriodAndOnlyOneTopicPerGroup() {
        Department department = getOrCreateDepartment();
        Major major = getOrCreateMajor(department);
        Lecturer supervisor = createLecturer("ext_gv_4");
        RegistrationPeriod period = createOpenPeriod("Đợt kiểm thử ràng buộc nhóm", "NCKH");

        Student leader = createStudent("ext_sv_group_1");
        StudentGroup firstGroup = studentService.createGroup(leader, period.getId(), "Nhóm 1");

        assertThrows(IllegalStateException.class, () -> {
            studentService.createGroup(leader, period.getId(), "Nhóm 2 vi phạm");
        }, "Một sinh viên không được tạo 2 nhóm trong cùng một đợt");

        Topic topic = new Topic();
        topic.setCode("EXT-ONE-TOPIC-" + System.nanoTime());
        topic.setTitle("Đề tài kiểm thử một nhóm một đề tài");
        topic.setDescription("Kiểm tra ràng buộc mỗi nhóm chỉ đăng ký 1 đề tài");
        topic.setRequirements("Nghiên cứu ngoài trường");
        topic.setAcademicYear("2026-2027");
        topic.setSemester(1);

        Topic createdTopic = lecturerService.createTopic(supervisor, period.getId(), major.getId(), null, topic);
        departmentHeadService.approveTopic(createdTopic.getId(), department.getId());

        String firstRegistration = studentService.registerTopicForGroup(leader, firstGroup, createdTopic.getId());
        assertNull(firstRegistration, "Nhóm trưởng nên đăng ký đề tài thành công lần đầu");

        String duplicateRegistration = studentService.registerTopicForGroup(leader, firstGroup, createdTopic.getId());
        assertNotNull(duplicateRegistration, "Nhóm không được đăng ký lại đề tài thứ hai");
        assertTrue(duplicateRegistration.toLowerCase(Locale.ROOT).contains("đã đăng ký đề tài"),
                "Thông báo lỗi phải phù hợp với quy tắc một nhóm chỉ được đăng ký một đề tài");
    }

    @Test
    @Order(4)
    void shouldCreateAndDeleteAnnouncementsForDepartmentAndPeriod() {
        Department department = getOrCreateDepartment();
        RegistrationPeriod period = createOpenPeriod("Đợt thông báo kiểm thử", "MON_HOC");

        AppUser author = createUser("ann_author", "DEPARTMENT_HEAD", "Nguyễn Thị Ban Quản Lý");
        Announcement announcement = announcementService.createAnnouncement(
                author,
                department,
                period.getId(),
                "Thông báo lịch bảo vệ đợt kiểm thử",
                "Sinh viên cần theo dõi lịch bảo vệ và thông tin phê duyệt đề tài.",
                "PERIOD",
                true,
                "/attachments/announcement.pdf");

        assertNotNull(announcement.getId());
        assertEquals(author.getId(), announcement.getAuthor().getId());
        assertEquals(period.getId(), announcement.getPeriod().getId());

        List<Announcement> departmentAnnouncements = announcementService
                .getAnnouncementsByDepartment(department.getId());
        assertTrue(departmentAnnouncements.stream().anyMatch(item -> item.getId().equals(announcement.getId())),
                "Thông báo vừa tạo phải xuất hiện trong danh sách của khoa");

        boolean deleted = announcementService.deleteAnnouncement(announcement.getId());
        assertTrue(deleted, "Xóa thông báo phải thành công");
        assertNull(announcementService.getAnnouncementById(announcement.getId()),
                "Thông báo đã xóa không còn tồn tại trong hệ thống");
    }

    private Department getOrCreateDepartment() {
        return departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department department = new Department();
            department.setCode("CNTT-EXT");
            department.setName("Khoa Công nghệ Thông tin mở rộng");
            return departmentRepository.save(department);
        });
    }

    private Major getOrCreateMajor(Department department) {
        return majorRepository.findAll().stream()
                .filter(major -> major.getDepartment() != null
                        && major.getDepartment().getId().equals(department.getId()))
                .findFirst()
                .orElseGet(() -> {
                    Major major = new Major();
                    major.setDepartment(department);
                    major.setCode("KTPM-EXT");
                    major.setName("Kỹ thuật Phần mềm mở rộng");
                    return majorRepository.save(major);
                });
    }

    private RegistrationPeriod createOpenPeriod(String name, String type) {
        OffsetDateTime now = OffsetDateTime.now();
        return departmentHeadService.createPeriod(
                getOrCreateDepartment(),
                name,
                type,
                "2026-2027",
                1,
                now.minusDays(5),
                now.plusDays(10),
                now.minusDays(2),
                now.plusDays(20),
                now.plusDays(50),
                now.plusDays(70));
    }

    private Lecturer createLecturer(String prefix) {
        AppUser user = createUser(prefix, "LECTURER", "GV " + prefix);
        return lecturerService.getLecturerByUser(user);
    }

    private Student createStudent(String prefix) {
        AppUser user = createUser(prefix, "STUDENT", "SV " + prefix);
        return studentService.getStudentByUser(user);
    }

    private AppUser createUser(String prefix, String roleCode, String fullName) {
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role không tồn tại: " + roleCode));

        String unique = String.valueOf(System.nanoTime());
        AppUser user = new AppUser();
        user.setUsername(prefix + "_" + unique);
        user.setPasswordHash("123456");
        user.setFullName(fullName);
        user.setEmail(prefix + "_" + unique + "@test.edu.vn");
        user.setRole(role);
        user.setIsActive(true);
        return userRepository.save(user);
    }
}
