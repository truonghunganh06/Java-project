package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Major;
import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    List<Major> findByIsActiveTrue();
    List<Major> findByDepartmentId(Long departmentId);
    Optional<Major> findByCode(String code);
}

