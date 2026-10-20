package topicmanagement.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import topicmanagement.entity.*;
import topicmanagement.repository.*;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class AnnouncementService {
    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepository;
    private final RegistrationPeriodRepository periodRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            RegistrationPeriodRepository periodRepository) {
        this.announcementRepository = announcementRepository;
        this.periodRepository = periodRepository;
    }

    @Scheduled(fixedDelayString = "${app.deadline-check-delay-ms:60000}", initialDelayString = "${app.deadline-check-initial-delay-ms:10000}")
    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public void createDeadlineReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        for (RegistrationPeriod period : periodRepository.findByIsActiveTrueOrderByIdDesc()) {
            if (!"OPEN".equalsIgnoreCase(period.getStatus()) && !"IN_PROGRESS".equalsIgnoreCase(period.getStatus())) continue;
            remindDeadline(period, "PROPOSAL", "đề xuất đề tài", period.getLecturerProposalEnd(), now);
            remindDeadline(period, "REGISTRATION", "đăng ký đề tài", period.getStudentRegistrationEnd(), now);
            if ("TLCN".equalsIgnoreCase(period.getPeriodType()) || "KLTN".equalsIgnoreCase(period.getPeriodType())) {
                remindDeadline(period, "REVIEWER", "chấm phản biện", period.getReviewerDeadline(), now);
            }
            if ("KLTN".equalsIgnoreCase(period.getPeriodType())) {
                remindDeadline(period, "DEFENSE", "bảo vệ hội đồng", period.getCouncilDefenseDate(), now);
            }
        }
    }

    private void remindDeadline(RegistrationPeriod period, String kind, String label, OffsetDateTime deadline, OffsetDateTime now) {
        if (deadline == null || now.isBefore(deadline.minusHours(24))) return;
        boolean reached = !now.isBefore(deadline);
        String eventKey = "deadline:" + period.getId() + ":" + kind + ":" + deadline.toInstant()
                + (reached ? ":DUE" : ":SOON");
        String title = (reached ? "Đã đến hạn " : "Nhắc hạn ") + label + ": " + period.getName();
        String formattedDeadline = deadline.atZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        int inserted = announcementRepository.insertDeadlineOnce(eventKey,
                period.getDepartment() != null ? period.getDepartment().getId() : null, period.getId(), title,
                "Đợt " + period.getName() + ": hạn " + label + " là " + formattedDeadline
                        + " (giờ Việt Nam). " + (reached ? "Mốc thời gian này đã đến hoặc đã qua." : "Vui lòng hoàn tất trước hạn."), now);
        if (inserted > 0) log.info("Đã lưu thông báo hạn {} cho đợt {}", kind, period.getId());
    }

    @Cacheable("announcements")
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

    public List<Announcement> getAnnouncementsForDepartment(Long departmentId) {
        if (departmentId == null) {
            return getAllAnnouncements();
        }
        return announcementRepository.findByDepartmentIdOrGlobalOrderByIsPinnedDescCreatedAtDesc(departmentId);
    }

    public List<Announcement> getAnnouncementsByPeriod(Long periodId) {
        return announcementRepository.findByPeriodId(periodId);
    }

    public Announcement getAnnouncementById(Long id) {
        if (id == null) return null;
        return announcementRepository.findById(id).orElse(null);
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
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
        Announcement saved = announcementRepository.save(a);
        System.out.println("[DB-ANNOUNCEMENT] Đã tạo thông báo mới: '" + title + "' (ID=" + saved.getId() + ")");
        return saved;
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public boolean deleteAnnouncement(Long id) {
        if (id == null || !announcementRepository.existsById(id)) return false;
        announcementRepository.deleteById(id);
        System.out.println("[DB-ANNOUNCEMENT] Đã xóa thông báo ID=" + id);
        return true;
    }

    @Transactional
    @CacheEvict(value = "announcements", allEntries = true)
    public boolean togglePin(Long id) {
        Announcement a = getAnnouncementById(id);
        if (a == null) return false;
        a.setIsPinned(!Boolean.TRUE.equals(a.getIsPinned()));
        announcementRepository.save(a);
        System.out.println("[DB-ANNOUNCEMENT] Đã đổi trạng thái ghim thông báo ID=" + id + " -> " + a.getIsPinned());
        return true;
    }
}
