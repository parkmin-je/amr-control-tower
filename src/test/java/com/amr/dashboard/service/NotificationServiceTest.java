package com.amr.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("NotificationService 단위 테스트")
class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
        // Slack webhook URL을 빈 문자열로 설정 — HTTP 호출 없이 스킵 경로만 테스트
        ReflectionTestUtils.setField(notificationService, "slackWebhookUrl", "");
    }

    @Test
    @DisplayName("sendSlack — webhook URL 미설정 시 예외 없이 조용히 종료됨")
    void sendSlack_blankUrl_noException() {
        assertThatCode(() -> notificationService.sendSlack("테스트 메시지"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("alertError — webhook URL 미설정 시 예외 없이 종료됨")
    void alertError_blankUrl_noException() {
        assertThatCode(() -> notificationService.alertError("robot-01", "센서 오류"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("alertLowBattery — webhook URL 미설정 시 예외 없이 종료됨")
    void alertLowBattery_blankUrl_noException() {
        assertThatCode(() -> notificationService.alertLowBattery("robot-01", 15.0))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("alertStopped — webhook URL 미설정 시 예외 없이 종료됨")
    void alertStopped_blankUrl_noException() {
        assertThatCode(() -> notificationService.alertStopped("robot-01"))
                .doesNotThrowAnyException();
    }
}
