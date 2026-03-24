package com.kcode.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 配置异步请求支持，包括 SSE 流式响应的超时设置
 *
 * @author liwenguang
 * @since 2026/3/17
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 设置异步请求超时时间（毫秒）
        // 对于 SSE 流式响应，AI 对话可能需要较长时间
        // 设置为 10 分钟
        configurer.setDefaultTimeout(10 * 60 * 1000);
    }
}