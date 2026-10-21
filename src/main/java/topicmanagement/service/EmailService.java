package topicmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import topicmanagement.entity.AppUser;
import topicmanagement.entity.GroupMember;
import topicmanagement.entity.Student;
import topicmanagement.entity.StudentGroup;
import topicmanagement.entity.Topic;

import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Dịch vụ gửi email thông báo kết quả công bố điểm cho sinh viên theo Rubric môn học.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public EmailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email thông báo kết quả điểm số cho tất cả sinh viên trong nhóm thực hiện đề tài.
     */
    public record DeliveryResult(int sent, int failed) {}

    public DeliveryResult sendTopicResultEmailsToGroup(Topic topic, StudentGroup group) {
        if (topic == null || group == null) return new DeliveryResult(0, 1);

        List<Student> students = new ArrayList<>();
        if (group.getLeader() != null) {
            students.add(group.getLeader());
        }
        if (group.getMembers() != null) {
            for (GroupMember gm : group.getMembers()) {
                if (gm.getStudent() != null && !students.contains(gm.getStudent())) {
                    students.add(gm.getStudent());
                }
            }
        }

        int sent = 0;
        for (Student s : students) {
            if (sendResultEmailToStudent(topic, group, s)) sent++;
        }
        return new DeliveryResult(sent, students.isEmpty() ? 1 : students.size() - sent);
    }

    private boolean sendResultEmailToStudent(Topic topic, StudentGroup group, Student student) {
        if (student == null || student.getUser() == null) return false;
        AppUser user = student.getUser();
        String toEmail = user.getEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EMAIL-RESULT] Sinh viên {} ({}) chưa có email để nhận thông báo kết quả.",
                    user.getFullName(), student.getStudentCode());
            return false;
        }

        String subject = "[TopicHub] Công bố kết quả đánh giá đề tài: " + topic.getTitle();
        String finalScoreStr = topic.getFinalScore() != null ? String.format("%.2f", topic.getFinalScore()) : "Chưa có";
        String advisorScoreStr = topic.getAdvisorScore() != null ? String.format("%.2f", topic.getAdvisorScore()) : "Chưa có";
        String reviewerScoreStr = topic.getReviewerScore() != null ? String.format("%.2f", topic.getReviewerScore()) : "Chưa có";
        String councilScoreStr = topic.getCouncilScore() != null ? String.format("%.2f", topic.getCouncilScore()) : "Chưa có";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #1e293b; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 12px;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #0f172a; margin: 0;">TopicHub - Thông Báo Kết Quả Đề Tài</h2>
                    <p style="color: #64748b; font-size: 14px; margin: 5px 0 0 0;">Khoa Công nghệ Thông tin</p>
                </div>
                <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 15px 0;" />
                <p>Kính gửi sinh viên <strong>%s</strong> (MSSV: <strong>%s</strong>),</p>
                <p>Khoa xin thông báo kết quả đánh giá chính thức cho đề tài của nhóm bạn:</p>
                <table style="width: 100%%; border-collapse: collapse; margin: 15px 0; font-size: 14px;">
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #f8fafc; font-weight: bold; width: 35%%;">Tên đề tài:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0;">%s (%s)</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #f8fafc; font-weight: bold;">Tên nhóm:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0;">%s</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #f8fafc; font-weight: bold;">Điểm GV Hướng dẫn:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0;">%s / 10</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #f8fafc; font-weight: bold;">Điểm GV Phản biện:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0;">%s / 10</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #f8fafc; font-weight: bold;">Điểm Hội đồng:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0;">%s / 10</td>
                    </tr>
                    <tr>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #e0f2fe; color: #0284c7; font-weight: bold; font-size: 16px;">Điểm Tổng Kết:</td>
                        <td style="padding: 8px; border: 1px solid #e2e8f0; background: #e0f2fe; color: #0284c7; font-weight: bold; font-size: 18px;">%s / 10</td>
                    </tr>
                </table>
                <p>Bạn có thể đăng nhập vào hệ thống để tra cứu chi tiết nhận xét của các thầy cô tại trang Tra cứu kết quả.</p>
                <div style="margin-top: 25px; padding-top: 15px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8; text-align: center;">
                    Email này được gửi tự động từ Hệ thống Quản lý Đề tài Sinh viên TopicHub.
                </div>
            </div>
            """.formatted(
                user.getFullName(),
                student.getStudentCode() != null ? student.getStudentCode() : user.getUsername(),
                topic.getTitle(),
                topic.getCode() != null ? topic.getCode() : "N/A",
                group.getGroupName(),
                advisorScoreStr,
                reviewerScoreStr,
                councilScoreStr,
                finalScoreStr
        );

        if (mailSender == null || senderEmail == null || senderEmail.isBlank()) {
            log.warn("[EMAIL-NOT-CONFIGURED] Chưa cấu hình SMTP; email kết quả chưa được gửi.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[EMAIL-SUCCESS] Đã gửi thông báo kết quả đề tài '{}' tới sinh viên {} <{}>",
                    topic.getTitle(), user.getFullName(), toEmail);
            return true;
        } catch (Exception e) {
            log.error("[EMAIL-ERROR] Gửi email kết quả tới {} thất bại: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
