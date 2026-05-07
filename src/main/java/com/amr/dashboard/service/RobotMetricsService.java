package com.amr.dashboard.service;

import com.amr.dashboard.config.RosBridgeConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AMR 전용 커스텀 Micrometer 메트릭.
 * Prometheus가 /actuator/prometheus 를 스크랩하면 아래 지표를 수집합니다.
 *
 *  amr_robot_battery{robot_id}         — 배터리 잔량 (%)
 *  amr_robot_event_total{robot_id, event_type} — 이벤트 발생 횟수
 *  amr_robot_command_total{robot_id, command_type} — 명령 전송 횟수
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RobotMetricsService {

    private final MeterRegistry meterRegistry;
    private final RosBridgeConfig rosBridgeConfig;

    // 배터리 게이지 (로봇별)
    private final Map<String, AtomicInteger> batteryGauges = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        rosBridgeConfig.getRobots().forEach(robot -> registerBatteryGauge(robot.getRobotId(), 100));
        log.info("[Metrics] AMR 메트릭 초기화 완료: {} 대 등록", rosBridgeConfig.getRobots().size());
    }

    /** 배터리 잔량 업데이트 */
    public void updateBattery(String robotId, int battery) {
        batteryGauges.computeIfAbsent(robotId, id -> registerBatteryGauge(id, battery)).set(battery);
    }

    /** 이벤트 발생 카운터 증가 */
    public void recordEvent(String robotId, String eventType) {
        Counter.builder("amr.robot.event")
                .tag("robot_id", robotId)
                .tag("event_type", eventType)
                .description("AMR 이벤트 발생 횟수")
                .register(meterRegistry)
                .increment();
    }

    /** 명령 전송 카운터 증가 */
    public void recordCommand(String robotId, String commandType) {
        Counter.builder("amr.robot.command")
                .tag("robot_id", robotId)
                .tag("command_type", commandType)
                .description("AMR 로봇 명령 전송 횟수")
                .register(meterRegistry)
                .increment();
    }

    private AtomicInteger registerBatteryGauge(String robotId, int initialValue) {
        AtomicInteger gauge = new AtomicInteger(initialValue);
        Gauge.builder("amr.robot.battery", gauge, AtomicInteger::get)
                .tag("robot_id", robotId)
                .description("AMR 로봇 배터리 잔량 (%)")
                .register(meterRegistry);
        return gauge;
    }
}
