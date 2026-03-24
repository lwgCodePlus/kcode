package com.kcode.web.dto.chat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * 消息 DTO
 * <p>
 * 用于返回历史消息
 *
 * @param role      角色（user/assistant）
 * @param content   消息内容
 * @param blocks    消息块列表
 * @param timestamp 时间戳
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageDto(
        String role,
        String content,
        List<ContentBlockDto> blocks,
        Instant timestamp
) {
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    /**
     * 创建用户消息
     *
     * @param content 消息内容
     * @return MessageDto 实例
     */
    public static MessageDto userMessage(String content) {
        return new MessageDto(ROLE_USER, content, null, null);
    }

    /**
     * 创建用户消息（带消息块）
     *
     * @param blocks 消息块列表
     * @return MessageDto 实例
     */
    public static MessageDto userMessage(List<ContentBlockDto> blocks) {
        String textContent = extractTextContent(blocks);
        return new MessageDto(ROLE_USER, textContent, blocks, null);
    }

    /**
     * 创建助手消息
     *
     * @param content 消息内容
     * @return MessageDto 实例
     */
    public static MessageDto assistantMessage(String content) {
        return new MessageDto(ROLE_ASSISTANT, content, null, null);
    }

    /**
     * 创建助手消息（带消息块）
     *
     * @param blocks 消息块列表
     * @return MessageDto 实例
     */
    public static MessageDto assistantMessage(List<ContentBlockDto> blocks) {
        String textContent = extractTextContent(blocks);
        return new MessageDto(ROLE_ASSISTANT, textContent, blocks, null);
    }

    /**
     * 从消息块中提取文本内容
     */
    private static String extractTextContent(List<ContentBlockDto> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        return blocks.stream()
                .filter(b -> ContentBlockDto.TYPE_TEXT.equals(b.type()))
                .map(ContentBlockDto::content)
                .reduce("", (a, b) -> a + (b != null ? b : ""));
    }
}