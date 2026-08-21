package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.CouncilMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    List<CouncilMember> findByCouncilId(Long councilId);
    List<CouncilMember> findByLecturerId(Long lecturerId);
    Optional<CouncilMember> findByCouncilIdAndLecturerId(Long councilId, Long lecturerId);
    void deleteByCouncilId(Long councilId);
}
