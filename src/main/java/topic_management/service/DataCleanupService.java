package topic_management.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * DataCleanupService — Dọn dẹp triệt để các dữ liệu rác, đề tài ảo/test,
 * tài khoản test phát sinh từ quá trình kiểm thử tự động,
 * và chuẩn hóa các đợt đăng ký & hội đồng về trạng thái hoạt động thực tế.
 */
@Service
@Order(1)
public class DataCleanupService {

    private final JdbcTemplate jdbcTemplate;

    public DataCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupAllJunkDataOnStartup() {
        try {
            System.out.println("\n[CLEANUP] Bắt đầu rà soát và dọn dẹp dữ liệu rác / đề tài ảo trong CSDL...");

            // 1. Dọn dẹp các đề tài test/ảo được tạo tự động từ integration tests
            cleanupDummyTopics();

            // 2. Dọn dẹp người dùng test tạm thời
            cleanupDummyUsers();

            // 3. Chuẩn hóa & Kích hoạt đợt đăng ký chuẩn cho SV đăng ký
            normalizeRegistrationPeriods();

            System.out.println("[CLEANUP] Hoàn tất rà soát và dọn dẹp dữ liệu hệ thống!\n");
        } catch (Exception e) {
            System.err.println("[CLEANUP-WARN] Lỗi khi dọn dẹp dữ liệu: " + e.getMessage());
        }
    }

    private void cleanupDummyTopics() {
        try {
            // Danh sách ID các đề tài ảo / test
            String findJunkTopicIdsSql = "SELECT id FROM topics WHERE "
                    + "code LIKE 'DT-ROUGH%' OR code LIKE 'DT-EARLY%' OR code LIKE 'DT-KTPM-%' "
                    + "OR code LIKE 'DT-PEND%' OR code LIKE 'DT-QUOTA%' OR code LIKE 'DT-1GV%' "
                    + "OR code LIKE 'DT-2GV%' OR code LIKE 'EXT-%' OR code LIKE 'TOPIC-A%' "
                    + "OR code LIKE 'TOPIC-B%' OR code LIKE 'TOPIC-MH%' OR code LIKE 'TOPIC-TLCN%' "
                    + "OR code LIKE 'DT-GRP%' OR code LIKE 'DT-TEST%' OR code LIKE '%NANOTIME%' "
                    + "OR code ~ '^DT-[0-9]{5,}$' "
                    + "OR LOWER(title) LIKE '%testtt%' OR LOWER(title) = 'test' "
                    + "OR LOWER(title) LIKE '%kiểm thử%' OR LOWER(title) LIKE '%tự động%'";

            // Gỡ liên kết trong student_groups
            jdbcTemplate.update("UPDATE student_groups SET topic_id = NULL WHERE topic_id IN (" + findJunkTopicIdsSql + ")");

            // Xóa evaluations của topic rác
            jdbcTemplate.update("DELETE FROM evaluations WHERE topic_id IN (" + findJunkTopicIdsSql + ")");

            // Xóa submissions của topic rác
            jdbcTemplate.update("DELETE FROM submissions WHERE topic_id IN (" + findJunkTopicIdsSql + ")");

            // Xóa topic rác
            int deleted = jdbcTemplate.update("DELETE FROM topics WHERE id IN (" + findJunkTopicIdsSql + ")");
            if (deleted > 0) {
                System.out.println("[CLEANUP] Đã xóa " + deleted + " đề tài ảo/test không hợp lệ.");
            }
        } catch (Exception e) {
            System.err.println("[CLEANUP-TOPICS] " + e.getMessage());
        }
    }

    private void cleanupDummyUsers() {
        try {
            String findJunkUserIdsSql = "SELECT id FROM app_users WHERE "
                    + "LOWER(username) LIKE 'user_sec_%' OR LOWER(username) LIKE 'user_lock_%' "
                    + "OR LOWER(username) LIKE 'user_reset_%' "
                    + "OR (LOWER(username) LIKE 'test_%' AND LOWER(username) != 'test') "
                    + "OR (LOWER(username) LIKE 'student_%' AND LOWER(username) NOT LIKE 'sv_%') "
                    + "OR (LOWER(username) LIKE 'lecturer_%' AND LOWER(username) NOT LIKE 'gv_%')";

            // Xóa group_members của sinh viên rác
            jdbcTemplate.update("DELETE FROM group_members WHERE student_id IN (SELECT id FROM students WHERE user_id IN (" + findJunkUserIdsSql + "))");

            // Xóa student_groups của sinh viên rác làm nhóm trưởng
            jdbcTemplate.update("DELETE FROM group_members WHERE group_id IN (SELECT id FROM student_groups WHERE leader_id IN (SELECT id FROM students WHERE user_id IN (" + findJunkUserIdsSql + ")))");
            jdbcTemplate.update("DELETE FROM student_groups WHERE leader_id IN (SELECT id FROM students WHERE user_id IN (" + findJunkUserIdsSql + "))");

            // Xóa sinh viên & giảng viên
            jdbcTemplate.update("DELETE FROM students WHERE user_id IN (" + findJunkUserIdsSql + ")");
            jdbcTemplate.update("DELETE FROM lecturers WHERE user_id IN (" + findJunkUserIdsSql + ")");

            // Xóa app_users
            int deleted = jdbcTemplate.update("DELETE FROM app_users WHERE id IN (" + findJunkUserIdsSql + ")");
            if (deleted > 0) {
                System.out.println("[CLEANUP] Đã xóa " + deleted + " tài khoản test tạm thời.");
            }
        } catch (Exception e) {
            System.err.println("[CLEANUP-USERS] " + e.getMessage());
        }
    }

    private void normalizeRegistrationPeriods() {
        try {
            // Mở thời gian đăng ký cho sinh viên cho tất cả các đợt đang active (hoặc đợt chuẩn)
            jdbcTemplate.update("UPDATE registration_periods SET "
                    + "is_active = true, "
                    + "status = 'OPEN', "
                    + "lecturer_proposal_start = NOW() - INTERVAL '20 days', "
                    + "lecturer_proposal_end = NOW() - INTERVAL '5 days', "
                    + "student_registration_start = NOW() - INTERVAL '10 days', "
                    + "student_registration_end = NOW() + INTERVAL '30 days', "
                    + "reviewer_deadline = NOW() + INTERVAL '40 days', "
                    + "council_defense_date = NOW() + INTERVAL '50 days' "
                    + "WHERE is_active = true OR LOWER(name) NOT LIKE '%test%'");

            System.out.println("[CLEANUP] Đã chuẩn hóa & mở thời gian đăng ký cho sinh viên.");
        } catch (Exception e) {
            System.err.println("[CLEANUP-PERIODS] " + e.getMessage());
        }
    }
}
