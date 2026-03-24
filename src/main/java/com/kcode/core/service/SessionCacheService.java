package com.kcode.core.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.kcode.core.config.AgentFactory;
import com.kcode.core.config.AgentFactory.AgentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 会话缓存服务
 * <p>
 * 管理 Agent 实例的 LRU 缓存，支持：
 * <ul>
 *   <li>最大缓存数量限制</li>
 *   <li>空闲超时自动淘汰</li>
 *   <li>淘汰时自动持久化</li>
 * </ul>
 *
 * @author liwenguang
 * @since 2026/3/22
 */
@Service
public class SessionCacheService {

    private static final Logger logger = LoggerFactory.getLogger(SessionCacheService.class);

    /**
     * 最大缓存会话数
     */
    private static final int MAX_CACHE_SIZE = 5;

    /**
     * 会话空闲超时时间（分钟）
     */
    private static final int SESSION_IDLE_TIMEOUT_MINUTES = 30;

    private final AgentFactory agentFactory;
    private final Cache<String, AgentInstance> sessionCache;

    /**
     * 构造 SessionCacheService
     *
     * @param agentFactory Agent 工厂
     */
    public SessionCacheService(AgentFactory agentFactory) {
        this.agentFactory = agentFactory;
        this.sessionCache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .expireAfterAccess(SESSION_IDLE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
                .removalListener((String sessionId, AgentInstance instance, RemovalCause cause) -> {
                    if (instance != null && cause.wasEvicted()) {
                        saveSessionToStorage(instance);
                        logger.info("会话 {} 已从缓存淘汰并保存到存储，原因: {}", sessionId, cause);
                    }
                })
                .build();

        logger.info("SessionCacheService 初始化完成，最大缓存会话数: {}", MAX_CACHE_SIZE);
    }

    /**
     * 获取或创建会话的 Agent 实例
     * <p>
     * 如果会话已存在，从缓存获取；否则创建新实例
     *
     * @param sessionId 会话ID
     * @return AgentInstance 实例，如果 Model 未配置则抛出异常
     * @throws IllegalStateException 如果 Model 未配置
     */
    public AgentInstance getOrCreate(String sessionId) {
        return sessionCache.get(sessionId, id -> {
            logger.info("创建 Agent 实例: {}", id);
            return agentFactory.createAgent(sessionId);
        });
    }

    /**
     * 获取已缓存的 Agent 实例（不创建新的）
     *
     * @param sessionId 会话ID
     * @return AgentInstance 实例，如果不存在返回 null
     */
    public AgentInstance getIfPresent(String sessionId) {
        return sessionCache.getIfPresent(sessionId);
    }

    /**
     * 使指定会话失效
     *
     * @param sessionId 会话ID
     */
    public void invalidate(String sessionId) {
        sessionCache.invalidate(sessionId);
        logger.debug("会话 {} 已从缓存移除", sessionId);
    }

    /**
     * 使所有缓存的 Agent 实例失效
     * <p>
     * 用于配置更新后强制刷新所有 Agent
     */
    public void invalidateAll() {
        sessionCache.invalidateAll();
        logger.info("已清除所有缓存的 Agent 实例");
    }

    /**
     * 保存会话状态到存储
     *
     * @param instance AgentInstance 实例
     */
    public void saveSessionToStorage(AgentInstance instance) {
        if (instance != null) {
            try {
                instance.sessionManager().saveSession();
                logger.debug("会话状态已保存: {}", instance.agent().getName());
            } catch (Exception e) {
                logger.error("保存会话状态失败: {}, 错误: {}", instance.agent().getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * 获取当前缓存大小
     *
     * @return 缓存中的会话数量
     */
    public long cacheSize() {
        return sessionCache.estimatedSize();
    }
}