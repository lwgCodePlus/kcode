package com.kcode.core.tool.astgrep;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AstGrep 工具 - AST 级别代码搜索与重构
 * <p>
 * 支持 25+ 种语言的 AST 代码搜索、替换、计数操作。
 * 使用 ast-grep CLI 实现。
 * <p>
 * 提供四个核心工具：
 * <ul>
 *   <li>dump_syntax_tree - 转储代码的语法树结构</li>
 *   <li>test_match_code_rule - 测试代码是否匹配 YAML 规则</li>
 *   <li>find_code - 使用模式搜索代码</li>
 *   <li>find_code_by_rule - 使用 YAML 规则搜索代码</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/20
 */
public class AstGrepTool {

    private static final Logger logger = LoggerFactory.getLogger(AstGrepTool.class);

    /**
     * 支持的语言列表
     */
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "bash", "c", "cpp", "csharp", "css", "elixir", "go", "haskell",
            "html", "java", "javascript", "json", "jsx", "kotlin", "lua",
            "nix", "php", "python", "ruby", "rust", "scala", "solidity",
            "swift", "tsx", "typescript", "yaml"
    );

    private static final String SUPPORTED_LANGUAGES_STR = String.join(", ", SUPPORTED_LANGUAGES);

    /**
     * 最大结果数量限制
     */
    private static final int MAX_RESULTS = 1000;

    private final Path baseDir;

    public AstGrepTool() {
        this(null);
    }

    public AstGrepTool(String baseDir) {
        this.baseDir = baseDir != null ? Paths.get(baseDir).toAbsolutePath().normalize() : null;
    }

    // ==================== Tool 1: dump_syntax_tree ====================

    /**
     * 转储代码的语法树结构
     * <p>
     * 用于发现正确的语法类型和语法树结构，调试规则时非常有用。
     */
    @Tool(description = """
            转储代码的语法树结构或查询模式的结构。
            这对于发现正确的语法类型和语法树结构很有用。在调试规则时调用它。
            
            该工具需要三个参数：code、language 和 format。前两个不言自明。
            `format` 是语法树的输出格式：
            - 使用 `format=cst` 检查代码的具体语法树结构，用于调试目标代码
            - 使用 `format=pattern` 检查 ast-grep 如何解释模式，用于调试模式规则
            - 使用 `format=ast` 检查抽象语法树
            
            内部调用: ast-grep run --pattern <code> --lang <language> --debug-query=<format>
            """)
    public Mono<ToolResultBlock> dump_syntax_tree(
            @ToolParam(name = "code", description = "需要分析的代码", required = true) String code,
            @ToolParam(name = "language", description = "代码语言。支持: bash, c, cpp, csharp, css, elixir, go, haskell, html, java, javascript, json, jsx, kotlin, lua, nix, php, python, ruby, rust, scala, solidity, swift, tsx, typescript, yaml", required = true) String language,
            @ToolParam(name = "format", description = "语法树输出格式。可用值: pattern, ast, cst。默认为 cst", required = false) String format
    ) {
        return Mono.fromCallable(() -> {
            if (code == null || code.trim().isEmpty()) {
                return ToolResultBlock.error("Error: code parameter is required");
            }
            if (language == null || language.trim().isEmpty()) {
                return ToolResultBlock.error("Error: language parameter is required");
            }

            String formatValue = (format != null && !format.trim().isEmpty()) ? format : "cst";

            try {
                List<String> command = buildBaseCommand();
                command.add("run");
                command.add("--pattern");
                command.add(code);
                command.add("--lang");
                command.add(language);
                command.add("--debug-query=" + formatValue);

                ProcessResult result = executeCommand(command, null, null);
                return ToolResultBlock.text(result.stderr);
            } catch (Exception e) {
                logger.error("Error executing dump_syntax_tree: {}", e.getMessage(), e);
                return ToolResultBlock.error("Error: " + e.getMessage());
            }
        });
    }

    // ==================== Tool 2: test_match_code_rule ====================

    /**
     * 测试代码是否匹配 YAML 规则
     * <p>
     * 用于在项目中使用规则之前测试它。
     */
    @Tool(description = """
            使用 ast-grep YAML 规则测试代码。
            这对于在项目中使用规则之前测试它很有用。
            
            参数:
            - code: 要测试的代码
            - yaml: ast-grep YAML 规则。必须包含 id, language, rule 字段
            
            内部调用: ast-grep scan --inline-rules <yaml> --json --stdin
            
            返回匹配列表，如果没有匹配则抛出错误。
            提示: 如果 inside/has 规则没有找到匹配，尝试添加 `stopBy: end`
            """)
    public Mono<ToolResultBlock> test_match_code_rule(
            @ToolParam(name = "code", description = "要测试的代码", required = true) String code,
            @ToolParam(name = "yaml", description = "ast-grep YAML 规则。必须包含 id, language, rule 字段", required = true) String yaml
    ) {
        return Mono.fromCallable(() -> {
            if (code == null || code.trim().isEmpty()) {
                return ToolResultBlock.error("Error: code parameter is required");
            }
            if (yaml == null || yaml.trim().isEmpty()) {
                return ToolResultBlock.error("Error: yaml parameter is required");
            }

            Path tempYamlFile = null;
            try {
                ProcessResult result;
                
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // 创建临时 YAML 文件
                    tempYamlFile = Files.createTempFile("ast-grep-rule-", ".yml");
                    Files.writeString(tempYamlFile, yaml);
                    
                    List<String> command = buildBaseCommand();
                    command.add("scan");
                    command.add("--rule");
                    command.add(tempYamlFile.toString());
                    command.add("--json");
                    command.add("--stdin");
                    
                    result = executeCommand(command, code, null);
                } else {
                    List<String> command = buildBaseCommand();
                    command.add("scan");
                    command.add("--inline-rules");
                    command.add(yaml);
                    command.add("--json");
                    command.add("--stdin");
                    
                    result = executeCommand(command, code, null);
                }

                String stdout = result.stdout.trim();
                if (stdout.isEmpty() || stdout.equals("[]")) {
                    return ToolResultBlock.error(
                            "No matches found for the given code and rule. " +
                            "Try adding `stopBy: end` to your inside/has rule."
                    );
                }

                List<MatchResult> matches = parseJsonLines(stdout);
                return ToolResultBlock.text(formatMatchesAsText(matches));
            } catch (Exception e) {
                logger.error("Error executing test_match_code_rule: {}", e.getMessage(), e);
                return ToolResultBlock.error("Error: " + e.getMessage());
            } finally {
                // 清理临时文件
                if (tempYamlFile != null) {
                    try {
                        Files.deleteIfExists(tempYamlFile);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temp file: {}", tempYamlFile);
                    }
                }
            }
        });
    }

    // ==================== Tool 3: find_code ====================

    /**
     * 使用模式搜索代码
     * <p>
     * Pattern 适用于简单和单个 AST 节点结果。
     * 对于更复杂的用法，请使用 find_code_by_rule。
     */
    @Tool(description = """
            在项目文件夹中查找匹配给定 ast-grep 模式的代码。
            Pattern 适用于简单和单个 AST 节点结果。
            对于更复杂的用法，请使用 find_code_by_rule。
            
            模式语法:
            - $VAR: 匹配单个 AST 节点
            - $$$: 匹配多个 AST 节点
            - 示例: function $NAME($ARGS) { $$$BODY }
            
            输出格式:
            - text (默认): 紧凑文本格式，包含 file:line-range 头和完整匹配文本
            - json: 完整匹配对象，包含范围、元变量等元数据
            
            内部调用: ast-grep run --pattern <pattern> [--json] <project_folder>
            
            示例用法:
            - find_code(project_folder="/path/to/project", pattern="class $NAME")
            - find_code(project_folder="/path/to/project", pattern="console.log($MSG)", language="javascript")
            """)
    public Mono<ToolResultBlock> find_code(
            @ToolParam(name = "project_folder", description = "项目文件夹的绝对路径。必须是绝对路径", required = true) String projectFolder,
            @ToolParam(name = "pattern", description = "要搜索的 ast-grep 模式。注意，模式必须具有有效的 AST 结构", required = true) String pattern,
            @ToolParam(name = "language", description = "代码语言。支持: bash, c, cpp, csharp, css, elixir, go, haskell, html, java, javascript, json, jsx, kotlin, lua, nix, php, python, ruby, rust, scala, solidity, swift, tsx, typescript, yaml。如果未指定，将根据文件扩展名自动检测", required = false) String language,
            @ToolParam(name = "max_results", description = "返回的最大结果数。0 表示不限制", required = false) Integer maxResults,
            @ToolParam(name = "output_format", description = "输出格式: 'text' 或 'json'。默认为 'text'", required = false) String outputFormat
    ) {
        return Mono.fromCallable(() -> {
            // 参数验证
            if (projectFolder == null || projectFolder.trim().isEmpty()) {
                return ToolResultBlock.error("Error: project_folder parameter is required");
            }
            if (pattern == null || pattern.trim().isEmpty()) {
                return ToolResultBlock.error("Error: pattern parameter is required");
            }

            Path searchPath = resolvePath(projectFolder);
            if (!Files.exists(searchPath)) {
                return ToolResultBlock.error("Error: Path does not exist: " + searchPath);
            }

            String format = (outputFormat != null && outputFormat.equals("json")) ? "json" : "text";
            int max = (maxResults != null && maxResults > 0) ? Math.min(maxResults, MAX_RESULTS) : MAX_RESULTS;

            try {
                List<String> command = buildBaseCommand();
                command.add("run");
                command.add("--pattern");
                command.add(pattern);
                command.add("--json=stream");

                if (language != null && !language.trim().isEmpty()) {
                    command.add("--lang");
                    command.add(language);
                }

                command.add(searchPath.toString());

                // 如果搜索路径是文件，使用父目录作为工作目录
                Path workDir = Files.isDirectory(searchPath) ? searchPath : searchPath.getParent();
                ProcessResult result = executeCommand(command, null, workDir);
                List<MatchResult> matches = parseJsonLines(result.stdout);
                int totalMatches = matches.size();

                // 限制结果数量
                if (matches.size() > max) {
                    matches = matches.subList(0, max);
                }

                if (format.equals("json")) {
                    return ToolResultBlock.text(matchesToJson(matches));
                }

                if (matches.isEmpty()) {
                    return ToolResultBlock.text("No matches found");
                }

                String header = "Found " + matches.size() + " matches";
                if (maxResults != null && maxResults > 0 && totalMatches > maxResults) {
                    header += " (showing first " + maxResults + " of " + totalMatches + ")";
                }

                return ToolResultBlock.text(header + ":\n\n" + formatMatchesAsText(matches));
            } catch (Exception e) {
                logger.error("Error executing find_code: {}", e.getMessage(), e);
                return ToolResultBlock.error("Error: " + e.getMessage());
            }
        });
    }

    // ==================== Tool 4: find_code_by_rule ====================

    /**
     * 使用 YAML 规则搜索代码
     * <p>
     * YAML 规则比简单模式更强大，可以执行复杂的搜索，
     * 如查找在另一个 AST 内部/具有另一个 AST 的 AST。
     */
    @Tool(description = """
            在项目文件夹中使用 ast-grep 的 YAML 规则查找代码。
            YAML 规则比简单模式更强大，可以执行复杂的搜索，
            如查找在另一个 AST 内部/具有另一个 AST 的 AST。
            它是比简单的 find_code 更高级的搜索工具。
            
            提示: 使用关系规则 (inside/has) 时，添加 `stopBy: end` 确保完整遍历。
            
            输出格式:
            - text (默认): 紧凑文本格式，包含 file:line-range 头和完整匹配文本
            - json: 完整匹配对象，包含范围、元变量等元数据
            
            内部调用: ast-grep scan --inline-rules <yaml> [--json] <project_folder>
            
            示例用法:
            - find_code_by_rule(project_folder="/path/to/project", yaml="id: x\\nlanguage: python\\nrule: {pattern: 'class $NAME'}")
            """)
    public Mono<ToolResultBlock> find_code_by_rule(
            @ToolParam(name = "project_folder", description = "项目文件夹的绝对路径。必须是绝对路径", required = true) String projectFolder,
            @ToolParam(name = "yaml", description = "ast-grep YAML 规则。必须包含 id, language, rule 字段", required = true) String yaml,
            @ToolParam(name = "max_results", description = "返回的最大结果数。0 表示不限制", required = false) Integer maxResults,
            @ToolParam(name = "output_format", description = "输出格式: 'text' 或 'json'。默认为 'text'", required = false) String outputFormat
    ) {
        return Mono.fromCallable(() -> {
            // 参数验证
            if (projectFolder == null || projectFolder.trim().isEmpty()) {
                return ToolResultBlock.error("Error: project_folder parameter is required");
            }
            if (yaml == null || yaml.trim().isEmpty()) {
                return ToolResultBlock.error("Error: yaml parameter is required");
            }

            Path searchPath = resolvePath(projectFolder);
            if (!Files.exists(searchPath)) {
                return ToolResultBlock.error("Error: Path does not exist: " + searchPath);
            }

            String format = (outputFormat != null && outputFormat.equals("json")) ? "json" : "text";
            int max = (maxResults != null && maxResults > 0) ? Math.min(maxResults, MAX_RESULTS) : MAX_RESULTS;

            Path tempYamlFile = null;
            try {
                ProcessResult result;
                
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    // 创建临时 YAML 文件
                    tempYamlFile = Files.createTempFile("ast-grep-rule-", ".yml");
                    Files.writeString(tempYamlFile, yaml);
                    logger.debug("Temp YAML file: {}", tempYamlFile);
                    logger.debug("YAML content: {}", yaml);
                    
                    List<String> command = buildBaseCommand();
                    command.add("scan");
                    command.add("--rule");
                    command.add(tempYamlFile.toString());
                    command.add("--json=stream");
                    command.add(searchPath.toString());
                    
                    // 如果搜索路径是文件，使用父目录作为工作目录
                    Path workDir = Files.isDirectory(searchPath) ? searchPath : searchPath.getParent();
                    result = executeCommand(command, null, workDir);
                } else {
                    List<String> command = buildBaseCommand();
                    command.add("scan");
                    command.add("--inline-rules");
                    command.add(yaml);
                    command.add("--json=stream");
                    command.add(searchPath.toString());
                    
                    // 如果搜索路径是文件，使用父目录作为工作目录
                    Path workDir = Files.isDirectory(searchPath) ? searchPath : searchPath.getParent();
                    result = executeCommand(command, null, workDir);
                }
                List<MatchResult> matches = parseJsonLines(result.stdout);
                int totalMatches = matches.size();

                // 限制结果数量
                if (matches.size() > max) {
                    matches = matches.subList(0, max);
                }

                if (format.equals("json")) {
                    return ToolResultBlock.text(matchesToJson(matches));
                }

                if (matches.isEmpty()) {
                    return ToolResultBlock.text("No matches found");
                }

                String header = "Found " + matches.size() + " matches";
                if (maxResults != null && maxResults > 0 && totalMatches > maxResults) {
                    header += " (showing first " + maxResults + " of " + totalMatches + ")";
                }

                return ToolResultBlock.text(header + ":\n\n" + formatMatchesAsText(matches));
            } catch (Exception e) {
                logger.error("Error executing find_code_by_rule: {}", e.getMessage(), e);
                return ToolResultBlock.error("Error: " + e.getMessage());
            } finally {
                // 清理临时文件
                if (tempYamlFile != null) {
                    try {
                        Files.deleteIfExists(tempYamlFile);
                    } catch (IOException e) {
                        logger.warn("Failed to delete temp file: {}", tempYamlFile);
                    }
                }
            }
        });
    }

    // ==================== 辅助方法 ====================

    private List<String> buildBaseCommand() {
        List<String> command = new ArrayList<>();
        command.add("ast-grep");
        return command;
    }


    /**
     * 解析路径
     */
    private Path resolvePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return baseDir != null ? baseDir : Paths.get(System.getProperty("user.dir"));
        }

        Path resolved = Paths.get(path);
        if (!resolved.isAbsolute()) {
            resolved = baseDir != null ? baseDir.resolve(path) : Paths.get(System.getProperty("user.dir")).resolve(path);
        }

        return resolved.toAbsolutePath().normalize();
    }

    private ProcessResult executeCommand(List<String> command, String input, Path workingDir) throws IOException, InterruptedException {
        logger.info("Executing command: {}", String.join(" ", command));
        
        ProcessBuilder pb;
        
        // Windows 上使用 shell 执行（类似 Python 的 shell=True）
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            // 使用 cmd /c 执行命令
            List<String> shellCommand = new ArrayList<>();
            shellCommand.add("cmd.exe");
            shellCommand.add("/c");
            // 将命令参数连接成字符串
            StringBuilder cmdBuilder = new StringBuilder();
            for (String arg : command) {
                // 对包含空格或特殊字符的参数加引号
                if (arg.contains(" ") || arg.contains("\"") || arg.contains("&") || arg.contains("|")) {
                    cmdBuilder.append("\"").append(arg.replace("\"", "\\\"")).append("\"");
                } else {
                    cmdBuilder.append(arg);
                }
                cmdBuilder.append(" ");
            }
            shellCommand.add(cmdBuilder.toString().trim());
            pb = new ProcessBuilder(shellCommand);
        } else {
            pb = new ProcessBuilder(command);
        }
        
        if (workingDir != null) {
            pb.directory(workingDir.toFile());
        }
        pb.redirectErrorStream(false);

        Process process = pb.start();

        // 如果有输入，写入 stdin
        if (input != null && !input.isEmpty()) {
            process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
        }

        // 读取输出
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        int exitCode = process.waitFor();

        // ast-grep 返回 exit code 1 表示没有找到匹配，这不是错误
        if (exitCode != 0 && exitCode != 1) {
            if (!stderr.isEmpty()) {
                throw new RuntimeException("Command failed with exit code " + exitCode + ": " + stderr);
            }
        }

        return new ProcessResult(stdout, stderr, exitCode);
    }

    /**
     * 读取输入流
     */
    private String readStream(java.io.InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 解析 JSONL 输出
     */
    private List<MatchResult> parseJsonLines(String jsonl) {
        List<MatchResult> results = new ArrayList<>();

        if (jsonl == null || jsonl.trim().isEmpty()) {
            return results;
        }

        String[] lines = jsonl.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.startsWith("{")) {
                continue;
            }

            try {
                MatchResult match = parseMatchResult(line);
                if (match != null) {
                    results.add(match);
                }
            } catch (Exception e) {
                logger.debug("Failed to parse JSON line: {}", line);
            }
        }

        return results;
    }

    /**
     * 解析单个匹配结果
     */
    private MatchResult parseMatchResult(String json) {
        MatchResult result = new MatchResult();

        // 提取 file
        result.file = extractJsonValue(json, "file");

        // 提取 text
        result.text = extractJsonValue(json, "text");

        // 提取 range - 使用更精确的方法
        result.startLine = extractNestedInt(json, "start", "line");
        result.endLine = extractNestedInt(json, "end", "line");

        return result;
    }

    /**
     * 提取嵌套的整数值
     */
    private int extractNestedInt(String json, String parent, String key) {
        // 匹配 "start": {..., "line": 123, ...}
        Pattern p = Pattern.compile("\"" + parent + "\"\\s*:\\s*\\{[^}]*\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    /**
     * 提取 JSON 字符串值
     */
    private String extractJsonValue(String json, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return unescapeJson(m.group(1));
        }
        return "";
    }

    /**
     * JSON 字符串反转义
     */
    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * 格式化匹配结果为文本
     */
    private String formatMatchesAsText(List<MatchResult> matches) {
        if (matches.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            MatchResult match = matches.get(i);
            // 转换为 1-based 行号
            int startLine = match.startLine + 1;
            int endLine = match.endLine + 1;

            if (startLine == endLine) {
                sb.append(match.file).append(":").append(startLine).append("\n");
            } else {
                sb.append(match.file).append(":").append(startLine).append("-").append(endLine).append("\n");
            }

            sb.append(match.text);
            
            if (i < matches.size() - 1) {
                sb.append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 将匹配结果转换为 JSON
     */
    private String matchesToJson(List<MatchResult> matches) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < matches.size(); i++) {
            MatchResult m = matches.get(i);
            sb.append("  {\n");
            sb.append("    \"file\": \"").append(escapeJson(m.file)).append("\",\n");
            sb.append("    \"startLine\": ").append(m.startLine + 1).append(",\n");
            sb.append("    \"endLine\": ").append(m.endLine + 1).append(",\n");
            sb.append("    \"text\": \"").append(escapeJson(m.text)).append("\"\n");
            sb.append("  }");
            if (i < matches.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\r", "\\r");
    }

    // ==================== 内部类 ====================

    /**
     * 进程执行结果
     */
    private static class ProcessResult {
        final String stdout;
        final String stderr;
        final int exitCode;

        ProcessResult(String stdout, String stderr, int exitCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }
    }

    /**
     * 匹配结果
     */
    private static class MatchResult {
        String file;
        String text;
        int startLine;
        int endLine;
    }
}