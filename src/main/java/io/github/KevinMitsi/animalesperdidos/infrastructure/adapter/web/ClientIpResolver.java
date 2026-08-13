package io.github.KevinMitsi.animalesperdidos.infrastructure.adapter.web;

import io.github.KevinMitsi.animalesperdidos.infrastructure.config.CloudflareProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClientIpResolver {
    private final CloudflareProperties properties;

    public String resolve(ServerHttpRequest request) {
        if (properties.isTrustConnectingIp()) {
            String cloudflareIp = request.getHeaders().getFirst("CF-Connecting-IP");
            if (cloudflareIp != null && !cloudflareIp.isBlank()) {
                return cloudflareIp;
            }
        }
        return request.getRemoteAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
