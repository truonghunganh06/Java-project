package topic_management.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity lưu trữ yêu cầu hủy nhóm / hủy đề tài của Nhóm trưởng ở Giai đoạn Đang thực hiện.
 * Phải qua GVHD duyệt và kích hoạt EC-03: giải tán nhóm, giải phóng mọi người,
 * chuyển trạng thái đề tài (Topic) quay lại APPROVED để nhóm khác có thể đăng ký.
 */
@Entity
@Table(name = "group_dissolve_requests")
public class GroupDissolveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "requested_by_student_id", nullable = false)
    private Student requestedBy; // Nhóm trưởng yêu cầu hủy

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic; // Đề tài cần giải phóng

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resolved_by_lecturer_id")
    private Lecturer resolvedBy; // GVHD duyệt/từ chối

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    public GroupDissolveRequest() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StudentGroup getGroup() {
        return group;
    }

    public void setGroup(StudentGroup group) {
        this.group = group;
    }

    public Student getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Student requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Lecturer getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Lecturer resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
