package topic_management.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "registration_periods")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "period_type", nullable = false, length = 50)
    private String periodType; // MON_HOC, NCKH, TLCN, KLTN

    @Column(name = "academic_year", nullable = false, length = 50)
    private String academicYear = "2026-2027";

    @Column(nullable = false)
    private Integer semester = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "lecturer_proposal_start")
    private OffsetDateTime lecturerProposalStart;

    @Column(name = "lecturer_proposal_end")
    private OffsetDateTime lecturerProposalEnd;

    @Column(name = "student_registration_start")
    private OffsetDateTime studentRegistrationStart;

    @Column(name = "student_registration_end")
    private OffsetDateTime studentRegistrationEnd;

    @Column(name = "reviewer_deadline")
    private OffsetDateTime reviewerDeadline;

    @Column(name = "council_defense_date")
    private OffsetDateTime councilDefenseDate;

    @Column(nullable = false, length = 50)
    private String status = "OPEN"; // DRAFT, OPEN, IN_PROGRESS, COMPLETED, CLOSED

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RegistrationPeriod() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getPeriodTypeName() {
        if (periodType == null) return "";
        return switch (periodType.toUpperCase()) {
            case "MON_HOC" -> "Đồ án Môn học";
            case "NCKH" -> "Nghiên cứu Khoa học";
            case "TLCN" -> "Tiểu luận Chuyên ngành";
            case "KLTN" -> "Khóa luận Tốt nghiệp";
            default -> periodType;
        };
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public OffsetDateTime getLecturerProposalStart() {
        return lecturerProposalStart;
    }

    public void setLecturerProposalStart(OffsetDateTime lecturerProposalStart) {
        this.lecturerProposalStart = lecturerProposalStart;
    }

    public OffsetDateTime getLecturerProposalEnd() {
        return lecturerProposalEnd;
    }

    public void setLecturerProposalEnd(OffsetDateTime lecturerProposalEnd) {
        this.lecturerProposalEnd = lecturerProposalEnd;
    }

    public OffsetDateTime getStudentRegistrationStart() {
        return studentRegistrationStart;
    }

    public void setStudentRegistrationStart(OffsetDateTime studentRegistrationStart) {
        this.studentRegistrationStart = studentRegistrationStart;
    }

    public OffsetDateTime getStudentRegistrationEnd() {
        return studentRegistrationEnd;
    }

    public void setStudentRegistrationEnd(OffsetDateTime studentRegistrationEnd) {
        this.studentRegistrationEnd = studentRegistrationEnd;
    }

    public OffsetDateTime getReviewerDeadline() {
        return reviewerDeadline;
    }

    public void setReviewerDeadline(OffsetDateTime reviewerDeadline) {
        this.reviewerDeadline = reviewerDeadline;
    }

    public OffsetDateTime getCouncilDefenseDate() {
        return councilDefenseDate;
    }

    public void setCouncilDefenseDate(OffsetDateTime councilDefenseDate) {
        this.councilDefenseDate = councilDefenseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Helper checks
    public boolean isLecturerProposalOpen() {
        OffsetDateTime now = OffsetDateTime.now();
        if (lecturerProposalStart != null && now.isBefore(lecturerProposalStart)) return false;
        if (lecturerProposalEnd != null && now.isAfter(lecturerProposalEnd)) return false;
        return Boolean.TRUE.equals(isActive) && ("OPEN".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status));
    }

    public boolean isStudentRegistrationOpen() {
        OffsetDateTime now = OffsetDateTime.now();
        if (studentRegistrationStart != null && now.isBefore(studentRegistrationStart)) return false;
        if (studentRegistrationEnd != null && now.isAfter(studentRegistrationEnd)) return false;
        return Boolean.TRUE.equals(isActive) && ("OPEN".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status));
    }
}
