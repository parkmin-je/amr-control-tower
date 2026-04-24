package com.amr.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotEventRepository extends JpaRepository<RobotEvent, Long> {

    List<RobotEvent> findTop20ByRobotIdOrderByOccurredAtDesc(String robotId);

    long countByRobotIdAndEventType(String robotId, RobotEvent.EventType eventType);
}
