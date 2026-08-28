package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.RegistrationPeriod;

import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    @Query("SELECT p FROM RegistrationPeriod p WHERE p.department.id = :departmentId")
    List<RegistrationPeriod> findByDepartmentId(@Param("departmentId") Long departmentId);
    
    List<RegistrationPeriod> findByIsActiveTrueOrderByIdDesc();
    List<RegistrationPeriod> findByPeriodType(String periodType);
}
