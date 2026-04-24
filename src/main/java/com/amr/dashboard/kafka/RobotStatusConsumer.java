package com.amr.dashboard.kafka;

import com.amr.dashboard.kafka.dto.RobotEventDto;
import com.amr.dashboard.kafka.dto.RobotStatusDto;
import com.amr.dashboard.service.RobotPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@RequiredArgsConstructor
public class RobotStatusConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RobotPersistenceService persistenceService;

    @KafkaListener(topics = "${kafka.topics.robot-status}", groupId = "amr-dashboard")
    public void consumeStatus(RobotStatusDto dto) {
        log.debug("[Kafka] status 수신: robotId={}", dto.getRobotId());
        messagingTemplate.convertAndSend("/topic/robot/" + dto.getRobotId() + "/status", dto);
        persistenceService.saveStatus(dto);
    }

    @KafkaListener(topics = "${kafka.topics.robot-event}", groupId = "amr-dashboard")
    public void consumeEvent(RobotEventDto dto) {
        log.debug("[Kafka] event 수신: robotId={}, type={}", dto.getRobotId(), dto.getEventType());
        messagingTemplate.convertAndSend("/topic/robot/" + dto.getRobotId() + "/event", dto);
        persistenceService.saveEvent(dto);
    }
}
