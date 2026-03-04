package com.powermanage.filter;

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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private SystemUserRepository systemUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        
        
        if (path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/register") || 
            path.startsWith("/ws/") ||
            path.startsWith("/static/") ||
            path.startsWith("/public/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        
        final String authorizationHeader = request.getHeader("Authorization");
        
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            String username = parseUsername(token);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Optional<SystemUser> userOpt = systemUserRepository.findByUsername(username);
                
                if (userOpt.isPresent()) {
                    SystemUser user = userOpt.get();
                    
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        
        filterChain.doFilter(request, response);
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