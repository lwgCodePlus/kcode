package com.kcode.web.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 模型配置响应 DTO
 * <p>
 * 用于返回当前模型配置信息
 *
 * @param baseUrl    API 基础 URL
 * @param apiKey     API 密钥
 * @param modelName  模型名称
 * @param valid      配置是否有效
 * @param configPath 配置文件路径
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelConfigResponse(
        @JsonProperty("base_url") String baseUrl,
        @JsonProperty("api_key") String apiKey,
        @JsonProperty("model_name") String modelName,
        Boolean valid,
        @JsonProperty("config_path") String configPath
) {
    /**
     * 创建响应
     *
     * @param baseUrl    API 基础 URL
     * @param apiKey     API 密钥
     * @param modelName  模型名称
     * @param valid      配置是否有效
     * @param configPath 配置文件路径
     * @return ModelConfigResponse 实例
     */
    public static ModelConfigResponse of(String baseUrl, String apiKey, String modelName, 
                                          boolean valid, String configPath) {
        return new ModelConfigResponse(
                baseUrl,
                apiKey,
                modelName,
                valid,
                configPath
        );
    }
}