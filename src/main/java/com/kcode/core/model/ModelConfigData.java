package com.kcode.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 模型配置数据类
 * <p>
 * 用于存储和序列化模型配置信息
 *
 * @param baseUrl   API 基础 URL
 * @param apiKey    API 密钥
 * @param modelName 模型名称
 * @author liwenguang
 * @since 2026/3/16
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ModelConfigData(
        @JsonProperty("base_url") String baseUrl,
        @JsonProperty("api_key") String apiKey,
        @JsonProperty("model_name") String modelName
) {
    /**
     * 默认构造（使用空值）
     */
    public static ModelConfigData empty() {
        return new ModelConfigData(null, null, null);
    }

    /**
     * 创建配置
     *
     * @param baseUrl   API 基础 URL
     * @param apiKey    API 密钥
     * @param modelName 模型名称
     * @return ModelConfigData 实例
     */
    public static ModelConfigData of(String baseUrl, String apiKey, String modelName) {
        return new ModelConfigData(baseUrl, apiKey, modelName);
    }

    /**
     * 检查配置是否有效
     *
     * @return true 表示配置有效
     */
    public boolean isValid() {
        return baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && modelName != null && !modelName.isBlank();
    }

    /**
     * 合并配置（非空值覆盖）
     *
     * @param other 其他配置
     * @return 合并后的配置
     */
    public ModelConfigData merge(ModelConfigData other) {
        if (other == null) {
            return this;
        }
        return new ModelConfigData(
                other.baseUrl != null && !other.baseUrl.isBlank() ? other.baseUrl : this.baseUrl,
                other.apiKey != null && !other.apiKey.isBlank() ? other.apiKey : this.apiKey,
                other.modelName != null && !other.modelName.isBlank() ? other.modelName : this.modelName
        );
    }
}