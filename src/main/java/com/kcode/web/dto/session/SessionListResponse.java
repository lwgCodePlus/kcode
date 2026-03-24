package com.kcode.web.dto.session;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 会话列表响应 DTO
 * <p>
 * 用于返回所有会话列表
 *
 * @param sessions 会话列表
 * @param total    总数
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionListResponse(
        List<SessionSummary> sessions,
        int total
) {
    /**
     * 创建会话列表响应
     *
     * @param sessions 会话列表
     * @return SessionListResponse 实例
     */
    public static SessionListResponse of(List<SessionSummary> sessions) {
        return new SessionListResponse(sessions, sessions != null ? sessions.size() : 0);
    }
}