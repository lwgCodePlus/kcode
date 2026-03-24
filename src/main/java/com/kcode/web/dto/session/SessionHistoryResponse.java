package com.kcode.web.dto.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kcode.web.dto.chat.MessageDto;

import java.util.List;

/**
 * 会话历史响应 DTO
 * <p>
 * 用于返回指定会话的历史消息
 *
 * @param sessionId    会话ID
 * @param messages     消息列表
 * @param messageCount 消息数量
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionHistoryResponse(
        @JsonProperty("session_id") String sessionId,
        List<MessageDto> messages,
        @JsonProperty("message_count") int messageCount
) {
    /**
     * 创建会话历史响应
     *
     * @param sessionId 会话ID
     * @param messages  消息列表
     * @return SessionHistoryResponse 实例
     */
    public static SessionHistoryResponse of(String sessionId, List<MessageDto> messages) {
        return new SessionHistoryResponse(sessionId, messages, messages != null ? messages.size() : 0);
    }
}