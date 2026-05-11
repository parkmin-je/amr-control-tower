package com.amr.dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket STOMP 채널 인증 인터셉터
 * CONNECT 프레임에서 세션 기반 인증 정보를 STOMP 세션에 전파.
 * 미인증 연결은 거부하여 WebSocket 레이어 보안 강화.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                    if (auth == null || !auth.isAuthenticated()
                            || auth instanceof AnonymousAuthenticationToken) {
                        // 미인증 연결 거부
                        throw new IllegalStateException("WebSocket 연결 인증 실패: 로그인이 필요합니다.");
                    }

                    accessor.setUser(auth);
                }

                // SUBSCRIBE/SEND 프레임: 기존 user 정보 유지 (CONNECT 시 설정됨)
                return message;
            }
        });
    }
}
