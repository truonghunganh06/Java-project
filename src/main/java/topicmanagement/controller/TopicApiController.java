package topicmanagement.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import topicmanagement.entity.StudentGroup;
import topicmanagement.entity.Topic;
import topicmanagement.repository.StudentGroupRepository;
import topicmanagement.repository.TopicRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/topics", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class TopicApiController {

    private final TopicRepository topicRepository;
    private final StudentGroupRepository groupRepository;

    public TopicApiController(TopicRepository topicRepository, StudentGroupRepository groupRepository) {
        this.topicRepository = topicRepository;
        this.groupRepository = groupRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTopicDetail(@PathVariable("id") Long id) {
        Topic t = topicRepository.findById(id).orElse(null);
        if (t == null) {
            return ResponseEntity.notFound().build();
        }

        List<StudentGroup> groups = groupRepository.findByTopicId(t.getId());
        boolean hasGroup = !groups.isEmpty();
        StudentGroup group = hasGroup ? groups.get(0) : null;

        Map<String, Object> data = new HashMap<>();
        data.put("id", t.getId());
        data.put("code", t.getCode() != null ? t.getCode() : "DT-" + t.getId());
        data.put("title", t.getTitle());
        data.put("period", t.getPeriod() != null ? t.getPeriod().getName() : "Chung");
        data.put("major", t.getMajor() != null ? t.getMajor().getName() : "CNTT");
        data.put("supervisor", t.getSupervisor() != null && t.getSupervisor().getUser() != null 
                ? t.getSupervisor().getUser().getFullName() 
                : "Chưa phân công");
        data.put("coSupervisor", t.getCoSupervisor() != null && t.getCoSupervisor().getUser() != null 
                ? t.getCoSupervisor().getUser().getFullName() 
                : "Không có");
        data.put("reviewer", t.getReviewer() != null && t.getReviewer().getUser() != null 
                ? t.getReviewer().getUser().getFullName() 
                : "Chưa phân công");
        data.put("council", t.getCouncil() != null ? t.getCouncil().getName() : "Chưa phân hội đồng");
        data.put("maxStudents", t.getMaxStudents() != null ? t.getMaxStudents() : 3);
        data.put("status", t.getStatus() != null ? t.getStatus() : "PENDING");
        data.put("description", t.getDescription() != null ? t.getDescription() : "Chưa có mô tả chi tiết.");
        data.put("requirements", t.getRequirements() != null ? t.getRequirements() : "Theo hướng dẫn của Giảng viên.");
        data.put("hasGroup", hasGroup);
        data.put("groupName", group != null ? group.getGroupName() : null);
        data.put("leaderName", group != null && group.getLeader() != null && group.getLeader().getUser() != null ? group.getLeader().getUser().getFullName() : null);
        data.put("leaderCode", group != null && group.getLeader() != null ? group.getLeader().getStudentCode() : null);
        data.put("divisionId", t.getDivision() != null ? t.getDivision().getId() : null);
        data.put("divisionName", t.getDivision() != null ? t.getDivision().getName() : (t.getMajor() != null && t.getMajor().getDivision() != null ? t.getMajor().getDivision().getName() : "Bộ môn Kỹ thuật Phần mềm"));
        data.put("departmentName", t.getDepartment() != null ? t.getDepartment().getName() : null);
        // Business Rule: If no group assigned, finalScore MUST be null
        data.put("finalScore", hasGroup ? t.getFinalScore() : null);
        data.put("isFinalized", hasGroup ? t.getIsFinalized() : false);
        data.put("rejectionReason", t.getRejectionReason());

        return ResponseEntity.ok(data);
    }
}
