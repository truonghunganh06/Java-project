package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topicmanagement.entity.Division;

import java.util.List;
import java.util.Optional;

@Repository
public interface DivisionRepository extends JpaRepository<Division, Long> {

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    @Override
    Optional<Division> findById(Long id);

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    @Override
    List<Division> findAll();

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    List<Division> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    Optional<Division> findByCodeIgnoreCase(String code);

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    Optional<Division> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    List<Division> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"department", "headLecturer", "headLecturer.user"})
    List<Division> findByDepartmentIdAndIsActiveTrue(Long departmentId);
}
