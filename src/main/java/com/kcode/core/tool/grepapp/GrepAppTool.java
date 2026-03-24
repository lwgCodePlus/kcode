package com.kcode.core.tool.grepapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GrepApp 工具 - 使用 grep.app API 搜索 GitHub 代码
 *
 * @author liwenguang
 * @since 2026/3/22
 */
public class GrepAppTool {
    private static final Logger logger = LoggerFactory.getLogger(GrepAppTool.class);
    private static final int DEFAULT_TIMEOUT = 30;
    private static final int MAX_QUERY_LENGTH = 1000;
    private static final int DEFAULT_RESULT_LIMIT = 10;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GrepAppTool() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Tool(description = "从 GitHub 仓库搜索代码示例")
    public Mono<ToolResultBlock> searchGitHub(
            @ToolParam(name = "query", description = "代码模式", required = true) String query,
            @ToolParam(name = "language", description = "语言过滤（TypeScript、Java、JavaScript、Python 注意命名）") String language,
            @ToolParam(name = "repo", description = "仓库过滤 (owner/repo)") String repo,
            @ToolParam(name = "path", description = "路径过滤") String path,
            @ToolParam(name = "matchCase", description = "区分大小写") Boolean matchCase,
            @ToolParam(name = "matchWholeWords", description = "全词匹配") Boolean matchWholeWords,
            @ToolParam(name = "useRegexp", description = "正则表达式") Boolean useRegexp
    ) {
        logger.info("GrepAppTool: query={}, repo={}", query, repo);
        
        if (query == null || query.isBlank()) {
            return Mono.just(ToolResultBlock.error("query is required"));
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            return Mono.just(ToolResultBlock.error("query too long"));
        }
        if (repo != null && !repo.contains("/")) {
            return Mono.just(ToolResultBlock.error("repo format: owner/repo"));
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query.trim());
        if (language != null && !language.isBlank()) params.put("f.lang", language.trim());
        if (repo != null && !repo.isBlank()) params.put("f.repo", repo.trim());
        if (path != null && !path.isBlank()) params.put("f.path", path.trim());

        return Mono.fromCallable(() -> {
            try {
                String json = fetch(params);
                logger.info("Response: {}", json != null ? json.substring(0, Math.min(300, json.length())) : "null");
                if (json == null || json.isEmpty()) {
                    return ToolResultBlock.error("Empty response");
                }
                return ToolResultBlock.text(format(json, query.trim()));
            } catch (Exception e) {
                logger.error("Error", e);
                return ToolResultBlock.error("Error: " + e.getMessage());
            }
        });
    }

    private String fetch(Map<String, String> params) throws Exception {
        StringBuilder url = new StringBuilder("https://grep.app/api/search?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) url.append("&");
            url.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        logger.info("URL: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.info("Status: {}", response.statusCode());
        return response.body();
    }

    private String format(String json, String query) {
        try {
            JsonNode root = objectMapper.readTree(json);
            int total = root.path("hits").path("total").asInt(0);
            logger.info("Total: {}", total);

            StringBuilder sb = new StringBuilder();
            sb.append("Query: ").append(query).append("\n\n");
            sb.append("Total Results: ").append(total).append("\n\n");

            JsonNode hits = root.path("hits").path("hits");
            if (hits.isArray()) {
                int cnt = 0;
                for (JsonNode hit : hits) {
                    if (cnt++ >= DEFAULT_RESULT_LIMIT) break;
                    
                    String repoName = hit.path("repo").asText("?");
                    String filePath = hit.path("path").asText("?");
                    String branch = hit.path("branch").asText("main");
                    String snippet = hit.path("content").path("snippet").asText("");
                    String clean = Jsoup.parse(snippet).text();
                    
                    sb.append("Repository: ").append(repoName).append("\n");
                    sb.append("File: ").append(filePath).append("\n");
                    sb.append("Branch: ").append(branch).append("\n");
                    sb.append("Code:\n```\n").append(clean.substring(0, Math.min(300, clean.length()))).append("\n```\n\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}