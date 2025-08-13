package com.example.demo.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.demo.common.Constants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

// 登陆拦截器（禁止访问其他页面信息或其他方法）
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 禁止自动创建Session（只起到验证作用）
        HttpSession session = request.getSession(false);
        if(session != null && session.getAttribute(Constants.SESSION_USER_KEY) != null) {
            return true;
        }
        // 用户未登录，返回401
        response.setStatus(401);
        return false;
    }
}
