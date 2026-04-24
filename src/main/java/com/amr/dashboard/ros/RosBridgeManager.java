package com.amr.dashboard.ros;

import com.amr.dashboard.config.RosBridgeConfig;
import com.amr.dashboard.service.RobotStatusService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RosBridgeManager {

    private final RosBridgeConfig config;
    private final RobotStatusService robotStatusService;
    private final List<RosBridgeClient> clients = new ArrayList<>();

    @PostConstruct
    public void init() {
        List<RosBridgeConfig.RobotConnection> robots = config.getRobots();
        if (robots.isEmpty()) {
            log.warn("[RosBridgeManager] 연결할 로봇이 설정에 없습니다.");
            return;
        }

        for (RosBridgeConfig.RobotConnection robot : robots) {
            log.info("[RosBridgeManager] 로봇 연결 시작: id={}, uri={}", robot.getRobotId(), robot.getUri());
            RosBridgeClient client = new RosBridgeClient(
                    robot.getRobotId(),
                    robot.getUri(),
                    config.getReconnectDelayMs(),
                    robotStatusService
            );
            clients.add(client);
            client.start();
        }
    }

    @PreDestroy
    public void destroy() {
        clients.forEach(RosBridgeClient::stop);
    }
}
