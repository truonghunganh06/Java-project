package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Evaluation;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByTopicId(Long topicId);
    List<Evaluation> findByLecturerId(Long lecturerId);
    List<Evaluation> findByGroupId(Long groupId);
    Optional<Evaluation> findByTopicIdAndLecturerIdAndEvaluationType(Long topicId, Long lecturerId, String evaluationType);
    Optional<Evaluation> findByTopicIdAndLecturerId(Long topicId, Long lecturerId);
}
