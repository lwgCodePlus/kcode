package com.kcode.web.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.agentscope.core.message.Msg;

/**
 * 对话响应 DTO
 * <p>
 * 包装 AgentScope 的 Msg 对象，添加业务字段（sessionId、finished、error）
 * 前端可以直接使用 Msg 的内容，无需了解内部转换逻辑
 *
 * @param sessionId 会话ID
 * @param msg       消息对象（直接来自 AgentScope）
 * @param finished  是否完成（true 表示整个流结束）
 * @param error     错误信息，可选
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        @JsonProperty("session_id") String sessionId,
        Msg msg,
        Boolean finished,
        String error
) {
    /**
     * 创建流式响应（包装 Msg）
     *
     * @param sessionId 会话ID
     * @param msg       消息对象
     * @return ChatResponse 实例，finished=false
     */
    public static ChatResponse of(String sessionId, Msg msg) {
        return new ChatResponse(sessionId, msg, false, null);
    }

    /**
     * 创建流完成响应（终端事件）
     * <p>
     * 用于标识整个流式响应的结束，finished=true 表示流已完成
     *
     * @param sessionId 会话ID
     * @return ChatResponse 实例，finished=true，无消息内容
     */
    public static ChatResponse complete(String sessionId) {
        return new ChatResponse(sessionId, null, true, null);
    }

    /**
     * 创建错误响应
     *
     * @param sessionId 会话ID
     * @param error     错误信息
     * @return ChatResponse 实例，finished=true
     */
    public static ChatResponse error(String sessionId, String error) {
        return new ChatResponse(sessionId, null, true, error);
    }
}