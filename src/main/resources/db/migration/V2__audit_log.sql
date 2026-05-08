-- AMR Control Tower — Flyway V2
-- 감사 로그 테이블 추가

CREATE TABLE audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)   NOT NULL,
    action        VARCHAR(100)  NOT NULL,
    detail        VARCHAR(500),
    ip_address    VARCHAR(50),
    created_at    DATETIME(6)   NOT NULL,
    success       BOOLEAN       NOT NULL DEFAULT TRUE,
    error_message VARCHAR(255)
);

CREATE INDEX idx_audit_log_username_time ON audit_log (username, created_at);
