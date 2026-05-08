package com.amr.dashboard.service;

import com.amr.dashboard.domain.NavResultEvent;
import com.amr.dashboard.domain.RobotEvent;
import com.amr.dashboard.domain.RobotEventRepository;
import com.amr.dashboard.domain.RobotState;
import com.amr.dashboard.domain.RobotStatus;
import com.amr.dashboard.domain.RobotStatusRepository;
import com.amr.dashboard.kafka.RobotStatusProducer;
import com.amr.dashboard.kafka.dto.RobotEventDto;
import com.amr.dashboard.kafka.dto.RobotStatusDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotStatusService {

    private final Optional<RobotStatusProducer> producer;
    private final RobotStatusRepository statusRepository;
    private final RobotEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MapService mapService;
    private final RobotMetricsService metricsService;
    private final NotificationService notificationService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // 최신 상태를 메모리에 캐싱
    private final Map<String, RobotStatusCache> cache = new ConcurrentHashMap<>();

    // /tf 메시지 처리 — map→odom 변환 추출
    public void onTf(String robotId, JsonNode msg) {
        JsonNode transforms = msg.path("transforms");
        for (JsonNode tf : transforms) {
            String parentFrame = tf.path("header").path("frame_id").asText();
            String childFrame = tf.path("child_frame_id").asText();
            if ("map".equals(parentFrame) && "odom".equals(childFrame)) {
                RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
                current.mapOdomTx = tf.path("transform").path("translation").path("x").asDouble();
                current.mapOdomTy = tf.path("transform").path("translation").path("y").asDouble();
                double qz = tf.path("transform").path("rotation").path("z").asDouble();
                double qw = tf.path("transform").path("rotation").path("w").asDouble(1.0);
                current.mapOdomYaw = Math.atan2(2.0 * qw * qz, 1.0 - 2.0 * qz * qz);
                break;
            }
        }
    }

    // /odom 메시지 처리
    public void onOdom(String robotId, JsonNode msg) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        current.lastMessageTime = Instant.now();
        if (current.state == RobotState.OFFLINE) {
            current.state = RobotState.IDLE;
            publishEvent(robotId, RobotEvent.EventType.STARTED, "로봇 온라인 복구");
        }

        JsonNode pose = msg.path("pose").path("pose");
        JsonNode twist = msg.path("twist").path("twist");

        double odomX = pose.path("position").path("x").asDouble();
        double odomY = pose.path("position").path("y").asDouble();
        current.linearVel = twist.path("linear").path("x").asDouble();
        current.angularVel = twist.path("angular").path("z").asDouble();

        // quaternion → yaw (2D: qx=qy=0)
        double qz = pose.path("orientation").path("z").asDouble();
        double qw = pose.path("orientation").path("w").asDouble(1.0);
        current.yaw = Math.atan2(2.0 * qw * qz, 1.0 - 2.0 * qz * qz);

        // odom 프레임 → map 프레임 변환 (map→odom TF 적용)
        double cos = Math.cos(current.mapOdomYaw);
        double sin = Math.sin(current.mapOdomYaw);
        current.posX = current.mapOdomTx + cos * odomX - sin * odomY;
        current.posY = current.mapOdomTy + sin * odomX + cos * odomY;

        if (!current.emergencyStopped) {
            if (Math.abs(current.linearVel) > 0.01 || Math.abs(current.angularVel) > 0.01) {
                current.state = RobotState.MOVING;
            } else {
                current.state = RobotState.IDLE;
            }
        }

        publishStatus(robotId, current);
    }

    // /scan 메시지 처리 (sensor_msgs/LaserScan)
    public void onScan(String robotId, JsonNode msg) {
        double angleMin = msg.path("angle_min").asDouble();
        double angleIncrement = msg.path("angle_increment").asDouble();
        JsonNode rangesNode = msg.path("ranges");

        // 3포인트마다 1개 샘플링 → 전송 데이터 1/3로 감소
        List<Double> ranges = new ArrayList<>();
        for (int i = 0; i < rangesNode.size(); i += 3) {
            double r = rangesNode.get(i).asDouble();
            ranges.add(Double.isFinite(r) ? r : -1.0);
        }

        messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/scan", Map.of(
                "angleMin", angleMin,
                "angleIncrement", angleIncrement * 3,
                "ranges", ranges
        ));
    }

    // /map 메시지 처리
    public void onMap(String robotId, JsonNode msg) {
        mapService.updateMap(robotId, msg);
        MapService.MapData data = mapService.getMapData(robotId);
        if (data != null) {
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/map", Map.of(
                    "width", data.width(),
                    "height", data.height(),
                    "resolution", data.resolution(),
                    "originX", data.originX(),
                    "originY", data.originY()
            ));
        }
    }

    // /battery_state 메시지 처리
    public void onBattery(String robotId, JsonNode msg) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        current.lastMessageTime = Instant.now();

        // percentage 필드: ROS 표준은 0.0~1.0, 일부 로봇은 0~100 또는 누락
        double percentage = msg.path("percentage").asDouble(-1.0);
        if (percentage < 0) {
            // percentage 없음 → voltage 기반 추정 (design_capacity 있을 때)
            double voltage = msg.path("voltage").asDouble(0);
            double capacity = msg.path("design_capacity").asDouble(0);
            percentage = (voltage > 0 && capacity > 0) ? (voltage / capacity) : 1.0;
        } else if (percentage > 1.0) {
            percentage = percentage / 100.0; // 이미 0~100 범위인 경우
        }
        percentage = Math.max(0, Math.min(1, percentage));
        current.battery = (int) (percentage * 100);
        metricsService.updateBattery(robotId, current.battery);

        // 배터리 20% 이하 이벤트
        if (current.battery <= 20 && !current.lowBatteryAlerted) {
            publishEvent(robotId, RobotEvent.EventType.LOW_BATTERY, "배터리 부족: " + current.battery + "%");
            notificationService.alertLowBattery(robotId, current.battery);
            current.lowBatteryAlerted = true;
        } else if (current.battery > 20) {
            current.lowBatteryAlerted = false;
        }

        publishStatus(robotId, current);
    }

    // Kafka가 있으면 Kafka로, 없으면(dev) 직접 WebSocket 푸시
    private void publishStatus(String robotId, RobotStatusCache c) {
        RobotStatusDto dto = RobotStatusDto.builder()
                .robotId(robotId)
                .timestamp(LocalDateTime.now().toString())
                .posX(c.posX)
                .posY(c.posY)
                .linearVel(c.linearVel)
                .angularVel(c.angularVel)
                .yaw(c.yaw)
                .battery(c.battery)
                .robotState(c.state.name())
                .build();

        if (producer.isPresent()) {
            producer.get().sendStatus(dto);
        } else {
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/status", dto);
        }
    }

    public void publishEvent(String robotId, RobotEvent.EventType type, String message) {
        RobotEventDto dto = RobotEventDto.builder()
                .robotId(robotId)
                .timestamp(LocalDateTime.now().toString())
                .eventType(type.name())
                .message(message)
                .build();

        metricsService.recordEvent(robotId, type.name());

        if (type == RobotEvent.EventType.ERROR) {
            notificationService.alertError(robotId, message);
        } else if (type == RobotEvent.EventType.STOPPED) {
            notificationService.alertStopped(robotId);
        }

        if (producer.isPresent()) {
            producer.get().sendEvent(dto);
        } else {
            RobotEvent saved = saveEventDirect(robotId, type, message);
            RobotEventDto dtoWithId = RobotEventDto.builder()
                    .id(saved.getId())
                    .robotId(dto.getRobotId())
                    .timestamp(dto.getTimestamp())
                    .eventType(dto.getEventType())
                    .message(dto.getMessage())
                    .build();
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/event", dtoWithId);
        }
    }

    /**
     * Nav2 액션 피드백 수신 (/navigate_to_pose/_action/feedback)
     * remaining_distance, estimated_time_remaining 추출 → WebSocket 푸시
     */
    public void onNavFeedback(String robotId, JsonNode msg) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        current.lastMessageTime = Instant.now();

        JsonNode feedback = msg.path("feedback");
        double remaining = feedback.path("remaining_distance").asDouble(-1);
        int recoveries = feedback.path("number_of_recoveries").asInt(0);
        double etaSec = feedback.path("estimated_time_remaining").path("sec").asDouble(0);

        if (remaining >= 0) {
            current.navRemainingDistance = remaining;
            current.navEtaSec = etaSec;
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/nav", Map.of(
                    "remainingDistance", Math.round(remaining * 100.0) / 100.0,
                    "etaSec", (int) etaSec,
                    "recoveries", recoveries
            ));
        }
    }

    /**
     * Nav2 액션 상태 수신 (/navigate_to_pose/_action/status)
     * status: 4=SUCCEEDED, 6=ABORTED → TaskService에 결과 전달
     */
    public void onNavStatus(String robotId, JsonNode msg) {
        JsonNode statusList = msg.path("status_list");
        if (!statusList.isArray() || statusList.isEmpty()) return;

        // 가장 최근 goal의 상태만 처리
        JsonNode latest = statusList.get(statusList.size() - 1);
        int status = latest.path("status").asInt(0);

        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);

        if (status == 4 && current.navStatus != 4) { // SUCCEEDED (중복 방지)
            current.navStatus = status;
            current.navRemainingDistance = 0;
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/nav",
                    Map.of("result", "SUCCEEDED", "remainingDistance", 0.0));
            eventPublisher.publishEvent(new NavResultEvent(this, robotId, 4));
            log.info("[Nav2][{}] 목표 도달 (SUCCEEDED)", robotId);
        } else if (status == 6 && current.navStatus != 6) { // ABORTED
            current.navStatus = status;
            messagingTemplate.convertAndSend("/topic/robot/" + robotId + "/nav",
                    Map.of("result", "ABORTED", "remainingDistance", -1.0));
            eventPublisher.publishEvent(new NavResultEvent(this, robotId, 6));
            log.warn("[Nav2][{}] 내비게이션 실패 (ABORTED)", robotId);
        } else if (status == 2) { // EXECUTING
            current.navStatus = status;
        }
    }

    /** 수동 드라이브 명령 수신 시 호출 — watchdog 타이머 갱신 */
    public void onManualDriveCmd(String robotId) {
        RobotStatusCache c = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        c.lastManualCmdTime = Instant.now();
        c.manualDriveActive = true;
    }

    /** 수동 드라이브 타임아웃 시 RobotWatchdogService에서 호출 */
    public void onManualDriveTimeout(String robotId) {
        RobotStatusCache c = cache.get(robotId);
        if (c != null) c.manualDriveActive = false;
    }

    /** watchdog이 체크할 수동 드라이브 만료 로봇 ID 목록 반환 */
    public List<String> getManualDriveExpiredRobots(Instant threshold) {
        List<String> expired = new ArrayList<>();
        cache.forEach((robotId, c) -> {
            if (c.manualDriveActive && c.lastManualCmdTime != null && c.lastManualCmdTime.isBefore(threshold))
                expired.add(robotId);
        });
        return expired;
    }

    /** offline 감지 대상 로봇 ID 목록 반환 */
    public List<String> getOfflineDetectedRobots(Instant threshold) {
        List<String> result = new ArrayList<>();
        cache.forEach((robotId, c) -> {
            if (c.lastMessageTime != null && c.lastMessageTime.isBefore(threshold)
                    && c.state != RobotState.OFFLINE)
                result.add(robotId);
        });
        return result;
    }

    /** RobotWatchdogService에서 호출 — OFFLINE 상태 전환 */
    public void markOffline(String robotId) {
        RobotStatusCache c = cache.get(robotId);
        if (c != null) {
            c.state = RobotState.OFFLINE;
            publishStatus(robotId, c);
            publishEvent(robotId, RobotEvent.EventType.ERROR, "로봇 통신 끊김 (5초 이상 응답 없음)");
        }
    }

    // dev 환경 전용: Kafka 없이 직접 DB 저장
    @Scheduled(fixedRateString = "${robot.status-save-interval-ms}")
    @Transactional
    public void saveStatusPeriodically() {
        if (producer.isPresent()) return; // prod에선 Consumer가 저장
        cache.forEach((robotId, c) -> statusRepository.save(RobotStatus.builder()
                .robotId(robotId)
                .recordedAt(LocalDateTime.now())
                .posX(c.posX)
                .posY(c.posY)
                .linearVel(c.linearVel)
                .angularVel(c.angularVel)
                .battery(c.battery)
                .build()));
    }

    @Transactional
    public RobotEvent saveEventDirect(String robotId, RobotEvent.EventType type, String message) {
        RobotEvent event = eventRepository.save(RobotEvent.builder()
                .robotId(robotId)
                .occurredAt(LocalDateTime.now())
                .eventType(type)
                .message(message)
                .build());
        log.info("[Event] robotId={}, type={}, message={}", robotId, type, message);
        return event;
    }

    public RobotStatusDto getCurrentStatus(String robotId) {
        RobotStatusCache c = cache.get(robotId);
        if (c == null) return null;
        return RobotStatusDto.builder()
                .robotId(robotId)
                .timestamp(LocalDateTime.now().toString())
                .posX(c.posX)
                .posY(c.posY)
                .linearVel(c.linearVel)
                .angularVel(c.angularVel)
                .battery(c.battery)
                .robotState(c.state.name())
                .build();
    }

    public void setEmergencyStop(String robotId) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        current.emergencyStopped = true;
        current.state = RobotState.EMERGENCY_STOP;
        publishStatus(robotId, current);
    }

    public void clearEmergencyStop(String robotId) {
        RobotStatusCache current = cache.computeIfAbsent(robotId, RobotStatusCache::new);
        current.emergencyStopped = false;
        current.state = RobotState.IDLE;
        publishStatus(robotId, current);
    }

    private static class RobotStatusCache {
        String robotId;
        double posX, posY, linearVel, angularVel, yaw;
        double mapOdomTx = 0, mapOdomTy = 0, mapOdomYaw = 0;
        int battery = 100;
        boolean lowBatteryAlerted = false;
        RobotState state = RobotState.IDLE;
        boolean emergencyStopped = false;
        // 오프라인 감지
        Instant lastMessageTime = null;
        // WASD watchdog
        Instant lastManualCmdTime = null;
        boolean manualDriveActive = false;
        // Nav2 피드백
        double navRemainingDistance = -1;
        double navEtaSec = 0;
        int navStatus = 0; // 0=없음, 2=실행중, 4=성공, 6=실패

        RobotStatusCache(String robotId) {
            this.robotId = robotId;
        }
    }
}
