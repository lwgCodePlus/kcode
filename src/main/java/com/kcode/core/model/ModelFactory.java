package com.kcode.core.model;

import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 模型工厂
 * <p>
 * 负责创建和管理 Model 实例，支持配置热更新
 * <p>
 * 配置来源由 {@link ModelConfigService} 统一管理
 *
 * @author liwenguang
 * @since 2026/3/9
 */
@Component
public class ModelFactory {

    private static final Logger logger = LoggerFactory.getLogger(ModelFactory.class);

    private final ModelConfigService modelConfigService;

    /**
     * 缓存的 Model 实例
     */
    private volatile Model cachedModel;

    /**
     * 当前配置的哈希值，用于检测配置变化
     */
    private volatile int configHash;

    /**
     * 构造 ModelFactory
     *
     * @param modelConfigService 模型配置服务
     */
    public ModelFactory(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    /**
     * 获取 Model 实例
     * <p>
     * 如果配置发生变化，会自动重建 Model
     *
     * @return Model 实例，如果配置无效则返回 null
     */
    public Model getModel() {
        ModelConfigData config = modelConfigService.getConfig();
        int newHash = computeConfigHash(config);

        if (cachedModel == null || newHash != configHash) {
            synchronized (this) {
                if (cachedModel == null || newHash != configHash) {
                    cachedModel = createModel(config);
                    configHash = newHash;
                    if (cachedModel != null) {
                        logger.info("Model 已创建: modelName={}", config.modelName());
                    } else {
                        logger.info("Model 配置无效，未创建实例");
                    }
                }
            }
        }

        return cachedModel;
    }

    /**
     * 强制重建 Model
     * <p>
     * 在配置更新后调用
     */
    public void rebuildModel() {
        synchronized (this) {
            ModelConfigData config = modelConfigService.getConfig();
            cachedModel = createModel(config);
            configHash = computeConfigHash(config);
            if (cachedModel != null) {
                logger.info("Model 已重建: modelName={}", config.modelName());
            } else {
                logger.info("Model 配置无效，未重建实例");
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 创建 Model 实例
     *
     * @param config 模型配置
     * @return Model 实例，如果配置无效则返回 null
     */
    private Model createModel(ModelConfigData config) {
        String baseUrl = config.baseUrl();
        String apiKey = config.apiKey();
        String modelName = config.modelName();

        // 验证必要参数
        if (baseUrl == null || baseUrl.isBlank()) {
            logger.warn("Model baseUrl 未配置");
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Model apiKey 未配置");
            return null;
        }
        if (modelName == null || modelName.isBlank()) {
            logger.warn("Model modelName 未配置");
            return null;
        }

        return OpenAIChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /**
     * 计算配置哈希值
     *
     * @param config 配置数据
     * @return 哈希值
     */
    private int computeConfigHash(ModelConfigData config) {
        if (config == null) {
            return 0;
        }
        int result = config.baseUrl() != null ? config.baseUrl().hashCode() : 0;
        result = 31 * result + (config.apiKey() != null ? config.apiKey().hashCode() : 0);
        result = 31 * result + (config.modelName() != null ? config.modelName().hashCode() : 0);
        return result;
    }
}