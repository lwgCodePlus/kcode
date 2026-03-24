package com.kcode.core.tool.webfetch;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

/**
 * WebFetch 工具 - 从指定 URL 获取内容并转换为指定格式
 * <p>
 * 支持 HTML、Markdown、Text 三种输出格式
 *
 * @author liwenguang
 * @since 2026/3/12
 */
public class WebFetchTool {
    private static final Logger logger = LoggerFactory.getLogger(WebFetchTool.class);

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * 最大内容长度限制（1MB）
     */
    private static final int MAX_CONTENT_LENGTH = 1024 * 1024;

    /**
     * 默认格式
     */
    private static final String DEFAULT_FORMAT = "markdown";

    private final WebClient webClient;
    private final FlexmarkHtmlConverter htmlToMarkdownConverter;

    public WebFetchTool() {
        // Native Image 兼容性：使用 JVM 内置 DNS 解析器替代 Netty DNS 解析器
        // Netty DNS 解析器在 GraalVM Native Image 中存在已知问题（Windows 尤其）
        // 参考：https://github.com/oracle/graal/issues/11280
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);

        this.webClient = WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_CONTENT_LENGTH))
                .build();

        MutableDataSet options = new MutableDataSet()
                .set(FlexmarkHtmlConverter.MAX_BLANK_LINES, 2)
                .set(FlexmarkHtmlConverter.MAX_TRAILING_BLANK_LINES, 1)
                .set(FlexmarkHtmlConverter.SETEXT_HEADINGS, false)
                .set(FlexmarkHtmlConverter.OUTPUT_ATTRIBUTES_ID, false);

        this.htmlToMarkdownConverter = FlexmarkHtmlConverter.builder(options).build();
    }

    @Tool(description = """
            - 从指定的 URL 获取内容
            - 接受 URL 和可选格式作为输入
            - 获取 URL 内容，转换为请求的格式（默认为 markdown）
            - 以指定格式返回内容
            - 当你需要检索和分析网页内容时使用此工具
            使用说明：
              - URL 必须是完全有效的 URL
              - HTTP URL 将自动升级为 HTTPS
              - 格式选项："markdown"（默认）、"text" 或 "html"
              - 此工具是只读的，不会修改任何文件
              - 如果内容非常大，结果可能会被摘要
            """)
    public Mono<ToolResultBlock> webFetch( @ToolParam(name = "url", description = "有效的 URL", required = true) String url,
            @ToolParam(name = "format", description = """
                    格式选项："markdown"（默认）、"text" 或 "html"
                    """) String format) {
        logger.info("WebFetchTool执行：{}",url);
        return Mono.fromCallable(() -> {
            // 1. 参数验证
            if (url == null || url.trim().isEmpty()) {
                return ToolResultBlock.error("Error: url parameter is required");
            }

            // 2. 规范化 URL（HTTP -> HTTPS）
            String normalizedUrl = normalizeUrl(url.trim());

            // 3. 验证 URL 格式
            try {
                URI.create(normalizedUrl);
            } catch (IllegalArgumentException e) {
                return ToolResultBlock.error("Error: Invalid URL format: " + url);
            }

            // 4. 确定输出格式
            String outputFormat = normalizeFormat(format);

            // 5. 获取网页内容
            String htmlContent = fetchUrl(normalizedUrl);

            // 6. 转换格式
            String result = convertContent(htmlContent, outputFormat, normalizedUrl);

            return ToolResultBlock.text(result);

        }).onErrorResume(e -> {
            logger.error("Error executing webFetch for URL '{}': {}", url, e.getMessage(), e);
            return Mono.just(ToolResultBlock.error("Error fetching URL: " + e.getMessage()));
        });
    }

    /**
     * 规范化 URL（HTTP 自动升级为 HTTPS）
     */
    private String normalizeUrl(String url) {
        if (url.toLowerCase().startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    /**
     * 规范化输出格式
     */
    private String normalizeFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            return DEFAULT_FORMAT;
        }
        String normalized = format.trim().toLowerCase();
        return switch (normalized) {
            case "markdown", "md" -> "markdown";
            case "text" -> "text";
            case "html" -> "html";
            default -> DEFAULT_FORMAT;
        };
    }

    /**
     * 获取 URL 内容
     */
    private String fetchUrl(String url) {
        try {
            return webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch URL: " + e.getMessage(), e);
        }
    }

    /**
     * 根据格式转换内容
     */
    private String convertContent(String htmlContent, String format, String sourceUrl) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return "No content available";
        }

        return switch (format) {
            case "html" -> htmlContent;
            case "text" -> convertHtmlToText(htmlContent);
            default -> convertHtmlToMarkdown(htmlContent);
        };
    }

    /**
     * 将 HTML 转换为 Markdown
     */
    private String convertHtmlToMarkdown(String html) {
        try {
            // 使用 Jsoup 清理和规范化 HTML
            Document doc = Jsoup.parse(html);
            doc.outputSettings().prettyPrint(false);

            // 转换为 Markdown
            String markdown = htmlToMarkdownConverter.convert(doc.outerHtml());

            // 清理多余的空白行
            return markdown.trim()
                    .replaceAll("\n{3,}", "\n\n");
        } catch (Exception e) {
            logger.warn("Failed to convert HTML to Markdown: {}", e.getMessage());
            // 降级为纯文本
            return convertHtmlToText(html);
        }
    }

    /**
     * 将 HTML 转换为纯文本
     */
    private String convertHtmlToText(String html) {
        try {
            Document doc = Jsoup.parse(html);
            return doc.text();
        } catch (Exception e) {
            logger.warn("Failed to convert HTML to text: {}", e.getMessage());
            return html;
        }
    }
}
