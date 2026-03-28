package com.kcode.core.config;

import com.kcode.core.hook.AgentHook;
import com.kcode.core.model.ModelFactory;
import com.kcode.core.task.DefaultTaskRepository;
import com.kcode.core.task.TaskOutputTool;
import com.kcode.core.task.TaskRepository;
import com.kcode.core.task.TaskTool;
import com.kcode.core.task.TaskToolsBuilder;
import com.kcode.core.tool.astgrep.AstGrepTool;
import com.kcode.core.tool.context7.Context7Tool;
import com.kcode.core.tool.glob.GlobTool;
import com.kcode.core.tool.grep.GrepTool;
import com.kcode.core.tool.grepapp.GrepAppTool;
import com.kcode.core.tool.webfetch.WebFetchTool;
import com.kcode.core.utils.CommonUtil;
import io.agentscope.core.ReActAgent;
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
import io.agentscope.core.tool.coding.ShellCommandTool;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent 工厂类
 * <p>
 * 动态创建 Agent 实例，支持：
 * <ul>
 *   <li>每个会话独立的 Memory</li>
 *   <li>会话持久化和状态恢复</li>
 *   <li>动态模型配置更新</li>
 *   <li>多智能体支持（动态创建 SubAgentTools）</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/16
 */
public class AgentFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentFactory.class);

    private static final String AGENT_CONFIG_PATH = "agents/orchestratorAgent.md";
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
     * <p>
     * 每次调用时动态创建 SubAgentTools（因为 model 可能变化）
     *
     * @param sessionId 会话ID
     * @return AgentInstance 实例，如果 Model 未配置则返回 null
     */
    public AgentInstance createAgent(String sessionId) {
        Model model = modelFactory.getModel();
        if (model == null) {
            log.warn("Model 未配置，无法创建 Agent");
            return null;
        }

        // 动态创建并注册子智能体工具
        registerSubAgentToolsIfNeeded(model);

        Memory memory = createAutoContextMemory();

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
     * @return Memory 实例，如果 Model 未配置则返回 null
     */
    public Memory createAutoContextMemory() {
        Model model = modelFactory.getModel();
        AutoContextConfig config = AutoContextConfig.builder().build();
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
        AutoContextConfig config = AutoContextConfig.builder().build();
        return new AutoContextMemory(config, null);
    }

    // ==================== 私有方法 ====================

    /**
     * 按需注册子智能体工具
     * <p>
     * 由于 model 是动态的，只在第一次创建 Agent 时注册
     */
    private synchronized void registerSubAgentToolsIfNeeded(Model model) {
        try {
            SubAgentTools subAgentTools = createSubAgentTools(model);
            if (subAgentTools.taskTool() != null) {
                toolkit.registerTool(subAgentTools.taskTool());
                toolkit.registerTool(subAgentTools.taskOutputTool());
                log.info("已注册 Task 和 TaskOutput 工具到 Toolkit");
            }
        } catch (Exception e) {
            log.error("注册子智能体工具失败", e);
        }
    }

    /**
     * 创建子智能体工具
     *
     * @param model 模型实例
     * @return 子智能体工具包装
     */
    private SubAgentTools createSubAgentTools(Model model) throws IOException {
        String workDir = CommonUtil.getWorkingDir();

        // 创建默认工具
        ReadFileTool readFileTool = new ReadFileTool(workDir);
        WriteFileTool writeFileTool = new WriteFileTool(workDir);
        GlobTool globTool = new GlobTool(workDir);
        GrepTool grepTool = new GrepTool(workDir);
        AstGrepTool astGrepTool = new AstGrepTool(workDir);
        ShellCommandTool shellCommandTool = new ShellCommandTool();
        WebFetchTool webFetchTool = new WebFetchTool();
        GrepAppTool grepAppTool = new GrepAppTool();
        Context7Tool context7Tool = new Context7Tool();

        // MCP 工具
        McpClientWrapper webSearchExa = null;
        try {
            webSearchExa = McpClientBuilder.create("web_search_exa")
                    .streamableHttpTransport("https://mcp.exa.ai/mcp")
                    .queryParam("tools", "web_search_exa")
                    .buildAsync()
                    .block();
        } catch (Exception ignored) {
        }

        // 工具名称映射
        Map<String, Object> defaultToolsByName = new HashMap<>();
        defaultToolsByName.put("read", readFileTool);
        defaultToolsByName.put("write", writeFileTool);
        defaultToolsByName.put("glob", globTool);
        defaultToolsByName.put("grep", grepTool);
        defaultToolsByName.put("astGrep", astGrepTool);
        defaultToolsByName.put("bash", shellCommandTool);
        defaultToolsByName.put("webFetch", webFetchTool);
        if (webSearchExa != null) {
            defaultToolsByName.put("webSearch", webSearchExa);
        }
        defaultToolsByName.put("grepApp", grepAppTool);
        defaultToolsByName.put("context7", context7Tool);
        // 别名
        defaultToolsByName.put("view_text_file", readFileTool);
        defaultToolsByName.put("write_text_file", writeFileTool);
        defaultToolsByName.put("insert_text_file", writeFileTool);
        defaultToolsByName.put("web_search_exa", webSearchExa);

        // 任务存储
        TaskRepository taskRepository = new DefaultTaskRepository();

        // 使用 TaskToolsBuilder 构建工具
        TaskToolsBuilder builder = TaskToolsBuilder.builder()
                .model(model)
                .defaultToolsByName(defaultToolsByName)
                .taskRepository(taskRepository);

        // 加载 classpath 中的智能体定义文件
        Resource[] agentResources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:agents/*Agent.md");

        for (Resource resource : agentResources) {
            if (resource.getFilename() != null && !resource.getFilename().equals("orchestratorAgent.md")) {
                builder.addAgentResource(resource);
                log.info("加载子智能体定义: {}", resource.getFilename());
            }
        }

        TaskToolsBuilder.TaskToolsResult result = builder.build();
        return new SubAgentTools(result.taskTool(), result.taskOutputTool(), defaultToolsByName);
    }

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

    /**
     * 子智能体工具包装
     *
     * @param taskTool           Task 工具
     * @param taskOutputTool     TaskOutput 工具
     * @param defaultToolsByName 默认工具映射
     */
    public record SubAgentTools(
            TaskTool taskTool,
            TaskOutputTool taskOutputTool,
            Map<String, Object> defaultToolsByName
    ) {
    }
}