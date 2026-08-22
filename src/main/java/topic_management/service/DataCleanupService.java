package topic_management.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.util.*;

@Service
public class DataCleanupService {

    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final CouncilRepository councilRepository;
    private final StudentGroupRepository groupRepository;
    private final AnnouncementRepository announcementRepository;

    public DataCleanupService(
            RegistrationPeriodRepository periodRepository,
            TopicRepository topicRepository,
            CouncilRepository councilRepository,
            StudentGroupRepository groupRepository,
            AnnouncementRepository announcementRepository) {
        this.periodRepository = periodRepository;
        this.topicRepository = topicRepository;
        this.councilRepository = councilRepository;
        this.groupRepository = groupRepository;
        this.announcementRepository = announcementRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupDuplicatePeriodsOnStartup() {
        try {
            List<RegistrationPeriod> allPeriods = periodRepository.findAll();
            if (allPeriods.isEmpty()) return;

            // Group periods by departmentId + periodType to keep exactly 1 primary period per type per department
            Map<String, List<RegistrationPeriod>> grouped = new HashMap<>();
            for (RegistrationPeriod p : allPeriods) {
                String deptId = (p.getDepartment() != null) ? String.valueOf(p.getDepartment().getId()) : "0";
                String pType = (p.getPeriodType() != null && !p.getPeriodType().trim().isEmpty()) ? p.getPeriodType().trim().toUpperCase() : "KLTN";
                String key = deptId + "|" + pType;

                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
            }

            int cleanedCount = 0;
            for (Map.Entry<String, List<RegistrationPeriod>> entry : grouped.entrySet()) {
                List<RegistrationPeriod> list = entry.getValue();
                if (list.size() > 1) {
                    // Sort: prioritize non-test periods, then active ones, then lower ID
                    list.sort((a, b) -> {
                        boolean aIsTest = a.getName() != null && (a.getName().toLowerCase().contains("test") || a.getName().toLowerCase().contains("tự động"));
                        boolean bIsTest = b.getName() != null && (b.getName().toLowerCase().contains("test") || b.getName().toLowerCase().contains("tự động"));
                        if (aIsTest != bIsTest) {
                            return aIsTest ? 1 : -1;
                        }
                        boolean aActive = Boolean.TRUE.equals(a.getIsActive());
                        boolean bActive = Boolean.TRUE.equals(b.getIsActive());
                        if (aActive != bActive) {
                            return aActive ? -1 : 1;
                        }
                        return a.getId().compareTo(b.getId());
                    });

                    RegistrationPeriod primary = list.get(0);
                    primary.setIsActive(true);
                    primary.setStatus("OPEN");
                    // Clean up test words in primary name if any
                    if (primary.getName() != null && (primary.getName().toLowerCase().contains("test") || primary.getName().toLowerCase().contains("tự động"))) {
                        if ("KLTN".equals(primary.getPeriodType())) primary.setName("Đợt Khóa luận Tốt nghiệp 2026-2027");
                        else if ("TLCN".equals(primary.getPeriodType())) primary.setName("Đợt Tiểu luận Chuyên ngành HK1 2026");
                        else if ("NCKH".equals(primary.getPeriodType())) primary.setName("Đợt Nghiên cứu Khoa học Sinh viên 2026");
                        else if ("MON_HOC".equals(primary.getPeriodType())) primary.setName("Đợt Đồ án Môn học Chuyên ngành 2026");
                    }
                    periodRepository.save(primary);

                    for (int i = 1; i < list.size(); i++) {
                        RegistrationPeriod duplicate = list.get(i);

                        // Reassign topics
                        List<Topic> topics = topicRepository.findByPeriodId(duplicate.getId());
                        for (Topic t : topics) {
                            t.setPeriod(primary);
                            topicRepository.save(t);
                        }

                        // Reassign councils
                        List<Council> councils = councilRepository.findByPeriodId(duplicate.getId());
                        for (Council c : councils) {
                            c.setPeriod(primary);
                            councilRepository.save(c);
                        }

                        // Reassign student groups
                        List<StudentGroup> groups = groupRepository.findByPeriodId(duplicate.getId());
                        for (StudentGroup g : groups) {
                            g.setPeriod(primary);
                            groupRepository.save(g);
                        }

                        // Reassign announcements
                        List<Announcement> announcements = announcementRepository.findByPeriodId(duplicate.getId());
                        for (Announcement a : announcements) {
                            a.setPeriod(primary);
                            announcementRepository.save(a);
                        }

                        // Now safely delete or deactivate duplicate period
                        try {
                            periodRepository.delete(duplicate);
                            cleanedCount++;
                        } catch (Exception ex) {
                            duplicate.setIsActive(false);
                            duplicate.setStatus("CLOSED");
                            periodRepository.save(duplicate);
                            cleanedCount++;
                        }
                    }
                }
            }

            if (cleanedCount > 0) {
                System.out.println("[CLEANUP] Đã tự động dọn dẹp và hợp nhất " + cleanedCount + " đợt đăng ký bị trùng lặp/test trong cơ sở dữ liệu!");
            }
        } catch (Exception e) {
            System.err.println("[CLEANUP-WARN] Lỗi khi dọn dẹp dữ liệu đợt đăng ký: " + e.getMessage());
        }
    }
}
