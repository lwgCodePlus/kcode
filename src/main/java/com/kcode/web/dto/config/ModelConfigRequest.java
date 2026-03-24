package com.kcode.web.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 模型配置请求 DTO
 * <p>
 * 用于接收模型配置更新请求
 *
 * @param baseUrl   API 基础 URL
 * @param apiKey    API 密钥
 * @param modelName 模型名称
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelConfigRequest(
        @JsonProperty("base_url") String baseUrl,
        @JsonProperty("api_key") String apiKey,
        @JsonProperty("model_name") String modelName
) {
    /**
     * 验证请求是否有效
     *
     * @return true 表示请求有效
     */
    public boolean isValid() {
        return (baseUrl != null && !baseUrl.isBlank())
                || (apiKey != null && !apiKey.isBlank())
                || (modelName != null && !modelName.isBlank());
    }
}