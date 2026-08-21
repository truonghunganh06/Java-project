package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.Topic;

import java.util.List;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySupervisorId(Long supervisorId);
    List<Topic> findByReviewerId(Long reviewerId);
    List<Topic> findByCouncilId(Long councilId);
    List<Topic> findByPeriodId(Long periodId);
    List<Topic> findByStatus(String status);
    List<Topic> findByMajorId(Long majorId);
    List<Topic> findByMajorDepartmentId(Long departmentId);
    List<Topic> findByMajorDepartmentIdAndStatus(Long departmentId, String status);

    @Query("SELECT t FROM Topic t WHERE t.supervisor.id = :lecturerId OR (t.coSupervisor IS NOT NULL AND t.coSupervisor.id = :lecturerId)")
    List<Topic> findBySupervisorOrCoSupervisorId(@Param("lecturerId") Long lecturerId);

    @Query("SELECT t FROM Topic t WHERE t.period.id = :periodId AND t.status = 'APPROVED'")
    List<Topic> findApprovedByPeriodId(@Param("periodId") Long periodId);

    boolean existsByCode(String code);
    long countByMajorDepartmentIdAndStatus(Long departmentId, String status);
    long countByMajorDepartmentId(Long departmentId);
    long countBySupervisorId(Long supervisorId);
}
