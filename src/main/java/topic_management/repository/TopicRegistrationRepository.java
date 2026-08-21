package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.TopicRegistration;
import java.util.List;

@Repository
public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
    List<TopicRegistration> findByTopicSupervisorId(Long supervisorId);
    List<TopicRegistration> findByTopicId(Long topicId);
    List<TopicRegistration> findByStudentId(Long studentId);
}
