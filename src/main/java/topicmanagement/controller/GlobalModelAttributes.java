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
    private final topicmanagement.repository.RegistrationPeriodRepository registrationPeriodRepository;

    private static final List<String> TEST_USERNAMES = List.of(
            "admin",
            "truongkhoa",
            "giangvien01",
            "giangvien02",
            "sinhvien01",
            "sinhvien02",
            "sinhvien03"
    );

    public GlobalModelAttributes(UserRepository userRepository, topicmanagement.repository.RegistrationPeriodRepository registrationPeriodRepository) {
        this.userRepository = userRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
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

    /**
     * Học kỳ hiện tại (lấy từ session nếu đã chọn hoặc mặc định từ đợt mới nhất)
     */
    @ModelAttribute("currentSemester")
    public Integer getCurrentSemester(jakarta.servlet.http.HttpSession session) {
        if (session != null && session.getAttribute("selectedSemester") != null) {
            return (Integer) session.getAttribute("selectedSemester");
        }
        var activePeriods = registrationPeriodRepository.findByIsActiveTrueOrderByIdDesc();
        if (!activePeriods.isEmpty() && activePeriods.get(0).getSemester() != null) {
            return activePeriods.get(0).getSemester();
        }
        return 1;
    }

    /**
     * Năm học hiện tại (lấy từ session nếu đã chọn hoặc mặc định từ đợt mới nhất)
     */
    @ModelAttribute("currentAcademicYear")
    public String getCurrentAcademicYear(jakarta.servlet.http.HttpSession session) {
        if (session != null && session.getAttribute("selectedAcademicYear") != null) {
            return (String) session.getAttribute("selectedAcademicYear");
        }
        var activePeriods = registrationPeriodRepository.findByIsActiveTrueOrderByIdDesc();
        if (!activePeriods.isEmpty() && activePeriods.get(0).getAcademicYear() != null && !activePeriods.get(0).getAcademicYear().isBlank()) {
            return activePeriods.get(0).getAcademicYear();
        }
        return "2026-2027";
    }
}
