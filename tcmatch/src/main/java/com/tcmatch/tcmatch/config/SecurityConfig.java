package com.tcmatch.tcmatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
                .requestMatchers("/h2-console/**").permitAll() // Разрешить доступ к H2
                .anyRequest().authenticated()
        );

        // Для H2 Console нужно отключить CSRF и включить frames
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**"));
        http.headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
