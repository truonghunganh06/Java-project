package topicmanagement.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import topicmanagement.service.StudentService;

import java.util.Map;

@RestController
@RequestMapping(value = "/api/topics", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class TopicApiController {

    private final StudentService studentService;

    public TopicApiController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTopicDetail(@PathVariable("id") Long id) {
        Map<String, Object> data = studentService.getTopicDetailMap(id);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }
}
