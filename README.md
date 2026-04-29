# AMR Fleet 관제 대시보드

실시간으로 여러 대의 AMR(Autonomous Mobile Robot)을 모니터링하는 Fleet 관제 플랫폼입니다.
ROS2 rosbridge를 통해 로봇 상태를 수집하고, Kafka를 거쳐 WebSocket으로 대시보드에 실시간 반영합니다.

---

## 주요 기능

- **멀티 로봇 실시간 모니터링** — 로봇별 탭 전환으로 위치·속도·배터리 상태 확인
- **로봇 상태머신** — IDLE / MOVING / EMERGENCY_STOP / CHARGING / ERROR 상태 실시간 뱃지 표시
- **Fleet 전체 뷰** — 전체 로봇 현황을 한 화면에서 카드 그리드로 모니터링 (`/fleet`)
- **로봇 명령 전송** — 긴급 정지(E-Stop), 정지 해제, Nav2 목표 지점(x, y, theta) 전송
- **YOLO 감지 REST 연동** — Python YOLO 노드에서 감지 결과를 POST로 수신하여 이벤트 발행
- **이벤트 ACK** — 이벤트 확인 처리 (ackStatus, ackedAt 저장)
- **SLAM 맵 오버레이** — slam_toolbox OccupancyGrid를 실시간으로 렌더링하고 로봇 위치를 맵 위에 오버레이
- **2D 실시간 경로 맵** — Canvas 기반 주행 궤적 시각화 (맵 없을 시 그리드 폴백)
- **속도 추이 차트** — 최근 60초 선속도 실시간 라인 차트
- **주간/월간 주행 거리 통계** — 일별 주행 거리 바 차트
- **이벤트 로그** — 배터리 저하, 장애물 감지, YOLO 감지, 목표 도달 등 실시간 이벤트 표시
- **Kafka 이벤트 스트리밍** — 운영 환경에서 Kafka를 통한 데이터 파이프라인

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot 3.2.4, Spring WebSocket (STOMP), Spring Data JPA |
| Frontend | Thymeleaf, Chart.js 4.4, SockJS, STOMP.js |
| Database | H2 (dev), MySQL 8.0 (prod) |
| Message Broker | Apache Kafka 7.6 (Confluent) |
| ROS | ROS2 Humble, rosbridge_suite, slam_toolbox, TurtleBot3 Gazebo |
| DevOps | Docker, Docker Compose, Gradle 8.8 |

---

## 아키텍처

```
[TurtleBot3 / ROS2]  [YOLO Python 노드]
        |                    |
   rosbridge (WebSocket)     | POST /api/robot/{id}/detection
        |                    |
[RosBridgeClient] ---> [RobotStatusService] <--- [CommandController]
  subscribe: odom,            |  상태머신              ↑ E-Stop / Goal
  battery, map      +---------+---------+        [RobotCommandService]
  publish: cmd_vel, |                   |             |
  goal            [Kafka Producer]  [WebSocket Push]  | publish
                    |                (dev only)   [RosBridgeManager]
            [Kafka Consumer]
                    |
            [DB 저장 + WebSocket Push]
                    |
            [Spring WebSocket / STOMP]
                    |
       [브라우저 대시보드 / Fleet 뷰]
```

- **dev 프로파일**: H2 인메모리 DB, Kafka 비활성화, rosbridge → 직접 WebSocket 푸시
- **prod 프로파일**: MySQL, Kafka 활성화, rosbridge → Kafka → Consumer → DB + WebSocket 푸시

---

## 실행 방법

### 로컬 개발 (dev)

> MySQL, Kafka 없이 H2 인메모리 DB로 바로 실행

```bash
gradle bootRun
```

브라우저: `http://localhost:8080`

### Docker Compose (prod)

> MySQL + Kafka + Spring Boot 풀 스택 실행

```bash
docker compose up --build
```

브라우저: `http://localhost:8080`

**서비스 구성**

| 서비스 | 포트 |
|--------|------|
| amr-dashboard | 8080 |
| MySQL | 3306 |
| Kafka | 9092 |
| Zookeeper | 2181 |

---

## API 명세

### 조회

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/robot/list` | 등록된 로봇 ID 목록 |
| GET | `/api/robot/{id}/status` | 최신 상태 1건 (DB) |
| GET | `/api/robot/{id}/status/live` | 실시간 인메모리 상태 (robotState 포함) |
| GET | `/api/robot/{id}/stats/today` | 오늘 주행 거리 · 가동 시간 |
| GET | `/api/robot/{id}/stats/weekly` | 최근 7일 일별 주행 거리 |
| GET | `/api/robot/{id}/stats/monthly` | 최근 30일 일별 주행 거리 |
| GET | `/api/robot/{id}/history` | 기간별 상태 이력 (`from`, `to` 파라미터) |
| GET | `/api/robot/{id}/events` | 최근 이벤트 20건 |
| GET | `/api/robot/{id}/map` | 최신 SLAM 맵 (PNG 이미지) |
| GET | `/api/robot/{id}/map/info` | 맵 메타데이터 (width, height, resolution, origin) |

### 제어

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/robot/{id}/command/estop` | 긴급 정지 (`cmd_vel` zero twist 발행) |
| POST | `/api/robot/{id}/command/estop/clear` | 긴급 정지 해제 |
| POST | `/api/robot/{id}/command/goal` | Nav2 목표 전송 (`{"x":0,"y":0,"theta":0}`) |
| POST | `/api/robot/{id}/detection` | YOLO 감지 결과 수신 (`{"label","confidence","x","y","w","h"}`) |
| POST | `/api/robot/{id}/events/{eventId}/ack` | 이벤트 확인(ACK) 처리 |

