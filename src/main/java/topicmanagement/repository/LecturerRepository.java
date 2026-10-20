package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import topicmanagement.entity.Lecturer;
import java.util.List;
import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByUserId(Long userId);
    Optional<Lecturer> findByLecturerCode(String lecturerCode);
    
    @Query("SELECT l FROM Lecturer l WHERE l.department IS NOT NULL AND l.department.id = :departmentId")
    List<Lecturer> findByDepartmentId(@Param("departmentId") Long departmentId);
}
