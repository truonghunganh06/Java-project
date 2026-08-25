package topic_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.TopicRegistration;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    @Override
    Optional<TopicRegistration> findById(Long id);

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    @Override
    List<TopicRegistration> findAll();

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    @Override
    Page<TopicRegistration> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByTopicSupervisorId(Long supervisorId);

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    Page<TopicRegistration> findByTopicSupervisorId(Long supervisorId, Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "student", "student.user", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByStudentId(Long studentId);
}
