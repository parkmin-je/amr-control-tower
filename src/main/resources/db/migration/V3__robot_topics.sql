-- 로봇별 커맨드 토픽 이름 설정 컬럼 추가
-- 현장마다 다른 ROS 토픽 이름을 Admin 패널에서 설정 가능하도록
ALTER TABLE robot_registration
    ADD COLUMN goal_topic    VARCHAR(100) NOT NULL DEFAULT '/goal_pose',
    ADD COLUMN cmd_vel_topic VARCHAR(100) NOT NULL DEFAULT '/cmd_vel';
