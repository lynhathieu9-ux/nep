package org.nep.system.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    /** 普通对话 */
    String chat(String message);
    /** 流式对话（SSE） */
    SseEmitter chatStream(String message);
}
