package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import topicmanagement.entity.Major;
import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {
    List<Major> findByIsActiveTrue();
    List<Major> findByDepartmentId(Long departmentId);
    
    @Query("SELECT m FROM Major m WHERE m.division.id = :divisionId")
    List<Major> findByDivisionId(@Param("divisionId") Long divisionId);
    
    @Query("SELECT m FROM Major m WHERE m.division.id = :divisionId AND m.isActive = true")
    List<Major> findByDivisionIdAndIsActiveTrue(@Param("divisionId") Long divisionId);
    
    Optional<Major> findByCode(String code);
    Optional<Major> findByCodeIgnoreCase(String code);
}
