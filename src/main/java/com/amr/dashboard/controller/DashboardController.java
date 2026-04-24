package com.amr.dashboard.controller;

import com.amr.dashboard.config.RosBridgeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final RosBridgeConfig rosBridgeConfig;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("robots", rosBridgeConfig.getRobots());
        return "dashboard";
    }
}
