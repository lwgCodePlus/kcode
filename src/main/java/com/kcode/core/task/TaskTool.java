/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kcode.core.task;

import io.agentscope.core.agent.CallableAgent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Task 工具
 * <p>
 * 编排智能体使用此工具将任务委托给子智能体
 * <p>
 * 参数：
 * - subagent_type: 子智能体类型名称
 * - prompt: 发送给子智能体的任务描述
 * - run_in_background: 是否后台运行（可选）
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public class TaskTool {

    private static final Logger log = LoggerFactory.getLogger(TaskTool.class);

    private final Map<String, CallableAgent> subAgents;
    private final TaskRepository taskRepository;

    /**
     * 构造 Task 工具
     *
     * @param subAgents      子智能体映射
     * @param taskRepository 任务存储
     */
    public TaskTool(Map<String, CallableAgent> subAgents, TaskRepository taskRepository) {
        this.subAgents = subAgents;
        this.taskRepository = taskRepository;
    }

    /**
     * 执行任务
     *
     * @param subagentType     子智能体类型
     * @param prompt           任务描述
     * @param runInBackground  是否后台运行
     * @return 任务结果或任务ID
     */
    @Tool(description = """
            将任务委托给子智能体执行。
            
            参数：
            - subagent_type: 子智能体类型名称（如 explorer、oracle、librarian、designer、fixer）
            - prompt: 发送给子智能体的任务描述
            - run_in_background: 可选，是否后台运行，默认 false
            """)
    public Mono<ToolResultBlock> task(
            @ToolParam(name = "subagent_type", description = "子智能体类型名称", required = true) String subagentType,
            @ToolParam(name = "prompt", description = "发送给子智能体的任务描述", required = true) String prompt,
            @ToolParam(name = "run_in_background", description = "是否后台运行，默认 false", required = false) String runInBackground
    ) {
        log.debug("执行任务: subagentType={}, runInBackground={}", subagentType, runInBackground);

        CallableAgent agent = subAgents.get(subagentType);
        if (agent == null) {
            String error = "未找到子智能体类型: " + subagentType + "。可用类型: " + String.join(", ", subAgents.keySet());
            log.warn(error);
            return Mono.just(ToolResultBlock.error(error));
        }

        boolean background = "true".equalsIgnoreCase(runInBackground);

        if (background) {
            // 后台执行，返回任务ID
            String taskId = taskRepository.createTask(subagentType, prompt, agent);
            // 异步执行任务
            executeTaskAsync(taskId, agent, prompt);
            return Mono.just(ToolResultBlock.text("<task_id>" + taskId + "</task_id>"));
        } else {
            // 同步执行
            return executeTaskSync(agent, prompt);
        }
    }

    /**
     * 同步执行任务
     *
     * @param agent 可调用智能体
     * @param prompt 任务描述
     * @return 执行结果
     */
    private Mono<ToolResultBlock> executeTaskSync(CallableAgent agent, String prompt) {
        Msg inputMsg = Msg.builder()
                .content(List.of(TextBlock.builder().text(prompt).build()))
                .build();

        return agent.call(inputMsg)
                .map(result -> {
                    if (result != null) {
                        String content = extractTextContent(result);
                        log.debug("任务完成: result长度={}", content != null ? content.length() : 0);
                        return ToolResultBlock.text(content != null ? content : "<empty_result/>");
                    }
                    return ToolResultBlock.text("<empty_result/>");
                })
                .onErrorResume(e -> {
                    log.error("任务执行失败: {}", e.getMessage(), e);
                    return Mono.just(ToolResultBlock.error("任务执行失败: " + e.getMessage()));
                });
    }

    /**
     * 异步执行任务
     *
     * @param taskId 任务ID
     * @param agent  可调用智能体
     * @param prompt 任务描述
     */
    private void executeTaskAsync(String taskId, CallableAgent agent, String prompt) {
        taskRepository.updateTaskStatus(taskId, TaskRepository.TaskStatus.RUNNING);

        Msg inputMsg = Msg.builder()
                .content(List.of(TextBlock.builder().text(prompt).build()))
                .build();

        agent.call(inputMsg)
                .subscribe(
                        result -> {
                            TaskRepository.TaskResult taskResult = new TaskRepository.TaskResult(
                                    taskId,
                                    result,
                                    null,
                                    System.currentTimeMillis()
                            );
                            taskRepository.storeTaskResult(taskId, taskResult);
                            taskRepository.updateTaskStatus(taskId, TaskRepository.TaskStatus.COMPLETED);
                            log.debug("后台任务完成: taskId={}", taskId);
                        },
                        error -> {
                            TaskRepository.TaskResult taskResult = new TaskRepository.TaskResult(
                                    taskId,
                                    null,
                                    error.getMessage(),
                                    System.currentTimeMillis()
                            );
                            taskRepository.storeTaskResult(taskId, taskResult);
                            taskRepository.updateTaskStatus(taskId, TaskRepository.TaskStatus.FAILED);
                            log.error("后台任务失败: taskId={}, error={}", taskId, error.getMessage());
                        }
                );
    }

    /**
     * 获取可用的子智能体类型列表
     *
     * @return 类型描述
     */
    public String getAvailableSubAgents() {
        StringBuilder sb = new StringBuilder("可用子智能体类型:\n");
        for (String name : subAgents.keySet()) {
            sb.append("- ").append(name).append("\n");
        }
        return sb.toString();
    }

    /**
     * 从 Msg 中提取文本内容
     *
     * @param msg 消息
     * @return 文本内容
     */
    private String extractTextContent(Msg msg) {
        if (msg == null || msg.getContent() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock textBlock) {
                String text = textBlock.getText();
                if (text != null) {
                    sb.append(text);
                }
            }
        }
        return sb.toString();
    }
}