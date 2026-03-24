package com.kcode.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 模型配置服务
 * <p>
 * 管理模型配置的持久化存储，配置文件存储在用户主目录下的 .kcode/model-config.json
 *
 * @author liwenguang
 * @since 2026/3/16
 */
@Service
public class ModelConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ModelConfigService.class);

    private static final String CONFIG_DIR = ".kcode";
    private static final String CONFIG_FILE = "model-config.json";

    private final Path configPath;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 当前配置缓存
     */
    private volatile ModelConfigData currentConfig;

    /**
     * 构造 ModelConfigService
     */
    public ModelConfigService() {
        this.configPath = Path.of(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE);
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 获取当前配置
     *
     * @return 当前模型配置
     */
    public ModelConfigData getConfig() {
        lock.readLock().lock();
        try {
            this.currentConfig = loadConfig();
            return currentConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 更新配置
     *
     * @param newConfig 新配置
     * @return 更新后的配置
     */
    public ModelConfigData updateConfig(ModelConfigData newConfig) {
        lock.writeLock().lock();
        try {
            ModelConfigData mergedConfig = currentConfig.merge(newConfig);
            saveConfigToFile(mergedConfig);
            this.currentConfig = mergedConfig;

            logger.info("模型配置已更新: baseUrl={}, modelName={}",
                    maskSensitive(mergedConfig.baseUrl()), mergedConfig.modelName());

            return mergedConfig;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 设置完整配置
     *
     * @param baseUrl   API 基础 URL
     * @param apiKey    API 密钥
     * @param modelName 模型名称
     * @return 更新后的配置
     */
    public ModelConfigData setConfig(String baseUrl, String apiKey, String modelName) {
        ModelConfigData newConfig = ModelConfigData.of(baseUrl, apiKey, modelName);
        return updateConfig(newConfig);
    }

    /**
     * 重置配置为默认值
     */
    public void resetConfig() {
        lock.writeLock().lock();
        try {
            this.currentConfig = ModelConfigData.empty();
            if (Files.exists(configPath)) {
                Files.delete(configPath);
                logger.info("配置文件已删除: {}", configPath);
            }
        } catch (IOException e) {
            logger.error("删除配置文件失败: {}", e.getMessage(), e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取配置文件路径
     *
     * @return 配置文件路径
     */
    public Path getConfigPath() {
        return configPath;
    }

    // ==================== 私有方法 ====================

    /**
     * 从文件加载配置
     *
     * @return 加载的配置，如果文件不存在则返回空配置
     */
    private ModelConfigData loadConfig() {
        try {
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                ModelConfigData config = objectMapper.readValue(content, ModelConfigData.class);
                logger.debug("从文件加载模型配置: {}", configPath);
                return config;
            }
        } catch (IOException e) {
            logger.error("加载配置文件失败: {}", e.getMessage(), e);
        }
        return ModelConfigData.empty();
    }

    /**
     * 保存配置到文件
     *
     * @param config 配置数据
     */
    private void saveConfigToFile(ModelConfigData config) {
        try {
            Path parentDir = configPath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                logger.info("创建配置目录: {}", parentDir);
            }

            String content = objectMapper.writeValueAsString(config);
            Files.writeString(configPath, content);
            logger.debug("配置已保存到文件: {}", configPath);
        } catch (IOException e) {
            logger.error("保存配置文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存配置文件失败", e);
        }
    }

    /**
     * 脱敏显示敏感信息
     *
     * @param value 原始值
     * @return 脱敏后的字符串
     */
    private String maskSensitive(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}