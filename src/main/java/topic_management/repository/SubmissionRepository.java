package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Submission;
import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudentId(Long studentId);
    List<Submission> findByTopicSupervisorId(Long supervisorId);
    List<Submission> findByTopicId(Long topicId);
    List<Submission> findByStudentIdAndTopicId(Long studentId, Long topicId);
}
