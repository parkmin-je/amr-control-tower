package com.amr.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RobotRegistrationRepository extends JpaRepository<RobotRegistration, String> {
    List<RobotRegistration> findByEnabledTrue();
}
