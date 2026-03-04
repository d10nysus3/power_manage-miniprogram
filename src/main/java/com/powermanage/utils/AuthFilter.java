package com.powermanage.utils;

import com.powermanage.entity.SystemUser;
import com.powermanage.repository.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;


public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        System.out.println("AuthFilter: Processing request to " + path);
        
        
        System.out.println("AuthFilter: Skipping authentication for " + path);
        filterChain.doFilter(request, response);
        return;
    }

    
    private String parseUsername(String token) {
        try {
            String raw = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            if (raw.endsWith(":token")) {
                return raw.substring(0, raw.length() - 6);
            }
        } catch (Exception ignored) {}
        return null;
    }
}