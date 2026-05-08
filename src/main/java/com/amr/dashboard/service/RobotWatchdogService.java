package com.amr.dashboard.service;

import com.amr.dashboard.ros.RosBridgeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 안전 Watchdog 서비스
 * - WASD 수동 드라이브: 브라우저/네트워크 단절 시 1.5초 후 자동 정지
 * - 로봇 오프라인 감지: 5초 이상 메시지 없으면 OFFLINE 상태 전환
 *
 * RobotStatusService ↔ RosBridgeManager 순환 의존성을 피하기 위해 별도 서비스로 분리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotWatchdogService {

    private static final String ZERO_TWIST =
            "{\"linear\":{\"x\":0,\"y\":0,\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":0}}";

    private final RosBridgeManager rosBridgeManager;
    private final RobotStatusService robotStatusService;

    /**
     * WASD Watchdog — 500ms 마다 실행
     * 마지막 수동 명령 후 1.5초 경과 시 zero velocity 전송하여 로봇 정지
     */
    @Scheduled(fixedRate = 500)
    public void manualDriveWatchdog() {
        Instant threshold = Instant.now().minusMillis(1500);
        for (String robotId : robotStatusService.getManualDriveExpiredRobots(threshold)) {
            robotStatusService.onManualDriveTimeout(robotId);
            rosBridgeManager.publishToRobot(robotId, "/cmd_vel", "geometry_msgs/Twist", ZERO_TWIST);
            log.warn("[Watchdog][{}] 수동 드라이브 타임아웃 — 강제 정지", robotId);
        }
    }

    /**
     * 오프라인 감지 — 3초 마다 실행
     * 마지막 메시지 수신 후 5초 경과 시 OFFLINE 상태 전환 및 이벤트 발행
     */
    @Scheduled(fixedRate = 3000)
    public void offlineDetection() {
        Instant threshold = Instant.now().minusSeconds(5);
        for (String robotId : robotStatusService.getOfflineDetectedRobots(threshold)) {
            robotStatusService.markOffline(robotId);
            log.warn("[Offline][{}] 로봇 오프라인 감지", robotId);
        }
    }
}
