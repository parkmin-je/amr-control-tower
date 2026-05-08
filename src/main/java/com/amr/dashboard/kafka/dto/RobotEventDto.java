package com.amr.dashboard.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotEventDto {
    private Long id;
    private String robotId;
    private String timestamp;
    private String eventType;
    private String message;
}
