package com.amr.dashboard.service;

import com.amr.dashboard.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RobotCommandService robotCommandService;
    private final RobotStatusService robotStatusService;

    @Transactional
    public Task createTask(String robotId, String title, TaskType type,
                           Double targetX, Double targetY, Double targetTheta,
                           String description, Integer priority, String createdBy) {
        Task task = Task.builder()
                .robotId(robotId)
                .title(title)
                .type(type)
                .targetX(targetX)
                .targetY(targetY)
                .targetTheta(targetTheta)
                .description(description)
                .priority(priority)
                .createdBy(createdBy)
                .build();
        Task saved = taskRepository.save(task);
        log.info("[TaskService] 태스크 생성: id={}, robot={}, type={}", saved.getId(), robotId, type);
        return saved;
    }

    @Transactional
    public Task executeTask(Long taskId) {
        Task task = findById(taskId);
        if (task.getStatus() != TaskStatus.QUEUED && task.getStatus() != TaskStatus.PAUSED) {
            throw new IllegalStateException("실행 불가 상태: " + task.getStatus());
        }
        task.start();
        taskRepository.save(task);

        if (task.getType() == TaskType.NAVIGATE && task.getTargetX() != null) {
            robotCommandService.sendNavigationGoal(
                    task.getRobotId(),
                    task.getTargetX(),
                    task.getTargetY(),
                    task.getTargetTheta() != null ? task.getTargetTheta() : 0.0
            );
        }

        robotStatusService.publishEvent(task.getRobotId(), RobotEvent.EventType.INFO,
                "태스크 시작: " + task.getTitle());
        log.info("[TaskService] 태스크 실행: id={}, robot={}", taskId, task.getRobotId());
        return task;
    }

    @Transactional
    public Task completeTask(Long taskId) {
        Task task = findById(taskId);
        task.complete();
        taskRepository.save(task);
        robotStatusService.publishEvent(task.getRobotId(), RobotEvent.EventType.INFO,
                "태스크 완료: " + task.getTitle());
        return task;
    }

    @Transactional
    public Task failTask(Long taskId, String reason) {
        Task task = findById(taskId);
        task.fail(reason);
        taskRepository.save(task);
        robotStatusService.publishEvent(task.getRobotId(), RobotEvent.EventType.ERROR,
                "태스크 실패: " + task.getTitle() + " — " + reason);
        return task;
    }

    @Transactional
    public Task cancelTask(Long taskId) {
        Task task = findById(taskId);
        task.cancel();
        taskRepository.save(task);
        robotStatusService.publishEvent(task.getRobotId(), RobotEvent.EventType.INFO,
                "태스크 취소: " + task.getTitle());
        return task;
    }

    @Transactional
    public Task pauseTask(Long taskId) {
        Task task = findById(taskId);
        task.pause();
        taskRepository.save(task);
        return task;
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByRobot(String robotId) {
        return taskRepository.findByRobotIdOrderByCreatedAtDesc(robotId);
    }

    @Transactional(readOnly = true)
    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatusOrderByPriorityAscCreatedAtAsc(status);
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Task findById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("태스크를 찾을 수 없음: " + taskId));
    }
}
