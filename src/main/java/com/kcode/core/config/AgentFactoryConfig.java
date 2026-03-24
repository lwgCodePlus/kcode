package com.kcode.core.config;

import com.kcode.core.model.ModelFactory;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/**
 * AgentFactory 配置类
 * <p>
 * 创建 AgentFactory Bean，用于动态创建 Agent 实例
 *
 * @author liwenguang
 * @since 2026/3/16
 */
@Configuration
public class AgentFactoryConfig {

    /**
     * 默认会话存储路径
     */
    private static final String DEFAULT_SESSION_DIR = ".kcode/sessions";

    private final Toolkit toolkit;
    private final SkillBox skillBox;
    private final ModelFactory modelFactory;

    public AgentFactoryConfig(Toolkit toolkit, SkillBox skillBox, ModelFactory modelFactory) {
        this.toolkit = toolkit;
        this.skillBox = skillBox;
        this.modelFactory = modelFactory;
    }

    /**
     * 创建 AgentFactory Bean
     * <p>
     *
     * @return AgentFactory 实例
     */
    @Bean
    public AgentFactory agentFactory() {
        return new AgentFactory(toolkit, skillBox, session(), modelFactory);
    }

    @Bean
    public Session session() {
        return new JsonSession(Path.of(System.getProperty("user.home"), DEFAULT_SESSION_DIR));
    }
}