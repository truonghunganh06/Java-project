package topicmanagement.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "period_id")
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "major_id")
    private Major major;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id")
    private Division division;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supervisor_id")
    private Lecturer supervisor; // GVHD chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_supervisor_id")
    private Lecturer coSupervisor; // GVHD phụ (nếu có)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Lecturer reviewer; // GVPB (Giảng viên phản biện do Trưởng bộ môn phân công)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id")
    private Council council; // Hội đồng bảo vệ (nếu là KLTN / TLCN)

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "max_students")
    private Integer maxStudents = 5;

    @Column(length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "academic_year", length = 50)
    private String academicYear = "2026-2027";

    private Integer semester = 1;

    @Column(name = "is_finalized")
    private Boolean isFinalized = false;

    @Column(name = "advisor_score")
    private Double advisorScore; // Điểm quá trình từ Giảng viên Hướng dẫn

    @Column(name = "reviewer_score")
    private Double reviewerScore; // Điểm phản biện từ Giảng viên Phản biện

    @Column(name = "council_score")
    private Double councilScore; // Điểm trung bình từ Hội đồng bảo vệ

    @Column(name = "final_score")
    private Double finalScore; // Điểm tổng kết theo trọng số học thuật

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Topic() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public RegistrationPeriod getPeriod() {
        return period;
    }

    public void setPeriod(RegistrationPeriod period) {
        this.period = period;
    }

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
        if (major != null && major.getDivision() != null && this.division == null) {
            this.division = major.getDivision();
        }
    }

    public Division getDivision() {
        if (division != null) return division;
        if (major != null && major.getDivision() != null) return major.getDivision();
        if (supervisor != null && supervisor.getDivision() != null) return supervisor.getDivision();
        return null;
    }

    public void setDivision(Division division) {
        this.division = division;
    }

    public Lecturer getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Lecturer supervisor) {
        this.supervisor = supervisor;
        if (supervisor != null && supervisor.getDivision() != null && this.division == null) {
            this.division = supervisor.getDivision();
        }
    }

    public Lecturer getCoSupervisor() {
        return coSupervisor;
    }

    public void setCoSupervisor(Lecturer coSupervisor) {
        this.coSupervisor = coSupervisor;
    }

    public Lecturer getReviewer() {
        return reviewer;
    }

    public void setReviewer(Lecturer reviewer) {
        this.reviewer = reviewer;
    }

    public Council getCouncil() {
        return council;
    }

    public void setCouncil(Council council) {
        this.council = council;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public Boolean getIsFinalized() {
        return isFinalized;
    }

    public void setIsFinalized(Boolean isFinalized) {
        this.isFinalized = isFinalized;
    }

    public Double getAdvisorScore() {
        return advisorScore;
    }

    public void setAdvisorScore(Double advisorScore) {
        this.advisorScore = advisorScore;
    }

    public Double getReviewerScore() {
        return reviewerScore;
    }

    public void setReviewerScore(Double reviewerScore) {
        this.reviewerScore = reviewerScore;
    }

    public Double getCouncilScore() {
        return councilScore;
    }

    public void setCouncilScore(Double councilScore) {
        this.councilScore = councilScore;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isSupervisorOrCoSupervisor(Lecturer lecturer) {
        if (lecturer == null || lecturer.getId() == null) return false;
        if (supervisor != null && lecturer.getId().equals(supervisor.getId())) return true;
        return coSupervisor != null && lecturer.getId().equals(coSupervisor.getId());
    }

    @Transient
    public Department getDepartment() {
        if (division != null && division.getDepartment() != null) {
            return division.getDepartment();
        }
        if (major != null && major.getDepartment() != null) {
            return major.getDepartment();
        }
        if (period != null && period.getDepartment() != null) {
            return period.getDepartment();
        }
        return null;
    }
}
