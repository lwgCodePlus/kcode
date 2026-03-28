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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认任务存储实现
 * <p>
 * 使用内存 ConcurrentHashMap 存储任务状态和结果
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public class DefaultTaskRepository implements TaskRepository {

    private static final Logger log = LoggerFactory.getLogger(DefaultTaskRepository.class);

    private final Map<String, TaskInfo> taskInfoMap = new ConcurrentHashMap<>();
    private final Map<String, TaskResult> taskResultMap = new ConcurrentHashMap<>();
    private final Map<String, CallableAgent> taskAgentMap = new ConcurrentHashMap<>();
    private final Map<String, String> taskPromptMap = new ConcurrentHashMap<>();

    @Override
    public String createTask(String subAgentType, String prompt, CallableAgent agent) {
        String taskId = "task_" + UUID.randomUUID().toString().substring(0, 8);
        long createdAt = System.currentTimeMillis();

        TaskInfo info = new TaskInfo(taskId, subAgentType, prompt, TaskStatus.PENDING, createdAt);
        taskInfoMap.put(taskId, info);
        taskAgentMap.put(taskId, agent);
        taskPromptMap.put(taskId, prompt);

        log.debug("创建任务: taskId={}, subAgentType={}", taskId, subAgentType);
        return taskId;
    }

    @Override
    public TaskStatus getTaskStatus(String taskId) {
        TaskInfo info = taskInfoMap.get(taskId);
        return info != null ? info.status() : null;
    }

    @Override
    public TaskResult getTaskResult(String taskId) {
        return taskResultMap.get(taskId);
    }

    @Override
    public void updateTaskStatus(String taskId, TaskStatus status) {
        TaskInfo info = taskInfoMap.get(taskId);
        if (info != null) {
            TaskInfo updated = new TaskInfo(
                    info.taskId(),
                    info.subAgentType(),
                    info.prompt(),
                    status,
                    info.createdAt()
            );
            taskInfoMap.put(taskId, updated);
            log.debug("更新任务状态: taskId={}, status={}", taskId, status);
        }
    }

    @Override
    public void storeTaskResult(String taskId, TaskResult result) {
        taskResultMap.put(taskId, result);
        log.debug("存储任务结果: taskId={}", taskId);
    }

    @Override
    public void deleteTask(String taskId) {
        taskInfoMap.remove(taskId);
        taskResultMap.remove(taskId);
        taskAgentMap.remove(taskId);
        taskPromptMap.remove(taskId);
        log.debug("删除任务: taskId={}", taskId);
    }

    @Override
    public Map<String, TaskInfo> getAllTasks() {
        return Map.copyOf(taskInfoMap);
    }

    /**
     * 获取任务对应的智能体
     *
     * @param taskId 任务ID
     * @return 可调用智能体
     */
    public CallableAgent getTaskAgent(String taskId) {
        return taskAgentMap.get(taskId);
    }

    /**
     * 获取任务对应的提示
     *
     * @param taskId 任务ID
     * @return 任务提示
     */
    public String getTaskPrompt(String taskId) {
        return taskPromptMap.get(taskId);
    }

    /**
     * 清除所有任务
     */
    public void clearAll() {
        taskInfoMap.clear();
        taskResultMap.clear();
        taskAgentMap.clear();
        taskPromptMap.clear();
        log.debug("清除所有任务");
    }
}