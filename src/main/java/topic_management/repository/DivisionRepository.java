package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Division;

import java.util.List;
import java.util.Optional;

@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {
    List<Division> findByDepartmentId(Long departmentId);
    Optional<Division> findByCodeIgnoreCase(String code);
    Optional<Division> findByCode(String code);
    boolean existsByCode(String code);
    List<Division> findByIsActiveTrue();
    List<Division> findByDepartmentIdAndIsActiveTrue(Long departmentId);
}
