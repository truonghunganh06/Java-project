package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.GroupLeaveRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupLeaveRequestRepository extends JpaRepository<GroupLeaveRequest, Long> {

    List<GroupLeaveRequest> findByGroupIdAndStatus(Long groupId, String status);

    List<GroupLeaveRequest> findByStudentIdAndStatus(Long studentId, String status);

    Optional<GroupLeaveRequest> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, String status);

    @Query("SELECT r FROM GroupLeaveRequest r WHERE r.group.topic IS NOT NULL AND (r.group.topic.supervisor.id = :supervisorId OR (r.group.topic.coSupervisor IS NOT NULL AND r.group.topic.coSupervisor.id = :supervisorId)) AND r.status = :status")
    List<GroupLeaveRequest> findBySupervisorIdAndStatus(@Param("supervisorId") Long supervisorId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM GroupLeaveRequest r WHERE r.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
