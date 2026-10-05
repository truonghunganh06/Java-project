package topicmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class TopicManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TopicManagementApplication.class, args);
    }

}