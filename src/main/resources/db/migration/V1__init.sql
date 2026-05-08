-- AMR Control Tower — 초기 스키마
-- Flyway V1: 전체 테이블 생성
-- 이 파일이 실행된 후 application-prod.yml의 ddl-auto: validate 가 적용됨

-- ── 사용자 ─────────────────────────────────────────────────────────
CREATE TABLE users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   DATETIME(6)  NOT NULL
);

-- ── 로봇 등록 ──────────────────────────────────────────────────────
CREATE TABLE robot_registration (
    robot_id       VARCHAR(50)  PRIMARY KEY,
    display_name   VARCHAR(100) NOT NULL,
    rosbridge_uri  VARCHAR(200) NOT NULL,
    odom_topic     VARCHAR(50)  DEFAULT '/odom',
    battery_topic  VARCHAR(50)  DEFAULT '/battery_state',
    map_topic      VARCHAR(50)  DEFAULT '/map',
    scan_topic     VARCHAR(50)  DEFAULT '/scan',
    enabled        BOOLEAN      NOT NULL DEFAULT TRUE,
    registered_at  DATETIME(6)  NOT NULL
);

-- ── 태스크 ─────────────────────────────────────────────────────────
CREATE TABLE task (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id      VARCHAR(50)  NOT NULL,
    title         VARCHAR(100) NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    target_x      DOUBLE,
    target_y      DOUBLE,
    target_theta  DOUBLE,
    description   VARCHAR(255),
    priority      INT          NOT NULL DEFAULT 3,
    created_by    VARCHAR(50)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    started_at    DATETIME(6),
    completed_at  DATETIME(6),
    error_message VARCHAR(255)
);

CREATE INDEX idx_task_robot_status ON task (robot_id, status);

-- ── 로봇 상태 이력 ────────────────────────────────────────────────
CREATE TABLE robot_status (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id     VARCHAR(50) NOT NULL,
    recorded_at  DATETIME(6) NOT NULL,
    pos_x        DOUBLE,
    pos_y        DOUBLE,
    linear_vel   DOUBLE,
    angular_vel  DOUBLE,
    battery      INT
);

CREATE INDEX idx_robot_status_robot_id_time ON robot_status (robot_id, recorded_at);

-- ── 로봇 이벤트 로그 ──────────────────────────────────────────────
CREATE TABLE robot_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    robot_id    VARCHAR(50) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    event_type  VARCHAR(30) NOT NULL,
    message     TEXT,
    ack_status  BOOLEAN     NOT NULL DEFAULT FALSE,
    acked_at    DATETIME(6)
);

CREATE INDEX idx_robot_event_robot_id_time ON robot_event (robot_id, occurred_at);
