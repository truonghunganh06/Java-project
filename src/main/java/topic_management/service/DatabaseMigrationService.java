package topic_management.service;

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

            System.out.println("[DB-MIGRATION] Bắt đầu kiểm tra và cập nhật cấu trúc cơ sở dữ liệu...");

            // Cập nhật các cột điểm thành phần chuẩn học thuật cho bảng topics
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS advisor_score DOUBLE PRECISION;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS reviewer_score DOUBLE PRECISION;");
            stmt.execute("ALTER TABLE topics ADD COLUMN IF NOT EXISTS council_score DOUBLE PRECISION;");

            System.out.println("[DB-MIGRATION] Hoàn tất cập nhật các cột điểm thành phần (advisor_score, reviewer_score, council_score) vào bảng topics.");
        } catch (Exception e) {
            System.err.println("[DB-MIGRATION-WARN] Lỗi khi chạy migration cấu trúc DB: " + e.getMessage());
        }
    }
}

