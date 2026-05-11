package com.amr.dashboard.controller;

import com.amr.dashboard.domain.RobotRegistrationRepository;
import com.amr.dashboard.service.RobotCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final RobotCommandService commandService;
    private final RobotRegistrationRepository registrationRepository;

    /** 전체 로봇 일괄 긴급 정지 */
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    @PostMapping("/command/estop")
    public ResponseEntity<Map<String, Object>> emergencyStopAll() {
        List<String> robotIds = registrationRepository.findByEnabledTrue()
                .stream()
                .map(r -> r.getRobotId())
                .toList();
        commandService.sendEmergencyStopAll(robotIds);
        return ResponseEntity.ok(Map.of("stopped", robotIds.size(), "robots", robotIds));
    }
}
