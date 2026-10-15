package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Major;
import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {

    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    @Override
    Optional<Major> findById(Long id);

    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    @Override
    List<Major> findAll();

    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    List<Major> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    List<Major> findByDepartmentId(Long departmentId);
    
    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    @Query("SELECT m FROM Major m WHERE m.division.id = :divisionId")
    List<Major> findByDivisionId(@Param("divisionId") Long divisionId);
    
    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    @Query("SELECT m FROM Major m WHERE m.division.id = :divisionId AND m.isActive = true")
    List<Major> findByDivisionIdAndIsActiveTrue(@Param("divisionId") Long divisionId);
    
    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    Optional<Major> findByCode(String code);

    @EntityGraph(attributePaths = {"department", "division", "division.department"})
    Optional<Major> findByCodeIgnoreCase(String code);
}
