package com.kcode.core.tool.glob;

import com.kcode.core.utils.CommonUtil;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Glob tool for fast file pattern matching with safety limits.
 */
public class GlobTool {

    private static final Logger logger = LoggerFactory.getLogger(GlobTool.class);

    private final Path baseDir;

    public GlobTool(String baseDir) {
        this.baseDir = baseDir != null ? Paths.get(baseDir).toAbsolutePath().normalize() : null;
    }

    @Tool(description = """
            - 快速文件模式匹配工具，适用于任何大小的代码库
            - 支持 glob 模式，如 "**/*.js" 或 "src/**/*.ts"
            - 返回匹配的文件路径，按修改时间排序
            - 当你需要按名称模式查找文件时使用此工具
            """)
    public Mono<ToolResultBlock> glob(
            @ToolParam(name = "pattern", description = "用于匹配文件的 glob 模式", required = true) String pattern,
            @ToolParam(name = "path",
                    description = """
                            要搜索的目录。如果未指定，则使用当前工作目录。
                            重要提示：若要使用默认目录，请省略此字段。不要填写 "undefined" 或 "null" 只需省略此字段即可使用默认行为。
                            """,
                    required = false) String path
    ) {

        return Mono.fromCallable(() -> {
            return ToolResultBlock.text(Globber.builder(CommonUtil.validatePath(path, this.baseDir))
                    .include(pattern)
                    .exclude("**/target/**")
                    .exclude("**/node_modules/**")
                    .exclude("**/.git/**")
                    .exclude("**/.idea/**")
                    .exclude("**/*.class")
                    .exclude("**/*.jar")
                    .exclude("**/*.bin")
                    .recursive(true)
                    .build()
                    .findAsStrings(Globber.SortOrder.MODIFIED_ASC, 2000));
        }).onErrorResume((e) -> {
            logger.error("Error listing directory '{}': {}", new Object[]{baseDir, path, e.getMessage(), e});
            return Mono.just(ToolResultBlock.error("Error: " + e.getMessage()));
        });
    }
}
