package com.kcode.core.service;

import com.kcode.web.dto.chat.ContentBlockDto;
import com.kcode.web.dto.chat.MessageDto;
import io.agentscope.core.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息转换器
 * <p>
 * 负责 AgentScope 消息与前端 DTO 之间的转换，包括：
 * <ul>
 *   <li>Msg 到 MessageDto 的转换</li>
 *   <li>ContentBlock 到 ContentBlockDto 的转换</li>
 *   <li>内部消息块（如 __fragment__）的过滤</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/22
 */
@Component
public class MessageConverter {

    private static final Logger logger = LoggerFactory.getLogger(MessageConverter.class);

    /**
     * 内部消息块名称，需要过滤
     */
    private static final String FRAGMENT_BLOCK_NAME = "__fragment__";

    /**
     * 将 Msg 列表转换为 MessageDto 列表
     *
     * @param messages Msg 列表
     * @return MessageDto 列表
     */
    public List<MessageDto> convertMessages(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        return messages.stream()
                .map(this::convertMessage)
                .collect(Collectors.toList());
    }

    /**
     * 将单个 Msg 转换为 MessageDto
     *
     * @param msg Msg 实例
     * @return MessageDto 实例
     */
    public MessageDto convertMessage(Msg msg) {
        if (msg == null) {
            return null;
        }

        List<ContentBlockDto> blocks = convertContentBlocks(msg.getContent());
        String role = msg.getRole() != null ? msg.getRole().name().toLowerCase() : "user";

        if ("user".equals(role)) {
            return MessageDto.userMessage(blocks);
        } else {
            return MessageDto.assistantMessage(blocks);
        }
    }

    /**
     * 将 AgentScope 的 ContentBlock 列表转换为 DTO 列表
     *
     * @param contentBlocks AgentScope 的 ContentBlock 列表
     * @return DTO 列表
     */
    public List<ContentBlockDto> convertContentBlocks(List<ContentBlock> contentBlocks) {
        if (contentBlocks == null || contentBlocks.isEmpty()) {
            return new ArrayList<>();
        }

        List<ContentBlockDto> result = new ArrayList<>();
        for (ContentBlock block : contentBlocks) {
            ContentBlockDto dto = convertContentBlock(block);
            if (dto != null) {
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 将单个 ContentBlock 转换为 DTO
     *
     * @param block ContentBlock 实例
     * @return ContentBlockDto 实例，如果无法转换或需要过滤则返回 null
     */
    public ContentBlockDto convertContentBlock(ContentBlock block) {
        if (block == null) {
            return null;
        }

        if (block instanceof TextBlock textBlock) {
            String text = textBlock.getText();
            return text != null && !text.isEmpty() ? ContentBlockDto.text(text) : null;

        } else if (block instanceof ThinkingBlock thinkingBlock) {
            String thinking = thinkingBlock.getThinking();
            return thinking != null && !thinking.isEmpty() ? ContentBlockDto.thinking(thinking) : null;

        } else if (block instanceof ToolUseBlock toolUseBlock) {
            return convertToolUseBlock(toolUseBlock);

        } else if (block instanceof ToolResultBlock toolResultBlock) {
            return convertToolResultBlock(toolResultBlock);
        }

        logger.debug("未知的 ContentBlock 类型: {}", block.getClass().getName());
        return null;
    }

    /**
     * 过滤 Msg 中的内部消息块
     * <p>
     * 移除工具名称为 "__fragment__" 的消息块，这些是 agentscope 内部使用的增量块
     *
     * @param msg 原始消息
     * @return 过滤后的消息
     */
    public Msg filterInternalBlocks(Msg msg) {
        if (msg == null || msg.getContent() == null || msg.getContent().isEmpty()) {
            return msg;
        }

        List<ContentBlock> filteredBlocks = msg.getContent().stream()
                .filter(block -> !isInternalBlock(block))
                .collect(Collectors.toList());

        return Msg.builder()
                .id(msg.getId())
                .role(msg.getRole())
                .content(filteredBlocks)
                .build();
    }

    /**
     * 判断是否为内部消息块
     *
     * @param block 消息块
     * @return true 表示是内部消息块，需要过滤
     */
    public boolean isInternalBlock(ContentBlock block) {
        if (block instanceof ToolUseBlock toolUseBlock) {
            return FRAGMENT_BLOCK_NAME.equals(toolUseBlock.getName());
        }
        if (block instanceof ToolResultBlock toolResultBlock) {
            return FRAGMENT_BLOCK_NAME.equals(toolResultBlock.getName());
        }
        return false;
    }

    // ==================== 私有方法 ====================

    private ContentBlockDto convertToolUseBlock(ToolUseBlock toolUseBlock) {
        String name = toolUseBlock.getName();
        Object input = toolUseBlock.getInput();

        // 过滤掉 name 等于 __fragment__ 的消息
        if (name == null || FRAGMENT_BLOCK_NAME.equals(name)) {
            return null;
        }
        return ContentBlockDto.toolUse(name, input);
    }

    private ContentBlockDto convertToolResultBlock(ToolResultBlock toolResultBlock) {
        String name = toolResultBlock.getName();
        List<ContentBlock> outputBlocks = toolResultBlock.getOutput();

        // 过滤掉 name 等于 __fragment__ 的消息
        if (name == null || FRAGMENT_BLOCK_NAME.equals(name)) {
            return null;
        }

        List<ContentBlockDto> outputDtos = convertContentBlocks(outputBlocks);
        return ContentBlockDto.toolResult(name, outputDtos);
    }
}