package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.RegistrationPeriod;

import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    @EntityGraph(attributePaths = {"department"})
    @Override
    java.util.Optional<RegistrationPeriod> findById(Long id);

    @EntityGraph(attributePaths = {"department"})
    @Override
    List<RegistrationPeriod> findAll();

    @EntityGraph(attributePaths = {"department"})
    @Query("SELECT p FROM RegistrationPeriod p WHERE p.department.id = :departmentId AND p.isActive = true ORDER BY p.id DESC")
    List<RegistrationPeriod> findByDepartmentId(@Param("departmentId") Long departmentId);
    
    @EntityGraph(attributePaths = {"department"})
    List<RegistrationPeriod> findByIsActiveTrueOrderByIdDesc();

    @EntityGraph(attributePaths = {"department"})
    List<RegistrationPeriod> findByPeriodType(String periodType);
}
