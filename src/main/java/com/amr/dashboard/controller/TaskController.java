package com.amr.dashboard.controller;

import com.amr.dashboard.domain.Task;
import com.amr.dashboard.domain.TaskStatus;
import com.amr.dashboard.domain.TaskType;
import com.amr.dashboard.service.RobotRegistrationService;
import com.amr.dashboard.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final RobotRegistrationService robotRegistrationService;

    @GetMapping("/tasks")
    public String tasksPage(Model model) {
        model.addAttribute("tasks", taskService.getAllTasks());
        model.addAttribute("robots", robotRegistrationService.findEnabled());
        model.addAttribute("taskTypes", TaskType.values());
        return "tasks";
    }

    // REST API
    @GetMapping("/api/tasks")
    @ResponseBody
    public List<Task> listTasks(@RequestParam(required = false) String robotId,
                                @RequestParam(required = false) TaskStatus status) {
        if (robotId != null) return taskService.getTasksByRobot(robotId);
        if (status != null) return taskService.getTasksByStatus(status);
        return taskService.getAllTasks();
    }

    @PostMapping("/api/tasks")
    @ResponseBody
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public Task createTask(@RequestBody CreateTaskRequest req,
                           @AuthenticationPrincipal UserDetails user) {
        return taskService.createTask(
                req.robotId(), req.title(), req.type(),
                req.targetX(), req.targetY(), req.targetTheta(),
                req.description(), req.priority(), user.getUsername()
        );
    }

    @PostMapping("/api/tasks/{id}/execute")
    @ResponseBody
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public Task executeTask(@PathVariable Long id) {
        return taskService.executeTask(id);
    }

    @PostMapping("/api/tasks/{id}/complete")
    @ResponseBody
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public Task completeTask(@PathVariable Long id) {
        return taskService.completeTask(id);
    }

    @PostMapping("/api/tasks/{id}/cancel")
    @ResponseBody
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public Task cancelTask(@PathVariable Long id) {
        return taskService.cancelTask(id);
    }

    @PostMapping("/api/tasks/{id}/pause")
    @ResponseBody
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public Task pauseTask(@PathVariable Long id) {
        return taskService.pauseTask(id);
    }

    @DeleteMapping("/api/tasks/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteTask(@PathVariable Long id) {
        taskService.cancelTask(id);
        return ResponseEntity.ok(Map.of("status", "cancelled"));
    }

    record CreateTaskRequest(
            String robotId, String title, TaskType type,
            Double targetX, Double targetY, Double targetTheta,
            String description, Integer priority
    ) {}
}
