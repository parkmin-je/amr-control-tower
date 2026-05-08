package com.amr.dashboard.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Task 도메인 상태 전환 테스트")
class TaskDomainTest {

    private Task buildTask(TaskType type) {
        return Task.builder()
                .robotId("robot-01")
                .title("테스트 태스크")
                .type(type)
                .priority(2)
                .createdBy("operator")
                .build();
    }

    @Test
    @DisplayName("Task 생성 시 기본 상태는 QUEUED")
    void create_defaultStatus_isQueued() {
        Task task = buildTask(TaskType.DOCK);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.QUEUED);
    }

    @Test
    @DisplayName("priority 미지정 시 기본값 3")
    void create_defaultPriority_is3() {
        Task task = Task.builder()
                .robotId("robot-01")
                .title("우선순위 기본값 테스트")
                .type(TaskType.CHARGE)
                .createdBy("admin")
                .build();
        assertThat(task.getPriority()).isEqualTo(3);
    }

    @Test
    @DisplayName("start() 호출 시 EXECUTING으로 전환되고 startedAt이 설정됨")
    void start_setsExecutingAndStartedAt() {
        Task task = buildTask(TaskType.NAVIGATE);

        task.start();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.EXECUTING);
        assertThat(task.getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("complete() 호출 시 COMPLETED로 전환되고 completedAt이 설정됨")
    void complete_setsCompletedAndCompletedAt() {
        Task task = buildTask(TaskType.NAVIGATE);
        task.start();

        task.complete();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("fail() 호출 시 FAILED로 전환되고 errorMessage가 저장됨")
    void fail_setsFailedWithErrorMessage() {
        Task task = buildTask(TaskType.INSPECT);
        task.start();

        task.fail("센서 오류");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).isEqualTo("센서 오류");
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel() 호출 시 CANCELLED로 전환되고 completedAt이 설정됨")
    void cancel_setsCancelledAndCompletedAt() {
        Task task = buildTask(TaskType.DOCK);

        task.cancel();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("pause() 호출 시 PAUSED로 전환됨")
    void pause_setsPaused() {
        Task task = buildTask(TaskType.NAVIGATE);
        task.start();

        task.pause();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PAUSED);
    }

    @Test
    @DisplayName("createdAt은 생성 시점에 자동 설정됨")
    void create_createdAtIsSet() {
        Task task = buildTask(TaskType.CHARGE);
        assertThat(task.getCreatedAt()).isNotNull();
    }
}
