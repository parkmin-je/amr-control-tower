package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "robot_registration")
@Getter
@Setter
@NoArgsConstructor
public class RobotRegistration {

    @Id
    @Column(length = 50)
    private String robotId;

    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false, length = 200)
    private String rosbridgeUri;

    @Column(length = 50)
    private String odomTopic = "/odom";

    @Column(length = 50)
    private String batteryTopic = "/battery_state";

    @Column(length = 50)
    private String mapTopic = "/map";

    @Column(length = 50)
    private String scanTopic = "/scan";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    public void prePersist() {
        if (registeredAt == null) registeredAt = LocalDateTime.now();
    }
}
