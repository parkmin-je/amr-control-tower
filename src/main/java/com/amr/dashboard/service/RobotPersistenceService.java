package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.domain.RobotEventRepository;
import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import com.amr.dashboard.kafka.dto.RobotEventDto;
import com.amr.dashboard.kafka.dto.RobotStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotPersistenceService {

    private final RobotStatusRepository statusRepository;
    private final RobotEventRepository eventRepository;

    @Transactional
    public void saveStatus(RobotStatusDto dto) {
        statusRepository.save(RobotStatus.builder()
                .robotId(dto.getRobotId())
                .recordedAt(LocalDateTime.now())
                .posX(dto.getPosX())
                .posY(dto.getPosY())
                .linearVel(dto.getLinearVel())
                .angularVel(dto.getAngularVel())
                .battery(dto.getBattery())
                .build());
    }

    @Transactional
    public void saveEvent(RobotEventDto dto) {
        eventRepository.save(RobotEvent.builder()
                .robotId(dto.getRobotId())
                .occurredAt(LocalDateTime.now())
                .eventType(RobotEvent.EventType.valueOf(dto.getEventType()))
                .message(dto.getMessage())
                .build());
        log.info("[Event] robotId={}, type={}", dto.getRobotId(), dto.getEventType());
    }
}
