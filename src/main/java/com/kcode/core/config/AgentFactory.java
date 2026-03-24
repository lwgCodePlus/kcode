package com.kcode.core.config;

import com.kcode.core.hook.AgentHook;
import com.kcode.core.model.ModelFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.memory.autocontext.ContextOffloadTool;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.Session;
import io.agentscope.core.session.SessionManager;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Agent 工厂类
 * <p>
 * 动态创建 Agent 实例，支持：
 * <ul>
 *   <li>每个会话独立的 Memory</li>
 *   <li>会话持久化和状态恢复</li>
 *   <li>动态模型配置更新</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/16
 */
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private static final String AGENT_CONFIG_PATH = "agents/kcodeAgent.md";
    private static final String PROJECT_NAME = "kcode";

    private final Toolkit toolkit;
    private final SkillBox skillBox;
    private final Session session;
    private final ModelFactory modelFactory;
    private final String agentPrompt;
    private final ExecutionConfig executionConfig;

    /**
     * 构造 AgentFactory
     *
     * @param toolkit      工具集
     * @param skillBox     技能集
     * @param session      会话存储
     * @param modelFactory 模型工厂
     */
    public AgentFactory(Toolkit toolkit, SkillBox skillBox, Session session,
                        ModelFactory modelFactory) {
        this.toolkit = toolkit;
        this.skillBox = skillBox;
        this.session = session;
        this.modelFactory = modelFactory;
        this.agentPrompt = loadAgentPrompt();
        this.executionConfig = ExecutionConfig.builder()
                .timeout(Duration.ofMinutes(2))
                .maxAttempts(2)
                .build();
    }

    /**
     * 创建新的 Agent 实例
     *
     * @param sessionId 会话ID
     * @return AgentInstance 实例
     */
    public AgentInstance createAgent(String sessionId) {

        Memory memory = createAutoContextMemory();

        Model model = modelFactory.getModel();

        ReActAgent agent = ReActAgent.builder()
                .name(PROJECT_NAME)
                .sysPrompt(agentPrompt)
                .model(model)
                .toolkit(toolkit)
                .skillBox(skillBox)
                .memory(memory)
                .maxIters(300)
                .hook(new AgentHook())
                .modelExecutionConfig(executionConfig)
                .build();

        SessionManager sessionManager = SessionManager.forSessionId(sessionId)
                .withSession(session)
                .addComponent(agent)
                .addComponent(memory);

        sessionManager.loadIfExists();

        log.debug("创建 Agent 实例完成: sessionId={}", sessionId);
        return new AgentInstance(agent, memory, sessionManager);
    }

    /**
     * 创建 Memory 实例,并注册 ContextOffloadTool
     *
     * @return Memory 实例
     */
    public Memory createAutoContextMemory() {
        AutoContextConfig config = AutoContextConfig.builder().build();
        Model model = modelFactory.getModel();
        AutoContextMemory memory = new AutoContextMemory(config, model);
        toolkit.registerTool(new ContextOffloadTool(memory));
        return memory;
    }

    /**
     * 创建 Memory 实例,用于历史消息管理（查询）
     *
     * @return Memory 实例
     */
    public Memory createMemory() {
        return new InMemoryMemory();
    }

    // ==================== 私有方法 ====================

    /**
     * 从 classpath 加载 Agent prompt配置
     *
     * @return Agent 配置内容
     */
    private String loadAgentPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource(AGENT_CONFIG_PATH);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("无法加载 Agent 配置: {}", AGENT_CONFIG_PATH, e);
            throw new IllegalStateException("无法加载 Agent 配置", e);
        }
    }

    /**
     * Agent 实例包装类
     *
     * @param agent          ReActAgent 实例
     * @param memory         Memory 实例
     * @param sessionManager 会话管理器
     */
    public record AgentInstance(ReActAgent agent, Memory memory, SessionManager sessionManager) {
    }
}