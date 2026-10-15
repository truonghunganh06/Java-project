package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Evaluation;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    @Override
    Optional<Evaluation> findById(Long id);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    List<Evaluation> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    List<Evaluation> findByTopicIdIn(List<Long> topicIds);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    List<Evaluation> findByLecturerId(Long lecturerId);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    List<Evaluation> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    Optional<Evaluation> findByTopicIdAndLecturerIdAndEvaluationType(Long topicId, Long lecturerId, String evaluationType);

    @EntityGraph(attributePaths = {"topic", "lecturer", "lecturer.user", "group"})
    Optional<Evaluation> findByTopicIdAndLecturerId(Long topicId, Long lecturerId);
}
