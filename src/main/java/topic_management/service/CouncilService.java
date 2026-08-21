package topic_management.service;

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

    public CouncilService(
            CouncilRepository councilRepository,
            CouncilMemberRepository councilMemberRepository,
            EvaluationRepository evaluationRepository,
            TopicRepository topicRepository,
            LecturerRepository lecturerRepository,
            RegistrationPeriodRepository periodRepository,
            DepartmentRepository departmentRepository) {
        this.councilRepository = councilRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.evaluationRepository = evaluationRepository;
        this.topicRepository = topicRepository;
        this.lecturerRepository = lecturerRepository;
        this.periodRepository = periodRepository;
        this.departmentRepository = departmentRepository;
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
    public Council createCouncil(Long departmentId, Long periodId, String code, String name, String location, OffsetDateTime defenseDate, List<Long> lecturerIds, List<String> roles) {
        Council council = new Council();
        if (departmentId != null) {
            council.setDepartment(departmentRepository.findById(departmentId).orElse(null));
        }
        if (periodId != null) {
            council.setPeriod(periodRepository.findById(periodId).orElse(null));
        }
        council.setCode(code != null && !code.trim().isEmpty() ? code.trim() : "HD-" + System.currentTimeMillis() % 10000);
        council.setName(name);
        council.setLocation(location);
        council.setDefenseDate(defenseDate != null ? defenseDate : OffsetDateTime.now().plusDays(7));
        council.setStatus("ESTABLISHED");
        council.setCreatedAt(OffsetDateTime.now());

        Council saved = councilRepository.save(council);

        // Add members (3 to 5 lecturers)
        if (lecturerIds != null && !lecturerIds.isEmpty()) {
            for (int i = 0; i < lecturerIds.size(); i++) {
                Long lId = lecturerIds.get(i);
                if (lId == null) continue;
                Optional<Lecturer> lOpt = lecturerRepository.findById(lId);
                if (lOpt.isPresent()) {
                    CouncilMember cm = new CouncilMember();
                    cm.setCouncil(saved);
                    cm.setLecturer(lOpt.get());
                    String role = (roles != null && i < roles.size()) ? roles.get(i) : "MEMBER";
                    cm.setCouncilRole(role);
                    cm.setCreatedAt(OffsetDateTime.now());
                    councilMemberRepository.save(cm);
                }
            }
        }

        System.out.println("[DB-COUNCIL] Đã thành lập hội đồng: '" + name + "' (ID=" + saved.getId() + ")");
        return saved;
    }

    @Transactional
    public boolean assignTopicToCouncil(Long topicId, Long councilId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;

        if (councilId != null) {
            Council council = councilRepository.findById(councilId).orElse(null);
            topic.setCouncil(council);
        } else {
            topic.setCouncil(null);
        }
        topicRepository.save(topic);
        System.out.println("[DB-COUNCIL] Gán đề tài ID=" + topicId + " vào hội đồng ID=" + councilId);
        return true;
    }

    @Transactional
    public boolean assignReviewer(Long topicId, Long reviewerId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;

        if (reviewerId != null) {
            Lecturer reviewer = lecturerRepository.findById(reviewerId).orElse(null);
            // Check reviewer is not supervisor or co-supervisor
            if (reviewer != null && topic.isSupervisorOrCoSupervisor(reviewer)) {
                System.err.println("[DB-COUNCIL] Không thể phân công GVPB trùng với GVHD!");
                return false;
            }
            topic.setReviewer(reviewer);
        } else {
            topic.setReviewer(null);
        }
        topicRepository.save(topic);
        System.out.println("[DB-COUNCIL] Phân công GVPB ID=" + reviewerId + " cho đề tài ID=" + topicId);
        return true;
    }

    public List<Topic> getTopicsInCouncil(Long councilId) {
        if (councilId == null) return Collections.emptyList();
        return topicRepository.findByCouncilId(councilId);
    }

    public List<Evaluation> getEvaluationsForTopic(Long topicId) {
        if (topicId == null) return Collections.emptyList();
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

    @Transactional
    public void calculateAndSetFinalScore(Topic topic) {
        List<Evaluation> evals = evaluationRepository.findByTopicId(topic.getId());
        List<Evaluation> validEvals = evals.stream().filter(e -> e.getScore() != null).toList();
        if (!validEvals.isEmpty()) {
            double sum = validEvals.stream().mapToDouble(Evaluation::getScore).sum();
            double avg = Math.round((sum / validEvals.size()) * 100.0) / 100.0;
            topic.setFinalScore(avg);
            topicRepository.save(topic);
            System.out.println("[DB-EVAL] Đã cập nhật điểm TB đề tài ID=" + topic.getId() + " -> " + avg + " (Tổng " + validEvals.size() + " phiếu chấm)");
        }
    }

    @Transactional
    public boolean publishTopicResults(Long topicId, Boolean isFinalized) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;
        topic.setIsFinalized(Boolean.TRUE.equals(isFinalized));
        topicRepository.save(topic);
        System.out.println("[DB-EVAL] Cập nhật công bố kết quả đề tài ID=" + topicId + " -> isFinalized=" + topic.getIsFinalized());
        return true;
    }
}
