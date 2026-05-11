package com.amr.dashboard.service;

import com.amr.dashboard.domain.Role;
import com.amr.dashboard.domain.User;
import com.amr.dashboard.domain.UserRepository;
import com.amr.dashboard.domain.UserRobotPermission;
import com.amr.dashboard.domain.UserRobotPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 로봇별 사용자 권한 서비스.
 * - ADMIN: 모든 로봇 제어 가능
 * - OPERATOR: 담당 로봇이 없으면 전체 제어 가능 (기존 호환), 담당 로봇이 있으면 해당 로봇만 제어 가능
 */
@Service
@RequiredArgsConstructor
public class RobotPermissionService {

    private final UserRepository userRepository;
    private final UserRobotPermissionRepository permissionRepository;

    /** username이 robotId를 제어할 수 있는지 확인. 권한 없으면 403 예외 */
    public void assertCanControl(String username, String robotId) {
        if (canControl(username, robotId)) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "로봇 [" + robotId + "] 제어 권한이 없습니다");
    }

    @Transactional(readOnly = true)
    public boolean canControl(String username, String robotId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.getRole() == Role.ADMIN) return true;

        List<UserRobotPermission> perms = permissionRepository.findByUserId(user.getId());
        if (perms.isEmpty()) return true;   // 담당 로봇 미지정 → 전체 제어 허용
        return perms.stream().anyMatch(p -> p.getRobotId().equals(robotId));
    }

    @Transactional(readOnly = true)
    public List<UserRobotPermission> findByUser(Long userId) {
        return permissionRepository.findByUserId(userId);
    }

    @Transactional
    public void assign(Long userId, String robotId) {
        if (!permissionRepository.existsByUserIdAndRobotId(userId, robotId)) {
            permissionRepository.save(new UserRobotPermission(userId, robotId));
        }
    }

    @Transactional
    public void unassign(Long userId, String robotId) {
        permissionRepository.deleteByUserIdAndRobotId(userId, robotId);
    }
}
