package com.kcode.web;

import com.kcode.core.model.ModelConfigData;
import com.kcode.core.model.ModelConfigService;
import com.kcode.core.model.ModelFactory;
import com.kcode.core.service.AgentService;
import com.kcode.web.dto.config.ModelConfigRequest;
import com.kcode.web.dto.config.ModelConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 模型配置 REST 控制器
 * <p>
 * 提供模型配置的查询和修改接口
 *
 * @author liwenguang
 * @since 2026/3/16
 */
@RestController
@RequestMapping("/api/config")
public class ModelConfigController {

    private static final Logger logger = LoggerFactory.getLogger(ModelConfigController.class);

    private final ModelConfigService modelConfigService;
    private final ModelFactory modelFactory;
    private final AgentService agentService;

    public ModelConfigController(ModelConfigService modelConfigService, 
                                  ModelFactory modelFactory,
                                  AgentService agentService) {
        this.modelConfigService = modelConfigService;
        this.modelFactory = modelFactory;
        this.agentService = agentService;
    }

    /**
     * 获取当前模型配置
     * <p>
     * GET /api/config/model
     *
     * @return 模型配置信息
     */
    @GetMapping("/model")
    public ResponseEntity<ModelConfigResponse> getConfig() {
        ModelConfigData config = modelConfigService.getConfig();
        ModelConfigResponse response = ModelConfigResponse.of(
                config.baseUrl(),
                config.apiKey(),
                config.modelName(),
                config.isValid(),
                modelConfigService.getConfigPath().toString()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 更新模型配置
     * <p>
     * PUT /api/config/model
     * <p>
     * 可以只更新部分字段，未提供的字段保持不变
     *
     * @param request 配置请求
     * @return 更新后的配置信息
     */
    @PutMapping("/model")
    public ResponseEntity<ModelConfigResponse> updateConfig(@RequestBody ModelConfigRequest request) {
        logger.info("更新模型配置: baseUrl={}, modelName={}", 
                request.baseUrl() != null ? "***" : null, 
                request.modelName());

        ModelConfigData newConfig = ModelConfigData.of(
                request.baseUrl(),
                request.apiKey(),
                request.modelName()
        );

        ModelConfigData updated = modelConfigService.updateConfig(newConfig);
        
        // 重建 Model 并清除缓存的 Agent 实例
        modelFactory.rebuildModel();
        agentService.invalidateAllCachedAgents();
        logger.info("模型配置已更新，已清除所有缓存的 Agent 实例");
        
        ModelConfigResponse response = ModelConfigResponse.of(
                updated.baseUrl(),
                updated.apiKey(),
                updated.modelName(),
                updated.isValid(),
                modelConfigService.getConfigPath().toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 设置完整模型配置
     * <p>
     * POST /api/config/model
     * <p>
     * 设置完整的模型配置（所有字段）
     *
     * @param request 配置请求
     * @return 设置后的配置信息
     */
    @PostMapping("/model")
    public ResponseEntity<ModelConfigResponse> setConfig(@RequestBody ModelConfigRequest request) {
        logger.info("设置模型配置: modelName={}", request.modelName());

        if (request.baseUrl() == null || request.baseUrl().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ModelConfigResponse.of(null, null, null, false, null));
        }

        ModelConfigData updated = modelConfigService.setConfig(
                request.baseUrl(),
                request.apiKey(),
                request.modelName()
        );
        
        // 重建 Model 并清除缓存的 Agent 实例
        modelFactory.rebuildModel();
        agentService.invalidateAllCachedAgents();
        logger.info("模型配置已设置，已清除所有缓存的 Agent 实例");

        ModelConfigResponse response = ModelConfigResponse.of(
                updated.baseUrl(),
                updated.apiKey(),
                updated.modelName(),
                updated.isValid(),
                modelConfigService.getConfigPath().toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 重置模型配置
     * <p>
     * DELETE /api/config/model
     *
     * @return 无内容响应
     */
    @DeleteMapping("/model")
    public ResponseEntity<Void> resetConfig() {
logger.info("重置模型配置");
        modelConfigService.resetConfig();
        
        // 重建 Model 并清除缓存的 Agent 实例
        modelFactory.rebuildModel();
        agentService.invalidateAllCachedAgents();
        logger.info("模型配置已重置，已清除所有缓存的 Agent 实例");
return ResponseEntity.noContent().build();
    }
}