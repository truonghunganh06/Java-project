package topicmanagement.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import topicmanagement.entity.Announcement;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findByPeriodId(Long periodId);

    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"department", "period", "author"})
    @org.springframework.data.jpa.repository.Query("SELECT a FROM Announcement a WHERE a.department IS NULL OR a.department.id = :departmentId ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<Announcement> findByDepartmentIdOrGlobalOrderByIsPinnedDescCreatedAtDesc(@org.springframework.data.repository.query.Param("departmentId") Long departmentId);
}
