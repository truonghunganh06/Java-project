package topic_management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TopicManagementApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureAllColumnsExist() {
        System.out.println("====== [ENSURE ALL COLUMNS EXIST] ======");

        // 1. councils
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS code VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS name VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS defense_date TIMESTAMPTZ");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ESTABLISHED'");
        jdbcTemplate.execute("ALTER TABLE councils ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 2. council_members
        jdbcTemplate.execute("ALTER TABLE council_members ADD COLUMN IF NOT EXISTS council_id BIGINT REFERENCES councils(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE council_members ADD COLUMN IF NOT EXISTS lecturer_id BIGINT REFERENCES lecturers(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE council_members ADD COLUMN IF NOT EXISTS council_role VARCHAR(50) DEFAULT 'MEMBER'");
        jdbcTemplate.execute("ALTER TABLE council_members ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 3. student_groups
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS group_code VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS group_name VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS leader_id BIGINT REFERENCES students(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS topic_id BIGINT REFERENCES topics(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'DRAFT'");
        jdbcTemplate.execute("ALTER TABLE student_groups ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 4. group_members
        jdbcTemplate.execute("ALTER TABLE group_members ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES student_groups(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE group_members ADD COLUMN IF NOT EXISTS student_id BIGINT REFERENCES students(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE group_members ADD COLUMN IF NOT EXISTS role_in_group VARCHAR(50) DEFAULT 'MEMBER'");
        jdbcTemplate.execute("ALTER TABLE group_members ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 5. evaluations
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS topic_id BIGINT REFERENCES topics(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES student_groups(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS lecturer_id BIGINT REFERENCES lecturers(id) ON DELETE CASCADE");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS evaluation_type VARCHAR(50)");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS score DOUBLE PRECISION");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS comments TEXT");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS is_submitted BOOLEAN DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE evaluations ADD COLUMN IF NOT EXISTS evaluated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 6. announcements
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS author_id BIGINT REFERENCES users(id) ON DELETE SET NULL");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS title VARCHAR(255)");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS content TEXT");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS category VARCHAR(50) DEFAULT 'GENERAL'");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS attachment_path VARCHAR(500)");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE");
        jdbcTemplate.execute("ALTER TABLE announcements ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP");

        // 7. registration_periods column type drop not null
        jdbcTemplate.execute("ALTER TABLE registration_periods ALTER COLUMN type DROP NOT NULL");

        System.out.println("====== [ALL DB SCHEMAS FULLY ENSURED] ======");
    }
}
