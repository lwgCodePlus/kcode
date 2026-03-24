package com.kcode.web;

import com.kcode.core.service.AgentService;
import com.kcode.web.dto.chat.ChatRequest;
import com.kcode.web.dto.chat.ChatResponse;
import com.kcode.web.dto.session.SessionHistoryResponse;
import com.kcode.web.dto.session.SessionListResponse;
import com.kcode.web.dto.session.SessionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agent 对话 REST 控制器
 * <p>
 * 提供对话接口，支持流式响应（SSE）和完整响应
 * 提供会话管理接口，支持会话查询和历史消息获取
 *
 * @author liwenguang
 * @since 2026/3/16
 */
@RestController
@RequestMapping("/api")
public class ChartController {

    private static final Logger logger = LoggerFactory.getLogger(ChartController.class);

    private final AgentService agentService;

    public ChartController(AgentService agentService) {
        this.agentService = agentService;
    }

    // ==================== 会话管理接口 ====================

    /**
     * 创建新会话
     * <p>
     * POST /api/session
     *
     * @return 会话ID
     */
    @PostMapping("/session")
    public ResponseEntity<SessionResponse> createSession() {
        String sessionId = agentService.createSession();
        return ResponseEntity.ok(SessionResponse.of(sessionId));
    }

    /**
     * 获取所有会话列表
     * <p>
     * GET /api/sessions
     *
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<SessionListResponse> listSessions() {
        SessionListResponse response = agentService.listSessions();
        return ResponseEntity.ok(response);
    }

    /**
     * 获取指定会话的历史消息
     * <p>
     * GET /api/session/{sessionId}/history
     *
     * @param sessionId 会话ID
     * @return 会话历史消息
     */
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<SessionHistoryResponse> getSessionHistory(@PathVariable String sessionId) {
        SessionHistoryResponse response = agentService.getSessionHistory(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * 清除会话
     * <p>
     * DELETE /api/session/{sessionId}
     *
     * @param sessionId 会话ID
     * @return 无内容响应
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        agentService.clearSession(sessionId);
        return ResponseEntity.ok().build();
    }

    // ==================== 对话接口 ====================

    /**
     * 对话接口（流式响应）
     * <p>
     * POST /api/chat
     * Content-Type: application/json
     * Accept: text/event-stream
     *
     * @param request 对话请求
     * @return SSE 流式响应
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = agentService.createSession();
        }
        
        if (request.message() == null || request.message().isBlank()) {
            return Flux.just(ChatResponse.error(sessionId, "消息内容不能为空"));
        }

        final String finalSessionId = sessionId;
        return agentService.chatStream(finalSessionId, request.message());
    }

    /**
     * 对话接口（完整响应）
     * <p>
     * POST /api/chat/complete
     * Content-Type: application/json
     *
     * @param request 对话请求
     * @return 完整响应
     */
    @PostMapping("/chat/complete")
    public Mono<ChatResponse> chatComplete(@RequestBody ChatRequest request) {
        String sessionId = request.sessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = agentService.createSession();
        }
        
        if (request.message() == null || request.message().isBlank()) {
            return Mono.just(ChatResponse.error(sessionId, "消息内容不能为空"));
        }

        final String finalSessionId = sessionId;
        return agentService.chat(finalSessionId, request.message());
    }

    /**
     * 中断对话
     * <p>
     * POST /api/chat/interrupt/{sessionId}
     *
     * @param sessionId 会话ID
     * @return 无内容响应
     */
    @PostMapping("/chat/interrupt/{sessionId}")
    public ResponseEntity<Void> interruptChat(@PathVariable String sessionId) {
        logger.info("中断对话: {}", sessionId);
        agentService.interruptSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}