package com.amr.dashboard.ros;

import com.amr.dashboard.config.RosBridgeConfig;
import com.amr.dashboard.domain.RobotRegistration;
import com.amr.dashboard.service.RobotStatusService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RosBridgeManager {

    private final RosBridgeConfig config;
    private final RobotStatusService robotStatusService;
    private final Map<String, RosBridgeClient> clients = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (config.getRobots().isEmpty()) {
            log.warn("[RosBridgeManager] 연결할 로봇이 설정에 없습니다.");
            return;
        }

        for (RosBridgeConfig.RobotConnection robot : config.getRobots()) {
            log.info("[RosBridgeManager] 로봇 연결 시작: id={}, uri={}", robot.getRobotId(), robot.getUri());
            RosBridgeClient client = new RosBridgeClient(
                    robot.getRobotId(),
                    robot.getUri(),
                    config.getReconnectDelayMs(),
                    robotStatusService,
                    robot.getOdomTopic(),
                    robot.getBatteryTopic(),
                    robot.getMapTopic(),
                    robot.getScanTopic()
            );
            clients.put(robot.getRobotId(), client);
            client.start();
        }
    }

    public void publishToRobot(String robotId, String topic, String type, String msgJson) {
        RosBridgeClient client = clients.get(robotId);
        if (client != null) {
            client.publish(topic, type, msgJson);
        } else {
            log.warn("[RosBridgeManager] 알 수 없는 로봇 ID: {}", robotId);
        }
    }

    public void connectRobot(RobotRegistration reg) {
        if (clients.containsKey(reg.getRobotId())) return;
        log.info("[RosBridgeManager] 동적 로봇 연결: id={}, uri={}", reg.getRobotId(), reg.getRosbridgeUri());
        RosBridgeClient client = new RosBridgeClient(
                reg.getRobotId(),
                reg.getRosbridgeUri(),
                config.getReconnectDelayMs(),
                robotStatusService,
                reg.getOdomTopic(),
                reg.getBatteryTopic(),
                reg.getMapTopic(),
                reg.getScanTopic()
        );
        clients.put(reg.getRobotId(), client);
        client.start();
    }

    public void disconnectRobot(String robotId) {
        RosBridgeClient client = clients.remove(robotId);
        if (client != null) {
            client.stop();
            log.info("[RosBridgeManager] 로봇 연결 해제: id={}", robotId);
        }
    }

    @PreDestroy
    public void destroy() {
        clients.values().forEach(RosBridgeClient::stop);
    }
}
