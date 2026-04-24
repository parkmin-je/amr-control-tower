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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketClient wsClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RosBridgeClient(String robotId, String uri, long reconnectDelayMs,
                           RobotStatusService robotStatusService) {
        this.robotId = robotId;
        this.uri = uri;
        this.reconnectDelayMs = reconnectDelayMs;
        this.robotStatusService = robotStatusService;
    }

    public void start() {
        connect();
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
                "{\"op\":\"subscribe\",\"topic\":\"/%s/odom\",\"type\":\"nav_msgs/Odometry\"}",
                robotId));
        send(String.format(
                "{\"op\":\"subscribe\",\"topic\":\"/%s/battery_state\",\"type\":\"sensor_msgs/BatteryState\"}",
                robotId));
        log.info("[rosbridge][{}] 토픽 구독 완료", robotId);
    }

    private void handleMessage(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String topic = root.path("topic").asText();
            JsonNode msg = root.path("msg");

            if (topic.endsWith("/odom")) {
                robotStatusService.onOdom(robotId, msg);
            } else if (topic.endsWith("/battery_state")) {
                robotStatusService.onBattery(robotId, msg);
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
