package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RobotStatsService {

    private final RobotStatusRepository statusRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStats(String robotId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<RobotStatus> statuses = statusRepository.findTodayStatuses(robotId, todayStart);

        double totalDistance = calcTotalDistance(statuses);
        long activeSeconds = statuses.size(); // 1초 주기 저장이므로 = 가동 초 수

        return Map.of(
                "robotId", robotId,
                "totalDistanceM", Math.round(totalDistance * 100.0) / 100.0,
                "activeSeconds", activeSeconds,
                "recordCount", statuses.size()
        );
    }

    @Transactional(readOnly = true)
    public List<RobotStatus> getHistory(String robotId, LocalDateTime from, LocalDateTime to) {
        return statusRepository.findByRobotIdAndRecordedAtBetweenOrderByRecordedAtAsc(robotId, from, to);
    }

    // 연속 좌표 간 유클리드 거리 합산
    private double calcTotalDistance(List<RobotStatus> statuses) {
        double total = 0;
        for (int i = 1; i < statuses.size(); i++) {
            RobotStatus prev = statuses.get(i - 1);
            RobotStatus curr = statuses.get(i);
            double dx = curr.getPosX() - prev.getPosX();
            double dy = curr.getPosY() - prev.getPosY();
            total += Math.sqrt(dx * dx + dy * dy);
        }
        return total;
    }
}
