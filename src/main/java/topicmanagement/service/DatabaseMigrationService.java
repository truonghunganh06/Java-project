package topicmanagement.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * DatabaseMigrationService — Tự động cập nhật / migrate cấu trúc bảng trong PostgreSQL
 * khi ứng dụng khởi chạy (ngay thời điểm khởi tạo Bean qua InitializingBean).
 */
@Service("databaseMigrationService")
public class DatabaseMigrationService implements InitializingBean {

    private final DataSource dataSource;

    public DatabaseMigrationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() {
        runMigrations();
    }

    public void runMigrations() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            System.out.println("[DB-MIGRATION] Bắt đầu kiểm tra và cập nhật cấu trúc cơ sở dữ liệu V2.0 (Divisions & Scoping)...");

            // 1. Tạo bảng divisions nếu chưa tồn tại
            stmt.execute("CREATE TABLE IF NOT EXISTS divisions (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "department_id BIGINT NOT NULL, " +
                    "code VARCHAR(50) NOT NULL UNIQUE, " +
                    "name VARCHAR(150) NOT NULL, " +
                    "description TEXT, " +
                    "head_lecturer_id BIGINT, " +
                    "is_active BOOLEAN DEFAULT TRUE" +
                    ");");

            // 2. Thêm cột division_id vào các bảng liên quan
            stmt.execute("ALTER TABLE majors ADD COLUMN IF NOT EXISTS division_id BIGINT;");
            stmt.execute("ALTER TABLE lecturers ADD COLUMN IF NOT EXISTS division_id BIGINT;");
            stmt.execute("ALTER TABLE students ADD COLUMN IF NOT EXISTS division_id BIGINT;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS division_id BIGINT;");
            stmt.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS division_id BIGINT;");

            // 3. Thêm cột version (Optimistic Locking)
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;");
            stmt.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;");

            // 4. Các cột điểm thành phần chuẩn học thuật cho topics
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS advisor_score DOUBLE PRECISION;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS reviewer_score DOUBLE PRECISION;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS council_score DOUBLE PRECISION;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS min_students INT DEFAULT 1;");

            // Thêm group_id cho topic_registrations nếu chưa có
            stmt.execute("ALTER TABLE topic_registrations ADD COLUMN IF NOT EXISTS group_id BIGINT;");
            stmt.execute("ALTER TABLE topic_registrations ADD COLUMN IF NOT EXISTS registered_by_student_id BIGINT;");

            // 5. Thêm cột min_students, max_students và min_topics_per_lecturer cho registration_periods
            stmt.execute("ALTER TABLE registration_periods ADD COLUMN IF NOT EXISTS min_students INT DEFAULT 2;");
            stmt.execute("ALTER TABLE registration_periods ADD COLUMN IF NOT EXISTS max_students INT DEFAULT 3;");
            stmt.execute("ALTER TABLE registration_periods ADD COLUMN IF NOT EXISTS min_topics_per_lecturer INT DEFAULT 3;");

            // Thêm cột cho submissions
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS original_file_name VARCHAR(255);");
            stmt.execute("ALTER TABLE submissions ADD COLUMN IF NOT EXISTS content_type VARCHAR(100);");

            // 6. Tạo bảng group_invitations
            stmt.execute("CREATE TABLE IF NOT EXISTS group_invitations (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "group_id BIGINT NOT NULL, " +
                    "student_id BIGINT NOT NULL, " +
                    "invited_by_student_id BIGINT NOT NULL, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'INVITED', " +
                    "rejection_reason TEXT, " +
                    "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                    "responded_at TIMESTAMP WITH TIME ZONE" +
                    ");");

            // 7. Tạo bảng group_leave_requests (EC-02)
            stmt.execute("CREATE TABLE IF NOT EXISTS group_leave_requests (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "group_id BIGINT NOT NULL, " +
                    "student_id BIGINT NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'PENDING', " +
                    "resolved_by_lecturer_id BIGINT, " +
                    "rejection_reason TEXT, " +
                    "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                    "resolved_at TIMESTAMP WITH TIME ZONE" +
                    ");");

            // 8. Tạo bảng group_dissolve_requests (EC-03)
            stmt.execute("CREATE TABLE IF NOT EXISTS group_dissolve_requests (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "group_id BIGINT NOT NULL, " +
                    "requested_by_student_id BIGINT NOT NULL, " +
                    "topic_id BIGINT NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'PENDING', " +
                    "resolved_by_lecturer_id BIGINT, " +
                    "rejection_reason TEXT, " +
                    "created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP, " +
                    "resolved_at TIMESTAMP WITH TIME ZONE" +
                    ");");



            // 9. Đồng bộ hóa sequence ID trong PostgreSQL để tránh lỗi duplicate key pkey
            String[] tables = {"roles", "departments", "divisions", "majors", "users", "lecturers", "students", "registration_periods", "topics", "student_groups", "group_members", "councils", "council_members", "announcements", "evaluations", "submissions", "group_invitations", "group_leave_requests", "group_dissolve_requests"};
            for (String table : tables) {
                try {
                    stmt.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE((SELECT MAX(id) FROM " + table + "), 1));");
                } catch (Exception ignored) {}
            }

            System.out.println("[DB-MIGRATION] Hoàn tất cập nhật cấu trúc bảng divisions, indexes, version và đồng bộ sequences.");
        } catch (Exception e) {
            System.err.println("[DB-MIGRATION-WARN] Lỗi khi chạy migration cấu trúc DB: " + e.getMessage());
        }
    }
}
