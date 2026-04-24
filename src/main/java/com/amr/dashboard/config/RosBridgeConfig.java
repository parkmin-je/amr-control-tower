package com.amr.dashboard.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class RosBridgeConfig {

    @Value("${rosbridge.uri}")
    private String uri;

    @Value("${rosbridge.reconnect-delay-ms}")
    private long reconnectDelayMs;

    @Value("${rosbridge.robot-id}")
    private String robotId;
}
