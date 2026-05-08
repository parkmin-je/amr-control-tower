package com.amr.dashboard.domain;

public enum Role {
    VIEWER,    // 조회 전용
    OPERATOR,  // 제어 가능 (속도, 목표, E-Stop)
    ADMIN      // 전체 관리 (로봇 등록, 사용자 관리)
}
