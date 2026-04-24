package com.amr.dashboard.kafka;

import com.amr.dashboard.kafka.dto.RobotEventDto;
import com.amr.dashboard.kafka.dto.RobotStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
public class RobotStatusProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.robot-status}")
    private String statusTopic;

    @Value("${kafka.topics.robot-event}")
    private String eventTopic;

    public void sendStatus(RobotStatusDto dto) {
        kafkaTemplate.send(statusTopic, dto.getRobotId(), dto);
        log.debug("[Kafka] status 전송: robotId={}", dto.getRobotId());
    }

    public void sendEvent(RobotEventDto dto) {
        kafkaTemplate.send(eventTopic, dto.getRobotId(), dto);
        log.debug("[Kafka] event 전송: robotId={}, type={}", dto.getRobotId(), dto.getEventType());
    }
}
