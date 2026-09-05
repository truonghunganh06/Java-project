package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.GroupDissolveRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupDissolveRequestRepository extends JpaRepository<GroupDissolveRequest, Long> {

    List<GroupDissolveRequest> findByGroupIdAndStatus(Long groupId, String status);

    Optional<GroupDissolveRequest> findByGroupIdAndRequestedByIdAndStatus(Long groupId, Long requestedById, String status);

    @Query("SELECT r FROM GroupDissolveRequest r WHERE r.topic IS NOT NULL AND (r.topic.supervisor.id = :supervisorId OR (r.topic.coSupervisor IS NOT NULL AND r.topic.coSupervisor.id = :supervisorId)) AND r.status = :status")
    List<GroupDissolveRequest> findBySupervisorIdAndStatus(@Param("supervisorId") Long supervisorId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM GroupDissolveRequest r WHERE r.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
