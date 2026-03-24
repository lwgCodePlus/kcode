package com.kcode.web.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 会话响应 DTO
 * <p>
 * 用于返回新创建的会话信息
 *
 * @param sessionId 会话ID
 * @param message   描述信息
 * @author liwenguang
 * @since 2026/3/16
 */
public record SessionResponse(
        @JsonProperty("session_id") String sessionId,
        String message
) {
    /**
     * 创建会话响应
     *
     * @param sessionId 会话ID
     * @return SessionResponse 实例
     */
    public static SessionResponse of(String sessionId) {
        return new SessionResponse(sessionId, "Session created successfully");
    }
}