---

## WebSocket 토픽

| Topic | 설명 |
|-------|------|
| `/topic/robot/{id}/status` | 실시간 위치·속도·배터리 상태 |
| `/topic/robot/{id}/event` | 이벤트 발생 알림 |
| `/topic/robot/{id}/map` | SLAM 맵 업데이트 알림 (메타데이터) |

---

## 프로젝트 구조

```
src/main/java/com/amr/dashboard/
├── config/
│   ├── RosBridgeConfig.java       # 멀티 로봇 rosbridge 설정
│   └── WebSocketConfig.java       # STOMP WebSocket 설정
├── controller/
│   ├── DashboardController.java   # 대시보드 / Fleet 페이지
│   ├── RobotApiController.java    # 상태 조회 · 통계 · 이벤트 ACK REST API
│   ├── CommandController.java     # 로봇 제어 명령 REST API
│   ├── DetectionController.java   # YOLO 감지 결과 수신 REST API
│   └── MapController.java         # SLAM 맵 이미지 REST API
├── domain/
│   ├── RobotState.java            # 로봇 상태 enum (IDLE/MOVING/EMERGENCY_STOP/…)
│   ├── RobotStatus.java           # 상태 엔티티
│   └── RobotEvent.java            # 이벤트 엔티티 (YOLO_DETECTED, ackStatus)
├── kafka/
│   ├── RobotStatusProducer.java   # Kafka 프로듀서 (prod)
│   ├── RobotStatusConsumer.java   # Kafka 컨슈머 (prod)
│   └── dto/                       # Kafka 메시지 DTO (robotState 포함)
├── ros/
│   ├── RosBridgeClient.java       # 로봇별 rosbridge WebSocket 클라이언트 (subscribe + publish)
│   └── RosBridgeManager.java      # 멀티 로봇 Map 구조 관리 + publishToRobot()
└── service/
    ├── RobotStatusService.java    # 상태 처리 · 상태머신 · WebSocket 푸시
    ├── RobotCommandService.java   # E-Stop · Nav2 목표 전송 서비스
    ├── MapService.java            # SLAM OccupancyGrid 저장 · PNG 변환
    ├── RobotStatsService.java     # 통계 집계
    └── RobotPersistenceService.java # DB 저장
```

---

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka 브로커 주소 |
| `SPRING_PROFILES_ACTIVE` | `dev` | 실행 프로파일 |

---

## 로봇 설정 추가

`application.yml`의 `rosbridge.robots` 리스트에 로봇을 추가합니다:

```yaml
rosbridge:
  reconnect-delay-ms: 3000
  robots:
    - robot-id: robot-01
      uri: ws://192.168.1.101:9090
      odom-topic: /odom
      battery-topic: /battery_state
      map-topic: /map
    - robot-id: robot-02
      uri: ws://192.168.1.102:9090
      odom-topic: /odom
      battery-topic: /battery_state
      map-topic: /map
```

---

## 개발 현황

### 미니 프로젝트 (완료)
- [x] RosBridgeClient — rosbridge WebSocket 연결 및 토픽 구독
- [x] 멀티 로봇 Fleet 지원 (RosBridgeConfig, RosBridgeManager)
- [x] 실시간 WebSocket 상태 푸시 (STOMP)
- [x] JPA + H2/MySQL 상태·이벤트 저장
- [x] Kafka 이벤트 파이프라인 (prod 프로파일)
- [x] 대시보드 UI — 2D 맵, 속도 차트, 배터리, 통계
- [x] Docker Compose 풀스택 배포

### 파이널 프로젝트 — Phase 1: 제어·상태머신·Fleet (완료)
- [x] Ubuntu 22.04 + ROS2 Humble 환경 구축
- [x] TurtleBot3 Gazebo 시뮬레이션 연동
- [x] rosbridge → Spring Boot 실제 데이터 연결 (odom, battery_state 토픽 구독)
- [x] slam_toolbox OccupancyGrid 맵 실시간 오버레이
- [x] 로봇 상태머신 (IDLE / MOVING / EMERGENCY_STOP / CHARGING / ERROR)
- [x] 로봇 명령 전송 — 긴급 정지(E-Stop), Nav2 목표 지점 (rosbridge publish)
- [x] Fleet 전체 뷰 (`/fleet`) — 전체 로봇 카드 그리드 + 개별 긴급 정지
- [x] YOLO REST 연동 엔드포인트 (`POST /api/robot/{id}/detection`)
- [x] 이벤트 ACK 처리 (ackStatus, ackedAt)
- [x] 이벤트 타입 확장 (`YOLO_DETECTED`)

### 파이널 프로젝트 — Phase 2: MSA 분리 (예정)
- [ ] Spring Cloud Gateway — API Gateway 도입
- [ ] Eureka 서비스 디스커버리
- [ ] 서비스 분리: telemetry / map / alert / stats / command
- [ ] Resilience4j Circuit Breaker (rosbridge 장애 격리)
- [ ] Prometheus + Grafana 메트릭 모니터링

### 파이널 프로젝트 — Phase 3: 운영 품질 (예정)
- [ ] 분산 추적 (Zipkin + Sleuth)
- [ ] ELK Stack 중앙 로그 수집
- [ ] 로봇별 운행 경로 히스토리 재생
- [ ] 알림 필터 설정 UI (배터리 임계값, 이벤트 타입별)
