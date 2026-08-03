package com.rabbot.gateway.filter;

import com.rabbot.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate; // Реактивный клиент Redis
    
    private final Pattern workspaceIdPattern = Pattern.compile("/api/[^/]+/(\\d+)");

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Отсутствует заголовок Authorization", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0);
            if (authHeader.startsWith("Bearer ")) {
                authHeader = authHeader.substring(7);
            } else {
                return onError(exchange, "Неверный формат токена", HttpStatus.UNAUTHORIZED);
            }

            try {
                jwtUtil.validateToken(authHeader);
                String email = jwtUtil.extractEmail(authHeader);

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Email", email)
                        .build();
                
                Matcher matcher = workspaceIdPattern.matcher(path);
                
                if (matcher.find()) {
                    String requestedWorkspaceId = matcher.group(1);
                    String redisKey = "user:" + email + ":workspaces";

                    return redisTemplate.opsForSet().isMember(redisKey, requestedWorkspaceId)
                            .flatMap(isAllowed -> {
                                if (Boolean.TRUE.equals(isAllowed)) {
                                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                } else {
                                    return onError(exchange, "Доступ к рабочему пространству запрещен", HttpStatus.FORBIDDEN);
                                }
                            });
                }

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                return onError(exchange, "Невалидный токен", HttpStatus.FORBIDDEN);
            }
        });
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    public static class Config {
    }
}