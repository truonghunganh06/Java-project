package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Student;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    @Override
    Optional<Student> findById(Long id);

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    @Override
    List<Student> findAll();

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    Optional<Student> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    Optional<Student> findByStudentCode(String studentCode);

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    List<Student> findByMajorId(Long majorId);
    
    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    @Query("SELECT s FROM Student s WHERE s.division.id = :divisionId")
    List<Student> findByDivisionId(@Param("divisionId") Long divisionId);
    
    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    @Query("SELECT s FROM Student s WHERE (s.major IS NOT NULL AND s.major.department.id = :departmentId) OR (s.division IS NOT NULL AND s.division.department.id = :departmentId)")
    List<Student> findByMajorDepartmentId(@Param("departmentId") Long departmentId);

    @EntityGraph(attributePaths = {"user", "major", "major.department", "major.division", "division", "division.department"})
    @Query("SELECT s FROM Student s WHERE (s.division IS NOT NULL AND s.division.id = :divisionId) OR (s.major IS NOT NULL AND s.major.division IS NOT NULL AND s.major.division.id = :divisionId)")
    List<Student> findByDivisionOrMajorDivisionId(@Param("divisionId") Long divisionId);
}
