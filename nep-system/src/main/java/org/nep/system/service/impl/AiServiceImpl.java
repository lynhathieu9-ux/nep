package org.nep.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.nep.system.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
    private static final String API_KEY = "sk-b702e0aace564c48bf8fd12df40e1bd2";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String chat(String message) {
        try {
            String body = buildJsonBody(message, false);
            String response = sendHttpRequest(body);
            return extractContent(response);
        } catch (Exception e) {
            log.error("AI调用失败: {}", e.getMessage(), e);
            return "抱歉，AI服务暂时不可用。错误: " + e.getMessage();
        }
    }

    @Override
    public SseEmitter chatStream(String message) {
        SseEmitter emitter = new SseEmitter(300000L);

        new Thread(() -> {
            try {
                String body = buildJsonBody(message, true);
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + API_KEY)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                client.send(request, java.net.http.HttpResponse.BodyHandlers.ofLines()).body()
                        .forEach(line -> {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) return;
                                try {
                                    String content = extractStreamContent(data);
                                    if (content != null && !content.isEmpty()) {
                                        emitter.send(SseEmitter.event().data(content));
                                    }
                                } catch (Exception ignored) {}
                            }
                        });
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().data("[ERROR] AI服务请求失败: " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {}
            }
        }).start();

        return emitter;
    }

    /** 用 Jackson 构建 JSON（避免编码问题） */
    private String buildJsonBody(String message, boolean stream) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", List.of(Map.of("role", "user", "content", message)));
        body.put("stream", stream);
        return mapper.writeValueAsString(body);
    }

    /** 用 Java 11 HttpClient 发送请求（支持中文） */
    private String sendHttpRequest(String body) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        log.info("DeepSeek API response code: {}", response.statusCode());
        if (response.statusCode() != 200) {
            log.error("DeepSeek API error: {}", response.body());
            throw new RuntimeException("API返回错误: " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private String extractContent(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) return (String) msg.get("content");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractStreamContent(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(json, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                if (delta != null) return (String) delta.get("content");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
