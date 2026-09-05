package topic_management.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.TopicRegistration;

import java.util.List;
import java.util.Optional;

/**
 * BUG-J FIX: Repository được cập nhật để phản ánh việc TopicRegistration
 * đã đổi từ student_id sang group_id.
 */
@Repository
public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    @Override
    Optional<TopicRegistration> findById(Long id);

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    @Override
    List<TopicRegistration> findAll();

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    Optional<TopicRegistration> findByTopicIdAndGroupId(Long topicId, Long groupId);

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByTopicSupervisorId(Long supervisorId);

    @EntityGraph(attributePaths = {"topic", "group", "group.leader", "registeredBy", "topic.supervisor", "topic.supervisor.user"})
    List<TopicRegistration> findByStatus(String status);

    @Modifying
    @Query("DELETE FROM TopicRegistration tr WHERE tr.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
