package com.kcode.core.config;

import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * tool 工具配置
 *
 * @author liwenguang
 * @since 2026/3/9
 */
@Configuration
public class ToolkitBeanConfig {

    @Bean
    public Toolkit toolkit() {
        return new Toolkit(ToolkitConfig.builder()
                .executionConfig(ExecutionConfig.builder()
                        .timeout(Duration.ofSeconds(180))
                        .build())
                .build());
    }

}