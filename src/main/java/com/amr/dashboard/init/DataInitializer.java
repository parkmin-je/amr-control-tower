package com.amr.dashboard.init;

import com.amr.dashboard.config.RosBridgeConfig;
import com.amr.dashboard.domain.Role;
import com.amr.dashboard.domain.RobotRegistration;
import com.amr.dashboard.domain.RobotRegistrationRepository;
import com.amr.dashboard.domain.UserRepository;
import com.amr.dashboard.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RobotRegistrationRepository robotRegistrationRepository;
    private final AuthService authService;
    private final RosBridgeConfig rosBridgeConfig;

    @Override
    public void run(String... args) {
        initUsers();
        initRobots();
    }

    private void initUsers() {
        if (userRepository.count() > 0) return;

        authService.createUser("admin",    "Admin1234",    Role.ADMIN);
        authService.createUser("operator", "Operator1234", Role.OPERATOR);
        authService.createUser("viewer",   "Viewer1234",   Role.VIEWER);

        log.info("[DataInitializer] 기본 사용자 3명 생성 완료 (admin/operator/viewer)");
    }

    private void initRobots() {
        if (robotRegistrationRepository.count() > 0) return;

        rosBridgeConfig.getRobots().forEach(r -> {
            RobotRegistration reg = new RobotRegistration();
            reg.setRobotId(r.getRobotId());
            reg.setDisplayName(r.getRobotId());
            reg.setRosbridgeUri(r.getUri());
            reg.setOdomTopic(r.getOdomTopic());
            reg.setBatteryTopic(r.getBatteryTopic());
            reg.setMapTopic(r.getMapTopic());
            reg.setScanTopic(r.getScanTopic());
            robotRegistrationRepository.save(reg);
        });

        log.info("[DataInitializer] YAML 로봇 {} 대 DB 시딩 완료", rosBridgeConfig.getRobots().size());
    }
}
