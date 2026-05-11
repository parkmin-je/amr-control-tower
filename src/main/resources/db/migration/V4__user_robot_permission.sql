-- AMR Control Tower — V4: 로봇별 사용자 권한 분리
-- Operator 계정에 담당 로봇을 지정. 지정이 없으면 전체 로봇 제어 가능.
CREATE TABLE user_robot_permission (
    user_id  BIGINT       NOT NULL,
    robot_id VARCHAR(50)  NOT NULL,
    PRIMARY KEY (user_id, robot_id),
    CONSTRAINT fk_urp_user  FOREIGN KEY (user_id)  REFERENCES users(id)               ON DELETE CASCADE,
    CONSTRAINT fk_urp_robot FOREIGN KEY (robot_id) REFERENCES robot_registration(robot_id) ON DELETE CASCADE
);

CREATE INDEX idx_urp_user_id ON user_robot_permission (user_id);
