package topic_management.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CouncilService {

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final EvaluationRepository evaluationRepository;
    private final TopicRepository topicRepository;
    private final LecturerRepository lecturerRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final DepartmentRepository departmentRepository;
    private final StudentGroupRepository groupRepository;

    public CouncilService(
            CouncilRepository councilRepository,
            CouncilMemberRepository councilMemberRepository,
            EvaluationRepository evaluationRepository,
            TopicRepository topicRepository,
            LecturerRepository lecturerRepository,
            RegistrationPeriodRepository periodRepository,
            DepartmentRepository departmentRepository,
            StudentGroupRepository groupRepository) {
        this.councilRepository = councilRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.evaluationRepository = evaluationRepository;
        this.topicRepository = topicRepository;
        this.lecturerRepository = lecturerRepository;
        this.periodRepository = periodRepository;
        this.departmentRepository = departmentRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Data Cleanup Runner: Removes orphan evaluations and resets scores on topics that have no student group.
     */
    @PostConstruct
    @Transactional
    public void cleanupOrphanEvaluationsAndScores() {
        List<Topic> allTopics = topicRepository.findAll();
        int cleanedScores = 0;
        int deletedEvals = 0;
        for (Topic t : allTopics) {
            List<StudentGroup> groups = groupRepository.findByTopicId(t.getId());
            if (groups.isEmpty()) {
                if (t.getFinalScore() != null || Boolean.TRUE.equals(t.getIsFinalized())) {
                    t.setFinalScore(null);
                    t.setIsFinalized(false);
                    topicRepository.save(t);
                    cleanedScores++;
                }
                List<Evaluation> evals = evaluationRepository.findByTopicId(t.getId());
                if (!evals.isEmpty()) {
                    evaluationRepository.deleteAll(evals);
                    deletedEvals += evals.size();
                }
            }
        }
        if (cleanedScores > 0 || deletedEvals > 0) {
            System.out.println("[DATA-CLEANUP] Đã xóa " + deletedEvals + " phiếu chấm rác và reset điểm cho " + cleanedScores + " đề tài chưa có nhóm sinh viên!");
        }
    }

    public List<Council> getAllCouncils() {
        return councilRepository.findAll();
    }

    public List<Council> getCouncilsByDepartment(Long departmentId) {
        if (departmentId == null) return councilRepository.findAll();
        return councilRepository.findByDepartmentId(departmentId);
    }

    public List<Council> getCouncilsByPeriod(Long periodId) {
        return councilRepository.findByPeriodId(periodId);
    }

    public List<Council> getCouncilsByLecturer(Long lecturerId) {
        return councilRepository.findByLecturerId(lecturerId);
    }

    public Council getCouncilById(Long id) {
        if (id == null) return null;
        return councilRepository.findById(id).orElse(null);
    }

    @Transactional
    public Council createCouncil(
            Long departmentId,
            Long periodId,
            String councilCode,
            String councilName,
            String location,
            OffsetDateTime defenseDate,
            List<Long> lecturerIds,
            List<String> roles) {

        // 1. Validation Tên Hội đồng
        if (councilName == null || councilName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hội đồng không được để trống!");
        }
        String trimmedName = councilName.trim();

        // 2. Chặn trùng lặp Tên Hội đồng trong cùng một đợt đăng ký
        if (councilRepository.existsByPeriodAndNameIgnoreCase(periodId, trimmedName)) {
            throw new IllegalArgumentException("Tên hội đồng '" + trimmedName + "' đã tồn tại trong đợt đăng ký này! Vui lòng đặt tên khác.");
        }

        // 3. Kiểm tra trùng lặp Mã Hội đồng nếu có cung cấp
        String trimmedCode = (councilCode != null && !councilCode.trim().isEmpty()) ? councilCode.trim() : null;
        if (trimmedCode != null) {
            if (councilRepository.findByCode(trimmedCode).isPresent()) {
                throw new IllegalArgumentException("Mã hội đồng '" + trimmedCode + "' đã tồn tại trong hệ thống! Vui lòng chọn mã khác.");
            }
        }

        // 4. Kiểm tra trùng lặp giảng viên trong cùng hội đồng
        if (lecturerIds != null) {
            Set<Long> seenLecturerIds = new HashSet<>();
            for (Long lid : lecturerIds) {
                if (lid != null && !seenLecturerIds.add(lid)) {
                    throw new IllegalArgumentException("Một giảng viên không thể đảm nhiệm nhiều vị trí trong cùng một Hội đồng!");
                }
            }
        }

        Department dept = departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null;
        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;

        Council council = new Council();
        council.setDepartment(dept);
        council.setPeriod(period);
        council.setCode(trimmedCode);
        council.setName(trimmedName);
        council.setLocation(location != null ? location.trim() : null);
        council.setDefenseDate(defenseDate);
        council.setStatus("ESTABLISHED");

        Council savedCouncil = councilRepository.save(council);

        if (lecturerIds != null && roles != null) {
            for (int i = 0; i < lecturerIds.size(); i++) {
                Long lecturerId = lecturerIds.get(i);
                String role = (i < roles.size()) ? roles.get(i) : "MEMBER";

                Lecturer lecturer = lecturerRepository.findById(lecturerId).orElse(null);
                if (lecturer != null) {
                    CouncilMember member = new CouncilMember();
                    member.setCouncil(savedCouncil);
                    member.setLecturer(lecturer);
                    member.setCouncilRole(role);
                    councilMemberRepository.save(member);
                }
            }
        }

        System.out.println("[DB-COUNCIL] Tạo mới Hội đồng: " + trimmedName + " (Mã: " + trimmedCode + ")");
        return savedCouncil;
    }

    @Transactional
    public Council updateCouncil(
            Long councilId,
            Long periodId,
            String councilCode,
            String councilName,
            String location,
            OffsetDateTime defenseDate,
            List<Long> lecturerIds,
            List<String> roles) {

        Council council = councilRepository.findById(councilId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Hội đồng với ID=" + councilId));

        if (councilName == null || councilName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hội đồng không được để trống!");
        }
        String trimmedName = councilName.trim();

        if (councilRepository.existsByPeriodAndNameIgnoreCaseExcludingId(periodId, trimmedName, councilId)) {
            throw new IllegalArgumentException("Tên hội đồng '" + trimmedName + "' đã tồn tại trong đợt đăng ký này! Vui lòng đặt tên khác.");
        }

        String trimmedCode = (councilCode != null && !councilCode.trim().isEmpty()) ? councilCode.trim() : null;
        if (trimmedCode != null) {
            Optional<Council> existingWithCode = councilRepository.findByCode(trimmedCode);
            if (existingWithCode.isPresent() && !existingWithCode.get().getId().equals(councilId)) {
                throw new IllegalArgumentException("Mã hội đồng '" + trimmedCode + "' đã tồn tại trong hệ thống! Vui lòng chọn mã khác.");
            }
        }

        if (lecturerIds != null) {
            Set<Long> seenLecturerIds = new HashSet<>();
            for (Long lid : lecturerIds) {
                if (lid != null && !seenLecturerIds.add(lid)) {
                    throw new IllegalArgumentException("Một giảng viên không thể đảm nhiệm nhiều vị trí trong cùng một Hội đồng!");
                }
            }
        }

        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;
        council.setPeriod(period);
        council.setCode(trimmedCode);
        council.setName(trimmedName);
        council.setLocation(location != null ? location.trim() : null);
        council.setDefenseDate(defenseDate);

        if (lecturerIds != null && roles != null) {
            councilMemberRepository.deleteByCouncilId(councilId);
            council.getMembers().clear();
            for (int i = 0; i < lecturerIds.size(); i++) {
                Long lecturerId = lecturerIds.get(i);
                String role = (i < roles.size()) ? roles.get(i) : "MEMBER";
                Lecturer lecturer = lecturerRepository.findById(lecturerId).orElse(null);
                if (lecturer != null) {
                    CouncilMember member = new CouncilMember();
                    member.setCouncil(council);
                    member.setLecturer(lecturer);
                    member.setCouncilRole(role);
                    councilMemberRepository.save(member);
                    council.getMembers().add(member);
                }
            }
        }

        System.out.println("[DB-COUNCIL] Cập nhật Hội đồng ID=" + councilId + ": " + trimmedName);
        return councilRepository.save(council);
    }

    @Transactional
    public boolean assignTopicToCouncil(Long topicId, Long councilId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Council council = councilId != null ? councilRepository.findById(councilId).orElse(null) : null;

        if (topic == null) return false;

        // Conflict of interest check: Council members CANNOT be supervisor or coSupervisor of the topic
        if (council != null) {
            List<CouncilMember> members = councilMemberRepository.findByCouncilId(council.getId());
            for (CouncilMember m : members) {
                if (topic.isSupervisorOrCoSupervisor(m.getLecturer())) {
                    System.err.println("[DB-COUNCIL] Xung đột lợi ích! Giảng viên hướng dẫn '"
                            + (m.getLecturer().getUser() != null ? m.getLecturer().getUser().getFullName() : "ID=" + m.getLecturer().getId())
                            + "' là thành viên của Hội đồng '" + council.getName() + "'. Không thể gán đề tài!");
                    return false;
                }
            }
        }

        topic.setCouncil(council);
        topicRepository.save(topic);
        System.out.println("[DB-COUNCIL] Gán đề tài ID=" + topicId + " vào Hội đồng: " + (council != null ? council.getName() : "NULL"));
        return true;
    }

    @Transactional
    public boolean assignReviewer(Long topicId, Long reviewerId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Lecturer reviewer = reviewerId != null ? lecturerRepository.findById(reviewerId).orElse(null) : null;

        if (topic == null) return false;

        // Conflict of interest check: Reviewer CANNOT be supervisor or coSupervisor
        if (reviewer != null && topic.isSupervisorOrCoSupervisor(reviewer)) {
            System.err.println("[DB-COUNCIL] Xung đột lợi ích! Giảng viên hướng dẫn không thể làm Giảng viên phản biện cho đề tài của chính mình.");
            return false;
        }

        topic.setReviewer(reviewer);
        topicRepository.save(topic);
        System.out.println("[DB-COUNCIL] Phân công GVPB cho đề tài ID=" + topicId + ": " + (reviewer != null && reviewer.getUser() != null ? reviewer.getUser().getFullName() : "NULL"));
        return true;
    }

    public List<Topic> getTopicsInCouncil(Long councilId) {
        return topicRepository.findByCouncilId(councilId);
    }

    public List<Topic> getTopicsByReviewer(Long reviewerId) {
        return topicRepository.findByReviewerId(reviewerId);
    }

    public List<CouncilMember> getCouncilMembers(Long councilId) {
        return councilMemberRepository.findByCouncilId(councilId);
    }

    public List<Evaluation> getEvaluationsForTopic(Long topicId) {
        return evaluationRepository.findByTopicId(topicId);
    }

    public Optional<Evaluation> getEvaluation(Long topicId, Long lecturerId, String evaluationType) {
        return evaluationRepository.findByTopicIdAndLecturerIdAndEvaluationType(topicId, lecturerId, evaluationType);
    }

    @Transactional
    public boolean submitEvaluation(Long topicId, Long lecturerId, String evaluationType, Double score, String comments) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Lecturer lecturer = lecturerRepository.findById(lecturerId).orElse(null);
        if (topic == null || lecturer == null) return false;

        // Business Rule: Topic MUST have a registered student group to receive evaluation
        List<StudentGroup> groups = groupRepository.findByTopicId(topicId);
        if (groups.isEmpty()) {
            System.err.println("[DB-EVAL] Lỗi! Đề tài ID=" + topicId + " chưa có nhóm sinh viên đăng ký thực hiện, tuyệt đối không được chấm điểm!");
            return false;
        }

        // Conflict of interest check: If evaluationType is COUNCIL or REVIEWER, lecturer CANNOT be supervisor or coSupervisor
        if (("COUNCIL".equalsIgnoreCase(evaluationType) || "REVIEWER".equalsIgnoreCase(evaluationType))
                && topic.isSupervisorOrCoSupervisor(lecturer)) {
            System.err.println("[DB-EVAL] Xung đột lợi ích! Giảng viên hướng dẫn không được chấm điểm đề tài của mình ở vai trò " + evaluationType);
            return false;
        }

        Evaluation eval = evaluationRepository.findByTopicIdAndLecturerIdAndEvaluationType(topicId, lecturerId, evaluationType)
                .orElse(new Evaluation());

        eval.setTopic(topic);
        eval.setLecturer(lecturer);
        eval.setEvaluationType(evaluationType);
        eval.setScore(score);
        eval.setComments(comments);
        eval.setIsSubmitted(true);
        eval.setEvaluatedAt(OffsetDateTime.now());

        evaluationRepository.save(eval);
        System.out.println("[DB-EVAL] Lưu điểm (" + score + ") loại " + evaluationType + " cho đề tài ID=" + topicId + " bởi GV ID=" + lecturerId);

        // Auto update topic average score
        calculateAndSetFinalScore(topic);
        return true;
    }

    /**
     * BUG-09 FIX: Tính điểm trung bình có trọng số theo evaluationType
     * SUPERVISOR: 40%, REVIEWER: 30%, COUNCIL: 30%
     */
    @Transactional
    public void calculateAndSetFinalScore(Topic topic) {
        List<StudentGroup> groups = groupRepository.findByTopicId(topic.getId());
        if (groups.isEmpty()) {
            // Strictly reset to null if topic has no student group
            topic.setFinalScore(null);
            topic.setIsFinalized(false);
            topicRepository.save(topic);
            return;
        }

        List<Evaluation> evals = evaluationRepository.findByTopicId(topic.getId());
        List<Evaluation> validEvals = evals.stream().filter(e -> e.getScore() != null).toList();
        if (!validEvals.isEmpty()) {
            // Trọng số mặc định theo loại đánh giá
            Map<String, Double> weights = Map.of(
                    "SUPERVISOR", 0.40,
                    "REVIEWER", 0.30,
                    "COUNCIL", 0.30
            );

            // Nhóm evaluations theo loại
            Map<String, List<Evaluation>> groupedByType = validEvals.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            e -> e.getEvaluationType() != null ? e.getEvaluationType().toUpperCase() : "OTHER"
                    ));

            double weightedSum = 0;
            double totalWeight = 0;

            for (Map.Entry<String, List<Evaluation>> entry : groupedByType.entrySet()) {
                String type = entry.getKey();
                List<Evaluation> typeEvals = entry.getValue();
                double typeAvg = typeEvals.stream().mapToDouble(Evaluation::getScore).average().orElse(0);
                double w = weights.getOrDefault(type, 1.0 / groupedByType.size());
                weightedSum += typeAvg * w;
                totalWeight += w;
            }

            double finalScore = totalWeight > 0
                    ? Math.round((weightedSum / totalWeight) * 100.0) / 100.0
                    : 0;
            topic.setFinalScore(finalScore);
            topicRepository.save(topic);
            System.out.println("[DB-EVAL] Đã cập nhật điểm TB (có trọng số) đề tài ID=" + topic.getId()
                    + " -> " + finalScore + " (Tổng " + validEvals.size() + " phiếu chấm, "
                    + groupedByType.size() + " loại)");
        } else {
            topic.setFinalScore(null);
            topicRepository.save(topic);
        }
    }

    @Transactional
    public boolean publishTopicResults(Long topicId, Boolean isFinalized) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;

        List<StudentGroup> groups = groupRepository.findByTopicId(topicId);
        if (groups.isEmpty()) {
            System.err.println("[DB-EVAL] Không thể công bố điểm cho đề tài chưa có nhóm sinh viên!");
            return false;
        }

        topic.setIsFinalized(Boolean.TRUE.equals(isFinalized));
        topicRepository.save(topic);
        System.out.println("[DB-EVAL] Cập nhật công bố kết quả đề tài ID=" + topicId + " -> isFinalized=" + topic.getIsFinalized());
        return true;
    }

    @Transactional
    public int publishAllResults(Long departmentId, Long periodId, Boolean isFinalized) {
        List<Topic> topics = (departmentId != null) 
                ? topicRepository.findByMajorDepartmentId(departmentId) 
                : topicRepository.findAll();
        int count = 0;
        for (Topic t : topics) {
            if (periodId == null || (t.getPeriod() != null && t.getPeriod().getId().equals(periodId))) {
                List<StudentGroup> groups = groupRepository.findByTopicId(t.getId());
                if ("APPROVED".equalsIgnoreCase(t.getStatus()) && !groups.isEmpty() && t.getFinalScore() != null) {
                    t.setIsFinalized(Boolean.TRUE.equals(isFinalized));
                    topicRepository.save(t);
                    count++;
                }
            }
        }
        System.out.println("[DB-EVAL] Đã cập nhật công bố toàn đợt cho " + count + " đề tài hợp lệ (isFinalized=" + isFinalized + ")");
        return count;
    }

    public Map<Long, List<Evaluation>> getEvaluationsMapForTopics(List<Topic> topics) {
        Map<Long, List<Evaluation>> map = new HashMap<>();
        if (topics == null || topics.isEmpty()) return map;
        for (Topic t : topics) {
            List<StudentGroup> groups = groupRepository.findByTopicId(t.getId());
            // Only return evaluations for topics that have a registered student group
            if (!groups.isEmpty()) {
                map.put(t.getId(), evaluationRepository.findByTopicId(t.getId()));
            } else {
                map.put(t.getId(), Collections.emptyList());
            }
        }
        return map;
    }

    public Map<Long, StudentGroup> getGroupsMapForTopics(List<Topic> topics) {
        Map<Long, StudentGroup> map = new HashMap<>();
        if (topics == null || topics.isEmpty()) return map;
        for (Topic t : topics) {
            List<StudentGroup> groups = groupRepository.findByTopicId(t.getId());
            if (!groups.isEmpty()) {
                map.put(t.getId(), groups.get(0));
            }
        }
        return map;
    }
}
