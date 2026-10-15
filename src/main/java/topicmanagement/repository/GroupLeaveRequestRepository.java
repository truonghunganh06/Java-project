package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    @Override
    Optional<GroupLeaveRequest> findById(Long id);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    @Override
    List<GroupLeaveRequest> findAll();

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    List<GroupLeaveRequest> findByGroupIdAndStatus(Long groupId, String status);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    List<GroupLeaveRequest> findByStudentIdAndStatus(Long studentId, String status);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    Optional<GroupLeaveRequest> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, String status);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "resolvedBy", "resolvedBy.user"})
    @Query("SELECT r FROM GroupLeaveRequest r WHERE r.group.topic IS NOT NULL AND (r.group.topic.supervisor.id = :supervisorId OR (r.group.topic.coSupervisor IS NOT NULL AND r.group.topic.coSupervisor.id = :supervisorId)) AND r.status = :status")
    List<GroupLeaveRequest> findBySupervisorIdAndStatus(@Param("supervisorId") Long supervisorId, @Param("status") String status);

    @Modifying
    @Query("DELETE FROM GroupLeaveRequest r WHERE r.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
