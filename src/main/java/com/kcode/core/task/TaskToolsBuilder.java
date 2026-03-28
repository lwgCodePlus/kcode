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
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 构建 Task 和 TaskOutput 工具
 * <p>
 * 使用方式：
 * <pre>
 * TaskToolsBuilder.builder()
 *     .model(model)
 *     .defaultToolsByName(defaultToolsByName)
 *     .subAgent("dependency-analyzer", dependencyAnalyzerReAct)
 *     .addAgentResource(resource)
 *     .build();
 * </pre>
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public final class TaskToolsBuilder {

    private static final Logger log = LoggerFactory.getLogger(TaskToolsBuilder.class);

    private TaskRepository taskRepository = new DefaultTaskRepository();
    private final Map<String, CallableAgent> subAgents = new HashMap<>();
    private final List<Resource> agentResources = new ArrayList<>();
    private Model model;
    private Map<String, Object> defaultToolsByName = Map.of();

    private TaskToolsBuilder() {}

    /**
     * 创建构建器
     *
     * @return 构建器实例
     */
    public static TaskToolsBuilder builder() {
        return new TaskToolsBuilder();
    }

    /**
     * 设置任务存储
     *
     * @param taskRepository 任务存储
     * @return 构建器
     */
    public TaskToolsBuilder taskRepository(TaskRepository taskRepository) {
        Assert.notNull(taskRepository, "taskRepository must not be null");
        this.taskRepository = taskRepository;
        return this;
    }

    /**
     * 设置模型
     *
     * @param model 模型实例
     * @return 构建器
     */
    public TaskToolsBuilder model(Model model) {
        this.model = model;
        return this;
    }

    /**
     * 设置默认工具映射
     *
     * @param defaultToolsByName 工具名称到实例的映射
     * @return 构建器
     */
    public TaskToolsBuilder defaultToolsByName(Map<String, Object> defaultToolsByName) {
        this.defaultToolsByName =
                defaultToolsByName != null ? Map.copyOf(defaultToolsByName) : Map.of();
        return this;
    }

    /**
     * 注册编程式子智能体
     *
     * @param type  智能体类型名称
     * @param agent ReActAgent 实例
     * @return 构建器
     */
    public TaskToolsBuilder subAgent(String type, ReActAgent agent) {
        Assert.hasText(type, "type must not be empty");
        Assert.notNull(agent, "agent must not be null");
        this.subAgents.put(type, agent);
        log.debug("注册编程式子智能体: type={}", type);
        return this;
    }

    /**
     * 添加智能体资源（Markdown 文件）
     *
     * @param resource Spring 资源
     * @return 构建器
     */
    public TaskToolsBuilder addAgentResource(Resource resource) {
        if (resource != null) {
            this.agentResources.add(resource);
            log.debug("添加智能体资源: {}", resource.getDescription());
        }
        return this;
    }

    /**
     * 构建并返回 Task 和 TaskOutput 工具
     *
     * @return 工具结果
     */
    public TaskToolsResult build() {
        Assert.notNull(taskRepository, "taskRepository must be provided");

        Map<String, CallableAgent> resolved = new HashMap<>(this.subAgents);
        loadFromResourcesAndMerge(resolved);

        Assert.notEmpty(
                resolved,
                "At least one sub-agent must be configured (via subAgent or addAgentResource)");

        TaskTool taskTool = new TaskTool(resolved, taskRepository);
        TaskOutputTool taskOutputTool = new TaskOutputTool(taskRepository);

        log.info("构建 Task 工具完成: {} 个子智能体已注册", resolved.size());
        return new TaskToolsResult(taskTool, taskOutputTool);
    }

    /**
     * 从资源加载智能体并合并
     *
     * @param into 目标映射
     */
    private void loadFromResourcesAndMerge(Map<String, CallableAgent> into) {
        if (agentResources.isEmpty()) {
            return;
        }

        Assert.notNull(model, "model must be set when using addAgentResource");
        Assert.notEmpty(defaultToolsByName, "defaultToolsByName must be set when using addAgentResource");

        AgentSpecReActAgentFactory factory =
                new AgentSpecReActAgentFactory(model, defaultToolsByName);

        for (Resource resource : agentResources) {
            try {
                if (!resource.exists() || !resource.isReadable()) {
                    log.warn("资源不存在或不可读: {}", resource.getDescription());
                    continue;
                }

                AgentSpec spec = AgentSpecLoader.loadFromResource(resource);
                if (spec != null && StringUtils.hasText(spec.name())) {
                    ReActAgent agent = factory.create(spec);
                    into.put(spec.name(), agent);
                    log.info("从资源加载子智能体: name={}, source={}", spec.name(), resource.getDescription());
                }
            } catch (IOException e) {
                log.error("加载智能体资源失败: {}", resource.getDescription(), e);
            }
        }
    }

    /**
     * 工具构建结果
     *
     * @param taskTool        Task 工具
     * @param taskOutputTool  TaskOutput 工具
     */
    public record TaskToolsResult(TaskTool taskTool, TaskOutputTool taskOutputTool) {}
}