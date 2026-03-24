package com.kcode.core.tool.grepapp;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GrepAppTool 测试类
 *
 * @author liwenguang
 * @since 2026/3/22
 */
class GrepAppToolTest {

    private GrepAppTool grepAppTool;

    @BeforeEach
    void setUp() {
        grepAppTool = new GrepAppTool();
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
    void testSearchGitHubBasicQuery() {
        // 测试基本搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "useState(",
                null,
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Basic Query Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        // 验证返回的内容不为空
        assertFalse(text.isEmpty(), "Content should not be empty");
        // 验证包含查询信息
        assertTrue(text.contains("Query:") || text.contains("useState"), 
                "Content should contain query information");
    }

    @Test
    void testSearchGitHubWithLanguageFilter() {
        // 测试带语言过滤的搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "async function",
                "TypeScript",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Language Filter Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===============================");

        assertFalse(text.isEmpty(), "Content should not be empty");
        // 验证包含语言信息
        assertTrue(text.contains("Language:") || text.contains("typescript") || text.length() > 100,
                "Content should contain language information or be substantial");
    }

    @Test
    void testSearchGitHubWithRepoFilter() {
        // 测试带仓库过滤的搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "function",
                null,
                "facebook/react",
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Repo Filter Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubWithPathFilter() {
        // 测试带路径过滤的搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "export",
                "TypeScript",
                null,
                "src/",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Path Filter Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubWithAllFilters() {
        // 测试带所有过滤器的搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "useState",
                "TypeScript",
                "facebook/react",
                "packages/",
                false,
                false,
                false
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== All Filters Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubEmptyQuery() {
        // 测试空的查询
        ToolResultBlock result = grepAppTool.searchGitHub(
                "",
                null,
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Empty Query Output ===");
        System.out.println(text);
        System.out.println("===========================");

        assertTrue(text.contains("Error") || text.contains("required"), 
                "Should return error for empty query");
    }

    @Test
    void testSearchGitHubNullQuery() {
        // 测试 null 查询
        ToolResultBlock result = grepAppTool.searchGitHub(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Null Query Output ===");
        System.out.println(text);
        System.out.println("==========================");

        assertTrue(text.contains("Error") || text.contains("required"), 
                "Should return error for null query");
    }

    @Test
    void testSearchGitHubInvalidRepoFormat() {
        // 测试无效的仓库格式
        ToolResultBlock result = grepAppTool.searchGitHub(
                "function",
                null,
                "invalid-repo-format",  // 缺少 "/" 分隔符
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Invalid Repo Format Output ===");
        System.out.println(text);
        System.out.println("==================================");

        assertTrue(text.contains("Error") || text.contains("format"), 
                "Should return error for invalid repo format");
    }

    @Test
    void testSearchGitHubTooLongQuery() {
        // 测试超长的查询
        StringBuilder longQuery = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longQuery.append("verylongquery");
        }

        ToolResultBlock result = grepAppTool.searchGitHub(
                longQuery.toString(),
                null,
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Too Long Query Output ===");
        System.out.println(text);
        System.out.println("=============================");

        assertTrue(text.contains("Error") || text.contains("too long"), 
                "Should return error for too long query");
    }

    @Test
    void testSearchGitHubReactHookPattern() {
        // 测试搜索 React Hook 模式
        ToolResultBlock result = grepAppTool.searchGitHub(
                "useEffect(() => {",
                "TypeScript",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== React Hook Pattern Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("==================================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubPythonPattern() {
        // 测试搜索 Python 代码模式
        ToolResultBlock result = grepAppTool.searchGitHub(
                "def __init__",
                "Python",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Python Pattern Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("==============================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubJavaPattern() {
        // 测试搜索 Java 代码模式
        ToolResultBlock result = grepAppTool.searchGitHub(
                "public static void main",
                "Java",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Java Pattern Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubWithMatchCase() {
        // 测试区分大小写的搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "React",
                "TypeScript",
                null,
                null,
                true,  // matchCase = true
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Match Case Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("===========================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubComplexPattern() {
        // 测试复杂模式搜索
        ToolResultBlock result = grepAppTool.searchGitHub(
                "async function $NAME() {",
                "TypeScript",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Complex Pattern Output ===");
        System.out.println(text.substring(0, Math.min(1000, text.length())));
        System.out.println("==============================");

        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testSearchGitHubResponseStructure() {
        // 测试响应结构是否正确
        ToolResultBlock result = grepAppTool.searchGitHub(
                "console.log",
                "JavaScript",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Response Structure Output ===");
        System.out.println(text.substring(0, Math.min(1500, text.length())));
        System.out.println("=================================");

        // 验证响应包含预期的结构
        assertFalse(text.isEmpty(), "Content should not be empty");
        // 应该包含 Summary 部分
        assertTrue(text.contains("Summary:") || text.contains("Query:") || text.contains("Results"), 
                "Content should contain summary or results information");
    }

    @Test
    void testSearchGitHubCodeSnippetFormat() {
        // 测试代码片段格式
        ToolResultBlock result = grepAppTool.searchGitHub(
                "import React",
                "TypeScript",
                null,
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Code Snippet Format Output ===");
        System.out.println(text.substring(0, Math.min(1500, text.length())));
        System.out.println("==================================");

        // 验证代码片段格式（应该包含代码块标记或文件路径）
        assertFalse(text.isEmpty(), "Content should not be empty");
        // 应该包含代码相关的内容
        assertTrue(text.contains("```") || text.contains("Code:") || text.contains("File:"), 
                "Content should contain code blocks or file information");
    }

    @Test
    void testSearchGitHubMultipleLanguages() {
        // 测试不同编程语言的搜索
        String[] languages = {"Python", "Java", "Go", "Rust"};

        for (String lang : languages) {
            ToolResultBlock result = grepAppTool.searchGitHub(
                    "func main",
                    lang,
                    null,
                    null,
                    null,
                    null,
                    null
            ).block();

            assertNotNull(result, "Result should not be null for language: " + lang);
            String text = extractText(result);

            System.out.println("=== " + lang + " Output ===");
            System.out.println(text.substring(0, Math.min(500, text.length())));
            System.out.println("=========================");

            assertFalse(text.isEmpty(), "Content should not be empty for language: " + lang);
        }
    }
}