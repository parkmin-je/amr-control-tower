package com.amr.dashboard.service;

import com.amr.dashboard.ros.RosBridgeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotCommandService {

    private final RosBridgeManager rosBridgeManager;
    private final RobotStatusService robotStatusService;

    /** 긴급 정지: /cmd_vel 에 zero twist 발행 */
    public void sendEmergencyStop(String robotId) {
        String zeroTwist = "{\"linear\":{\"x\":0,\"y\":0,\"z\":0},\"angular\":{\"x\":0,\"y\":0,\"z\":0}}";
        rosBridgeManager.publishToRobot(robotId, "/cmd_vel", "geometry_msgs/Twist", zeroTwist);
        robotStatusService.setEmergencyStop(robotId);
        log.info("[Command][{}] 긴급 정지 명령 전송", robotId);
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
        robotStatusService.clearEmergencyStop(robotId);
        log.info("[Command][{}] 내비게이션 목표 전송: x={}, y={}, theta={}", robotId, x, y, theta);
    }
}
