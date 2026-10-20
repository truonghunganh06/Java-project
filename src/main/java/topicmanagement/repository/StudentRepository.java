package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import topicmanagement.entity.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByStudentCode(String studentCode);
    
    @Query("SELECT s FROM Student s WHERE s.department IS NOT NULL AND s.department.id = :departmentId")
    List<Student> findByDepartmentId(@Param("departmentId") Long departmentId);
}
