package com.amr.dashboard.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RobotStatusDto {
    private String robotId;
    private String timestamp;
    private double posX;
    private double posY;
    private double linearVel;
    private double angularVel;
    private int battery;
}
