package topicmanagement.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Lecturer;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByUserId(Long userId);
    Optional<Lecturer> findByLecturerCode(String lecturerCode);
    List<Lecturer> findByDepartmentId(Long departmentId);
    
    @Query("SELECT l FROM Lecturer l WHERE l.division.id = :divisionId")
    List<Lecturer> findByDivisionId(@Param("divisionId") Long divisionId);
}
