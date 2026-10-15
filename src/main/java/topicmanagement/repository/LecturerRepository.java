package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Lecturer;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    @Override
    Optional<Lecturer> findById(Long id);

    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    @Override
    List<Lecturer> findAll();

    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    Optional<Lecturer> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    Optional<Lecturer> findByLecturerCode(String lecturerCode);

    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    List<Lecturer> findByDepartmentId(Long departmentId);
    
    @EntityGraph(attributePaths = {"user", "department", "division", "division.department"})
    @Query("SELECT l FROM Lecturer l WHERE l.division.id = :divisionId")
    List<Lecturer> findByDivisionId(@Param("divisionId") Long divisionId);
}
