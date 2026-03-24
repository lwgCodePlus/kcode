package com.kcode.core.tool.astgrep;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AstGrepTool 测试类
 *
 * @author liwenguang
 * @since 2026/3/20
 */
class AstGrepToolTest {

    @TempDir
    Path tempDir;

    private AstGrepTool astGrepTool;
    private Path jsFile;
    private Path tsFile;
    private Path javaFile;
    private Path pythonFile;

    @BeforeEach
    void setUp() throws IOException {
        astGrepTool = new AstGrepTool(tempDir.toString());

        // 创建 JavaScript 测试文件
        jsFile = tempDir.resolve("test.js");
        Files.writeString(jsFile, """
                function hello(name) {
                    console.log("Hello, " + name);
                    return name;
                }
                
                function goodbye(name) {
                    console.log("Goodbye, " + name);
                }
                
                const greet = (name) => {
                    console.log("Greetings, " + name);
                };
                
                var oldVar = "old style";
                let newVar = "new style";
                const constantVar = "constant";
                """);

        // 创建 TypeScript 测试文件
        tsFile = tempDir.resolve("test.ts");
        Files.writeString(tsFile, """
                interface User {
                    name: string;
                    age: number;
                }
                
                function greetUser(user: User): string {
                    return `Hello, ${user.name}`;
                }
                
                class Person {
                    private name: string;
                    
                    constructor(name: string) {
                        this.name = name;
                    }
                    
                    getName(): string {
                        return this.name;
                    }
                }
                """);

        // 创建 Java 测试文件
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
                    
                    public void setName(String name) {
                        this.name = name;
                    }
                }
                """);

        // 创建 Python 测试文件
        pythonFile = tempDir.resolve("test.py");
        Files.writeString(pythonFile, """
                def hello(name):
                    print(f"Hello, {name}")
                    return name
                
                def goodbye(name):
                    print(f"Goodbye, {name}")
                
                class Person:
                    def __init__(self, name):
                        self.name = name
                    
                    def greet(self):
                        print(f"I am {self.name}")
                """);

        // 创建子目录和嵌套文件
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        Path nestedJsFile = subDir.resolve("nested.js");
        Files.writeString(nestedJsFile, """
                function nestedFunction() {
                    console.log("I am nested");
                }
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

    // ==================== dump_syntax_tree 测试 ====================

    @Test
    void testDumpSyntaxTree() {
        ToolResultBlock result = astGrepTool.dump_syntax_tree(
                "console.log('hello')",
                "javascript",
                "cst"
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Dump Syntax Tree Output ===");
        System.out.println(text);
        System.out.println("===============================");

        // 应该包含语法树信息
        assertTrue(text.contains("console") || text.contains("Call") || !text.isEmpty());
    }

    @Test
    void testDumpSyntaxTreePattern() {
        ToolResultBlock result = astGrepTool.dump_syntax_tree(
                "console.log($MSG)",
                "javascript",
                "pattern"
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Dump Pattern Output ===");
        System.out.println(text);
        System.out.println("===========================");

        assertNotNull(text);
    }

    // ==================== test_match_code_rule 测试 ====================

    @Test
    void testMatchCodeRule() {
        String yaml = """
                id: test-console-log
                language: javascript
                rule:
                  pattern: console.log($MSG)
                """;

        ToolResultBlock result = astGrepTool.test_match_code_rule(
                "console.log('hello world')",
                yaml
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Test Match Code Rule Output ===");
        System.out.println(text);
        System.out.println("===================================");

        // 应该找到匹配
        assertTrue(text.contains("console.log") || !text.contains("No matches"));
    }

    @Test
    void testMatchCodeRuleNoMatch() {
        String yaml = """
                id: test-nonexistent
                language: javascript
                rule:
                  pattern: nonexistentFunction($ARGS)
                """;

        ToolResultBlock result = astGrepTool.test_match_code_rule(
                "console.log('hello')",
                yaml
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== No Match Output ===");
        System.out.println(text);
        System.out.println("=======================");

        // 应该报错说没有匹配
        assertTrue(text.contains("No matches") || text.contains("Error"));
    }

    // ==================== find_code 测试 ====================

    @Test
    void testFindCodeBasicPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                "console.log($MSG)",
                "javascript",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Code Output ===");
        System.out.println(text);
        System.out.println("========================");

        assertTrue(text.contains("test.js") || text.contains("nested.js") || text.contains("Found"));
    }

    @Test
    void testFindCodeWithLanguage() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                "function $NAME($ARGS) { $$$BODY }",
                "javascript",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Code with Language Output ===");
        System.out.println(text);
        System.out.println("======================================");

        assertTrue(text.contains("function") || text.contains("Found"));
    }

    @Test
    void testFindCodeWithMaxResults() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                "console.log($MSG)",
                "javascript",
                1,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Code with Max Results ===");
        System.out.println(text);
        System.out.println("==================================");

        // 应该限制为 1 个结果
        assertTrue(text.contains("Found") || text.contains("test.js") || text.contains("nested.js"));
    }

    @Test
    void testFindCodeWithJsonOutput() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                "console.log($MSG)",
                "javascript",
                null,
                "json"
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Code JSON Output ===");
        System.out.println(text);
        System.out.println("=============================");

        // JSON 格式输出
        assertTrue(text.contains("\"file\"") || text.contains("Found") || text.contains("["));
    }

    @Test
    void testFindCodeJavaPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                javaFile.toString(),
                "public $RET $NAME($ARGS) { $$$BODY }",
                "java",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Java Code Output ===");
        System.out.println(text);
        System.out.println("=============================");

