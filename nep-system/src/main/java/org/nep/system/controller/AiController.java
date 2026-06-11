package org.nep.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.nep.system.service.AiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI智能助手")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    public AiController(AiService s) { this.aiService = s; }

    @Operation(summary = "AI普通对话")
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody java.util.Map<String, String> body) {
        String reply = aiService.chat(body.getOrDefault("message", ""));
        return Result.ok(reply);
    }

    @Operation(summary = "AI流式对话（SSE）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody java.util.Map<String, String> body) {
        return aiService.chatStream(body.getOrDefault("message", ""));
    }
}
