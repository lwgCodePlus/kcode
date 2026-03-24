package com.kcode.web.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 对话请求 DTO
 * <p>
 * 用于接收客户端发送的对话消息
 *
 * @param sessionId 会话ID，可选，用于保持会话连续性
 * @param message   用户消息内容
 * @param stream    是否使用流式响应，默认 true
 * @author liwenguang
 * @since 2026/3/16
 */
public record ChatRequest(
        @JsonProperty("session_id") String sessionId,
        String message,
        Boolean stream
) {
    /**
     * 判断是否使用流式响应
     *
     * @return true 表示流式响应，默认为 true
     */
    public boolean isStream() {
        return stream == null || stream;
    }
}