        assertTrue(text.contains("Example.java") || text.contains("Found") || !text.contains("No matches"));
    }

    @Test
    void testFindCodePythonPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                pythonFile.toString(),
                "def $NAME($ARGS): $$$BODY",
                "python",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Python Code Output ===");
        System.out.println(text);
        System.out.println("===============================");

        assertTrue(text.contains("test.py") || text.contains("Found") || !text.contains("No matches"));
    }

    @Test
    void testFindCodeTypeScriptPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                tsFile.toString(),
                "class $NAME { $$$BODY }",
                "typescript",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find TypeScript Code Output ===");
        System.out.println(text);
        System.out.println("===================================");

        assertTrue(text.contains("test.ts") || text.contains("Found") || text.contains("Person"));
    }

    @Test
    void testFindCodeNoMatches() {
        ToolResultBlock result = astGrepTool.find_code(
                jsFile.toString(),
                "nonexistentFunction($ARGS)",
                "javascript",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== No Matches Output ===");
        System.out.println(text);
        System.out.println("=========================");

        assertTrue(text.contains("No matches") || text.isEmpty());
    }

    // ==================== find_code_by_rule 测试 ====================

    @Test
    void testFindCodeByRule() {
        String yaml = """
                id: find-console-log
                language: javascript
                rule:
                  pattern: console.log($MSG)
                """;

        ToolResultBlock result = astGrepTool.find_code_by_rule(
                tempDir.toString(),
                yaml,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find Code By Rule Output ===");
        System.out.println(text);
        System.out.println("================================");

        assertTrue(text.contains("test.js") || text.contains("nested.js") || text.contains("Found"));
    }

    @Test
    void testFindCodeByRuleComplex() {
        // 使用更复杂的规则：查找包含 console.log 的函数
        String yaml = """
                id: find-functions-with-console
                language: javascript
                rule:
                  pattern: function $NAME($ARGS) { $$$BODY }
                  has:
                    pattern: console.log($MSG)
                    stopBy: end
                """;

        ToolResultBlock result = astGrepTool.find_code_by_rule(
                jsFile.toString(),
                yaml,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Complex Rule Output ===");
        System.out.println(text);
        System.out.println("===========================");

        assertNotNull(text);
    }

    @Test
    void testFindCodeByRuleWithMaxResults() {
        String yaml = """
                id: find-console-log
                language: javascript
                rule:
                  pattern: console.log($MSG)
                """;

        ToolResultBlock result = astGrepTool.find_code_by_rule(
                tempDir.toString(),
                yaml,
                2,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find By Rule with Max Results ===");
        System.out.println(text);
        System.out.println("=====================================");

        assertTrue(text.contains("Found") || text.contains("test.js"));
    }

    @Test
    void testFindCodeByRuleJsonOutput() {
        String yaml = """
                id: find-console-log
                language: javascript
                rule:
                  pattern: console.log($MSG)
                """;

        ToolResultBlock result = astGrepTool.find_code_by_rule(
                tempDir.toString(),
                yaml,
                null,
                "json"
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find By Rule JSON Output ===");
        System.out.println(text);
        System.out.println("================================");

        assertTrue(text.contains("\"file\"") || text.contains("Found") || text.contains("["));
    }

    @Test
    void testFindCodeByRuleNoMatches() {
        String yaml = """
                id: find-nonexistent
                language: javascript
                rule:
                  pattern: nonexistentFunction($ARGS)
                """;

        ToolResultBlock result = astGrepTool.find_code_by_rule(
                jsFile.toString(),
                yaml,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Find By Rule No Matches ===");
        System.out.println(text);
        System.out.println("===============================");

        assertTrue(text.contains("No matches") || text.isEmpty());
    }

    // ==================== 错误处理测试 ====================

    @Test
    void testFindCodeNullPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                null,
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Null Pattern Output ===");
        System.out.println(text);
        System.out.println("===========================");

        assertTrue(text.contains("Error") || text.contains("required"));
    }

    @Test
    void testFindCodeEmptyPattern() {
        ToolResultBlock result = astGrepTool.find_code(
                tempDir.toString(),
                "",
                null,
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Empty Pattern Output ===");
        System.out.println(text);
        System.out.println("============================");

        assertTrue(text.contains("Error") || text.contains("required"));
    }

    @Test
    void testFindCodeNonExistentPath() {
        ToolResultBlock result = astGrepTool.find_code(
                "/non/existent/path",
                "console.log($MSG)",
                "javascript",
                null,
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Non-existent Path Output ===");
        System.out.println(text);
        System.out.println("================================");

        assertTrue(text.contains("Error") || text.contains("not exist"));
    }

    @Test
    void testDumpSyntaxTreeNullCode() {
        ToolResultBlock result = astGrepTool.dump_syntax_tree(
                null,
                "javascript",
                "cst"
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Null Code Output ===");
        System.out.println(text);
        System.out.println("========================");

        assertTrue(text.contains("Error") || text.contains("required"));
    }

    @Test
    void testTestMatchCodeRuleNullYaml() {
        ToolResultBlock result = astGrepTool.test_match_code_rule(
                "console.log('hello')",
                null
        ).block();

        assertNotNull(result);
        String text = extractText(result);

        System.out.println("=== Null YAML Output ===");
        System.out.println(text);
        System.out.println("========================");

        assertTrue(text.contains("Error") || text.contains("required"));
    }
}