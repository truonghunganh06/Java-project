package topic_management.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity lưu trữ lời mời tham gia nhóm sinh viên ("Bắt tay 2 chiều").
 * Trạng thái ban đầu: INVITED
 * Trạng thái sau xử lý: ACCEPTED, REJECTED, CANCELLED
 */
@Entity
@Table(name = "group_invitations")
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student; // Sinh viên được mời

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "invited_by_student_id", nullable = false)
    private Student invitedBy; // Nhóm trưởng gửi lời mời

    @Column(nullable = false, length = 50)
    private String status = "INVITED"; // INVITED, ACCEPTED, REJECTED, CANCELLED

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;

    public GroupInvitation() {}

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

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Student getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(Student invitedBy) {
        this.invitedBy = invitedBy;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(OffsetDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
