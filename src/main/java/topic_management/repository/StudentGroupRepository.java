package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.StudentGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    Optional<StudentGroup> findByGroupCode(String groupCode);
    List<StudentGroup> findByPeriodId(Long periodId);
    List<StudentGroup> findByTopicId(Long topicId);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.student.id = :studentId AND (:periodId IS NULL OR g.period.id = :periodId)")
    List<StudentGroup> findByStudentIdAndPeriodId(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    @Query("SELECT g FROM StudentGroup g JOIN g.members m WHERE m.student.id = :studentId")
    List<StudentGroup> findByStudentId(@Param("studentId") Long studentId);

    List<StudentGroup> findByPeriodDepartmentId(Long departmentId);
    List<StudentGroup> findByPeriodDepartmentIdAndPeriodId(Long departmentId, Long periodId);
}
