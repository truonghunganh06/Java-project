package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.RegistrationPeriod;

import java.util.List;

@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {
    List<RegistrationPeriod> findByDepartmentId(Long departmentId);
    List<RegistrationPeriod> findByIsActiveTrueOrderByIdDesc();
    List<RegistrationPeriod> findByPeriodType(String periodType);
}
