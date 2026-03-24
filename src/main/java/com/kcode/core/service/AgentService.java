package com.kcode.core.service;

import com.kcode.web.dto.chat.ChatResponse;
import com.kcode.web.dto.session.SessionHistoryResponse;
import com.kcode.web.dto.session.SessionListResponse;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SimpleSessionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Agent 服务门面
 * <p>
 * 作为统一的入口点，协调各专项服务：
 * <ul>
 *   <li>{@link SessionCacheService} - 会话缓存管理</li>
 *   <li>{@link ChatService} - 对话处理</li>
 *   <li>{@link SessionQueryService} - 会话查询</li>
 * </ul>
 * <p>
 * 设计原则：
 * <ul>
 *   <li>保持向后兼容，所有公共 API 不变</li>
 *   <li>委托给专项服务，不包含业务逻辑</li>
 *   <li>便于测试和扩展</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/16
 */
@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    private final Session session;
    private final SessionCacheService sessionCacheService;
    private final ChatService chatService;
    private final SessionQueryService sessionQueryService;

    /**
     * 构造 AgentService
     *
     * @param session             会话存储
     * @param sessionCacheService 会话缓存服务
     * @param chatService         对话服务
     * @param sessionQueryService 会话查询服务
     */
    public AgentService(Session session,
                        SessionCacheService sessionCacheService,
                        ChatService chatService,
                        SessionQueryService sessionQueryService) {
        this.session = session;
        this.sessionCacheService = sessionCacheService;
        this.chatService = chatService;
        this.sessionQueryService = sessionQueryService;

        logger.info("AgentService 初始化完成");
    }

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     *
     * @return 新会话ID
     */
    public String createSession() {
        String sessionId = "session_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        logger.info("创建新会话: {}", sessionId);
        return sessionId;
    }

    /**
     * 清除会话
     * <p>
     * 从缓存中移除并删除持久化数据
     *
     * @param sessionId 会话ID
     */
    public void clearSession(String sessionId) {
        sessionCacheService.invalidate(sessionId);
        session.delete(SimpleSessionKey.of(sessionId));
        logger.info("清除会话: {}", sessionId);
    }

    /**
     * 中断会话的流式输出
     *
     * @param sessionId 会话ID
     */
    public void interruptSession(String sessionId) {
        chatService.interrupt(sessionId);
    }

    /**
     * 清除所有缓存的 Agent 实例
     * <p>
     * 用于配置更新后强制刷新所有 Agent
     */
    public void invalidateAllCachedAgents() {
        sessionCacheService.invalidateAll();
    }

    // ==================== 对话处理 ====================

    /**
     * 发送消息并获取流式响应
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 流式响应
     */
    public Flux<ChatResponse> chatStream(String sessionId, String message) {
        return chatService.chatStream(sessionId, message);
    }

    /**
     * 发送消息并获取完整响应
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 完整响应
     */
    public Mono<ChatResponse> chat(String sessionId, String message) {
        return chatService.chat(sessionId, message);
    }

    // ==================== 会话查询 ====================

    /**
     * 获取所有会话列表
     *
     * @return 会话列表响应
     */
    public SessionListResponse listSessions() {
        return sessionQueryService.listSessions();
    }

    /**
     * 获取指定会话的历史消息
     *
     * @param sessionId 会话ID
     * @return 会话历史响应
     */
    public SessionHistoryResponse getSessionHistory(String sessionId) {
        return sessionQueryService.getSessionHistory(sessionId);
    }
}