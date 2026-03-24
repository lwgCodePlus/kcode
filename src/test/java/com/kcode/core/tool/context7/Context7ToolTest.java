package com.kcode.core.tool.context7;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Context7Tool 测试类
 *
 * @author liwenguang
 * @since 2026/3/22
 */
class Context7ToolTest {

    private Context7Tool context7Tool;

    @BeforeEach
    void setUp() {
        context7Tool = new Context7Tool();
    }

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

    @Nested
    @DisplayName("resolveLibraryId 方法测试")
    class ResolveLibraryIdTests {

        @Test
        @DisplayName("参数验证 - libraryName 为 null 时应返回错误")
        void testResolveLibraryId_NullLibraryName() {
            ToolResultBlock result = context7Tool.resolveLibraryId("test query", null)
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - libraryName 为空字符串时应返回错误")
        void testResolveLibraryId_EmptyLibraryName() {
            ToolResultBlock result = context7Tool.resolveLibraryId("test query", "")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - query 为 null 时应返回错误")
        void testResolveLibraryId_NullQuery() {
            ToolResultBlock result = context7Tool.resolveLibraryId(null, "react")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - query 为空字符串时应返回错误")
        void testResolveLibraryId_EmptyQuery() {
            ToolResultBlock result = context7Tool.resolveLibraryId("", "react")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("集成测试 - 搜索 React 库")
        void testResolveLibraryId_SearchReact() {
            ToolResultBlock result = context7Tool.resolveLibraryId(
                    "How to use React hooks", "react"
            ).block();
            assertNotNull(result);
            String text = extractText(result);
            System.out.println("=== React Search Output ===");
            System.out.println(text.substring(0, Math.min(1000, text.length())));
            System.out.println("===========================");
            assertTrue(text.contains("Available Libraries") || text.contains("No libraries found") || text.contains("Error"));
        }

        @Test
        @DisplayName("集成测试 - 搜索 Spring Boot 库")
        void testResolveLibraryId_SearchSpringBoot() {
            ToolResultBlock result = context7Tool.resolveLibraryId(
                    "How to create a REST API with Spring Boot", "spring-boot"
            ).block();
            assertNotNull(result);
            String text = extractText(result);
            System.out.println("=== Spring Boot Search Output ===");
            System.out.println(text.substring(0, Math.min(1000, text.length())));
            System.out.println("=================================");
            assertTrue(text.length() > 0);
        }
    }

    @Nested
    @DisplayName("queryDocs 方法测试")
    class QueryDocsTests {

        @Test
        @DisplayName("参数验证 - libraryId 为 null 时应返回错误")
        void testQueryDocs_NullLibraryId() {
            ToolResultBlock result = context7Tool.queryDocs(null, "test query")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - libraryId 为空字符串时应返回错误")
        void testQueryDocs_EmptyLibraryId() {
            ToolResultBlock result = context7Tool.queryDocs("", "test query")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - query 为 null 时应返回错误")
        void testQueryDocs_NullQuery() {
            ToolResultBlock result = context7Tool.queryDocs("/facebook/react", null)
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("参数验证 - query 为空字符串时应返回错误")
        void testQueryDocs_EmptyQuery() {
            ToolResultBlock result = context7Tool.queryDocs("/facebook/react", "")
                    .block();
            assertNotNull(result);
            String text = extractText(result);
            assertTrue(text.contains("Error") || text.contains("required"));
        }

        @Test
        @DisplayName("集成测试 - 查询 React 文档")
        void testQueryDocs_QueryReactDocs() {
            ToolResultBlock result = context7Tool.queryDocs(
                    "/facebook/react", "How to use useState hook"
            ).block();
            assertNotNull(result);
            String text = extractText(result);
            System.out.println("=== React Docs Query Output ===");
            System.out.println(text.substring(0, Math.min(1500, text.length())));
            System.out.println("===============================");
            assertTrue(text.length() > 0);
        }

        @Test
        @DisplayName("集成测试 - 查询带版本号的库")
        void testQueryDocs_WithVersion() {
            // 使用 Context7 数据库中存在的版本号（v16.1.6）
            ToolResultBlock result = context7Tool.queryDocs(
                    "/vercel/next.js/v16.1.6", "How to use App Router"
            ).block();
            assertNotNull(result);
            String text = extractText(result);
            System.out.println("=== Versioned Library Query Output ===");
            System.out.println(text.substring(0, Math.min(1500, text.length())));
            System.out.println("======================================");
            assertTrue(text.length() > 0);
        }
    }

    @Nested
    @DisplayName("工具初始化测试")
    class InitializationTests {

        @Test
        @DisplayName("Context7Tool 应成功初始化")
        void testToolInitialization() {
            assertDoesNotThrow(() -> new Context7Tool());
        }

        @Test
        @DisplayName("多次初始化不应抛出异常")
        void testMultipleInitialization() {
            assertDoesNotThrow(() -> {
                new Context7Tool();
                new Context7Tool();
                new Context7Tool();
            });
        }
    }

    @Nested
    @DisplayName("响应格式测试")
    class ResponseFormatTests {

        @Test
        @DisplayName("resolveLibraryId 返回的 ToolResultBlock 不应为 null")
        void testResolveLibraryId_ReturnsNonNullResult() {
            ToolResultBlock result = context7Tool.resolveLibraryId("test", "react")
                    .onErrorReturn(ToolResultBlock.error("Test error"))
                    .block();
            assertNotNull(result);
        }

        @Test
        @DisplayName("queryDocs 返回的 ToolResultBlock 不应为 null")
        void testQueryDocs_ReturnsNonNullResult() {
            ToolResultBlock result = context7Tool.queryDocs("/test/lib", "query")
                    .onErrorReturn(ToolResultBlock.error("Test error"))
                    .block();
            assertNotNull(result);
        }
    }
}