package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    
    @Value("${book.cover.base-dir:/sources/covers/}")
    private String coverBaseDir;
    
    @Value("${app.avatars.dir:/sources/avatars/}")
    private String avatarsDir;
    
    private List<String> excludePaths = Arrays.asList(
        "/user/login",
        "/user/register",
        "/view/login",
        "/view/register",
        "/view/ai",
        "/covers/**",      // 图书封面静态资源
        "/avatars/**"      // 用户头像静态资源
    );

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /covers/** 到图书封面目录
        // 优先使用 classpath（打包后的资源），然后是文件系统路径（开发时上传的文件）
        String baseDir = coverBaseDir;
        if (!baseDir.endsWith("/")) baseDir += "/";
        registry.addResourceHandler("/covers/**")
                .addResourceLocations(
                    "classpath:/static/covers/",  // 打包在jar中的资源
                    "file:" + baseDir              // 文件系统中的资源
                );
        
        // 映射 /avatars/** 到用户头像目录
        String avatarDir = avatarsDir;
        if (!avatarDir.endsWith("/")) avatarDir += "/";
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations(
                    "classpath:/static/avatars/",  // 打包在jar中的资源
                    "file:" + avatarDir             // 文件系统中的资源
                );
    }
}
