package com.powermanage.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, 
        WebSocketHandler wsHandler, Map<String, Object> attributes) {
        
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getPrincipal().equals("anonymousUser")) {
            
            String username = authentication.getName();
            if (username != null && !username.isEmpty()) {
                return new Principal() {
                    @Override
                    public String getName() {
                        return username;
                    }
                };
            }
        }
        
        
        return null;
    }
}