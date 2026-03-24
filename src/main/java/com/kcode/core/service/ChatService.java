package com.kcode.core.service;

import com.kcode.core.config.AgentFactory.AgentInstance;
import com.kcode.web.dto.chat.ChatResponse;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 对话服务
 * <p>
 * 处理 Agent 对话的核心逻辑，支持：
 * <ul>
 *   <li>流式响应处理</li>
 *   <li>非流式响应处理</li>
 *   <li>消息过滤和转换</li>
 *   <li>会话状态持久化</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/22
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final SessionCacheService sessionCacheService;
    private final MessageConverter messageConverter;

    /**
     * 流式选项 - 仅返回推理结果
     */
    private final StreamOptions streamOptions;

    /**
     * 构造 ChatService
     *
     * @param sessionCacheService 会话缓存服务
     * @param messageConverter    消息转换器
     */
    public ChatService(SessionCacheService sessionCacheService, MessageConverter messageConverter) {
        this.sessionCacheService = sessionCacheService;
        this.messageConverter = messageConverter;
        this.streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                .incremental(false)
                .build();
    }

    /**
     * 发送消息并获取流式响应
     * <p>
     * 流式响应设计原则：
     * <ul>
     *   <li>isLast=false: 中间块，发送给前端实时更新 UI</li>
     *   <li>isLast=true: 完整消息，可安全持久化，触发保存会话</li>
     *   <li>流结束: 通过 concatWith 追加 finished=true 的终端事件</li>
     *   <li>过滤: 移除 __fragment__ 等内部消息块</li>
     * </ul>
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 流式响应
     */
    public Flux<ChatResponse> chatStream(String sessionId, String message) {
        return Flux.defer(() -> {
            logger.info("会话 {} 收到消息: {}", sessionId, message);

            AgentInstance instance = sessionCacheService.getOrCreate(sessionId);

            Msg userMsg = Msg.builder()
                    .textContent(message)
                    .build();

            return instance.agent().stream(userMsg, streamOptions)
                    .doOnNext(event -> {
                        // 当 isLast=true 时保存会话（消息完整，可持久化）
                        if (event.isLast()) {
                            sessionCacheService.saveSessionToStorage(instance);
                        }
                    })
                    .map(event -> {
                        Msg msg = event.getMessage();
                        // 过滤掉 __fragment__ 等内部消息块
                        Msg filteredMsg = messageConverter.filterInternalBlocks(msg);
                        return ChatResponse.of(sessionId, filteredMsg);
                    })
                    .filter(response -> response.msg() != null)
                    // 流结束时追加 finished=true 的终端事件
                    .concatWith(Mono.fromCallable(() -> ChatResponse.complete(sessionId)))
                    // 兜底保存：确保流结束时一定会保存
                    .doOnComplete(() -> sessionCacheService.saveSessionToStorage(instance))
                    .onErrorResume(e -> {
                        logger.error("会话 {} 处理消息出错: {}", sessionId, e.getMessage(), e);
                        Model model = instance.agent().getModel();
                        if (model == null) {
                            return Flux.just(ChatResponse.error(sessionId, "处理消息出错，没有可用的模型"));
                        }
                        return Flux.just(ChatResponse.error(sessionId, e.getMessage()));
                    });
        });
    }

    /**
     * 发送消息并获取完整响应
     *
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return 完整响应
     */
    public Mono<ChatResponse> chat(String sessionId, String message) {
        return chatStream(sessionId, message)
                .filter(response -> response.msg() != null)
                .reduce((last, current) -> current) // 取最后一个有内容的响应
                .map(response -> new ChatResponse(sessionId, response.msg(), true, null))
                .onErrorResume(e -> {
                    logger.error("会话 {} 处理消息出错: {}", sessionId, e.getMessage(), e);
                    return Mono.just(ChatResponse.error(sessionId, e.getMessage()));
                });
    }

    /**
     * 中断会话的流式输出
     *
     * @param sessionId 会话ID
     */
    public void interrupt(String sessionId) {
        AgentInstance instance = sessionCacheService.getIfPresent(sessionId);
        if (instance != null) {
            instance.agent().interrupt();
            logger.info("已中断会话: {}", sessionId);
        }
    }
}