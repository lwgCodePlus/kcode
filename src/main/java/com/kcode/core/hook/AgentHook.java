package com.kcode.core.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 钩子
 * <p>
 * 在 Agent 推理前注入提示消息，引导 Agent 优先使用技能
 *
 * @author liwenguang
 * @since 2026/3/21
 */
public class AgentHook implements Hook {

    /**
     * 技能使用提示消息
     */
    private static final String SKILL_HINT_MESSAGE = 
            "若某项技能适用于你的任务，你别无选择，必须使用skill";

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            return handlePreReasoning(e).map(evt -> event);
        }
        return Mono.just(event);
    }

    /**
     * 处理推理前事件
     * <p>
     * 在用户消息前插入技能使用提示
     *
     * @param event 推理前事件
     * @return 处理后的事件
     */
    private Mono<PreReasoningEvent> handlePreReasoning(PreReasoningEvent event) {
        List<Msg> messages = new ArrayList<>(event.getInputMessages());
        
        if (messages.isEmpty()) {
            return Mono.just(event);
        }

        Msg lastMsg = messages.getLast();
        
        // 只在用户消息前插入提示
        if (MsgRole.USER.equals(lastMsg.getRole())) {
            Msg hintMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(List.of(TextBlock.builder()
                            .text(SKILL_HINT_MESSAGE)
                            .build()))
                    .build();
            
            // 在最后一条用户消息前插入提示
            messages.add(messages.size() - 1, hintMsg);
            event.setInputMessages(messages);
        }

        return Mono.just(event);
    }
}