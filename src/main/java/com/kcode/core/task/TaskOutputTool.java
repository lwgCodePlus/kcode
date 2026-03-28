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

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * TaskOutput 工具
 * <p>
 * 用于获取后台任务的结果
 * <p>
 * 参数：
 * - task_id: 任务ID
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public class TaskOutputTool {

    private static final Logger log = LoggerFactory.getLogger(TaskOutputTool.class);

    private final TaskRepository taskRepository;

    /**
     * 构造 TaskOutput 工具
     *
     * @param taskRepository 任务存储
     */
    public TaskOutputTool(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    @Tool(description = """
            获取后台任务的结果。
            
            参数：
            - task_id: 任务ID（由 task 工具返回）
            """)
    public Mono<ToolResultBlock> taskOutput(
            @ToolParam(name = "task_id", description = "任务ID", required = true) String taskId
    ) {
        log.debug("获取任务结果: taskId={}", taskId);

        TaskRepository.TaskStatus status = taskRepository.getTaskStatus(taskId);
        if (status == null) {
            log.warn("任务不存在: {}", taskId);
            return Mono.just(ToolResultBlock.error("任务不存在: " + taskId));
        }

        return Mono.defer(() -> {
            TaskRepository.TaskResult result = taskRepository.getTaskResult(taskId);

            if (result == null) {
                // 任务尚未完成
                TaskRepository.TaskInfo info = taskRepository.getAllTasks().get(taskId);
                if (info != null) {
                    return Mono.just(ToolResultBlock.text(
                            "<task_status>" + status.name().toLowerCase() + "</task_status>\n" +
                            "<subagent_type>" + info.subAgentType() + "</subagent_type>\n" +
                            "<prompt>" + info.prompt() + "</prompt>"));
                }
                return Mono.just(ToolResultBlock.error("任务信息不存在"));
            }

            if (result.error() != null) {
                return Mono.just(ToolResultBlock.error(result.error()));
            }

            if (result.result() != null) {
                String content = extractTextContent(result.result());
                log.debug("返回任务结果: taskId={}, result长度={}", taskId, content != null ? content.length() : 0);
                return Mono.just(ToolResultBlock.text(content != null ? content : "<empty_result/>"));
            }

            return Mono.just(ToolResultBlock.text("<empty_result/>"));
        });
    }

    /**
     * 检查任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态描述
     */
    @Tool(description = "检查任务状态。返回任务ID、状态、子智能体类型、创建时间等信息。")
    public Mono<ToolResultBlock> taskStatus(
            @ToolParam(name = "task_id", description = "任务ID", required = true) String taskId
    ) {
        TaskRepository.TaskStatus status = taskRepository.getTaskStatus(taskId);
        if (status == null) {
            return Mono.just(ToolResultBlock.error("任务不存在: " + taskId));
        }

        TaskRepository.TaskInfo info = taskRepository.getAllTasks().get(taskId);
        StringBuilder sb = new StringBuilder();
        sb.append("任务ID: ").append(taskId).append("\n");
        sb.append("状态: ").append(status.name().toLowerCase()).append("\n");
        if (info != null) {
            sb.append("子智能体: ").append(info.subAgentType()).append("\n");
            sb.append("创建时间: ").append(info.createdAt()).append("\n");
        }

        return Mono.just(ToolResultBlock.text(sb.toString()));
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 操作结果
     */
    @Tool(description = "取消任务。只能取消进行中的任务。")
    public Mono<ToolResultBlock> cancelTask(
            @ToolParam(name = "task_id", description = "任务ID", required = true) String taskId
    ) {
        TaskRepository.TaskStatus status = taskRepository.getTaskStatus(taskId);
        if (status == null) {
            return Mono.just(ToolResultBlock.error("任务不存在: " + taskId));
        }

        if (status == TaskRepository.TaskStatus.COMPLETED || status == TaskRepository.TaskStatus.FAILED) {
            return Mono.just(ToolResultBlock.text("任务已完成或失败，无法取消"));
        }

        taskRepository.updateTaskStatus(taskId, TaskRepository.TaskStatus.CANCELLED);
        log.info("取消任务: {}", taskId);
        return Mono.just(ToolResultBlock.text("任务已取消: " + taskId));
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