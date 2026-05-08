package com.amr.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Gateway Rate Limiter — Redis 기반 토큰 버킷 알고리즘
 *
 * Why Redis: Gateway 가 여러 인스턴스로 확장될 때도 단일 Redis 에서 카운터를 공유해
 *            클라이언트별 한도를 정확히 제어한다.
 * Why 토큰 버킷: 순간 burst 는 허용하되(burstCapacity) 평균 속도(replenishRate)는 제한해
 *               정상 사용자가 일시적 트래픽 급증에도 차단되지 않는다.
 * Key: 클라이언트 IP — 미인증 API 남용 방지에 적합한 가장 단순한 식별자.
 *      향후 인증 사용자 단위로 변경 시 authentication.getName() 으로 교체 가능.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.justOrEmpty(
                exchange.getRequest().getRemoteAddress()
        ).map(addr -> addr.getAddress().getHostAddress())
         .defaultIfEmpty("unknown");
    }
}
