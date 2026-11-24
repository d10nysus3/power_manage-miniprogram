package com.powermanage.config;



import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

import com.powermanage.filter.JwtAuthenticationFilter;



@Configuration

@EnableWebSecurity

public class SecurityConfig {



    private final JwtAuthenticationFilter jwtAuthenticationFilter;



    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

    }



    @Bean

    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http

            .cors().and()

            .csrf().disable()

            .authorizeRequests()
                // 静态资源和公共接口
                .antMatchers("/static/**", "/public/**", "/css/**", "/js/**", "/images/**").permitAll()
                .antMatchers("/ws/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")


                // 限制只有 ADMIN 可以访问的模块
                .antMatchers("/api/pattern-analysis/**").hasRole("ADMIN")
                .antMatchers("/api/anomalies/**").hasRole("ADMIN")
                .antMatchers("/api/emergency/**").hasRole("ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/system-management/**").hasRole("ADMIN")
                .antMatchers("/api/system-users/**").hasRole("ADMIN")
                .antMatchers("/api/audit-logs", "/api/audit-logs/**", "/api/settings", "/api/settings/**").hasRole("ADMIN")

                // 其他接口允许 ADMIN 和 MANAGER 访问
                .antMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .antMatchers("/api/electricity-data/**").hasAnyRole("ADMIN", "MANAGER")
                .antMatchers("/api/electricity-statistics/**").hasAnyRole("ADMIN", "MANAGER")
                .antMatchers("/api/users/**").hasAnyRole("ADMIN", "MANAGER")
                .antMatchers("/api/reports/**").hasAnyRole("ADMIN", "MANAGER")
                
                .anyRequest().authenticated()

                .and()

            .formLogin().disable()

            .httpBasic().disable()

            .exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)).and()
            .headers().frameOptions().disable(); 
        

        
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        

        return http.build();

    }

}
