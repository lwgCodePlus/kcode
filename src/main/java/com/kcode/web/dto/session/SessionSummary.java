package com.kcode.web.dto.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * 会话摘要 DTO
 * <p>
 * 用于会话列表展示
 *
 * @param sessionId    会话ID
 * @param messageCount 消息数量
 * @param lastMessage  最后一条消息摘要
 * @param createdAt    创建时间
 * @param updatedAt    更新时间
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionSummary(
        @JsonProperty("session_id") String sessionId,
        @JsonProperty("message_count") int messageCount,
        @JsonProperty("last_message") String lastMessage,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt
) {
    /**
     * 创建会话摘要（不带时间）
     *
     * @param sessionId    会话ID
     * @param messageCount 消息数量
     * @param lastMessage  最后消息摘要
     * @return SessionSummary 实例
     */
    public static SessionSummary of(String sessionId, int messageCount, String lastMessage) {
        return new SessionSummary(sessionId, messageCount, lastMessage, null, null);
    }

    /**
     * 创建会话摘要（带更新时间）
     *
     * @param sessionId    会话ID
     * @param messageCount 消息数量
     * @param lastMessage  最后消息摘要
     * @param updatedAt    更新时间
     * @return SessionSummary 实例
     */
    public static SessionSummary of(String sessionId, int messageCount, String lastMessage, Instant updatedAt) {
        return new SessionSummary(sessionId, messageCount, lastMessage, null, updatedAt);
    }

    /**
     * 创建会话摘要（带完整时间）
     *
     * @param sessionId    会话ID
     * @param messageCount 消息数量
     * @param lastMessage  最后消息摘要
     * @param createdAt    创建时间
     * @param updatedAt    更新时间
     * @return SessionSummary 实例
     */
    public static SessionSummary of(String sessionId, int messageCount, String lastMessage, 
                                     Instant createdAt, Instant updatedAt) {
        return new SessionSummary(sessionId, messageCount, lastMessage, createdAt, updatedAt);
    }
}