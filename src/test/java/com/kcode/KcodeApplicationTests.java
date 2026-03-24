package com.kcode;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class KcodeApplicationTests {

    @Resource
    private ReActAgent agent;

    @Test
    void contextLoads() {
        StreamOptions streamOptions = StreamOptions.builder()
                .eventTypes(EventType.REASONING)
                .incremental(true)
                .build();
        Msg userMsg = Msg.builder().textContent("获取 https://java.agentscope.io/zh/quickstart/agent.html 内容").build();
        agent.stream(userMsg, streamOptions).doOnNext(e -> {
            List<ContentBlock> content = e.getMessage().getContent();
            ContentBlock contentBlock = content.getFirst();
            if (contentBlock instanceof ThinkingBlock thinkingBlock) {
                System.out.println(thinkingBlock.getThinking());
            } else if (contentBlock instanceof TextBlock textBlock) {
                System.out.println(textBlock.getText());
            } else if (contentBlock instanceof ToolUseBlock toolUseBlock) {
                System.out.println(toolUseBlock.getName() + "--" + toolUseBlock.getInput() + "--" + toolUseBlock.getContent());
            }

        }).subscribe();

        try {
            Thread.sleep(10000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
