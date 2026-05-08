package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.ros.RosBridgeManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotCommandService {

    private static final String CB_NAME = "robot-command";

    private final RosBridgeManager rosBridgeManager;
    private final RobotStatusService robotStatusService;
    private final RobotMetricsService metricsService;

    /** 긴급 정지: /cmd_vel 에 zero twist 발행 */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "stopFallback")
    public void sendEmergencyStop(String robotId) {
        String zeroTwist = "{\"linear\":{\"x\":0,\"y\":0,\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":0}}";
        rosBridgeManager.publishToRobot(robotId, "/cmd_vel", "geometry_msgs/Twist", zeroTwist);
        robotStatusService.setEmergencyStop(robotId);
        metricsService.recordCommand(robotId, "ESTOP");
        log.info("[Command][{}] 긴급 정지 명령 전송", robotId);
    }

    private void stopFallback(String robotId, Exception e) {
        log.error("[CircuitBreaker][{}] E-Stop 전송 불가 (Circuit Open): {}", robotId, e.getMessage());
        robotStatusService.publishEvent(robotId, RobotEvent.EventType.ERROR,
                "E-Stop 전송 불가 (rosbridge 연결 오류): " + e.getMessage());
    }

    /** /cmd_vel 직접 속도 제어 */
    public void sendVelocity(String robotId, double linear, double angular) {
        String twist = String.format(
                "{\"linear\":{\"x\":%.4f,\"y\":0.0,\"z\":0.0},\"angular\":{\"x\":0.0,\"y\":0.0,\"z\":%.4f}}",
                linear, angular);
        rosBridgeManager.publishToRobot(robotId, "/cmd_vel", "geometry_msgs/Twist", twist);
    }

    /** 긴급 정지 해제 */
    public void clearEmergencyStop(String robotId) {
        robotStatusService.clearEmergencyStop(robotId);
        log.info("[Command][{}] 긴급 정지 해제", robotId);
    }

    /**
     * Nav2 목표 지점 전송: /move_base_simple/goal (PoseStamped)
     * theta: yaw (rad)
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "goalFallback")
    public void sendNavigationGoal(String robotId, double x, double y, double theta) {
        double sinH = Math.sin(theta / 2.0);
        double cosH = Math.cos(theta / 2.0);
        String poseStamped = String.format(
                "{\"header\":{\"frame_id\":\"map\"}," +
                "\"pose\":{\"position\":{\"x\":%.4f,\"y\":%.4f,\"z\":0}," +
                "\"orientation\":{\"x\":0,\"y\":0,\"z\":%.6f,\"w\":%.6f}}}",
                x, y, sinH, cosH);
        rosBridgeManager.publishToRobot(robotId, "/move_base_simple/goal",
                "geometry_msgs/PoseStamped", poseStamped);
        metricsService.recordCommand(robotId, "NAV_GOAL");
        log.info("[Command][{}] 내비게이션 목표 전송: x={}, y={}, theta={}", robotId, x, y, theta);
    }

    private void goalFallback(String robotId, double x, double y, double theta, Exception e) {
        log.error("[CircuitBreaker][{}] 목표 전송 불가 (Circuit Open): {}", robotId, e.getMessage());
        robotStatusService.publishEvent(robotId, RobotEvent.EventType.ERROR,
                "목표 전송 불가 (rosbridge 연결 오류): " + e.getMessage());
    }
}
