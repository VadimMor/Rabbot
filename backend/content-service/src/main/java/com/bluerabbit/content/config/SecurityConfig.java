package com.bluerabbit.content.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthFilter headerAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Разрешаем CORS, если фронтенд будет стучаться сюда в обход Gateway при разработке, 
            // но в проде запросы должны идти только через Gateway
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/content/**").authenticated() // Защищаем наши новые контроллеры
                .anyRequest().permitAll()
            )
            // Отключаем сессии, так как у нас каждый запрос изолирован (Stateless)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Внедряем наш кастомный фильтр заголовков
            .addFilterBefore(headerAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}