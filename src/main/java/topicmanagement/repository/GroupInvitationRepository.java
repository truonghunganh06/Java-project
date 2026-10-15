package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.GroupInvitation;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    @Override
    Optional<GroupInvitation> findById(Long id);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    @Override
    List<GroupInvitation> findAll();

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    List<GroupInvitation> findByStudentIdAndStatus(Long studentId, String status);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    List<GroupInvitation> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    List<GroupInvitation> findByGroupIdAndStatus(Long groupId, String status);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    List<GroupInvitation> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"group", "student", "student.user", "invitedBy", "invitedBy.user"})
    Optional<GroupInvitation> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, String status);

    @Modifying
    @Query("DELETE FROM GroupInvitation gi WHERE gi.student.id = :studentId AND gi.status = 'INVITED'")
    void deletePendingInvitationsForStudent(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM GroupInvitation gi WHERE gi.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
