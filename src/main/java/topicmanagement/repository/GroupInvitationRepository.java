package topicmanagement.repository;

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

    List<GroupInvitation> findByStudentIdAndStatus(Long studentId, String status);

    List<GroupInvitation> findByStudentId(Long studentId);

    List<GroupInvitation> findByGroupIdAndStatus(Long groupId, String status);

    List<GroupInvitation> findByGroupId(Long groupId);

    Optional<GroupInvitation> findByGroupIdAndStudentIdAndStatus(Long groupId, Long studentId, String status);

    @Modifying
    @Query("DELETE FROM GroupInvitation gi WHERE gi.student.id = :studentId AND gi.status = 'INVITED'")
    void deletePendingInvitationsForStudent(@Param("studentId") Long studentId);

    @Modifying
    @Query("DELETE FROM GroupInvitation gi WHERE gi.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}
