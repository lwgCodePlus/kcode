package com.kcode.web.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA 静态资源配置
 * <p>
 * 配置静态资源服务和 SPA 路由回退
 *
 * @author liwenguang
 * @since 2026/3/17
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/public/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(@NotNull String resourcePath, @NotNull Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        
                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        
                        // 对于 SPA 路由，返回 index.html
                        // 排除 API 路径和静态资源路径
                        if (!resourcePath.startsWith("api/") && 
                            !resourcePath.endsWith(".js") && 
                            !resourcePath.endsWith(".css") && 
                            !resourcePath.endsWith(".ico") && 
                            !resourcePath.endsWith(".png") && 
                            !resourcePath.endsWith(".jpg") && 
                            !resourcePath.endsWith(".svg") && 
                            !resourcePath.endsWith(".woff") && 
                            !resourcePath.endsWith(".woff2") && 
                            !resourcePath.endsWith(".ttf") &&
                            !resourcePath.endsWith(".map")) {
                            return new ClassPathResource("/public/index.html");
                        }
                        
                        return null;
                    }
                });
    }
}