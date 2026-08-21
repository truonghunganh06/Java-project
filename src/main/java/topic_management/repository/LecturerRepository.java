package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Lecturer;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByUserId(Long userId);
    Optional<Lecturer> findByLecturerCode(String lecturerCode);
    List<Lecturer> findByDepartmentId(Long departmentId);
}

