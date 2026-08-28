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
    @Query("SELECT g FROM StudentGroup g WHERE g.period.department.id = :departmentId")
    List<StudentGroup> findByPeriodDepartmentId(@Param("departmentId") Long departmentId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Query("SELECT g FROM StudentGroup g WHERE g.period.department.id = :departmentId")
    Page<StudentGroup> findByPeriodDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Query("SELECT g FROM StudentGroup g WHERE g.period.department.id = :departmentId AND g.period.id = :periodId")
    List<StudentGroup> findByPeriodDepartmentIdAndPeriodId(@Param("departmentId") Long departmentId, @Param("periodId") Long periodId);

    @EntityGraph(attributePaths = {"period", "leader", "topic"})
    @Query("SELECT g FROM StudentGroup g WHERE g.period.department.id = :departmentId AND g.period.id = :periodId")
    Page<StudentGroup> findByPeriodDepartmentIdAndPeriodId(@Param("departmentId") Long departmentId, @Param("periodId") Long periodId, Pageable pageable);

    /**
     * BUG-K FIX: Đếm số nhóm (không bị REJECTED/DRAFT) đang hướng dẫn bởi GV chính
     * trong một đợt cụ thể — dùng để kiểm tra maxTopicsQuota.
     */
    @Query("SELECT COUNT(g) FROM StudentGroup g WHERE g.topic.supervisor.id = :supervisorId " +
           "AND g.status NOT IN :excludedStatuses " +
           "AND (:periodId IS NULL OR g.period.id = :periodId)")
    long countActiveGroupsBySupervisorInPeriod(
            @Param("supervisorId") Long supervisorId,
            @Param("periodId") Long periodId,
            @Param("excludedStatuses") List<String> excludedStatuses);

    /**
     * BUG-K FIX: Đếm số nhóm (không bị REJECTED/DRAFT) đang hướng dẫn bởi GV phụ
     * trong một đợt cụ thể — dùng để kiểm tra maxTopicsQuota của coSupervisor.
     */
    @Query("SELECT COUNT(g) FROM StudentGroup g WHERE g.topic.coSupervisor IS NOT NULL " +
           "AND g.topic.coSupervisor.id = :coSupervisorId " +
           "AND g.status NOT IN :excludedStatuses " +
           "AND (:periodId IS NULL OR g.period.id = :periodId)")
    long countActiveGroupsByCoSupervisorInPeriod(
            @Param("coSupervisorId") Long coSupervisorId,
            @Param("periodId") Long periodId,
            @Param("excludedStatuses") List<String> excludedStatuses);
}
