package com.x3bits.springaireactagent.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web配置类，用于配置静态资源和路由
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置静态资源处理
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源路径，确保API路径不受影响
        registry.addResourceHandler("/springAiReactAgent/**")
                .addResourceLocations("classpath:/static/springAiReactAgentUi/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);

                        // 如果是API请求，不处理
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }

                        // 如果请求的资源存在，直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }

                        // 对于不存在的资源，如果不是静态文件（没有扩展名或者是HTML），返回index.html
                        // 这样可以支持前端路由
                        if (!resourcePath.contains(".") || resourcePath.endsWith(".html")) {
                            return new ClassPathResource("/static/springAiReactAgentUi/index.html");
                        }

                        return null;
                    }
                });
    }

    /**
     * 配置视图控制器，将/springAiReactAgent路径映射到前端主页
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 将/springAiReactAgent路径重定向到/springAiReactAgent/
        registry.addRedirectViewController("/springAiReactAgent", "/springAiReactAgent/");

        // 将/springAiReactAgent/路径映射到index.html
        registry.addViewController("/springAiReactAgent/").setViewName("forward:/springAiReactAgent/index.html");
    }
}
