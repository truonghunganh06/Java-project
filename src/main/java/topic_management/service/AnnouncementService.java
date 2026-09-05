package topic_management.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final RegistrationPeriodRepository periodRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            RegistrationPeriodRepository periodRepository) {
        this.announcementRepository = announcementRepository;
        this.periodRepository = periodRepository;
    }

    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByIsPinnedDescCreatedAtDesc();
    }

    public List<Announcement> getActiveAnnouncements() {
        return getAllAnnouncements();
    }

    public List<Announcement> getAnnouncementsByDepartment(Long departmentId) {
        if (departmentId == null) {
            return getAllAnnouncements();
        }
        return announcementRepository.findByDepartmentId(departmentId);
    }

    public List<Announcement> getAnnouncementsByPeriod(Long periodId) {
        return announcementRepository.findByPeriodId(periodId);
    }

    public Announcement getAnnouncementById(Long id) {
        if (id == null) return null;
        return announcementRepository.findById(id).orElse(null);
    }

    @Transactional
    public Announcement createAnnouncement(AppUser author, Department department, Long periodId, String title, String content, String category, Boolean isPinned, String attachmentPath) {
        Announcement a = new Announcement();
        a.setAuthor(author);
        a.setDepartment(department);
        if (periodId != null) {
            a.setPeriod(periodRepository.findById(periodId).orElse(null));
        }
        a.setTitle(title);
        a.setContent(content);
        a.setCategory(category != null ? category : "GENERAL");
        a.setIsPinned(Boolean.TRUE.equals(isPinned));
        a.setAttachmentPath(attachmentPath);
        a.setCreatedAt(OffsetDateTime.now());

        Announcement saved = announcementRepository.save(a);
        System.out.println("[DB-ANNOUNCEMENT] Đã tạo thông báo mới: '" + title + "' (ID=" + saved.getId() + ")");
        return saved;
    }

    @Transactional
    public boolean deleteAnnouncement(Long id) {
        if (id == null || !announcementRepository.existsById(id)) return false;
        announcementRepository.deleteById(id);
        System.out.println("[DB-ANNOUNCEMENT] Đã xóa thông báo ID=" + id);
        return true;
    }
}
