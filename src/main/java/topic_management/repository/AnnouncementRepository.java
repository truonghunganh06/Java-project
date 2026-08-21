package topic_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import topic_management.entity.Announcement;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByDepartmentId(Long departmentId);
    List<Announcement> findByPeriodId(Long periodId);
    List<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc();
}
