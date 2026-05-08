package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotRegistration;
import com.amr.dashboard.domain.RobotRegistrationRepository;
import com.amr.dashboard.ros.RosBridgeManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotRegistrationService {

    private final RobotRegistrationRepository repository;
    private final RosBridgeManager rosBridgeManager;

    @Transactional(readOnly = true)
    public List<RobotRegistration> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RobotRegistration> findEnabled() {
        return repository.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public RobotRegistration findById(String robotId) {
        return repository.findById(robotId)
                .orElseThrow(() -> new IllegalArgumentException("로봇을 찾을 수 없음: " + robotId));
    }

    @Transactional
    public RobotRegistration register(RobotRegistration reg) {
        if (repository.existsById(reg.getRobotId())) {
            throw new IllegalArgumentException("이미 등록된 로봇 ID: " + reg.getRobotId());
        }
        RobotRegistration saved = repository.save(reg);
        rosBridgeManager.connectRobot(saved);
        log.info("[RobotRegistration] 등록 완료: {}", saved.getRobotId());
        return saved;
    }

    @Transactional
    public void deregister(String robotId) {
        RobotRegistration reg = findById(robotId);
        reg.setEnabled(false);
        repository.save(reg);
        rosBridgeManager.disconnectRobot(robotId);
        log.info("[RobotRegistration] 비활성화: {}", robotId);
    }

    @Transactional
    public RobotRegistration update(String robotId, RobotRegistration patch) {
        RobotRegistration reg = findById(robotId);
        if (patch.getDisplayName() != null) reg.setDisplayName(patch.getDisplayName());
        if (patch.getRosbridgeUri() != null) reg.setRosbridgeUri(patch.getRosbridgeUri());
        if (patch.getOdomTopic() != null) reg.setOdomTopic(patch.getOdomTopic());
        if (patch.getBatteryTopic() != null) reg.setBatteryTopic(patch.getBatteryTopic());
        if (patch.getMapTopic() != null) reg.setMapTopic(patch.getMapTopic());
        if (patch.getScanTopic() != null) reg.setScanTopic(patch.getScanTopic());
        return repository.save(reg);
    }
}
