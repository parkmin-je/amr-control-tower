package com.amr.dashboard.service;

import com.amr.dashboard.config.RosBridgeConfig;
import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.domain.RobotEventRepository;
import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotStatusService {

    private final RosBridgeConfig rosBridgeConfig;
    private final RobotStatusRepository statusRepository;
    private final RobotEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 최신 상태를 메모리에 캐싱 (DB 저장은 1초 주기 스케줄러에서)
    private final Map<String, RobotStatusCache> cache = new ConcurrentHashMap<>();

    // /odom 메시지 처리
    public void onOdom(JsonNode msg) {
        String robotId = rosBridgeConfig.getRobotId();
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);

        JsonNode pose = msg.path("pose").path("pose");
        JsonNode twist = msg.path("twist").path("twist");

        current.posX = pose.path("position").path("x").asDouble();
        current.posY = pose.path("position").path("y").asDouble();
        current.linearVel = twist.path("linear").path("x").asDouble();
        current.angularVel = twist.path("angular").path("z").asDouble();

        pushToFront(robotId, current);
    }

    // /battery_state 메시지 처리
    public void onBattery(JsonNode msg) {
        String robotId = rosBridgeConfig.getRobotId();
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);

        double percentage = msg.path("percentage").asDouble(1.0);
        current.battery = (int) (percentage * 100);

        // 배터리 20% 이하 이벤트
        if (current.battery <= 20 && !current.lowBatteryAlerted) {
            saveEvent(robotId, RobotEvent.EventType.LOW_BATTERY,
                    "배터리 부족: " + current.battery + "%");
            current.lowBatteryAlerted = true;
        } else if (current.battery > 20) {
            current.lowBatteryAlerted = false;
        }

        pushToFront(robotId, current);
    }

    // WebSocket으로 프론트에 실시간 푸시
    private void pushToFront(String robotId, RobotStatusCache c) {
        Map<String, Object> payload = Map.of(
                "robotId", robotId,
                "timestamp", LocalDateTime.now().toString(),
                "posX", c.posX,
                "posY", c.posY,
                "linearVel", c.linearVel,
                "angularVel", c.angularVel,
                "battery", c.battery
        );
        messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/status", payload);
    }

    // 1초마다 DB 저장 (프론트 푸시와 분리)
    @Scheduled(fixedRateString = "${robot.status-save-interval-ms}")
    @Transactional
    public void saveStatusPeriodically() {
        cache.forEach((robotId, c) -> {
            RobotStatus status = RobotStatus.builder()
                    .robotId(robotId)
                    .recordedAt(LocalDateTime.now())
                    .posX(c.posX)
                    .posY(c.posY)
                    .linearVel(c.linearVel)
                    .angularVel(c.angularVel)
                    .battery(c.battery)
                    .build();
            statusRepository.save(status);
        });
    }

    @Transactional
    public void saveEvent(String robotId, RobotEvent.EventType type, String message) {
        RobotEvent event = RobotEvent.builder()
                .robotId(robotId)
                .occurredAt(LocalDateTime.now())
                .eventType(type)
                .message(message)
                .build();
        eventRepository.save(event);
        log.info("[Event] robotId={}, type={}, message={}", robotId, type, message);

        // 이벤트도 프론트에 실시간 푸시
        Map<String, Object> payload = Map.of(
                "robotId", robotId,
                "timestamp", LocalDateTime.now().toString(),
                "eventType", type.name(),
                "message", message
        );
        messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/event", payload);
    }

    // 내부 캐시 객체
    private static class RobotStatusCache {
        String robotId;
        double posX, posY, linearVel, angularVel;
        int battery = 100;
        boolean lowBatteryAlerted = false;

        RobotStatusCache(String robotId) {
            this.robotId = robotId;
        }
    }
}
