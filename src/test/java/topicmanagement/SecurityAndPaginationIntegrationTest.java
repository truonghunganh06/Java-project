package topicmanagement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import topicmanagement.entity.*;
import topicmanagement.repository.*;
import topicmanagement.service.DepartmentHeadService;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private DepartmentHeadService departmentHeadService;

    @Autowired
    private RoleRepository roleRepository;

    private AppUser createMockUser(String username, String roleCode) {
        AppUser user = new AppUser();
        user.setId(9999L);
        user.setUsername(username);
        user.setFullName("Test " + username);
        user.setEmail(username + "@test.edu.vn");
        user.setIsActive(true);

        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode);
        user.setRole(role);

        return user;
    }

    // ==================== 1. SECURITY & RBAC INTERCEPTOR TESTS ====================

    @Test
    @DisplayName("SEC-01: Chưa đăng nhập truy cập /student/dashboard -> Redirect /login")
    void unauthenticatedRequestToStudentAreaShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/student/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("SEC-02: Chưa đăng nhập gọi API /api/v2/registrations/... -> HTTP 401 JSON")
    void unauthenticatedRequestToRestApiShouldReturn401Json() throws Exception {
        mockMvc.perform(get("/api/v2/registrations/invitations/my-invitations")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("SEC-03: Sinh viên truy cập /admin/users -> Redirect /login?error=access_denied")
    void studentAccessingAdminAreaShouldBeDenied() throws Exception {
        AppUser studentUser = createMockUser("sv_test", "STUDENT");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", studentUser);

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=access_denied"));
    }

    @Test
    @DisplayName("SEC-04: Giảng viên truy cập /admin/users -> Redirect /login?error=access_denied")
    void lecturerAccessingAdminAreaShouldBeDenied() throws Exception {
        AppUser lecturerUser = createMockUser("gv_test", "LECTURER");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", lecturerUser);

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=access_denied"));
    }

    // ==================== 2. DATABASE PAGINATION & FILTERING TESTS ====================

    @Test
    @DisplayName("PAG-01: Phân trang đề tài qua DepartmentHeadService với PageRequest ở mức DB")
    void testDepartmentTopicsPagination() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Topic> page = departmentHeadService.getDepartmentTopicsPaged(null, "ALL", null, null, null, pageRequest);

        assertNotNull(page);
        assertTrue(page.getSize() == 5);
        System.out.println("[PAG-01] Total Elements: " + page.getTotalElements() + ", Total Pages: " + page.getTotalPages());
    }

    @Test
    @DisplayName("PAG-02: Phân trang nhóm sinh viên qua StudentGroupRepository ở mức DB")
    void testStudentGroupsPagination() {
        PageRequest pageRequest = PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StudentGroup> page = studentGroupRepository.findDepartmentGroupsPaged(null, null, pageRequest);

        assertNotNull(page);
        assertTrue(page.getSize() == 4);
        System.out.println("[PAG-02] Total Groups: " + page.getTotalElements() + ", Total Pages: " + page.getTotalPages());
    }
}
