package com.kcode.core.tool.grep;

import com.kcode.core.tool.glob.Globber;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unix4j.Unix4j;
import org.unix4j.unix.Grep;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Grep 工具 - 基于 Unix4j 实现
 * <p>
 * 支持正则表达式搜索文件内容
 * 输出格式：file:lineNum:content
 * 异步工具，返回 Mono<String>，框架自动转换为 ToolResultBlock
 *
 * @author liwenguang
 * @since 2026/3/11
 */
public class GrepTool {

    private static final Logger logger = LoggerFactory.getLogger(GrepTool.class);

    /**
     * 最大搜索文件数限制（安全限制）
     */
    private static final int MAX_FILES_TO_SEARCH = 1000;

    /**
     * 默认结果数量限制
     */
    private static final int DEFAULT_HEAD_LIMIT = 100;

    /**
     * 最大结果数量限制（安全限制）
     */
    private static final int MAX_HEAD_LIMIT = 2000;

    private final Path baseDir;

    public GrepTool(String baseDir) {
        this.baseDir = baseDir != null ? Paths.get(baseDir).toAbsolutePath().normalize() : null;
    }

    @Tool(description = """
            - 快速内容搜索工具，适用于任何大小的代码库
            - 使用正则表达式搜索文件内容
            - 支持完整的正则表达式语法（例如 "log.*Error"、"function\\s+\\w+" 等）
            - 使用 include 参数按模式过滤文件（例如 "*.js"、"*.{ts,tsx}"）
            - 返回至少有一个匹配的文件路径和行号和内容，按修改时间排序
            - 当你需要查找包含特定模式的文件时使用此工具
            - 输出格式：file:lineNum:content
            """)
    public Mono<ToolResultBlock> grep(
            @ToolParam(name = "pattern", description = "要在文件内容中搜索的正则表达式模式", required = true) String pattern,
            @ToolParam(name = "include", description = "搜索时要包含的文件匹配模式（例如：\"*.js\"、\"*.{ts,tsx}\"）") String include,
            @ToolParam(name = "path", description = "要在其中进行搜索的目录。默认为当前工作目录。") String path,
            @ToolParam(name = "headLimit", description = "将输出限制在前 N 条结果。0 或省略表示不限制。") String headLimit
    ) {

        return Mono.fromCallable(() -> {
            // 1. 参数验证
            if (pattern == null || pattern.trim().isEmpty()) {
                return ToolResultBlock.error("Error: pattern parameter is required");
            }

            // 验证正则表达式是否有效
            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                return ToolResultBlock.error("Error: Invalid regex pattern: " + e.getMessage());
            }

            // 确定搜索路径
            Path searchPath = resolveSearchPath(path);
            if (!Files.exists(searchPath)) {
                return ToolResultBlock.error("Error: Path does not exist: " + searchPath);
            }
            if (!Files.isDirectory(searchPath)) {
                return ToolResultBlock.error("Error: Path is not a directory: " + searchPath);
            }

            // 解析结果数量限制
            int limit = parseHeadLimit(headLimit);

            // 2. 获取要搜索的文件列表
            List<Path> filesToSearch = findFilesToSearch(searchPath, include);

            if (filesToSearch.isEmpty()) {
                return ToolResultBlock.text("No files found matching the include pattern");
            }

            // 3. 执行grep搜索
            String result = executeGrep(pattern, filesToSearch, limit);

            return ToolResultBlock.text(result);

        }).onErrorResume((e) -> {
            logger.error("Error executing grep: {}", e.getMessage(), e);
            return Mono.just(ToolResultBlock.error("Error: " + e.getMessage()));
        });
    }

    /**
     * 解析搜索路径
     */
    private Path resolveSearchPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return baseDir != null ? baseDir : Paths.get(System.getProperty("user.dir"));
        }

        Path resolved = Paths.get(path);
        if (!resolved.isAbsolute()) {
            resolved = baseDir != null ? baseDir.resolve(path) : Paths.get(System.getProperty("user.dir")).resolve(path);
        }

        return resolved.toAbsolutePath().normalize();
    }

    /**
     * 解析结果数量限制
     */
    private int parseHeadLimit(String headLimit) {
        if (headLimit == null || headLimit.trim().isEmpty()) {
            return DEFAULT_HEAD_LIMIT;
        }

        try {
            int limit = Integer.parseInt(headLimit.trim());
            if (limit <= 0) {
                return MAX_HEAD_LIMIT;
            }
            return Math.min(limit, MAX_HEAD_LIMIT);
        } catch (NumberFormatException e) {
            return DEFAULT_HEAD_LIMIT;
        }
    }

    /**
     * 查找要搜索的文件
     */
    private List<Path> findFilesToSearch(Path searchPath, String include) {
        try {
            String includePattern = parseIncludePattern(include);

            return Globber.builder(searchPath)
                    .include(includePattern)
                    .exclude("**/target/**")
                    .exclude("**/node_modules/**")
                    .exclude("**/.git/**")
                    .exclude("**/.idea/**")
                    .exclude("**/*.class")
                    .exclude("**/*.jar")
                    .exclude("**/*.bin")
                    .recursive(true)
                    .build()
                    .find(Globber.SortOrder.MODIFIED_ASC, MAX_FILES_TO_SEARCH);
        } catch (IOException e) {
            logger.warn("Error finding files to search: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 解析include模式，转换为glob格式
     */
    private String parseIncludePattern(String include) {
        if (include == null || include.trim().isEmpty()) {
            return "**/*";
        }

        String pattern = include.trim();

        // 处理类似 *.{ts,tsx} 的模式
        if (pattern.startsWith("*.{") && pattern.endsWith("}")) {
            String extensions = pattern.substring(2, pattern.length() - 1);
            String[] exts = extensions.split(",");
            return "**/*" + exts[0].trim();
        }

        // 如果模式不以 **/ 开头，添加 **/ 使其递归搜索
        if (!pattern.startsWith("**/")) {
            return "**/" + pattern;
        }

        return pattern;
    }

    /**
     * 执行grep搜索
     */
    private String executeGrep(String pattern, List<Path> files, int limit) {
        StringBuilder result = new StringBuilder();
        int totalCount = 0;

        for (Path file : files) {
            if (totalCount >= limit) {
                break;
            }

            try {
                String fileResult = grepFile(pattern, file.toFile(), limit - totalCount);
                if (fileResult != null && !fileResult.isEmpty()) {
                    result.append(fileResult);
                    String[] lines = fileResult.split("\n");
                    totalCount += lines.length;
                }
            } catch (Exception e) {
                logger.debug("Error grepping file {}: {}", file, e.getMessage());
            }
        }

        String output = result.toString().trim();
        return output.isEmpty() ? "No matches found" : output;
    }

    /**
     * 对单个文件执行grep
     */
    private String grepFile(String pattern, File file, int remainingLimit) {
        // 使用 unix4j 的 lineNumber 选项在输出中包含行号
        String contentWithLineNumbers = Unix4j.grep(Grep.Options.lineNumber, pattern, file).toStringResult();

        if (contentWithLineNumbers == null || contentWithLineNumbers.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = contentWithLineNumbers.split("\n");
        int count = 0;

        for (String line : lines) {
            if (count >= remainingLimit) {
                break;
            }
            // 格式：file:lineNum:content
            sb.append(file.toString()).append(":").append(line).append("\n");
            count++;
        }

        return sb.toString();
    }
}