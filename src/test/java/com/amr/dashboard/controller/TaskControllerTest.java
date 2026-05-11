package com.amr.dashboard.controller;

import com.amr.dashboard.config.SecurityConfig;
import com.amr.dashboard.domain.Task;
import com.amr.dashboard.domain.TaskStatus;
import com.amr.dashboard.domain.TaskType;
import com.amr.dashboard.service.RobotRegistrationService;
import com.amr.dashboard.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
@DisplayName("TaskController MockMvc 테스트")
class TaskControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TaskService taskService;

    @MockBean
    RobotRegistrationService robotRegistrationService;

    private Task buildTask(Long id, String robotId, TaskStatus status) {
        return Task.builder()
                .robotId(robotId)
                .title("Test Task")
                .type(TaskType.NAVIGATE)
                .priority(1)
                .createdBy("operator")
                .build();
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("GET /api/tasks — 전체 태스크 목록 반환")
    void listTasks_returnsAll() throws Exception {
        when(taskService.getAllTasks()).thenReturn(List.of(buildTask(1L, "robot-01", TaskStatus.QUEUED)));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].robotId").value("robot-01"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("GET /api/tasks?robotId=robot-01 — 로봇별 태스크 반환")
    void listTasks_byRobotId() throws Exception {
        when(taskService.getTasksByRobot("robot-01"))
                .thenReturn(List.of(buildTask(1L, "robot-01", TaskStatus.QUEUED)));

        mockMvc.perform(get("/api/tasks").param("robotId", "robot-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].robotId").value("robot-01"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("POST /api/tasks — OPERATOR 태스크 생성 성공")
    void createTask_operator_ok() throws Exception {
        Task created = buildTask(1L, "robot-01", TaskStatus.QUEUED);
        when(taskService.createTask(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(created);

        var body = Map.of(
                "robotId", "robot-01",
                "title", "Test Task",
                "type", "NAVIGATE",
                "targetX", 1.0,
                "targetY", 2.0,
                "targetTheta", 0.0,
                "description", "desc",
                "priority", 1
        );

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.robotId").value("robot-01"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/tasks — VIEWER 태스크 생성 거부 — 403")
    void createTask_viewer_forbidden() throws Exception {
        var body = Map.of("robotId", "robot-01", "title", "Test", "type", "NAVIGATION",
                "priority", 1);

        mockMvc.perform(post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("POST /api/tasks/{id}/execute — 태스크 실행 성공")
    void executeTask_ok() throws Exception {
        Task task = buildTask(1L, "robot-01", TaskStatus.QUEUED);
        when(taskService.executeTask(1L)).thenReturn(task);

        mockMvc.perform(post("/api/tasks/1/execute").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("POST /api/tasks/{id}/cancel — 태스크 취소 성공")
    void cancelTask_ok() throws Exception {
        Task task = buildTask(1L, "robot-01", TaskStatus.CANCELLED);
        when(taskService.cancelTask(1L)).thenReturn(task);

        mockMvc.perform(post("/api/tasks/1/cancel").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/tasks/{id} — ADMIN 태스크 삭제 성공")
    void deleteTask_admin_ok() throws Exception {
        Task task = buildTask(1L, "robot-01", TaskStatus.CANCELLED);
        when(taskService.cancelTask(1L)).thenReturn(task);

        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("cancelled"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("DELETE /api/tasks/{id} — OPERATOR 삭제 거부 — 403")
    void deleteTask_operator_forbidden() throws Exception {
        mockMvc.perform(delete("/api/tasks/1").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
