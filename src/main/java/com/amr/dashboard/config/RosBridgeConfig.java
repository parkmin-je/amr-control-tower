package com.amr.dashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rosbridge")
public class RosBridgeConfig {

    private long reconnectDelayMs = 3000;
    private List<RobotConnection> robots = new ArrayList<>();

    @Getter
    @Setter
    public static class RobotConnection {
        private String robotId;
        private String uri;
    }
}
