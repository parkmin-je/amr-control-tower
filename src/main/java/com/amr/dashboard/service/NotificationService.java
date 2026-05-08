package com.amr.dashboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

    @Value("${notification.slack.webhook-url:}")
    private String slackWebhookUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sendSlack(String message) {
        if (slackWebhookUrl == null || slackWebhookUrl.isBlank()) return;
        try {
            String body = objectMapper.writeValueAsString(Map.of("text", message));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(slackWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("[Notification] Slack 전송 실패: status={}", resp.statusCode());
            }
        } catch (Exception e) {
            log.error("[Notification] Slack 전송 오류: {}", e.getMessage());
        }
    }

    public void alertError(String robotId, String message) {
        sendSlack(String.format(":red_circle: *[ERROR]* Robot `%s` — %s", robotId, message));
    }

    public void alertLowBattery(String robotId, double level) {
        sendSlack(String.format(":battery: *[LOW BATTERY]* Robot `%s` — %.0f%%", robotId, level));
    }

    public void alertStopped(String robotId) {
        sendSlack(String.format(":octagonal_sign: *[E-STOP]* Robot `%s` 긴급 정지", robotId));
    }
}
