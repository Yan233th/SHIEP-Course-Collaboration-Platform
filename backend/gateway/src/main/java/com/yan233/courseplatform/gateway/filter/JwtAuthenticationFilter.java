package com.yan233.courseplatform.gateway.filter;

import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.auth.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {
    private static final List<String> WHITES = List.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/files/preview/",
            "/actuator"
    );

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (WHITES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token = JwtSupport.stripBearer(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (token == null || token.isBlank()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            CurrentUser user = JwtSupport.parse(jwtSecret, token);
            ServerWebExchange mutated = exchange.mutate()
                    .request(builder -> builder
                            .header("X-User-Id", String.valueOf(user.userId()))
                            .header("X-Username", user.username())
                            .header("X-Roles", String.join(",", user.roles())))
                    .build();
            return chain.filter(mutated);
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

