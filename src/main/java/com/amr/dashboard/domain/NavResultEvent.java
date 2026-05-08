package com.amr.dashboard.domain;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Nav2 내비게이션 결과 이벤트
 * RobotStatusService → (ApplicationEventPublisher) → TaskService
 *
 * statusCode: 4=SUCCEEDED, 6=ABORTED
 */
@Getter
public class NavResultEvent extends ApplicationEvent {

    private final String robotId;
    private final int statusCode;

    public NavResultEvent(Object source, String robotId, int statusCode) {
        super(source);
        this.robotId = robotId;
        this.statusCode = statusCode;
    }
}
