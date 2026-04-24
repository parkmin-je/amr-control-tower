package com.amr.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RobotStatusRepository extends JpaRepository<RobotStatus, Long> {

    Optional<RobotStatus> findTopByRobotIdOrderByRecordedAtDesc(String robotId);

    List<RobotStatus> findByRobotIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            String robotId, LocalDateTime from, LocalDateTime to);

    // 오늘 총 주행 거리 계산용 (연속 좌표 간 거리 합산은 서비스에서 처리)
    @Query("SELECT s FROM RobotStatus s WHERE s.robotId = :robotId AND s.recordedAt >= :from ORDER BY s.recordedAt ASC")
    List<RobotStatus> findTodayStatuses(@Param("robotId") String robotId,
                                         @Param("from") LocalDateTime from);
}
