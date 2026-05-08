package com.amr.dashboard.service;

import com.amr.dashboard.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService 단위 테스트")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RobotCommandService robotCommandService;

    @Mock
    private RobotStatusService robotStatusService;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .robotId("robot-01")
                .title("목표 이동")
                .type(TaskType.NAVIGATE)
                .targetX(1.0)
                .targetY(2.0)
                .targetTheta(0.0)
                .priority(1)
                .createdBy("operator")
                .build();
    }

    @Test
    @DisplayName("createTask — 필드가 올바르게 저장되고 반환됨")
    void createTask_savesAndReturns() {
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        Task result = taskService.createTask(
                "robot-01", "목표 이동", TaskType.NAVIGATE,
                1.0, 2.0, 0.0, null, 1, "operator");

        assertThat(result.getRobotId()).isEqualTo("robot-01");
        assertThat(result.getTitle()).isEqualTo("목표 이동");
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("executeTask — QUEUED 상태에서 EXECUTING으로 전환됨")
    void executeTask_fromQueued_becomesExecuting() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.executeTask(1L);

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.EXECUTING);
        verify(taskRepository).save(sampleTask);
    }

    @Test
    @DisplayName("executeTask — PAUSED 상태에서 EXECUTING으로 전환됨")
    void executeTask_fromPaused_becomesExecuting() {
        sampleTask.pause();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.executeTask(1L);

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.EXECUTING);
    }

    @Test
    @DisplayName("executeTask — 이미 EXECUTING 상태이면 IllegalStateException 발생")
    void executeTask_alreadyExecuting_throwsException() {
        sampleTask.start();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        assertThatThrownBy(() -> taskService.executeTask(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("실행 불가 상태");
    }

    @Test
    @DisplayName("executeTask — NAVIGATE 타입이면 sendNavigationGoal이 호출됨")
    void executeTask_navigateType_callsSendNavigationGoal() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.executeTask(1L);

        verify(robotCommandService).sendNavigationGoal(eq("robot-01"), eq(1.0), eq(2.0), eq(0.0));
    }

    @Test
    @DisplayName("executeTask — DOCK 타입이면 sendNavigationGoal이 호출되지 않음")
    void executeTask_dockType_doesNotCallNavigationGoal() {
        Task dockTask = Task.builder()
                .robotId("robot-01").title("도킹").type(TaskType.DOCK)
                .priority(2).createdBy("operator").build();
        when(taskRepository.findById(2L)).thenReturn(Optional.of(dockTask));
        when(taskRepository.save(any())).thenReturn(dockTask);

        taskService.executeTask(2L);

        verify(robotCommandService, never()).sendNavigationGoal(anyString(), anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("completeTask — COMPLETED 상태로 전환되고 이벤트 발행됨")
    void completeTask_setsCompletedAndPublishesEvent() {
        sampleTask.start();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.completeTask(1L);

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        verify(robotStatusService).publishEvent(eq("robot-01"), eq(RobotEvent.EventType.GOAL_REACHED), anyString());
    }

    @Test
    @DisplayName("failTask — FAILED 상태로 전환되고 errorMessage가 전달됨")
    void failTask_setsFailedWithReason() {
        sampleTask.start();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.failTask(1L, "통신 오류");

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(sampleTask.getErrorMessage()).isEqualTo("통신 오류");
        verify(robotStatusService).publishEvent(eq("robot-01"), eq(RobotEvent.EventType.ERROR), anyString());
    }

    @Test
    @DisplayName("cancelTask — CANCELLED 상태로 전환되고 이벤트 발행됨")
    void cancelTask_setsCancelledAndPublishesEvent() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.cancelTask(1L);

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.CANCELLED);
        verify(robotStatusService).publishEvent(eq("robot-01"), eq(RobotEvent.EventType.STOPPED), anyString());
    }

    @Test
    @DisplayName("pauseTask — PAUSED 상태로 전환됨")
    void pauseTask_setsPaused() {
        sampleTask.start();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any())).thenReturn(sampleTask);

        taskService.pauseTask(1L);

        assertThat(sampleTask.getStatus()).isEqualTo(TaskStatus.PAUSED);
    }

    @Test
    @DisplayName("findById — 존재하지 않는 ID이면 IllegalArgumentException 발생")
    void findById_notFound_throwsException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("태스크를 찾을 수 없음");
    }

    @Test
    @DisplayName("getAllTasks — 전체 태스크 목록 반환")
    void getAllTasks_returnsList() {
        when(taskRepository.findAll()).thenReturn(List.of(sampleTask));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getTasksByRobot — 로봇 ID로 필터링된 목록 반환")
    void getTasksByRobot_returnsFilteredList() {
        when(taskRepository.findByRobotIdOrderByCreatedAtDesc("robot-01"))
                .thenReturn(List.of(sampleTask));

        List<Task> result = taskService.getTasksByRobot("robot-01");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRobotId()).isEqualTo("robot-01");
    }
}
