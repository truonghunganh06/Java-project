package topic_management.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Announcement;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findByDepartmentId(Long departmentId);

    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findByPeriodId(Long periodId);

    @EntityGraph(attributePaths = {"department", "period", "author"})
    List<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc();
}
