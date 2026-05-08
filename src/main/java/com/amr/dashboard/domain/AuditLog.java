package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_log_username_time", columnList = "username, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 500)
    private String detail;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @Builder
    public AuditLog(String username, String action, String detail,
                    String ipAddress, LocalDateTime createdAt,
                    boolean success, String errorMessage) {
        this.username = username;
        this.action = action;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.success = success;
        this.errorMessage = errorMessage;
    }
}
