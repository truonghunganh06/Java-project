package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.Council;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {
    Optional<Council> findByCode(String code);
    List<Council> findByPeriodId(Long periodId);
    List<Council> findByDepartmentId(Long departmentId);

    @Query("SELECT DISTINCT c FROM Council c JOIN c.members m WHERE m.lecturer.id = :lecturerId")
    List<Council> findByLecturerId(@Param("lecturerId") Long lecturerId);
}
