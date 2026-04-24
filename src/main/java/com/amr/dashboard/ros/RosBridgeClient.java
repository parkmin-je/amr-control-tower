package com.amr.dashboard.ros;

import com.amr.dashboard.config.RosBridgeConfig;
import com.amr.dashboard.service.RobotStatusService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RosBridgeClient {

    private final RosBridgeConfig config;
    private final RobotStatusService robotStatusService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebSocketClient wsClient;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        connect();
    }

    private void connect() {
        try {
            wsClient = new WebSocketClient(new URI(config.getUri())) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("[rosbridge] 연결 성공: {}", config.getUri());
                    subscribeTopics();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("[rosbridge] 연결 종료 (code={}, reason={}), {}ms 후 재연결 시도",
                            code, reason, config.getReconnectDelayMs());
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("[rosbridge] 오류 발생: {}", ex.getMessage());
                }
            };
            wsClient.connectBlocking(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[rosbridge] 초기 연결 실패 (ROS 미실행 상태로 간주), 재연결 예약");
            scheduleReconnect();
        }
    }

    private void subscribeTopics() {
        // /odom - 위치, 속도
        send("""
                {"op":"subscribe","topic":"/odom","type":"nav_msgs/Odometry"}
                """);

        // /battery_state - 배터리
        send("""
                {"op":"subscribe","topic":"/battery_state","type":"sensor_msgs/BatteryState"}
                """);

        log.info("[rosbridge] 토픽 구독 완료 (/odom, /battery_state)");
    }

    private void handleMessage(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String topic = root.path("topic").asText();
            JsonNode msg = root.path("msg");

            switch (topic) {
                case "/odom" -> robotStatusService.onOdom(msg);
                case "/battery_state" -> robotStatusService.onBattery(msg);
                default -> { /* 무시 */ }
            }
        } catch (Exception e) {
            log.debug("[rosbridge] 메시지 파싱 오류: {}", e.getMessage());
        }
    }

    private void send(String json) {
        if (wsClient != null && wsClient.isOpen()) {
            wsClient.send(json.strip());
        }
    }

    private void scheduleReconnect() {
        scheduler.schedule(this::connect, config.getReconnectDelayMs(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdownNow();
        if (wsClient != null) {
            wsClient.close();
        }
    }
}
