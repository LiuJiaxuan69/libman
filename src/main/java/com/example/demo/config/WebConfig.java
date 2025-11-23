package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.util.List;
import java.util.Arrays;

import com.example.demo.interceptor.LoginInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    private List<String> excludePaths = Arrays.asList(
        "/user/login",
        "/user/register",
        "/view/login",
        "/view/register",
        "/view/ai",
        "/ai/**"
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /covers/** 到文件系统目录（使用 application.properties 中的 book.cover.base-dir 配置或默认 covers/）
        String baseDir = System.getProperty("book.cover.base-dir", "covers");
        if (!baseDir.endsWith("/")) baseDir += "/";
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:" + baseDir);
    }
}
