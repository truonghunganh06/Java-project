package topic_management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.Topic;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Override
    Optional<Topic> findById(Long id);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Override
    List<Topic> findAll();

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Override
    Page<Topic> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findBySupervisorId(Long supervisorId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByReviewerId(Long reviewerId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByCouncilId(Long councilId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByPeriodId(Long periodId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByStatus(String status);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    Page<Topic> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByMajorId(Long majorId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE t.division.id = :divisionId")
    List<Topic> findByDivisionId(@Param("divisionId") Long divisionId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE t.division.id = :divisionId AND t.status = :status")
    List<Topic> findByDivisionIdAndStatus(@Param("divisionId") Long divisionId, @Param("status") String status);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE t.division.id = :divisionId AND t.period.id = :periodId")
    List<Topic> findByDivisionIdAndPeriodId(@Param("divisionId") Long divisionId, @Param("periodId") Long periodId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByMajorDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    Page<Topic> findByMajorDepartmentId(Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    List<Topic> findByMajorDepartmentIdAndStatus(Long departmentId, String status);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE t.supervisor.id = :lecturerId OR (t.coSupervisor IS NOT NULL AND t.coSupervisor.id = :lecturerId)")
    List<Topic> findBySupervisorOrCoSupervisorId(@Param("lecturerId") Long lecturerId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE t.period.id = :periodId AND t.status = 'APPROVED'")
    List<Topic> findApprovedByPeriodId(@Param("periodId") Long periodId);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE (:departmentId IS NULL OR (t.division IS NOT NULL AND t.division.department.id = :departmentId) OR (t.major IS NOT NULL AND t.major.department.id = :departmentId)) " +
           "AND (:divisionId IS NULL OR (t.division IS NOT NULL AND t.division.id = :divisionId)) " +
           "AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR t.status = :status) " +
           "AND (:majorId IS NULL OR t.major.id = :majorId) " +
           "AND (:periodId IS NULL OR t.period.id = :periodId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (t.supervisor.user.fullName IS NOT NULL AND LOWER(t.supervisor.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Topic> findDepartmentTopicsPaged(
            @Param("departmentId") Long departmentId,
            @Param("divisionId") Long divisionId,
            @Param("status") String status,
            @Param("majorId") Long majorId,
            @Param("periodId") Long periodId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = {"division", "division.department", "major", "major.department", "supervisor", "supervisor.user", "coSupervisor", "coSupervisor.user", "reviewer", "reviewer.user", "period", "council"})
    @Query("SELECT t FROM Topic t WHERE (:divisionId IS NULL OR (t.division IS NOT NULL AND t.division.id = :divisionId)) " +
           "AND (:status IS NULL OR :status = '' OR :status = 'ALL' OR t.status = :status) " +
           "AND (:majorId IS NULL OR t.major.id = :majorId) " +
           "AND (:periodId IS NULL OR t.period.id = :periodId) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (t.supervisor.user.fullName IS NOT NULL AND LOWER(t.supervisor.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))))")
    Page<Topic> findDivisionTopicsPaged(
            @Param("divisionId") Long divisionId,
            @Param("status") String status,
            @Param("majorId") Long majorId,
            @Param("periodId") Long periodId,
            @Param("keyword") String keyword,
            Pageable pageable);

    boolean existsByCode(String code);
    long countByMajorDepartmentIdAndStatus(Long departmentId, String status);
    long countByMajorDepartmentId(Long departmentId);
    
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.division.id = :divisionId")
    long countByDivisionId(@Param("divisionId") Long divisionId);
    
    @Query("SELECT COUNT(t) FROM Topic t WHERE t.division.id = :divisionId AND t.status = :status")
    long countByDivisionIdAndStatus(@Param("divisionId") Long divisionId, @Param("status") String status);
    
    long countBySupervisorId(Long supervisorId);
}
