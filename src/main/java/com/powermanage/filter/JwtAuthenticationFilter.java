package com.powermanage.filter;

import com.powermanage.service.LoginSessionService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final LoginSessionService sessions;

    public JwtAuthenticationFilter(LoginSessionService sessions) { this.sessions = sessions; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            sessions.authenticate(authorization.substring(7)).ifPresent(user ->
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                            user.getUsername(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))));
        }
        chain.doFilter(request, response);
    }
}
