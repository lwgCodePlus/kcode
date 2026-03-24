package com.kcode.core.service;

import com.kcode.core.config.AgentFactory;
import com.kcode.core.config.AgentFactory.AgentInstance;
import com.kcode.web.dto.chat.MessageDto;
import com.kcode.web.dto.session.SessionHistoryResponse;
import com.kcode.web.dto.session.SessionListResponse;
import com.kcode.web.dto.session.SessionSummary;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 会话查询服务
 * <p>
 * 提供会话相关的查询功能：
 * <ul>
 *   <li>会话列表查询</li>
 *   <li>会话历史消息查询</li>
 *   <li>会话元数据获取</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/22
 */
@Service
public class SessionQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SessionQueryService.class);

    private final Session session;
    private final AgentFactory agentFactory;
    private final SessionCacheService sessionCacheService;
    private final MessageConverter messageConverter;

    /**
     * 构造 SessionQueryService
     *
     * @param session             会话存储
     * @param agentFactory        Agent 工厂
     * @param sessionCacheService 会话缓存服务
     * @param messageConverter    消息转换器
     */
    public SessionQueryService(Session session,
                               AgentFactory agentFactory,
                               SessionCacheService sessionCacheService,
                               MessageConverter messageConverter) {
        this.session = session;
        this.agentFactory = agentFactory;
        this.sessionCacheService = sessionCacheService;
        this.messageConverter = messageConverter;
    }

    /**
     * 获取所有会话列表
     *
     * @return 会话列表响应
     */
    public SessionListResponse listSessions() {
        try {
            Set<SessionKey> sessionKeys = session.listSessionKeys();
            List<SessionSummary> summaries = sessionKeys.stream()
                    .map(this::buildSessionSummary)
                    .sorted((a, b) -> {
                        // 按更新时间倒序排列（最新的在前面）
                        if (a.updatedAt() == null && b.updatedAt() == null) return 0;
                        if (a.updatedAt() == null) return 1;
                        if (b.updatedAt() == null) return -1;
                        return b.updatedAt().compareTo(a.updatedAt());
                    })
                    .collect(Collectors.toList());
            logger.info("查询会话列表，共 {} 个会话", summaries.size());
            return SessionListResponse.of(summaries);
        } catch (Exception e) {
            logger.error("查询会话列表失败: {}", e.getMessage(), e);
            return SessionListResponse.of(new ArrayList<>());
        }
    }

    /**
     * 获取指定会话的历史消息
     *
     * @param sessionId 会话ID
     * @return 会话历史响应
     */
    public SessionHistoryResponse getSessionHistory(String sessionId) {
        try {
            // 先尝试从缓存获取
            AgentInstance cachedInstance = sessionCacheService.getIfPresent(sessionId);
            if (cachedInstance != null) {
                List<Msg> messages = cachedInstance.memory().getMessages();
                List<MessageDto> messageDtos = messageConverter.convertMessages(messages);
                logger.info("从缓存获取会话 {} 历史消息，共 {} 条", sessionId, messageDtos.size());
                return SessionHistoryResponse.of(sessionId, messageDtos);
            }

            // 从持久化存储加载
            if (!session.exists(SimpleSessionKey.of(sessionId))) {
                logger.warn("会话不存在: {}", sessionId);
                return SessionHistoryResponse.of(sessionId, new ArrayList<>());
            }

            // 创建新的 Memory 并加载状态
            Memory memory = agentFactory.createMemory();
            memory.loadFrom(session, sessionId);

            List<Msg> messages = memory.getMessages();
            List<MessageDto> messageDtos = messageConverter.convertMessages(messages);
            return SessionHistoryResponse.of(sessionId, messageDtos);
        } catch (Exception e) {
            logger.error("获取会话 {} 历史消息失败: {}", sessionId, e.getMessage(), e);
            return SessionHistoryResponse.of(sessionId, new ArrayList<>());
        }
    }

    // ==================== 私有方法 ====================

    private SessionSummary buildSessionSummary(SessionKey key) {
        String sessionId = ((SimpleSessionKey) key).sessionId();
        int messageCount = getMessageCount(sessionId);
        String lastMessage = getLastMessage(sessionId);
        Instant updatedAt = getUpdatedAt(sessionId);
        return SessionSummary.of(sessionId, messageCount, lastMessage, updatedAt);
    }

    /**
     * 获取会话消息数量
     */
    private int getMessageCount(String sessionId) {
        try {
            if (!session.exists(SimpleSessionKey.of(sessionId))) {
                return 0;
            }
            Memory memory = agentFactory.createMemory();
            memory.loadFrom(session, sessionId);
            return memory.getMessages().size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 获取会话最后一条消息摘要
     */
    private String getLastMessage(String sessionId) {
        try {
            if (!session.exists(SimpleSessionKey.of(sessionId))) {
                return null;
            }
            Memory memory = agentFactory.createMemory();
            memory.loadFrom(session, sessionId);
            List<Msg> messages = memory.getMessages();
            if (messages.isEmpty()) {
                return null;
            }
            Msg lastMsg = messages.getLast();
            String textContent = lastMsg.getTextContent();
            if (textContent != null && textContent.length() > 100) {
                return textContent.substring(0, 100) + "...";
            }
            return textContent;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取会话更新时间
     * <p>
     * 通过会话目录的最后修改时间判断
     */
    private Instant getUpdatedAt(String sessionId) {
        try {
            // JsonSession 的存储路径格式: {sessionPath}/{sessionId}/
            Path sessionDir = Path.of(
                    System.getProperty("user.home"),
                    ".kcode",
                    "sessions",
                    sessionId
            );
            if (Files.exists(sessionDir)) {
                long lastModified = Files.getLastModifiedTime(sessionDir).toMillis();
                return Instant.ofEpochMilli(lastModified);
            }
        } catch (Exception e) {
            logger.debug("获取会话更新时间失败: {}", sessionId, e.getMessage());
        }
        return null;
    }
}