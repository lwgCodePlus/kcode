package com.kcode.core.tool.context7;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Context7 工具 - 从 Context7 获取最新的库文档和代码示例
 * <p>
 * 提供两个主要功能：
 * 1. resolve-library-id: 将库名称解析为 Context7 兼容的库 ID
 * 2. query-docs: 使用库 ID 查询文档和代码示例
 *
 * @author liwenguang
 * @since 2026/3/22
 */
public class Context7Tool {
    private static final Logger logger = LoggerFactory.getLogger(Context7Tool.class);

    /**
     * Context7 API 基础 URL
     */
    private static final String CONTEXT7_API_BASE_URL = "https://context7.com/api";

    /**
     * 服务器版本号
     */
    private static final String SERVER_VERSION = "1.0.0";

    /**
     * 默认超时时间（秒）
     */
    private static final int DEFAULT_TIMEOUT = 60;

    /**
     * 最大内容长度限制
     */
    private static final int MAX_CONTENT_LENGTH = 2 * 1024 * 1024;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public Context7Tool() {
        // Native Image 兼容性：使用 JVM 内置 DNS 解析器替代 Netty DNS 解析器
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);

        this.webClient = WebClient.builder()
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_CONTENT_LENGTH))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析库 ID - 将包/产品名称解析为 Context7 兼容的库 ID
     */
    @Tool(description = """
            解析 Context7 库 ID - 将包/产品名称解析为 Context7 兼容的库 ID 并返回匹配的库。
            
            在使用 'query-docs' 工具之前，必须先调用此函数获取有效的 Context7 兼容库 ID，
            除非用户在其查询中明确提供了格式为 '/org/project' 或 '/org/project/version' 的库 ID。
            
            每个结果包含：
            - Library ID: Context7 兼容标识符（格式：/org/project）
            - Name: 库或包名称
            - Description: 简短摘要
            - Code Snippets: 可用代码示例数量
            - Source Reputation: 权威性指标（High, Medium, Low, 或 Unknown）
            - Benchmark Score: 质量指标（100 是最高分）
            - Versions: 可用版本列表
            
            选择过程：
            1. 分析查询以了解用户正在寻找什么库/包
            2. 根据以下因素返回最相关的匹配：
               - 与查询的名称相似度（优先精确匹配）
               - 描述与查询意图的相关性
               - 文档覆盖率（优先选择代码片段数量较多的库）
               - 来源信誉（优先选择 High 或 Medium 信誉的库）
               - Benchmark Score: 质量指标（100 是最高分）
            
            重要：每个问题不要调用此工具超过 3 次。如果 3 次调用后仍找不到所需内容，请使用最佳结果。
            """)
    public Mono<ToolResultBlock> resolveLibraryId(
            @ToolParam(name = "query", description = """
                    你需要帮助的问题或任务。这用于根据用户尝试完成的内容对库结果进行相关性排名。
                    不要在查询中包含任何敏感或机密信息，如 API 密钥、密码、凭据、个人数据或专有代码。
                    """, required = true) String query,
            @ToolParam(name = "libraryName", description = "要搜索并检索 Context7 兼容库 ID 的库名称", required = true) String libraryName) {

        logger.info("resolve-library-id 执行：libraryName={}, query={}", libraryName, query);

        return Mono.fromCallable(() -> {
            // 1. 参数验证
            if (libraryName == null || libraryName.trim().isEmpty()) {
                return ToolResultBlock.error("Error: libraryName parameter is required");
            }
            if (query == null || query.trim().isEmpty()) {
                return ToolResultBlock.error("Error: query parameter is required");
            }

            // 2. 构建请求 URL
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String encodedLibraryName = URLEncoder.encode(libraryName.trim(), StandardCharsets.UTF_8);
            String url = String.format("%s/v2/libs/search?query=%s&libraryName=%s",
                    CONTEXT7_API_BASE_URL, encodedQuery, encodedLibraryName);

            // 3. 发送请求
            String responseBody = fetchUrl(url);

            // 4. 解析响应
            SearchResponse searchResponse = parseSearchResponse(responseBody);

            // 5. 格式化结果
            String result = formatSearchResults(searchResponse, libraryName);

            return ToolResultBlock.text(result);

        }).onErrorResume(e -> {
            logger.error("Error executing resolve-library-id for libraryName '{}': {}", libraryName, e.getMessage(), e);
            return Mono.just(ToolResultBlock.error("Error resolving library ID: " + e.getMessage()));
        });
    }

    /**
     * 查询文档 - 从 Context7 检索最新的文档和代码示例
     */
    @Tool(description = """
            查询文档 - 从 Context7 检索任何编程库或框架的最新文档和代码示例。
            
            你必须先调用 'resolve-library-id' 工具获取确切的 Context7 兼容库 ID 才能使用此工具，
            除非用户在其查询中明确提供了格式为 '/org/project' 或 '/org/project/version' 的库 ID。
            
            重要：每个问题不要调用此工具超过 3 次。如果 3 次调用后仍找不到所需内容，请使用最佳信息。
            """)
    public Mono<ToolResultBlock> queryDocs(
            @ToolParam(name = "libraryId", description = """
                    确切的 Context7 兼容库 ID（例如 '/mongodb/docs', '/vercel/next.js', '/supabase/supabase', '/vercel/next.js/v14.3.0-canary.87'），
                    从 'resolve-library-id' 检索或直接从用户查询中以 '/org/project' 或 '/org/project/version' 格式提供。
                    """, required = true) String libraryId,
            @ToolParam(name = "query", description = """
                    你需要帮助的问题或任务。要具体并包含相关细节。
                    好的例子：'如何在 Express.js 中使用 JWT 设置身份验证' 或 'React useEffect 清理函数示例'。
                    不好的例子：'auth' 或 'hooks'。
                    不要在查询中包含任何敏感或机密信息，如 API 密钥、密码、凭据、个人数据或专有代码。
                    """, required = true) String query) {

        logger.info("query-docs 执行：libraryId={}, query={}", libraryId, query);

        return Mono.fromCallable(() -> {
            // 1. 参数验证
            if (libraryId == null || libraryId.trim().isEmpty()) {
                return ToolResultBlock.error("Error: libraryId parameter is required");
            }
            if (query == null || query.trim().isEmpty()) {
                return ToolResultBlock.error("Error: query parameter is required");
            }

            // 2. 规范化 libraryId（确保以 / 开头）
            String normalizedLibraryId = libraryId.trim();
            if (!normalizedLibraryId.startsWith("/")) {
                normalizedLibraryId = "/" + normalizedLibraryId;
            }

            // 3. 构建请求 URL
            String encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String encodedLibraryId = URLEncoder.encode(normalizedLibraryId, StandardCharsets.UTF_8);
            String url = String.format("%s/v2/context?query=%s&libraryId=%s",
                    CONTEXT7_API_BASE_URL, encodedQuery, encodedLibraryId);

            // 4. 发送请求
            String responseBody = fetchUrl(url);

            // 5. 检查是否为空响应
            if (responseBody == null || responseBody.trim().isEmpty()) {
                return ToolResultBlock.text(
                        "Documentation not found or not finalized for this library. " +
                        "This might have happened because you used an invalid Context7-compatible library ID. " +
                        "To get a valid Context7-compatible library ID, use the 'resolve-library-id' tool " +
                        "with the package name you wish to retrieve documentation for.");
            }

            return ToolResultBlock.text(responseBody);

        }).onErrorResume(e -> {
            logger.error("Error executing query-docs for libraryId '{}': {}", libraryId, e.getMessage(), e);
            return Mono.just(ToolResultBlock.error("Error querying documentation: " + e.getMessage()));
        });
    }

    /**
     * 获取 URL 内容
     */
    private String fetchUrl(String url) {
        try {
            return webClient.get()
                    .uri(java.net.URI.create(url))
                    .header("X-Context7-Source", "mcp-server")
                    .header("X-Context7-Server-Version", SERVER_VERSION)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch URL: " + e.getMessage(), e);
        }
    }

    /**
     * 解析搜索响应
     */
    private SearchResponse parseSearchResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, SearchResponse.class);
        } catch (Exception e) {
            logger.error("Failed to parse search response: {}", e.getMessage());
            SearchResponse response = new SearchResponse();
            response.setError("Failed to parse search response: " + e.getMessage());
            response.setResults(List.of());
            return response;
        }
    }

    /**
     * 格式化搜索结果
     */
    private String formatSearchResults(SearchResponse searchResponse, String libraryName) {
        if (searchResponse.getError() != null && !searchResponse.getError().isEmpty()) {
            return searchResponse.getError();
        }

        if (searchResponse.getResults() == null || searchResponse.getResults().isEmpty()) {
            return "No libraries found matching the provided name.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Available Libraries:\n\n");

        for (int i = 0; i < searchResponse.getResults().size(); i++) {
            SearchResult result = searchResponse.getResults().get(i);
            sb.append(formatSearchResult(result));
            if (i < searchResponse.getResults().size() - 1) {
                sb.append("\n----------\n");
            }
        }

        return sb.toString();
    }

    /**
     * 格式化单个搜索结果
     */
    private String formatSearchResult(SearchResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("- Title: ").append(result.getTitle()).append("\n");
        sb.append("- Context7-compatible library ID: ").append(result.getId()).append("\n");
        sb.append("- Description: ").append(result.getDescription()).append("\n");

        if (result.getTotalSnippets() != null && result.getTotalSnippets() >= 0) {
            sb.append("- Code Snippets: ").append(result.getTotalSnippets()).append("\n");
        }

        String reputationLabel = getSourceReputationLabel(result.getTrustScore());
        sb.append("- Source Reputation: ").append(reputationLabel).append("\n");

        if (result.getBenchmarkScore() != null && result.getBenchmarkScore() > 0) {
            sb.append("- Benchmark Score: ").append(result.getBenchmarkScore()).append("\n");
        }

        if (result.getVersions() != null && !result.getVersions().isEmpty()) {
            sb.append("- Versions: ").append(String.join(", ", result.getVersions())).append("\n");
        }

        if (result.getSource() != null && !result.getSource().isEmpty()) {
            sb.append("- Source: ").append(result.getSource()).append("\n");
        }

        return sb.toString().stripTrailing();
    }

    /**
     * 将数字来源信誉分数映射为可解释的标签
     */
    private String getSourceReputationLabel(Integer trustScore) {
        if (trustScore == null || trustScore < 0) {
            return "Unknown";
        }
        if (trustScore >= 7) {
            return "High";
        }
        if (trustScore >= 4) {
            return "Medium";
        }
        return "Low";
    }

    // ==================== 内部数据类 ====================

    /**
     * 搜索响应
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResponse {
        private String error;
        private List<SearchResult> results;
        
        @JsonProperty("searchFilterApplied")
        private Boolean searchFilterApplied;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public List<SearchResult> getResults() {
            return results;
        }

        public void setResults(List<SearchResult> results) {
            this.results = results;
        }

        public Boolean getSearchFilterApplied() {
            return searchFilterApplied;
        }

        public void setSearchFilterApplied(Boolean searchFilterApplied) {
            this.searchFilterApplied = searchFilterApplied;
        }
    }

    /**
     * 搜索结果项
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        private String id;
        private String title;
        private String description;
        private String branch;
        private String lastUpdateDate;
        private Integer totalSnippets;
        private Integer trustScore;
        private Integer benchmarkScore;
        private List<String> versions;
        private String source;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getLastUpdateDate() {
            return lastUpdateDate;
        }

        public void setLastUpdateDate(String lastUpdateDate) {
            this.lastUpdateDate = lastUpdateDate;
        }

        public Integer getTotalSnippets() {
            return totalSnippets;
        }

        public void setTotalSnippets(Integer totalSnippets) {
            this.totalSnippets = totalSnippets;
        }

        public Integer getTrustScore() {
            return trustScore;
        }

        public void setTrustScore(Integer trustScore) {
            this.trustScore = trustScore;
        }

        public Integer getBenchmarkScore() {
            return benchmarkScore;
        }

        public void setBenchmarkScore(Integer benchmarkScore) {
            this.benchmarkScore = benchmarkScore;
        }

        public List<String> getVersions() {
            return versions;
        }

        public void setVersions(List<String> versions) {
            this.versions = versions;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}