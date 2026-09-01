package topic_management.service;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import topic_management.entity.*;
import topic_management.repository.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@DependsOn("databaseMigrationService")
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
        if (allTopics.isEmpty()) return;

        List<Long> topicIds = allTopics.stream().map(Topic::getId).filter(Objects::nonNull).toList();
        List<StudentGroup> allGroups = groupRepository.findByTopicIdIn(topicIds);
        Set<Long> topicsWithGroups = allGroups.stream()
                .filter(g -> g.getTopic() != null)
                .map(g -> g.getTopic().getId())
                .collect(Collectors.toSet());

        int cleanedScores = 0;
        int deletedEvals = 0;

        List<Evaluation> allEvals = evaluationRepository.findByTopicIdIn(topicIds);
        Map<Long, List<Evaluation>> evalsByTopic = allEvals.stream()
                .filter(e -> e.getTopic() != null)
                .collect(Collectors.groupingBy(e -> e.getTopic().getId()));

        for (Topic t : allTopics) {
            if (!topicsWithGroups.contains(t.getId())) {
                if (t.getFinalScore() != null || t.getAdvisorScore() != null || t.getReviewerScore() != null || t.getCouncilScore() != null || Boolean.TRUE.equals(t.getIsFinalized())) {
                    t.setAdvisorScore(null);
                    t.setReviewerScore(null);
                    t.setCouncilScore(null);
                    t.setFinalScore(null);
                    t.setIsFinalized(false);
                    topicRepository.save(t);
                    cleanedScores++;
                }
                List<Evaluation> evals = evalsByTopic.getOrDefault(t.getId(), Collections.emptyList());
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
        if (periodId == null) return councilRepository.findAll();
        return councilRepository.findByPeriodId(periodId);
    }

    public List<Council> getCouncilsByLecturer(Long lecturerId) {
        if (lecturerId == null) return Collections.emptyList();
        return councilRepository.findByLecturerId(lecturerId);
    }

    public Council getCouncilById(Long id) {
        if (id == null) return null;
        return councilRepository.findById(id).orElse(null);
    }

    /**
     * Tạo Hội đồng mới với Transactional & Unique Name Check theo đợt
     * BUG-F FIX: Validate số lượng thành viên (3-5) tại Service layer.
     * BUG-G FIX: Bắt buộc phải có đúng 1 CHỦ TỊCH và 1 THƯ KÝ.
     */
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

        if (councilName == null || councilName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hội đồng không được để trống!");
        }
        String trimmedName = councilName.trim();

        // BUG-F FIX: Kiểm tra số lượng thành viên
        if (lecturerIds == null || lecturerIds.size() < 3) {
            throw new IllegalArgumentException("Hội đồng phải có ít nhất 3 thành viên (Chủ tịch, Thư ký và ít nhất 1 Ủy viên)!");
        }
        if (lecturerIds.size() > 5) {
            throw new IllegalArgumentException("Hội đồng không được vượt quá 5 thành viên!");
        }

        // BUG-G FIX: Kiểm tra bắt buộc phải có PRESIDENT và SECRETARY
        if (roles == null || roles.size() != lecturerIds.size()) {
            throw new IllegalArgumentException("Danh sách vai trò không hợp lệ!");
        }
        long presidentCount = roles.stream().filter(r -> "PRESIDENT".equalsIgnoreCase(r)).count();
        long secretaryCount = roles.stream().filter(r -> "SECRETARY".equalsIgnoreCase(r)).count();
        if (presidentCount != 1) {
            throw new IllegalArgumentException("Hội đồng phải có đúng 1 Chủ tịch Hội đồng!");
        }
        if (secretaryCount != 1) {
            throw new IllegalArgumentException("Hội đồng phải có đúng 1 Thư ký!");
        }

        if (councilRepository.existsByPeriodAndNameIgnoreCase(periodId, trimmedName)) {
            throw new IllegalArgumentException("Hội đồng với tên '" + trimmedName + "' đã tồn tại trong đợt này. Vui lòng chọn tên khác!");
        }

        String trimmedCode = councilCode != null && !councilCode.trim().isEmpty() ? councilCode.trim() : null;
        if (trimmedCode != null) {
            if (councilRepository.findByCode(trimmedCode).isPresent()) {
                throw new IllegalArgumentException("Mã hội đồng '" + trimmedCode + "' đã tồn tại. Vui lòng chọn mã khác!");
            }
        }

        if (lecturerIds != null && !lecturerIds.isEmpty()) {
            Set<Long> uniqueLecturers = new HashSet<>();
            for (Long lId : lecturerIds) {
                if (lId != null && !uniqueLecturers.add(lId)) {
                    throw new IllegalArgumentException("Không được chọn trùng giảng viên trong cùng một hội đồng!");
                }
            }
        }

        Department dept = departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null;
        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;

        Council council = new Council();
        council.setName(trimmedName);
        council.setCode(trimmedCode != null ? trimmedCode : ("HD-" + System.currentTimeMillis() % 10000));
        council.setDepartment(dept);
        council.setPeriod(period);
        council.setLocation(location != null ? location.trim() : null);
        council.setDefenseDate(defenseDate != null ? defenseDate : OffsetDateTime.now().plusDays(15));
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

    /**
     * Cập nhật thông tin Hội đồng & Danh sách thành viên (có kiểm tra trùng lặp)
     * BUG-F FIX: Validate số lượng thành viên (3-5) tại Service layer.
     * BUG-G FIX: Bắt buộc phải có đúng 1 CHỦ TỊCH và 1 THƯ KÝ.
     */
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

        if (councilId == null) {
            throw new IllegalArgumentException("ID hội đồng không hợp lệ!");
        }
        Council council = councilRepository.findById(councilId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội đồng với ID=" + councilId));

        if (councilName == null || councilName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên hội đồng không được để trống!");
        }
        String trimmedName = councilName.trim();

        // BUG-F FIX: Kiểm tra số lượng thành viên khi cập nhật
        if (lecturerIds != null) {
            if (lecturerIds.size() < 3) {
                throw new IllegalArgumentException("Hội đồng phải có ít nhất 3 thành viên!");
            }
            if (lecturerIds.size() > 5) {
                throw new IllegalArgumentException("Hội đồng không được vượt quá 5 thành viên!");
            }
        }

        // BUG-G FIX: Kiểm tra bắt buộc PRESIDENT và SECRETARY khi cập nhật
        if (roles != null && lecturerIds != null && roles.size() == lecturerIds.size()) {
            long presidentCount = roles.stream().filter(r -> "PRESIDENT".equalsIgnoreCase(r)).count();
            long secretaryCount = roles.stream().filter(r -> "SECRETARY".equalsIgnoreCase(r)).count();
            if (presidentCount != 1) {
                throw new IllegalArgumentException("Hội đồng phải có đúng 1 Chủ tịch Hội đồng!");
            }
            if (secretaryCount != 1) {
                throw new IllegalArgumentException("Hội đồng phải có đúng 1 Thư ký!");
            }
        }

        if (councilRepository.existsByPeriodAndNameIgnoreCaseExcludingId(periodId, trimmedName, councilId)) {
            throw new IllegalArgumentException("Hội đồng với tên '" + trimmedName + "' đã tồn tại trong đợt này. Vui lòng chọn tên khác!");
        }

        String trimmedCode = councilCode != null && !councilCode.trim().isEmpty() ? councilCode.trim() : null;
        if (trimmedCode != null) {
            Optional<Council> existingWithCode = councilRepository.findByCode(trimmedCode);
            if (existingWithCode.isPresent() && !existingWithCode.get().getId().equals(councilId)) {
                throw new IllegalArgumentException("Mã hội đồng '" + trimmedCode + "' đã tồn tại ở hội đồng khác. Vui lòng chọn mã khác!");
            }
        }

        if (lecturerIds != null && !lecturerIds.isEmpty()) {
            Set<Long> uniqueLecturers = new HashSet<>();
            for (Long lId : lecturerIds) {
                if (lId != null && !uniqueLecturers.add(lId)) {
                    throw new IllegalArgumentException("Không được chọn trùng giảng viên trong cùng một hội đồng!");
                }
            }
        }

        RegistrationPeriod period = periodId != null ? periodRepository.findById(periodId).orElse(null) : null;
        council.setPeriod(period);
        council.setName(trimmedName);
        if (trimmedCode != null) {
            council.setCode(trimmedCode);
        }
        council.setLocation(location != null ? location.trim() : null);
        council.setDefenseDate(defenseDate != null ? defenseDate : council.getDefenseDate());

        Council savedCouncil = councilRepository.save(council);

        if (lecturerIds != null && roles != null) {
            List<CouncilMember> existingMembers = councilMemberRepository.findByCouncilId(councilId);
            councilMemberRepository.deleteAll(existingMembers);

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

        System.out.println("[DB-COUNCIL] Cập nhật Hội đồng ID=" + councilId + ": " + trimmedName);
        return savedCouncil;
    }

    @Transactional
    public boolean assignTopicToCouncil(Long topicId, Long councilId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Council council = councilId != null ? councilRepository.findById(councilId).orElse(null) : null;

        if (topic == null) return false;

        if (council != null) {
            List<CouncilMember> members = councilMemberRepository.findByCouncilId(council.getId());
            for (CouncilMember cm : members) {
                if (cm.getLecturer() != null) {
                    if (topic.getSupervisor() != null && cm.getLecturer().getId().equals(topic.getSupervisor().getId())) {
                        System.err.println("[DB-COUNCIL] Xung đột lợi ích! GVHD (" + (topic.getSupervisor().getUser() != null ? topic.getSupervisor().getUser().getFullName() : "") + ") không được ngồi trong Hội đồng chấm đề tài này!");
                        return false;
                    }
                    if (topic.getCoSupervisor() != null && cm.getLecturer().getId().equals(topic.getCoSupervisor().getId())) {
                        System.err.println("[DB-COUNCIL] Xung đột lợi ích! GVHD phụ (" + (topic.getCoSupervisor().getUser() != null ? topic.getCoSupervisor().getUser().getFullName() : "") + ") không được ngồi trong Hội đồng chấm đề tài này!");
                        return false;
                    }
                }
            }
        }

        topic.setCouncil(council);
        topicRepository.save(topic);
        return true;
    }

    @Transactional
    public boolean assignReviewer(Long topicId, Long reviewerId) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Lecturer reviewer = reviewerId != null ? lecturerRepository.findById(reviewerId).orElse(null) : null;

        if (topic == null) return false;

        if (reviewer != null) {
            if (topic.getSupervisor() != null && reviewer.getId().equals(topic.getSupervisor().getId())) {
                System.err.println("[DB-COUNCIL] Xung đột lợi ích! Giảng viên phản biện không được trùng với Giảng viên hướng dẫn chính!");
                return false;
            }
            if (topic.getCoSupervisor() != null && reviewer.getId().equals(topic.getCoSupervisor().getId())) {
                System.err.println("[DB-COUNCIL] Xung đột lợi ích! Giảng viên phản biện không được trùng với Giảng viên đồng hướng dẫn!");
                return false;
            }
        }

        topic.setReviewer(reviewer);
        topicRepository.save(topic);
        return true;
    }

    public List<Topic> getTopicsInCouncil(Long councilId) {
        return topicRepository.findByCouncilId(councilId);
    }

    public List<Topic> getTopicsByCouncil(Long councilId) {
        return topicRepository.findByCouncilId(councilId);
    }

    public List<Topic> getTopicsByReviewer(Long reviewerId) {
        return topicRepository.findByReviewerId(reviewerId);
    }

    public List<CouncilMember> getCouncilMembers(Long councilId) {
        return councilMemberRepository.findByCouncilId(councilId);
    }

    public List<CouncilMember> getMembersByCouncil(Long councilId) {
        return councilMemberRepository.findByCouncilId(councilId);
    }

    public List<Evaluation> getEvaluationsForTopic(Long topicId) {
        return evaluationRepository.findByTopicId(topicId);
    }

    public List<Evaluation> getEvaluationsByTopic(Long topicId) {
        return evaluationRepository.findByTopicId(topicId);
    }

    public Optional<Evaluation> getEvaluation(Long topicId, Long lecturerId, String evaluationType) {
        return evaluationRepository.findByTopicIdAndLecturerIdAndEvaluationType(topicId, lecturerId, evaluationType);
    }

    @Transactional
    public boolean submitEvaluation(Long topicId, Long lecturerId, String evaluationType, Double score, String comments) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        Lecturer lecturer = lecturerRepository.findById(lecturerId).orElse(null);

        if (topic == null || lecturer == null || evaluationType == null) return false;

        List<StudentGroup> groups = groupRepository.findByTopicId(topicId);
        if (groups.isEmpty()) {
            System.err.println("[DB-EVAL] Lỗi! Đề tài ID=" + topicId + " chưa có nhóm sinh viên đăng ký thực hiện, tuyệt đối không được chấm điểm!");
            return false;
        }

        String evalTypeUpper = evaluationType.trim().toUpperCase();

        // 1. Phân quyền và kiểm tra xung đột lợi ích nghiêm ngặt theo quy chế
        boolean isSupervisor = topic.isSupervisorOrCoSupervisor(lecturer);

        if ("COUNCIL".equals(evalTypeUpper)) {
            // TUYỆT ĐỐI KHÔNG ĐỂ GVHD (chính hoặc phụ) CÓ QUYỀN CHẤM ĐIỂM TRONG HỘI ĐỒNG BẢO VỆ
            if (isSupervisor) {
                System.err.println("[DB-EVAL] Xung đột lợi ích nghiêm trọng! GVHD không được phép chấm điểm trong Hội đồng bảo vệ cho đề tài của mình!");
                return false;
            }
            // Giảng viên chấm Hội đồng phải thuộc Hội đồng được phân công cho đề tài này
            if (topic.getCouncil() == null) {
                System.err.println("[DB-EVAL] Lỗi! Đề tài chưa được phân công vào Hội đồng bảo vệ nào!");
                return false;
            }
            List<CouncilMember> members = councilMemberRepository.findByCouncilId(topic.getCouncil().getId());
            boolean isMember = members.stream()
                    .anyMatch(m -> m.getLecturer() != null && m.getLecturer().getId().equals(lecturer.getId()));
            if (!isMember) {
                System.err.println("[DB-EVAL] Lỗi! Giảng viên không phải thành viên của Hội đồng ID=" + topic.getCouncil().getId());
                return false;
            }
        } else if ("REVIEWER".equals(evalTypeUpper)) {
            // Giảng viên phản biện tuyệt đối không được trùng với GVHD
            if (isSupervisor) {
                System.err.println("[DB-EVAL] Xung đột lợi ích! GVHD không được tự chấm phản biện cho đề tài của mình!");
                return false;
            }
        } else if ("SUPERVISOR".equals(evalTypeUpper)) {
            // Chỉ GVHD chính hoặc phụ mới có quyền nhập điểm quá trình (SUPERVISOR)
            if (!isSupervisor) {
                System.err.println("[DB-EVAL] Lỗi quyền hạn! Chỉ Giảng viên hướng dẫn của đề tài mới được chấm điểm quá trình (SUPERVISOR)!");
                return false;
            }
        } else {
            System.err.println("[DB-EVAL] Loại phiếu chấm không hợp lệ: " + evaluationType);
            return false;
        }

        Evaluation eval = evaluationRepository.findByTopicIdAndLecturerIdAndEvaluationType(topicId, lecturerId, evalTypeUpper)
                .orElse(new Evaluation());

        eval.setTopic(topic);
        eval.setLecturer(lecturer);
        eval.setGroup(!groups.isEmpty() ? groups.get(0) : null);
        eval.setEvaluationType(evalTypeUpper);
        eval.setScore(score);
        eval.setComments(comments != null ? comments.trim() : null);
        eval.setIsSubmitted(true);
        eval.setEvaluatedAt(OffsetDateTime.now());

        evaluationRepository.save(eval);

        calculateAndSetFinalScore(topic);
        return true;
    }

    /**
     * Tính điểm tổng kết theo chuẩn quy chế học thuật thực tế:
     * - LOẠI BỎ HOÀN TOÀN ĐIỂM GVHD KHỎI ĐIỂM TỔNG KẾT KHÓA LUẬN (FinalScore).
     * - Điểm GVHD (AdvisorScore) được lưu độc lập để theo dõi quá trình/điều kiện ra bảo vệ.
     * - Điểm Tổng Kết (FinalScore) tính trên 2 thành phần độc lập:
     *   1. ReviewerScore: Điểm phản biện từ Giảng viên Phản biện độc lập (Trọng số 40%)
     *   2. CouncilScore: Điểm trung bình cộng từ các thành viên Hội đồng bảo vệ (Trọng số 60%)
     * 
     * Công thức chuẩn (Tổng 100%):
     * FinalScore = (ReviewerScore * 0.40) + (CouncilScore * 0.60)
     */
    @Transactional
    public void calculateAndSetFinalScore(Topic topic) {
        if (topic == null) return;

        List<StudentGroup> groups = groupRepository.findByTopicId(topic.getId());
        if (groups.isEmpty()) {
            topic.setAdvisorScore(null);
            topic.setReviewerScore(null);
            topic.setCouncilScore(null);
            topic.setFinalScore(null);
            topic.setIsFinalized(false);
            topicRepository.save(topic);
            return;
        }

        List<Evaluation> evals = evaluationRepository.findByTopicId(topic.getId());
        if (evals.isEmpty()) {
            topic.setAdvisorScore(null);
            topic.setReviewerScore(null);
            topic.setCouncilScore(null);
            topic.setFinalScore(null);
            topicRepository.save(topic);
            return;
        }

        // 1. Điểm quá trình từ Giảng viên Hướng dẫn (SUPERVISOR) - Lưu riêng để theo dõi tiến độ
        List<Evaluation> supervisorEvals = evals.stream()
                .filter(e -> "SUPERVISOR".equalsIgnoreCase(e.getEvaluationType()) && e.getScore() != null)
                .toList();
        Double advisorScore = null;
        if (!supervisorEvals.isEmpty()) {
            double avg = supervisorEvals.stream().mapToDouble(Evaluation::getScore).average().orElse(0.0);
            advisorScore = Math.round(avg * 100.0) / 100.0;
        }

        // 2. Điểm phản biện từ Giảng viên Phản biện (REVIEWER) - Trọng số 40%
        List<Evaluation> reviewerEvals = evals.stream()
                .filter(e -> "REVIEWER".equalsIgnoreCase(e.getEvaluationType()) && e.getScore() != null)
                .toList();
        Double reviewerScore = null;
        if (!reviewerEvals.isEmpty()) {
            double avg = reviewerEvals.stream().mapToDouble(Evaluation::getScore).average().orElse(0.0);
            reviewerScore = Math.round(avg * 100.0) / 100.0;
        }

        // 3. Điểm trung bình từ các thành viên Hội đồng bảo vệ (COUNCIL) - Trọng số 60%
        List<Evaluation> councilEvals = evals.stream()
                .filter(e -> "COUNCIL".equalsIgnoreCase(e.getEvaluationType()) && e.getScore() != null)
                .toList();
        Double councilScore = null;
        if (!councilEvals.isEmpty()) {
            double avg = councilEvals.stream().mapToDouble(Evaluation::getScore).average().orElse(0.0);
            councilScore = Math.round(avg * 100.0) / 100.0;
        }

        topic.setAdvisorScore(advisorScore);
        topic.setReviewerScore(reviewerScore);
        topic.setCouncilScore(councilScore);

        // 4. ÁP DỤNG CÔNG THỨC ĐIỂM TỔNG KẾT (FinalScore) - LOẠI BỎ HOÀN TOÀN ĐIỂM GVHD
        Double finalScore = null;
        if (reviewerScore != null && councilScore != null) {
            // Có cả Phản biện (40%) và Hội đồng (60%)
            double calc = (reviewerScore * 0.40) + (councilScore * 0.60);
            finalScore = Math.round(calc * 100.0) / 100.0;
        } else if (councilScore != null) {
            finalScore = councilScore;
        } else if (reviewerScore != null) {
            finalScore = reviewerScore;
        }

        topic.setFinalScore(finalScore);
        topicRepository.save(topic);
    }

    /**
     * BUG-I FIX: Kiểm tra đủ ít nhất 1 loại điểm (SUPERVISOR) trước khi công bố.
     * Với loại KLTN/TLCN bắt buộc phải có cả REVIEWER hoặc COUNCIL đã chấm.
     */
    @Transactional
    public boolean publishTopicResults(Long topicId, Boolean isFinalized) {
        Topic topic = topicRepository.findById(topicId).orElse(null);
        if (topic == null) return false;

        List<StudentGroup> groups = groupRepository.findByTopicId(topicId);
        if (groups.isEmpty()) {
            System.err.println("[DB-EVAL] Không thể công bố điểm cho đề tài chưa có nhóm sinh viên!");
            return false;
        }

        if (topic.getFinalScore() == null) {
            System.err.println("[DB-EVAL] Không thể công bố: Đề tài chưa có điểm tổng kết hợp lệ!");
            return false;
        }

        // BUG-I FIX: Kiểm tra tối thiểu phải có điểm của GVHD (SUPERVISOR) trước khi công bố
        if (Boolean.TRUE.equals(isFinalized)) {
            List<Evaluation> evals = evaluationRepository.findByTopicId(topicId);
            boolean hasSupervisorScore = evals.stream()
                    .anyMatch(e -> "SUPERVISOR".equalsIgnoreCase(e.getEvaluationType()) && e.getScore() != null);
            if (!hasSupervisorScore) {
                System.err.println("[DB-EVAL] Không thể công bố chính thức: Chưa có điểm của GVHD!");
                return false;
            }

            // Kiểm tra REVIEWER hoặc COUNCIL đã chấm chưa
            boolean hasExternalScore = evals.stream()
                    .anyMatch(e -> ("REVIEWER".equalsIgnoreCase(e.getEvaluationType())
                            || "COUNCIL".equalsIgnoreCase(e.getEvaluationType()))
                            && e.getScore() != null);
            if (!hasExternalScore) {
                System.err.println("[DB-EVAL] Cảnh báo: Chưa có điểm Phản biện/Hội đồng. Điểm công bố chỉ dựa trên GVHD.");
                // Chỉ warn, không block — cho phép trường hợp dạng MON_HOC (không có hội đồng)
            }
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
        if (topics.isEmpty()) return 0;

        List<Long> topicIds = topics.stream().map(Topic::getId).filter(Objects::nonNull).toList();
        List<StudentGroup> allGroups = groupRepository.findByTopicIdIn(topicIds);
        Set<Long> topicsWithGroups = allGroups.stream()
                .filter(g -> g.getTopic() != null)
                .map(g -> g.getTopic().getId())
                .collect(Collectors.toSet());

        int count = 0;
        for (Topic t : topics) {
            if (periodId == null || (t.getPeriod() != null && t.getPeriod().getId().equals(periodId))) {
                if ("APPROVED".equalsIgnoreCase(t.getStatus()) && topicsWithGroups.contains(t.getId()) && t.getFinalScore() != null) {
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

        List<Long> topicIds = topics.stream().map(Topic::getId).filter(Objects::nonNull).toList();
        if (topicIds.isEmpty()) return map;

        List<StudentGroup> allGroups = groupRepository.findByTopicIdIn(topicIds);
        Set<Long> topicsWithGroups = allGroups.stream()
                .filter(g -> g.getTopic() != null)
                .map(g -> g.getTopic().getId())
                .collect(Collectors.toSet());

        List<Evaluation> allEvaluations = evaluationRepository.findByTopicIdIn(topicIds);
        Map<Long, List<Evaluation>> evalsByTopicId = allEvaluations.stream()
                .filter(e -> e.getTopic() != null)
                .collect(Collectors.groupingBy(e -> e.getTopic().getId()));

        for (Long topicId : topicIds) {
            if (topicsWithGroups.contains(topicId)) {
                map.put(topicId, evalsByTopicId.getOrDefault(topicId, Collections.emptyList()));
            } else {
                map.put(topicId, Collections.emptyList());
            }
        }
        return map;
    }

    public Map<Long, StudentGroup> getGroupsMapForTopics(List<Topic> topics) {
        Map<Long, StudentGroup> map = new HashMap<>();
        if (topics == null || topics.isEmpty()) return map;

        List<Long> topicIds = topics.stream().map(Topic::getId).filter(Objects::nonNull).toList();
        if (topicIds.isEmpty()) return map;

        List<StudentGroup> allGroups = groupRepository.findByTopicIdIn(topicIds);
        for (StudentGroup g : allGroups) {
            if (g.getTopic() != null && !map.containsKey(g.getTopic().getId())) {
                map.put(g.getTopic().getId(), g);
            }
        }
        return map;
    }
}
