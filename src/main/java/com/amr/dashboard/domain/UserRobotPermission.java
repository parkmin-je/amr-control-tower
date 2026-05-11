package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "user_robot_permission")
@Getter
@NoArgsConstructor
@IdClass(UserRobotPermission.Pk.class)
public class UserRobotPermission {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "robot_id", length = 50)
    private String robotId;

    public UserRobotPermission(Long userId, String robotId) {
        this.userId = userId;
        this.robotId = robotId;
    }

    public static class Pk implements Serializable {
        private Long userId;
        private String robotId;

        public Pk() {}
        public Pk(Long userId, String robotId) {
            this.userId = userId;
            this.robotId = robotId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return java.util.Objects.equals(userId, pk.userId) && java.util.Objects.equals(robotId, pk.robotId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, robotId);
        }
    }
}
