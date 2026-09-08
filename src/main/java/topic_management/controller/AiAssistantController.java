package topic_management.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import topic_management.entity.AppUser;
import topic_management.service.AiAssistantService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/ai", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(value = "message", required = false) String paramMessage,
            HttpSession session) {

        AppUser user = (AppUser) session.getAttribute("authenticatedUser");
        String message = (body != null && body.containsKey("message")) ? body.get("message") : paramMessage;

        AiAssistantService.AiResponse response = aiAssistantService.processQuery(user, message);

        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("reply", response.reply());
        res.put("suggestions", response.suggestions());

        return ResponseEntity.ok(res);
    }
}
