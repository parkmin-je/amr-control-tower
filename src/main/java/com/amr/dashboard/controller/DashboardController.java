package com.amr.dashboard.controller;

import com.amr.dashboard.service.RobotRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final RobotRegistrationService robotRegistrationService;

    @GetMapping("/")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("robots", robotRegistrationService.findEnabled());
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("isAdmin", user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        model.addAttribute("isOperator", user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR") || a.getAuthority().equals("ROLE_ADMIN")));
        return "dashboard";
    }

    @GetMapping("/fleet")
    public String fleet(Model model, @AuthenticationPrincipal UserDetails user) {
        model.addAttribute("robots", robotRegistrationService.findEnabled());
        model.addAttribute("username", user != null ? user.getUsername() : "");
        return "fleet";
    }
}
