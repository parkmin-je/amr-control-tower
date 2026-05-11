package com.amr.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRobotPermissionRepository
        extends JpaRepository<UserRobotPermission, UserRobotPermission.Pk> {

    List<UserRobotPermission> findByUserId(Long userId);

    boolean existsByUserIdAndRobotId(Long userId, String robotId);

    void deleteByUserIdAndRobotId(Long userId, String robotId);
}
