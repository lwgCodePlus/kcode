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
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 任务存储接口
 * <p>
 * 用于存储和管理后台任务的状态和结果
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public interface TaskRepository {

    /**
     * 创建新任务
     *
     * @param subAgentType 子智能体类型
     * @param prompt       任务提示
     * @param agent        可调用智能体
     * @return 任务ID
     */
    String createTask(String subAgentType, String prompt, CallableAgent agent);

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    TaskStatus getTaskStatus(String taskId);

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @return 任务结果
     */
    TaskResult getTaskResult(String taskId);

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     */
    void updateTaskStatus(String taskId, TaskStatus status);

    /**
     * 存储任务结果
     *
     * @param taskId 任务ID
     * @param result 任务结果
     */
    void storeTaskResult(String taskId, TaskResult result);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     */
    void deleteTask(String taskId);

    /**
     * 获取所有任务
     *
     * @return 任务映射
     */
    Map<String, TaskInfo> getAllTasks();

    /**
     * 任务状态枚举
     */
    enum TaskStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * 任务信息
     */
    record TaskInfo(
            String taskId,
            String subAgentType,
            String prompt,
            TaskStatus status,
            long createdAt
    ) {}

    /**
     * 任务结果
     */
    record TaskResult(
            String taskId,
            Msg result,
            String error,
            long completedAt
    ) {}
}