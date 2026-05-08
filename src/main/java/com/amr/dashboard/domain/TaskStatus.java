package com.amr.dashboard.domain;

public enum TaskStatus {
    QUEUED,     // 대기 중
    EXECUTING,  // 실행 중
    PAUSED,     // 일시 정지
    COMPLETED,  // 완료
    FAILED,     // 실패
    CANCELLED   // 취소
}
