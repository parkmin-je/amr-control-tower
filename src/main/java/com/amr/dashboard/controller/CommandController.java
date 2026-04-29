package com.amr.dashboard.controller;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.service.RobotCommandService;
import com.amr.dashboard.service.RobotStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/robot")
@RequiredArgsConstructor
public class CommandController {

    private final RobotCommandService commandService;
    private final RobotStatusService robotStatusService;

    /** 긴급 정지 */
    @PostMapping("/{robotId}/command/estop")
    public ResponseEntity<Void> emergencyStop(@PathVariable String robotId) {
        commandService.sendEmergencyStop(robotId);
        robotStatusService.publishEvent(robotId, RobotEvent.EventType.STOPPED, "긴급 정지 명령 수신");
        return ResponseEntity.ok().build();
    }

    /** 긴급 정지 해제 */
    @PostMapping("/{robotId}/command/estop/clear")
    public ResponseEntity<Void> clearEmergencyStop(@PathVariable String robotId) {
        commandService.clearEmergencyStop(robotId);
        return ResponseEntity.ok().build();
    }

    /** 내비게이션 목표 전송 */
    @PostMapping("/{robotId}/command/goal")
    public ResponseEntity<Void> sendGoal(@PathVariable String robotId,
                                          @RequestBody NavGoalRequest req) {
        commandService.sendNavigationGoal(robotId, req.x(), req.y(), req.theta());
        return ResponseEntity.ok().build();
    }

    record NavGoalRequest(double x, double y, double theta) {}
}
