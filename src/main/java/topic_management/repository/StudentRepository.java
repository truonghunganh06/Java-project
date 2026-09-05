package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topic_management.entity.Student;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUserId(Long userId);
    Optional<Student> findByStudentCode(String studentCode);
    List<Student> findByMajorId(Long majorId);
    
    @Query("SELECT s FROM Student s WHERE s.division.id = :divisionId")
    List<Student> findByDivisionId(@Param("divisionId") Long divisionId);
    
    @Query("SELECT s FROM Student s WHERE (s.major IS NOT NULL AND s.major.department.id = :departmentId) OR (s.division IS NOT NULL AND s.division.department.id = :departmentId)")
    List<Student> findByMajorDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT s FROM Student s WHERE (s.division IS NOT NULL AND s.division.id = :divisionId) OR (s.major IS NOT NULL AND s.major.division IS NOT NULL AND s.major.division.id = :divisionId)")
    List<Student> findByDivisionOrMajorDivisionId(@Param("divisionId") Long divisionId);
}
