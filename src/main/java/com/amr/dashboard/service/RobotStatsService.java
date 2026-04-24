package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RobotStatsService {

    private final RobotStatusRepository statusRepository;
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MM-dd");

    @Transactional(readOnly = true)
    public Map<String, Object> getTodayStats(String robotId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<RobotStatus> statuses = statusRepository.findTodayStatuses(robotId, todayStart);

        double totalDistance = calcTotalDistance(statuses);
        long activeSeconds = statuses.size();

        return Map.of(
                "robotId", robotId,
                "totalDistanceM", Math.round(totalDistance * 100.0) / 100.0,
                "activeSeconds", activeSeconds,
                "recordCount", statuses.size()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklyStats(String robotId) {
        return buildDailyStats(robotId, 7);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlyStats(String robotId) {
        return buildDailyStats(robotId, 30);
    }

    @Transactional(readOnly = true)
    public List<RobotStatus> getHistory(String robotId, LocalDateTime from, LocalDateTime to) {
        return statusRepository.findByRobotIdAndRecordedAtBetweenOrderByRecordedAtAsc(robotId, from, to);
    }

    // days일 동안의 일별 주행 거리 통계
    private Map<String, Object> buildDailyStats(String robotId, int days) {
        List<String> labels = new ArrayList<>();
        List<Double> distances = new ArrayList<>();

        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to = date.plusDays(1).atStartOfDay();

            List<RobotStatus> statuses =
                    statusRepository.findByRobotIdAndRecordedAtBetweenOrderByRecordedAtAsc(robotId, from, to);
            double dist = Math.round(calcTotalDistance(statuses) * 100.0) / 100.0;

            labels.add(date.format(LABEL_FMT));
            distances.add(dist);
        }

        return Map.of(
                "robotId", robotId,
                "labels", labels,
                "distances", distances
        );
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
