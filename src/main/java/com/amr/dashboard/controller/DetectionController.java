package com.amr.dashboard.controller;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.service.RobotStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * YOLO 감지 노드(Python)에서 POST 요청을 수신하는 엔드포인트.
 * 호출 예시:
 *   POST /api/robot/robot-01/detection
 *   {"label":"person","confidence":0.92,"x":120,"y":80,"w":60,"h":140}
 */
@Slf4j
@RestController
@RequestMapping("/api/robot")
@RequiredArgsConstructor
public class DetectionController {

    private final RobotStatusService robotStatusService;

    @PostMapping("/{robotId}/detection")
    public ResponseEntity<Void> receiveDetection(@PathVariable String robotId,
                                                  @RequestBody DetectionRequest req) {
        String message = String.format("YOLO 감지: %s (신뢰도=%.1f%%, bbox=[%.0f,%.0f,%.0f,%.0f])",
                req.label(), req.confidence() * 100, req.x(), req.y(), req.w(), req.h());
        robotStatusService.publishEvent(robotId, RobotEvent.EventType.YOLO_DETECTED, message);
        log.info("[Detection][{}] {}", robotId, message);
        return ResponseEntity.ok().build();
    }

    record DetectionRequest(String label, double confidence, double x, double y, double w, double h) {}
}
