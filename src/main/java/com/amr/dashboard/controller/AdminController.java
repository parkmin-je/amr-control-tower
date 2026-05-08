package com.amr.dashboard.controller;

import com.amr.dashboard.domain.Role;
import com.amr.dashboard.domain.RobotRegistration;
import com.amr.dashboard.domain.User;
import com.amr.dashboard.domain.UserRepository;
import com.amr.dashboard.service.AuthService;
import com.amr.dashboard.service.RobotRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RobotRegistrationService robotRegistrationService;

    @GetMapping("/admin")
    public String adminPage(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("robots", robotRegistrationService.findAll());
        model.addAttribute("roles", Role.values());
        return "admin";
    }

    // --- User Management ---

    @PostMapping("/api/admin/users")
    @ResponseBody
    public User createUser(@RequestBody CreateUserRequest req) {
        return authService.createUser(req.username(), req.password(), req.role());
    }

    @PatchMapping("/api/admin/users/{id}/role")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changeRole(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body) {
        authService.changeRole(id, Role.valueOf(body.get("role")));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PatchMapping("/api/admin/users/{id}/password")
    @ResponseBody
    public ResponseEntity<Map<String, String>> changePassword(@PathVariable Long id,
                                                              @RequestBody Map<String, String> body) {
        authService.changePassword(id, body.get("password"));
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PatchMapping("/api/admin/users/{id}/toggle")
    @ResponseBody
    public ResponseEntity<Map<String, String>> toggleEnabled(@PathVariable Long id) {
        authService.toggleEnabled(id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // --- Robot Management ---

    @GetMapping("/api/admin/robots")
    @ResponseBody
    public List<RobotRegistration> listRobots() {
        return robotRegistrationService.findAll();
    }

    @PostMapping("/api/admin/robots")
    @ResponseBody
    public RobotRegistration registerRobot(@RequestBody RobotRegistration reg) {
        return robotRegistrationService.register(reg);
    }

    @DeleteMapping("/api/admin/robots/{robotId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deregisterRobot(@PathVariable String robotId) {
        robotRegistrationService.deregister(robotId);
        return ResponseEntity.ok(Map.of("status", "disabled"));
    }

    record CreateUserRequest(String username, String password, Role role) {}
}
