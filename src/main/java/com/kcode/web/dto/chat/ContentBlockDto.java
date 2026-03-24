package com.kcode.web.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 消息块 DTO
 * <p>
 * 表示 Agent 响应中的单个消息块，支持多种类型：
 * - text: 文本内容
 * - thinking: 思考过程
 * - tool_use: 工具调用
 * - tool_result: 工具执行结果
 *
 * @param type       消息块类型 (text/thinking/tool_use/tool_result)
 * @param content    文本内容（text 类型）
 * @param thinking   思考内容（thinking 类型）
 * @param toolName   工具名称（tool_use/tool_result 类型）
 * @param toolInput  工具输入（tool_use 类型）
 * @param toolOutput 工具输出（tool_result 类型）
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContentBlockDto(
        String type,
        String content,
        String thinking,
        @JsonProperty("tool_name") String toolName,
        @JsonProperty("tool_input") Object toolInput,
        @JsonProperty("tool_output") List<ContentBlockDto> toolOutput
) {
    /** 消息块类型常量 */
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_THINKING = "thinking";
    public static final String TYPE_TOOL_USE = "tool_use";
    public static final String TYPE_TOOL_RESULT = "tool_result";

    /**
     * 创建文本消息块
     *
     * @param content 文本内容
     * @return ContentBlockDto 实例
     */
    public static ContentBlockDto text(String content) {
        return new ContentBlockDto(TYPE_TEXT, content, null, null, null, null);
    }

    /**
     * 创建思考消息块
     *
     * @param thinking 思考内容
     * @return ContentBlockDto 实例
     */
    public static ContentBlockDto thinking(String thinking) {
        return new ContentBlockDto(TYPE_THINKING, null, thinking, null, null, null);
    }

    /**
     * 创建工具调用消息块
     *
     * @param toolName  工具名称
     * @param toolInput 工具输入
     * @return ContentBlockDto 实例
     */
    public static ContentBlockDto toolUse(String toolName, Object toolInput) {
        return new ContentBlockDto(TYPE_TOOL_USE, null, null, toolName, toolInput, null);
    }

    /**
     * 创建工具执行结果消息块
     *
     * @param toolName   工具名称
     * @param toolOutput 工具输出
     * @return ContentBlockDto 实例
     */
    public static ContentBlockDto toolResult(String toolName, List<ContentBlockDto> toolOutput) {
        return new ContentBlockDto(TYPE_TOOL_RESULT, null, null, toolName, null, toolOutput);
    }
}