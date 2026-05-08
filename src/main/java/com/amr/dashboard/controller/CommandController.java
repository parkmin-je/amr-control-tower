package com.amr.dashboard.controller;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.service.RobotCommandService;
import com.amr.dashboard.service.RobotStatusService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/robot")
@RequiredArgsConstructor
public class CommandController {

    private final RobotCommandService commandService;
    private final RobotStatusService robotStatusService;

    /** 긴급 정지 */
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{robotId}/command/estop")
    public ResponseEntity<Void> emergencyStop(@PathVariable String robotId) {
        commandService.sendEmergencyStop(robotId);
        robotStatusService.publishEvent(robotId, RobotEvent.EventType.STOPPED, "긴급 정지 명령 수신");
        return ResponseEntity.ok().build();
    }

    /** 긴급 정지 해제 */
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{robotId}/command/estop/clear")
    public ResponseEntity<Void> clearEmergencyStop(@PathVariable String robotId) {
        commandService.clearEmergencyStop(robotId);
        return ResponseEntity.ok().build();
    }

    /** 속도 직접 제어 (/cmd_vel) */
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{robotId}/command/velocity")
    public ResponseEntity<Void> sendVelocity(@PathVariable String robotId,
                                              @Valid @RequestBody VelocityRequest req) {
        log.info("[Command][{}] velocity: linear={}, angular={}", robotId, req.linear(), req.angular());
        commandService.sendVelocity(robotId, req.linear(), req.angular());
        return ResponseEntity.ok().build();
    }

    /** 내비게이션 목표 전송 */
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/{robotId}/command/goal")
    public ResponseEntity<Void> sendGoal(@PathVariable String robotId,
                                          @Valid @RequestBody NavGoalRequest req) {
        commandService.sendNavigationGoal(robotId, req.x(), req.y(), req.theta());
        return ResponseEntity.ok().build();
    }

    record VelocityRequest(
            @DecimalMin("-2.0") @DecimalMax("2.0") double linear,
            @DecimalMin("-3.0") @DecimalMax("3.0") double angular) {}

    record NavGoalRequest(
            @DecimalMin("-100.0") @DecimalMax("100.0") double x,
            @DecimalMin("-100.0") @DecimalMax("100.0") double y,
            double theta) {}
}
