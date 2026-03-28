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

/**
 * 智能体规格定义
 * <p>
 * 从 Markdown 文件中解析的智能体元数据和系统提示
 *
 * @param name        智能体名称（唯一标识）
 * @param description 智能体描述（用于编排智能体选择）
 * @param tools       可用工具列表（逗号分隔）
 * @param sysPrompt   系统提示内容
 * @author liwenguang
 * @since 2026/3/28
 */
public record AgentSpec(
        String name,
        String description,
        String tools,
        String sysPrompt
) {
    /**
     * 获取工具名称数组
     *
     * @return 工具名称数组
     */
    public String[] getToolNames() {
        if (tools == null || tools.isBlank()) {
            return new String[0];
        }
        return tools.split(",");
    }
}