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

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 智能体规格 ReActAgent 工厂
 * <p>
 * 从 AgentSpec 创建 ReActAgent 实例，使用共享的 Model 和工具映射
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public class AgentSpecReActAgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentSpecReActAgentFactory.class);

    private final Model model;
    private final Map<String, Object> defaultToolsByName;

    /**
     * 构造工厂
     *
     * @param model              模型实例
     * @param defaultToolsByName 工具名称到实例的映射
     */
    public AgentSpecReActAgentFactory(Model model, Map<String, Object> defaultToolsByName) {
        Assert.notNull(model, "model must not be null");
        Assert.notNull(defaultToolsByName, "defaultToolsByName must not be null");
        this.model = model;
        this.defaultToolsByName = defaultToolsByName;
    }

    /**
     * 从规格创建 ReActAgent
     *
     * @param spec 智能体规格
     * @return ReActAgent 实例
     */
    public ReActAgent create(AgentSpec spec) {
        Assert.notNull(spec, "spec must not be null");

        // 创建 Toolkit 并注册指定工具
        Toolkit toolkit = new Toolkit();
        String[] toolNames = spec.getToolNames();
        for (String toolName : toolNames) {
            String name = toolName.trim();
            Object tool = defaultToolsByName.get(name);
            if (tool != null) {
                toolkit.registerTool(tool);
                log.debug("注册工具 {} 到智能体 {}", name, spec.name());
            } else {
                log.warn("工具 {} 不存在于默认工具映射，智能体 {}", name, spec.name());
            }
        }

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name(spec.name())
                .description(spec.description())
                .sysPrompt(spec.sysPrompt())
                .model(model)
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(50)
                .build();

        log.info("创建智能体: name={}, description={}", spec.name(), spec.description());
        return agent;
    }
}