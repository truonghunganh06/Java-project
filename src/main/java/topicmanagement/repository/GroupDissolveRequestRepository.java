package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.GroupDissolveRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupDissolveRequestRepository extends JpaRepository<GroupDissolveRequest, Long> {

    @EntityGraph(attributePaths = {"group", "requestedBy", "requestedBy.user", "topic", "resolvedBy", "resolvedBy.user"})
    @Override
    Optional<GroupDissolveRequest> findById(Long id);

    @EntityGraph(attributePaths = {"group", "requestedBy", "requestedBy.user", "topic", "resolvedBy", "resolvedBy.user"})
    @Override
    List<GroupDissolveRequest> findAll();

    @EntityGraph(attributePaths = {"group", "requestedBy", "requestedBy.user", "topic", "resolvedBy", "resolvedBy.user"})
    List<GroupDissolveRequest> findByGroupIdAndStatus(Long groupId, String status);

    @EntityGraph(attributePaths = {"group", "requestedBy", "requestedBy.user", "topic", "resolvedBy", "resolvedBy.user"})
    Optional<GroupDissolveRequest> findByGroupIdAndRequestedByIdAndStatus(Long groupId, Long requestedById, String status);

    @EntityGraph(attributePaths = {"group", "requestedBy", "requestedBy.user", "topic", "resolvedBy", "resolvedBy.user"})
    @Query("SELECT r FROM GroupDissolveRequest r WHERE r.topic IS NOT NULL AND (r.topic.supervisor.id = :supervisorId OR (r.topic.coSupervisor IS NOT NULL AND r.topic.coSupervisor.id = :supervisorId)) AND r.status = :status")
    List<GroupDissolveRequest> findBySupervisorIdAndStatus(@Param("supervisorId") Long supervisorId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM GroupDissolveRequest r WHERE r.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
