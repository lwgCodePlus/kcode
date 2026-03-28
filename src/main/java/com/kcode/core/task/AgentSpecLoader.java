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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能体规格加载器
 * <p>
 * 从 Markdown 文件中解析 YAML front matter 和系统提示
 * <p>
 * 支持格式：
 * <pre>
 * ---
 * name: agent-name
 * description: Agent description
 * tools: tool1, tool2
 * ---
 * System prompt content...
 * </pre>
 *
 * @author liwenguang
 * @since 2026/3/28
 */
public class AgentSpecLoader {

    private static final Logger log = LoggerFactory.getLogger(AgentSpecLoader.class);

    /**
     * YAML front matter 模式
     */
    private static final Pattern YAML_FRONT_MATTER_PATTERN = Pattern.compile(
            "^---\\s*\\n(.*?)\\n---\\s*\\n?(.*)$",
            Pattern.DOTALL
    );

    /**
     * YAML 字段模式
     */
    private static final Pattern YAML_FIELD_PATTERN = Pattern.compile(
            "^([a-zA-Z_-]+):\\s*(.*?)$",
            Pattern.MULTILINE
    );

    /**
     * 从资源加载智能体规格
     *
     * @param resource Spring 资源
     * @return 智能体规格，如果解析失败返回 null
     * @throws IOException 读取失败
     */
    public static AgentSpec loadFromResource(Resource resource) throws IOException {
        if (!resource.exists() || !resource.isReadable()) {
            log.warn("资源不存在或不可读: {}", resource.getDescription());
            return null;
        }

        String content = readContent(resource);
        return parseSpec(content, resource.getDescription());
    }

    /**
     * 从字符串内容解析智能体规格
     *
     * @param content     Markdown 内容
     * @param sourceName  来源名称（用于日志）
     * @return 智能体规格，如果解析失败返回 null
     */
    public static AgentSpec parseSpec(String content, String sourceName) {
        if (content == null || content.isBlank()) {
            log.warn("内容为空: {}", sourceName);
            return null;
        }

        Matcher matcher = YAML_FRONT_MATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            log.warn("未找到 YAML front matter: {}", sourceName);
            return null;
        }

        String yamlPart = matcher.group(1);
        String sysPrompt = matcher.group(2).trim();

        // 解析 YAML 字段
        String name = null;
        String description = null;
        String tools = null;

        Matcher fieldMatcher = YAML_FIELD_PATTERN.matcher(yamlPart);
        while (fieldMatcher.find()) {
            String fieldName = fieldMatcher.group(1).trim();
            String fieldValue = fieldMatcher.group(2).trim();

            switch (fieldName.toLowerCase()) {
                case "name" -> name = fieldValue;
                case "description" -> description = fieldValue;
                case "tools" -> tools = fieldValue;
            }
        }

        if (!StringUtils.hasText(name)) {
            log.warn("缺少 name 字段: {}", sourceName);
            return null;
        }

        log.debug("解析智能体规格成功: name={}, tools={}", name, tools);

        return new AgentSpec(name, description, tools, sysPrompt);
    }

    /**
     * 读取资源内容
     *
     * @param resource Spring 资源
     * @return 内容字符串
     * @throws IOException 读取失败
     */
    private static String readContent(Resource resource) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
            return sb.toString();
        }
    }
}