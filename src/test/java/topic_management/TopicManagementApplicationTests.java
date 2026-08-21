package topic_management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
class TopicManagementApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void executeSql() {
        System.out.println("====== [ALL TOPICS IN DB] ======");
        List<Map<String, Object>> topics = jdbcTemplate.queryForList("SELECT id, code, title, status, supervisor_id, major_id FROM topics");
        for (Map<String, Object> t : topics) {
            System.out.println("Topic -> " + t);
        }

        System.out.println("====== [ALL REGISTRATIONS IN DB] ======");
        List<Map<String, Object>> regs = jdbcTemplate.queryForList("SELECT id, topic_id, student_id, status FROM topic_registrations");
        for (Map<String, Object> r : regs) {
            System.out.println("Reg -> " + r);
        }

        System.out.println("====== [ALL SUBMISSIONS IN DB] ======");
        List<Map<String, Object>> subs = jdbcTemplate.queryForList("SELECT id, topic_id, student_id, title, grade, feedback FROM submissions");
        for (Map<String, Object> s : subs) {
            System.out.println("Sub -> " + s);
        }

        System.out.println("====== [ALL USERS IN DB] ======");
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, username, full_name, email, role_id FROM users");
        for (Map<String, Object> u : users) {
            System.out.println("User -> " + u);
        }
    }
}


