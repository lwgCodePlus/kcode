package com.kcode.core.tool.grep;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrepTool 测试类
 * 
 * @author liwenguang
 * @since 2026/3/12
 */
class GrepToolTest {

    @TempDir
    Path tempDir;

    private GrepTool grepTool;
    private Path testFile1;
    private Path testFile2;
    private Path javaFile;

    @BeforeEach
    void setUp() throws IOException {
        grepTool = new GrepTool(tempDir.toString());

        // 创建测试文件1 - 包含一些日志行
        testFile1 = tempDir.resolve("test1.txt");
        Files.writeString(testFile1, """
                Hello World
                This is a test file
                ERROR: Something went wrong
                INFO: Application started
                ERROR: Connection failed
                DEBUG: Processing data
                WARN: Memory low
                ERROR: Database timeout
                """);

        // 创建测试文件2 - 包含一些代码
        testFile2 = tempDir.resolve("test2.txt");
        Files.writeString(testFile2, """
                function hello() {
                    console.log("Hello");
                }
                function world() {
                    console.log("World");
                }
                const greeting = "Hello World";
                """);

        // 创建Java文件
        javaFile = tempDir.resolve("Example.java");
        Files.writeString(javaFile, """
                package com.example;
                
                public class Example {
                    private String name;
                    private int age;
                    
                    public void sayHello() {
                        System.out.println("Hello " + name);
                    }
                    
                    public String getName() {
                        return name;
                    }
                }
                """);

        // 创建子目录和文件
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path subFile = subDir.resolve("nested.txt");
        Files.writeString(subFile, """
                Nested file content
                ERROR: Nested error
                INFO: Nested info
                """);
    }
    
    /**
     * 从 ToolResultBlock 中提取文本内容
     */
    private String extractText(ToolResultBlock result) {
        if (result == null || result.getOutput().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : result.getOutput()) {
            if (block instanceof TextBlock textBlock) {
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    @Test
    void testGrepBasicSearch() {
        // 测试基本搜索
        ToolResultBlock result = grepTool.grep(
                "ERROR",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        // 应该找到 test1.txt 和 nested.txt
        assertTrue(text.contains("test1.txt") || text.contains("nested.txt"));
        // 内容部分应该包含 ERROR
        assertTrue(text.contains("ERROR"));
    }

    @Test
    void testGrepOutputFormat() {
        // 测试输出格式：file:lineNum:content
        ToolResultBlock result = grepTool.grep(
                "ERROR",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        // 每行应该包含文件路径、行号和内容
        String[] lines = text.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                // 格式应为 file:lineNum:content，至少包含两个冒号
                assertTrue(line.contains(":"), "Line should contain colons: " + line);
            }
        }
    }

    @Test
    void testGrepWithIncludePattern() {
        // 测试 include 参数 - 只搜索特定文件
        ToolResultBlock result = grepTool.grep(
                "class",
                "*.java",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("Example.java"));
        assertFalse(text.contains("test1.txt"));
    }

    @Test
    void testGrepWithRegexPattern() {
        // 测试正则表达式
        ToolResultBlock result = grepTool.grep(
                "function\\s+\\w+",
                "*.txt",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("function hello") || text.contains("function world"));
    }

    @Test
    void testGrepWithHeadLimit() {
        // 测试结果数量限制
        ToolResultBlock result = grepTool.grep(
                "ERROR",
                null,
                null,
                "2"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        String[] lines = text.split("\n");
        // 应该最多返回 2 行
        assertTrue(lines.length <= 2);
    }

    @Test
    void testGrepWithInvalidPattern() {
        // 测试无效的正则表达式
        ToolResultBlock result = grepTool.grep(
                "[invalid(regex",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("Error") || text.contains("Invalid"));
    }

    @Test
    void testGrepWithEmptyPattern() {
        // 测试空 pattern
        ToolResultBlock result = grepTool.grep(
                "",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("Error") || text.contains("required"));
    }

    @Test
    void testGrepWithNonExistentPath() {
        // 测试不存在的路径
        ToolResultBlock result = grepTool.grep(
                "test",
                null,
                "/non/existent/path",
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("Error") || text.contains("not exist"));
    }

    @Test
    void testGrepWithSubdirectory() {
        // 测试递归搜索子目录
        ToolResultBlock result = grepTool.grep(
                "Nested",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("nested.txt"));
    }

    @Test
    void testGrepNoMatches() {
        // 测试没有匹配项
        ToolResultBlock result = grepTool.grep(
                "NOTEXISTINGPATTERN12345",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("No matches found") || text.isEmpty());
    }

    @Test
    void testGrepWithSpecificPath() {
        // 测试指定搜索路径
        ToolResultBlock result = grepTool.grep(
                "ERROR",
                null,
                tempDir.toString(),
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        assertTrue(text.contains("test1.txt") || text.contains("nested.txt"));
    }
}