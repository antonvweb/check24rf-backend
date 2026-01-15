package org.example.billingService.interceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.billingService.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class SubscriptionInterceptor implements HandlerInterceptor {

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Проверяем только для определенных URL (например, премиум функций)
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/api/premium/")) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                UUID userId = UUID.fromString(auth.getName());

                if (!subscriptionService.hasActiveSubscription(userId)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("{\"error\": \"Требуется активная подписка\"}");
                    return false;
                }
            }
        }

        return true;
    }
}