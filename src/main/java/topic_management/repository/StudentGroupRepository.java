package topic_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.StudentGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Override
    Optional<StudentGroup> findById(Long id);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Override
    List<StudentGroup> findAll();

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Override
    Page<StudentGroup> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    Optional<StudentGroup> findByGroupCode(String groupCode);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    List<StudentGroup> findByPeriodId(Long periodId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    List<StudentGroup> findByTopicId(Long topicId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    List<StudentGroup> findByTopicIdIn(List<Long> topicIds);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.student.id = :studentId AND (:periodId IS NULL OR g.period.id = :periodId)")
    List<StudentGroup> findByStudentIdAndPeriodId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.student.id = :studentId")
    List<StudentGroup> findByStudentId(@Param("studentId") Long studentId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    List<StudentGroup> findByPeriodDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    Page<StudentGroup> findByPeriodDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    List<StudentGroup> findByPeriodDepartmentIdAndPeriodId(Long departmentId, Long periodId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    Page<StudentGroup> findByPeriodDepartmentIdAndPeriodId(Long departmentId, Long periodId, Pageable pageable);
}
