package com.amr.dashboard.controller;

import com.amr.dashboard.config.SecurityConfig;
import com.amr.dashboard.service.RobotCommandService;
import com.amr.dashboard.service.RobotStatusService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommandController.class)
@Import(SecurityConfig.class)
@DisplayName("CommandController MockMvc 테스트")
class CommandControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    RobotCommandService commandService;

    @MockBean
    RobotStatusService robotStatusService;

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("OPERATOR: 긴급 정지 요청 성공 — 200 OK")
    void estop_operator_ok() throws Exception {
        mockMvc.perform(post("/api/robot/robot-01/command/estop")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(commandService).sendEmergencyStop("robot-01");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER: 긴급 정지 요청 거부 — 403 Forbidden")
    void estop_viewer_forbidden() throws Exception {
        mockMvc.perform(post("/api/robot/robot-01/command/estop")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN: 긴급 정지 해제 요청 성공 — 200 OK")
    void clearEstop_admin_ok() throws Exception {
        mockMvc.perform(post("/api/robot/robot-01/command/estop/clear")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(commandService).clearEmergencyStop("robot-01");
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("velocity: 유효한 값으로 요청 성공 — 200 OK")
    void velocity_validRequest_ok() throws Exception {
        var body = Map.of("linear", 0.5, "angular", 0.3);

        mockMvc.perform(post("/api/robot/robot-01/command/velocity")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(commandService).sendVelocity(eq("robot-01"), eq(0.5), eq(0.3));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("velocity: 범위 초과 값 요청 거부 — 400 Bad Request")
    void velocity_outOfRange_badRequest() throws Exception {
        var body = Map.of("linear", 99.0, "angular", 0.0);

        mockMvc.perform(post("/api/robot/robot-01/command/velocity")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    @DisplayName("goal: 유효한 좌표로 내비게이션 목표 전송 성공 — 200 OK")
    void goal_validRequest_ok() throws Exception {
        var body = Map.of("x", 1.0, "y", 2.0, "theta", 0.0);

        mockMvc.perform(post("/api/robot/robot-01/command/goal")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        verify(commandService).sendNavigationGoal(eq("robot-01"), eq(1.0), eq(2.0), eq(0.0));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("goal: VIEWER 권한으로 요청 거부 — 403 Forbidden")
    void goal_viewer_forbidden() throws Exception {
        var body = Map.of("x", 1.0, "y", 2.0, "theta", 0.0);

        mockMvc.perform(post("/api/robot/robot-01/command/goal")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
