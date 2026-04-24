package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.domain.RobotEventRepository;
import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import com.amr.dashboard.kafka.RobotStatusProducer;
import com.amr.dashboard.kafka.dto.RobotEventDto;
import com.amr.dashboard.kafka.dto.RobotStatusDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotStatusService {

    private final Optional<RobotStatusProducer> producer;
    private final RobotStatusRepository statusRepository;
    private final RobotEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // 최신 상태를 메모리에 캐싱
    private final Map<String, RobotStatusCache> cache = new ConcurrentHashMap<>();

    // /odom 메시지 처리
    public void onOdom(String robotId, JsonNode msg) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);

        JsonNode pose = msg.path("pose").path("pose");
        JsonNode twist = msg.path("twist").path("twist");

        current.posX = pose.path("position").path("x").asDouble();
        current.posY = pose.path("position").path("y").asDouble();
        current.linearVel = twist.path("linear").path("x").asDouble();
        current.angularVel = twist.path("angular").path("z").asDouble();

        publishStatus(robotId, current);
    }

    // /battery_state 메시지 처리
    public void onBattery(String robotId, JsonNode msg) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);

        double percentage = msg.path("percentage").asDouble(1.0);
        current.battery = (int) (percentage * 100);

        // 배터리 20% 이하 이벤트
        if (current.battery <= 20 && !current.lowBatteryAlerted) {
            publishEvent(robotId, RobotEvent.EventType.LOW_BATTERY, "배터리 부족: " + current.battery + "%");
            current.lowBatteryAlerted = true;
        } else if (current.battery > 20) {
            current.lowBatteryAlerted = false;
        }

        publishStatus(robotId, current);
    }

    // Kafka가 있으면 Kafka로, 없으면(dev) 직접 WebSocket 푸시
    private void publishStatus(String robotId, RobotStatusCache c) {
        RobotStatusDto dto = RobotStatusDto.builder()
                .robotId(robotId)
                .timestamp(LocalDateTime.now().toString())
                .posX(c.posX)
                .posY(c.posY)
                .linearVel(c.linearVel)
                .angularVel(c.angularVel)
                .battery(c.battery)
                .build();

        if (producer.isPresent()) {
            producer.get().sendStatus(dto);
        } else {
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/status", dto);
        }
    }

    public void publishEvent(String robotId, RobotEvent.EventType type, String message) {
        RobotEventDto dto = RobotEventDto.builder()
                .robotId(robotId)
                .timestamp(LocalDateTime.now().toString())
                .eventType(type.name())
                .message(message)
                .build();

        if (producer.isPresent()) {
            producer.get().sendEvent(dto);
        } else {
            saveEventDirect(robotId, type, message);
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/event", dto);
        }
    }

    // dev 환경 전용: Kafka 없이 직접 DB 저장
    @Scheduled(fixedRateString = "${robot.status-save-interval-ms}")
    @Transactional
    public void saveStatusPeriodically() {
        if (producer.isPresent()) return; // prod에선 Consumer가 저장
        cache.forEach((robotId, c) -> statusRepository.save(RobotStatus.builder()
                .robotId(robotId)
                .recordedAt(LocalDateTime.now())
                .posX(c.posX)
                .posY(c.posY)
                .linearVel(c.linearVel)
                .angularVel(c.angularVel)
                .battery(c.battery)
                .build()));
    }

    @Transactional
    public void saveEventDirect(String robotId, RobotEvent.EventType type, String message) {
        eventRepository.save(RobotEvent.builder()
                .robotId(robotId)
                .occurredAt(LocalDateTime.now())
                .eventType(type)
                .message(message)
                .build());
        log.info("[Event] robotId={}, type={}, message={}", robotId, type, message);
    }

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
