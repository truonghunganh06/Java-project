package topic_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import topic_management.entity.*;
import topic_management.repository.*;
import topic_management.service.AiAssistantService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AiAssistantServiceTest {

    @Mock
    private TopicRepository topicRepository;
    @Mock
    private StudentGroupRepository groupRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LecturerRepository lecturerRepository;
    @Mock
    private RegistrationPeriodRepository periodRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private CouncilRepository councilRepository;
    @Mock
    private DivisionRepository divisionRepository;
    @Mock
    private MajorRepository majorRepository;

    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        aiAssistantService = new AiAssistantService(
                topicRepository,
                groupRepository,
                studentRepository,
                lecturerRepository,
                periodRepository,
                evaluationRepository,
                submissionRepository,
                announcementRepository,
                councilRepository,
                divisionRepository,
                majorRepository
        );
    }

    @Test
    @DisplayName("AI Test 1: Quy chế và công thức tính điểm")
    void testGradingFormulaQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("STUDENT");
        user.setRole(role);

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Công thức tính điểm KLTN như thế nào?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("Trung bình cộng") || response.reply().contains("KLTN"));
        assertFalse(response.suggestions().isEmpty());
    }

    @Test
    @DisplayName("AI Test 2: Tra cứu tiến độ nhóm và báo cáo")
    void testProgressQuery() {
        AppUser user = new AppUser();
        user.setId(101L);
        Role role = new Role();
        role.setCode("STUDENT");
        user.setRole(role);

        Student student = new Student();
        student.setId(1L);
        student.setUser(user);

        StudentGroup group = new StudentGroup();
        group.setId(10L);
        group.setGroupName("Nhóm Alpha");
        group.setGroupCode("GRP-1001");
        group.setStatus("APPROVED");
        group.setLeader(student);

        when(studentRepository.findByUserId(101L)).thenReturn(Optional.of(student));
        when(groupRepository.findByStudentId(1L)).thenReturn(List.of(group));
        when(submissionRepository.findByGroupId(10L)).thenReturn(List.of());

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Tiến độ nhóm của tôi hiện tại ra sao?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("Nhóm Alpha"));
    }

    @Test
    @DisplayName("AI Test 3: Tra cứu hạn chót và lịch đợt")
    void testDeadlineQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("LECTURER");
        user.setRole(role);

        RegistrationPeriod period = new RegistrationPeriod();
        period.setName("Khóa Luận Tốt Nghiệp HK1");
        period.setPeriodType("KLTN");

        when(periodRepository.findAll()).thenReturn(List.of(period));

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Lịch bảo vệ hội đồng và deadline các đợt?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("Khóa Luận Tốt Nghiệp HK1"));
    }

    @Test
    @DisplayName("AI Test 4: Thống kê số lượng đề tài")
    void testStatisticsQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("DEPARTMENT_HEAD");
        user.setRole(role);

        when(topicRepository.findAll()).thenReturn(List.of());
        when(groupRepository.findAll()).thenReturn(List.of());
        when(councilRepository.findAll()).thenReturn(List.of());

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Thống kê tổng số đề tài và đợt");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("đề tài"));
    }

    @Test
    @DisplayName("AI Test 5: Lời chào và menu gợi ý")
    void testGreetingQuery() {
        AppUser user = new AppUser();
        user.setFullName("Nguyễn Văn Test");
        Role role = new Role();
        role.setCode("STUDENT");
        user.setRole(role);

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Xin chào bạn!");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("TopicHub AI Assistant") || response.reply().contains("Nguyễn Văn Test"));
    }

    @Test
    @DisplayName("AI Test 6: Đề xuất giảng viên rảnh để phân công GVPB")
    void testReviewerWorkloadQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("DEPARTMENT_HEAD");
        user.setRole(role);

        Lecturer l1 = new Lecturer();
        l1.setId(1L);
        AppUser u1 = new AppUser();
        u1.setFullName("Trần Hướng Dẫn 1");
        u1.setUsername("gv1");
        l1.setUser(u1);

        when(lecturerRepository.findAll()).thenReturn(List.of(l1));
        when(topicRepository.findAll()).thenReturn(List.of());

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "giảng viên nào đang rảnh để phân gvpb");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("PHÂN CÔNG GIẢNG VIÊN PHẢN BIỆN") || response.reply().contains("Bảng tải công việc"));
        assertFalse(response.suggestions().isEmpty());
    }

    @Test
    @DisplayName("AI Test 7: Quy định quyền nộp báo cáo và nhóm")
    void testGroupSubmissionPermissionsQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("STUDENT");
        user.setRole(role);

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Tại sao tôi không nộp được bài báo cáo?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("Trưởng nhóm") || response.reply().contains("Leader"));
    }

    @Test
    @DisplayName("AI Test 8: Điều kiện bảo vệ KLTN và đạt môn")
    void testDefenseEligibilityQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("STUDENT");
        user.setRole(role);

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Điều kiện để được bảo vệ KLTN là gì?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("5.0") || response.reply().contains("Điều kiện"));
    }

    @Test
    @DisplayName("AI Test 9: Hướng dẫn sử dụng Dark Mode và In PDF")
    void testFeatureHowToQuery() {
        AppUser user = new AppUser();
        Role role = new Role();
        role.setCode("LECTURER");
        user.setRole(role);

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, "Làm sao để xuất PDF phiếu đánh giá và in ấn?");
        assertNotNull(response);
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("PDF") || response.reply().contains("In"));
    }
}
