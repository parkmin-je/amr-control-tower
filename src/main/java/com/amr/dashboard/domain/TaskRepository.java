package com.amr.dashboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByRobotIdOrderByCreatedAtDesc(String robotId);

    List<Task> findByStatusOrderByPriorityAscCreatedAtAsc(TaskStatus status);

    List<Task> findByRobotIdAndStatusOrderByPriorityAscCreatedAtAsc(String robotId, TaskStatus status);

    Optional<Task> findFirstByRobotIdAndStatusOrderByPriorityAscCreatedAtAsc(
            String robotId, TaskStatus status);

    List<Task> findTop10ByRobotIdOrderByCreatedAtDesc(String robotId);
}
