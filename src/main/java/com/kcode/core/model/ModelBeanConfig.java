package com.kcode.core.model;

import io.agentscope.core.model.Model;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型 Bean 配置类
 * <p>
 * 通过 ModelFactory 动态创建 Model Bean，支持配置热更新
 *
 * @author liwenguang
 * @since 2026/3/9
 */
@Configuration
public class ModelBeanConfig {

    private final ModelFactory modelFactory;

    public ModelBeanConfig(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    /**
     * 创建 Model Bean
     *
     * @return Model 实例
     */
    @Bean
    public Model chatModel() {
        return modelFactory.getModel();
    }
}