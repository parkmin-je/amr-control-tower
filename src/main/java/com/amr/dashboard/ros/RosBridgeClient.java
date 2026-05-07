package com.amr.dashboard.ros;

import com.amr.dashboard.service.RobotStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RosBridgeClient {

    private final String robotId;
    private final String uri;
    private final long reconnectDelayMs;
    private final RobotStatusService robotStatusService;
    private final String odomTopic;
    private final String batteryTopic;
    private final String mapTopic;
    private final String scanTopic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketClient wsClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RosBridgeClient(String robotId, String uri, long reconnectDelayMs,
                           RobotStatusService robotStatusService,
                           String odomTopic, String batteryTopic, String mapTopic, String scanTopic) {
        this.robotId = robotId;
        this.uri = uri;
        this.reconnectDelayMs = reconnectDelayMs;
        this.robotStatusService = robotStatusService;
        this.odomTopic = odomTopic;
        this.batteryTopic = batteryTopic;
        this.mapTopic = mapTopic;
        this.scanTopic = scanTopic;
    }

    public String getRobotId() {
        return robotId;
    }

    public void start() {
        connect();
    }

    public void publish(String topic, String type, String msgJson) {
        send(String.format(
                "{\"op\":\"publish\",\"topic\":\"%s\",\"type\":\"%s\",\"msg\":%s}",
                topic, type, msgJson));
    }

    private void connect() {
        try {
            wsClient = new WebSocketClient(new URI(uri)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("[rosbridge][{}] 연결 성공: {}", robotId, uri);
                    subscribeTopics();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("[rosbridge][{}] 연결 종료 (code={}, reason={}), {}ms 후 재연결 시도",
                            robotId, code, reason, reconnectDelayMs);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("[rosbridge][{}] 오류 발생: {}", robotId, ex.getMessage());
                }
            };
            wsClient.connectBlocking(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[rosbridge][{}] 초기 연결 실패, 재연결 예약", robotId);
            scheduleReconnect();
        }
    }

    private void subscribeTopics() {
        send(String.format(
                "{\"op\":\"subscribe\",\"topic\":\"%s\",\"type\":\"nav_msgs/Odometry\"}",
                odomTopic));
        send(String.format(
                "{\"op\":\"subscribe\",\"topic\":\"%s\",\"type\":\"sensor_msgs/BatteryState\"}",
                batteryTopic));
        if (mapTopic != null) {
            send(String.format(
                    "{\"op\":\"subscribe\",\"topic\":\"%s\",\"type\":\"nav_msgs/OccupancyGrid\"}",
                    mapTopic));
        }
        if (scanTopic != null) {
            // throttle_rate: 200ms → 최대 5Hz로 제한
            send(String.format(
                    "{\"op\":\"subscribe\",\"topic\":\"%s\",\"type\":\"sensor_msgs/LaserScan\",\"throttle_rate\":200}",
                    scanTopic));
        }
        log.info("[rosbridge][{}] 토픽 구독 완료 (odom={}, battery={}, map={}, scan={})", robotId, odomTopic, batteryTopic, mapTopic, scanTopic);
    }

    private void handleMessage(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String topic = root.path("topic").asText();
            JsonNode msg = root.path("msg");

            if (topic.equals(odomTopic)) {
                robotStatusService.onOdom(robotId, msg);
            } else if (topic.equals(batteryTopic)) {
                robotStatusService.onBattery(robotId, msg);
            } else if (mapTopic != null && topic.equals(mapTopic)) {
                robotStatusService.onMap(robotId, msg);
            } else if (scanTopic != null && topic.equals(scanTopic)) {
                robotStatusService.onScan(robotId, msg);
            }
        } catch (Exception e) {
            log.debug("[rosbridge][{}] 메시지 파싱 오류: {}", robotId, e.getMessage());
        }
    }

    private void send(String json) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(json);
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(this::connect, reconnectDelayMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
