package com.amr.dashboard.controller;

import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.domain.RobotEventRepository;
import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import com.amr.dashboard.service.RobotStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/robot")
@RequiredArgsConstructor
public class RobotApiController {

    private final RobotStatusRepository statusRepository;
    private final RobotEventRepository eventRepository;
    private final RobotStatsService statsService;

    // 최신 상태 1건
    @GetMapping("/{robotId}/status")
    public ResponseEntity<RobotStatus> getLatestStatus(@PathVariable String robotId) {
        return statusRepository.findTopByRobotIdOrderByRecordedAtDesc(robotId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    // 오늘 통계
    @GetMapping("/{robotId}/stats/today")
    public ResponseEntity<Map<String, Object>> getTodayStats(@PathVariable String robotId) {
        return ResponseEntity.ok(statsService.getTodayStats(robotId));
    }

    // 기간별 이력
    @GetMapping("/{robotId}/history")
    public ResponseEntity<List<RobotStatus>> getHistory(
            @PathVariable String robotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(statsService.getHistory(robotId, from, to));
    }

    // 최근 이벤트 20건
    @GetMapping("/{robotId}/events")
    public ResponseEntity<List<RobotEvent>> getEvents(@PathVariable String robotId) {
        return ResponseEntity.ok(eventRepository.findTop20ByRobotIdOrderByOccurredAtDesc(robotId));
    }
}
