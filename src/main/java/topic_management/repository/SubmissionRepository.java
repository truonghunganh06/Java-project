package topic_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.Submission;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    @Override
    Optional<Submission> findById(Long id);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    @Override
    List<Submission> findAll();

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    @Override
    Page<Submission> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    List<Submission> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    List<Submission> findByGroupId(Long groupId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    Page<Submission> findByGroupId(Long groupId, Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    List<Submission> findByTopicSupervisorId(Long supervisorId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    Page<Submission> findByTopicSupervisorId(Long supervisorId, Pageable pageable);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    List<Submission> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    List<Submission> findByStudentIdAndTopicId(Long studentId, Long topicId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    @Query("SELECT s FROM Submission s WHERE s.topic.supervisor.id = :lecturerId OR (s.topic.coSupervisor IS NOT NULL AND s.topic.coSupervisor.id = :lecturerId)")
    List<Submission> findBySupervisorOrCoSupervisorId(@Param("lecturerId") Long lecturerId);

    @EntityGraph(attributePaths = {"topic", "group", "student", "student.user", "submittedByStudent", "submittedByStudent.user"})
    @Query("SELECT s FROM Submission s WHERE s.topic.supervisor.id = :lecturerId OR (s.topic.coSupervisor IS NOT NULL AND s.topic.coSupervisor.id = :lecturerId)")
    Page<Submission> findBySupervisorOrCoSupervisorId(@Param("lecturerId") Long lecturerId, Pageable pageable);
}
