package com.kcode.core.tool;

import com.kcode.core.tool.astgrep.AstGrepTool;
import com.kcode.core.tool.context7.Context7Tool;
import com.kcode.core.tool.glob.GlobTool;
import com.kcode.core.tool.grep.GrepTool;
import com.kcode.core.tool.grepapp.GrepAppTool;
import com.kcode.core.tool.webfetch.WebFetchTool;
import com.kcode.core.utils.CommonUtil;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.coding.ShellCommandTool;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;


/**
 * 工具注册配置
 * <p>
 * 提供统一的工具注册机制：
 * <ul>
 *   <li>所有工具通过 @Bean 方法定义</li>
 *   <li>自动收集并注册到 Toolkit</li>
 *   <li>添加新工具只需在此类中添加 @Bean 方法</li>
 * </ul>
 * <p>
 * 扩展方式：
 * <pre>
 * &#64;Bean
 * public NewTool newTool() {
 *     return new NewTool(CommonUtil.getWorkingDir());
 * }
 * </pre>
 *
 * @author liwenguang
 * @since 2026/3/22
 */
@Configuration
public class ToolRegistryConfig {

    private static final Logger logger = LoggerFactory.getLogger(ToolRegistryConfig.class);

    private final Toolkit toolkit;

    public ToolRegistryConfig(Toolkit toolkit) {
        this.toolkit = toolkit;
    }

    /**
     * 自动注册所有工具到 Toolkit
     * <p>
     * 应用启动完成后自动执行，将所有工具注册到 Toolkit
     * 添加新工具只需：
     * 1. 添加 @Bean 方法定义
     * 2. 在此方法中调用 toolkit.registerTool(xxxTool())
     *
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void registerAllTools() {
        logger.info("开始注册工具到 Toolkit...");

        // 文件操作工具
        toolkit.registerTool(new ReadFileTool(CommonUtil.getWorkingDir()));
        toolkit.registerTool(new WriteFileTool(CommonUtil.getWorkingDir()));
        toolkit.registerTool(new GlobTool(CommonUtil.getWorkingDir()));
        toolkit.registerTool(new GrepTool(CommonUtil.getWorkingDir()));
        toolkit.registerTool(new AstGrepTool(CommonUtil.getWorkingDir()));
        toolkit.registerTool(new ShellCommandTool());

        toolkit.registerTool(new WebFetchTool());
        toolkit.registerTool(new GrepAppTool());
        toolkit.registerTool(new Context7Tool());

        McpClientWrapper webSearchExa = McpClientBuilder.create("web_search_exa")
                .streamableHttpTransport("https://mcp.exa.ai/mcp")
                .queryParam("tools", "web_search_exa")
                .buildAsync()
                .block();
        toolkit.registerMcpClient(webSearchExa).block();

        logger.info("工具注册完成");
    }
}