package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.GroupMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByStudentId(Long studentId);
    Optional<GroupMember> findByGroupIdAndStudentId(Long groupId, Long studentId);
    void deleteByGroupIdAndStudentId(Long groupId, Long studentId);
    void deleteByStudentId(Long studentId);
}
