package com.kcode.core.tool.webfetch;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebFetchTool 测试类
 *
 * @author liwenguang
 * @since 2026/3/12
 */
class WebFetchToolTest {

    private WebFetchTool webFetchTool;

    @BeforeEach
    void setUp() {
        webFetchTool = new WebFetchTool();
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
    void testWebFetchMarkdown() {
        // 测试获取 URL 内容并转换为 Markdown 格式
        ToolResultBlock result = webFetchTool.webFetch(
                "https://java.agentscope.io/zh/task/hook.html",
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== Markdown Output ===");
        System.out.println(text);
        System.out.println("======================");
        
        // 验证返回的内容不为空
        assertFalse(text.isEmpty(), "Content should not be empty");
        // 验证包含一些预期的内容（Markdown 格式通常包含标题）
        assertTrue(text.contains("#") || text.contains("-") || text.length() > 100, 
                "Content should contain markdown elements or be substantial");
    }

    @Test
    void testWebFetchHtml() {
        // 测试获取 URL 内容并返回原始 HTML 格式
        ToolResultBlock result = webFetchTool.webFetch(
                "https://opencode.ai/docs/zh-cn/rules/",
                "html"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== HTML Output (first 500 chars) ===");
        System.out.println(text.substring(0, Math.min(500, text.length())));
        System.out.println("=====================================");
        
        // 验证返回的是 HTML 内容
        assertFalse(text.isEmpty(), "Content should not be empty");
        assertTrue(text.contains("<") && text.contains(">"), 
                "HTML content should contain tags");
    }

    @Test
    void testWebFetchText() {
        // 测试获取 URL 内容并转换为纯文本格式
        ToolResultBlock result = webFetchTool.webFetch(
                "https://opencode.ai/docs/zh-cn/rules/",
                "text"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== Text Output (first 500 chars) ===");
        System.out.println(text.substring(0, Math.min(500, text.length())));
        System.out.println("=====================================");
        
        // 验证返回的是纯文本（不包含 HTML 标签）
        assertFalse(text.isEmpty(), "Content should not be empty");
        assertFalse(text.contains("<html>"), "Text should not contain HTML tags");
    }

    @Test
    void testWebFetchDefaultFormat() {
        // 测试默认格式（不指定 format 参数，应该使用 markdown）
        ToolResultBlock result = webFetchTool.webFetch(
                "https://opencode.ai/docs/zh-cn/rules/",
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== Default Format Output ===");
        System.out.println(text.substring(0, Math.min(500, text.length())));
        System.out.println("=============================");
        
        assertFalse(text.isEmpty(), "Content should not be empty");
    }

    @Test
    void testWebFetchHttpToHttps() {
        // 测试 HTTP URL 自动升级为 HTTPS
        ToolResultBlock result = webFetchTool.webFetch(
                "http://opencode.ai/docs/zh-cn/rules/",
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== HTTP to HTTPS Output ===");
        System.out.println(text.substring(0, Math.min(300, text.length())));
        System.out.println("============================");
        
        // 应该成功获取内容（HTTP 被升级为 HTTPS）
        assertFalse(text.isEmpty(), "Content should not be empty after HTTP->HTTPS upgrade");
    }

    @Test
    void testWebFetchInvalidUrl() {
        // 测试无效的 URL
        ToolResultBlock result = webFetchTool.webFetch(
                "not-a-valid-url",
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== Invalid URL Output ===");
        System.out.println(text);
        System.out.println("==========================");
        
        assertTrue(text.contains("Error") || text.contains("Invalid"), 
                "Should return error for invalid URL");
    }

    @Test
    void testWebFetchEmptyUrl() {
        // 测试空的 URL
        ToolResultBlock result = webFetchTool.webFetch(
                "",
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        assertTrue(text.contains("Error") || text.contains("required"), 
                "Should return error for empty URL");
    }

    @Test
    void testWebFetchNullUrl() {
        // 测试 null URL
        ToolResultBlock result = webFetchTool.webFetch(
                null,
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        assertTrue(text.contains("Error") || text.contains("required"), 
                "Should return error for null URL");
    }

    @Test
    void testWebFetchNonExistentUrl() {
        // 测试不存在的 URL（应该返回错误或 404）
        ToolResultBlock result = webFetchTool.webFetch(
                "https://this-domain-does-not-exist-12345.com/page",
                "markdown"
        ).block();

        assertNotNull(result);
        String text = extractText(result);
        
        System.out.println("=== Non-existent URL Output ===");
        System.out.println(text);
        System.out.println("===============================");
        
        // 应该返回错误信息
        assertTrue(text.contains("Error") || text.contains("Failed"), 
                "Should return error for non-existent domain");
    }
}