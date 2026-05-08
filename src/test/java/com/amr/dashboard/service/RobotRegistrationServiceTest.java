package com.amr.dashboard.service;

import com.amr.dashboard.domain.RobotRegistration;
import com.amr.dashboard.domain.RobotRegistrationRepository;
import com.amr.dashboard.ros.RosBridgeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RobotRegistrationService 단위 테스트")
class RobotRegistrationServiceTest {

    @Mock
    private RobotRegistrationRepository repository;

    @Mock
    private RosBridgeManager rosBridgeManager;

    @InjectMocks
    private RobotRegistrationService service;

    private RobotRegistration sampleReg;

    @BeforeEach
    void setUp() {
        sampleReg = new RobotRegistration();
        sampleReg.setRobotId("robot-01");
        sampleReg.setDisplayName("Robot 01");
        sampleReg.setRosbridgeUri("ws://localhost:9090");
        sampleReg.setEnabled(true);
    }

    @Test
    @DisplayName("findAll — 전체 로봇 목록 반환")
    void findAll_returnsList() {
        when(repository.findAll()).thenReturn(List.of(sampleReg));

        List<RobotRegistration> result = service.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRobotId()).isEqualTo("robot-01");
    }

    @Test
    @DisplayName("findEnabled — 활성화된 로봇만 반환")
    void findEnabled_returnsOnlyEnabled() {
        when(repository.findByEnabledTrue()).thenReturn(List.of(sampleReg));

        List<RobotRegistration> result = service.findEnabled();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("findById — 존재하는 ID이면 정상 반환")
    void findById_exists_returnsReg() {
        when(repository.findById("robot-01")).thenReturn(Optional.of(sampleReg));

        RobotRegistration result = service.findById("robot-01");

        assertThat(result.getRobotId()).isEqualTo("robot-01");
    }

    @Test
    @DisplayName("findById — 존재하지 않으면 IllegalArgumentException 발생")
    void findById_notFound_throwsException() {
        when(repository.findById("ghost-99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("ghost-99"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("로봇을 찾을 수 없음");
    }

    @Test
    @DisplayName("register — 저장 후 rosBridgeManager.connectRobot 호출됨")
    void register_savesAndConnects() {
        when(repository.existsById("robot-02")).thenReturn(false);
        when(repository.save(any())).thenReturn(sampleReg);

        RobotRegistration newReg = new RobotRegistration();
        newReg.setRobotId("robot-02");
        newReg.setDisplayName("Robot 02");
        newReg.setRosbridgeUri("ws://localhost:9091");

        service.register(newReg);

        verify(repository).save(newReg);
        verify(rosBridgeManager).connectRobot(sampleReg);
    }

    @Test
    @DisplayName("register — 이미 존재하는 ID이면 IllegalArgumentException 발생")
    void register_duplicateId_throwsException() {
        when(repository.existsById("robot-01")).thenReturn(true);

        assertThatThrownBy(() -> service.register(sampleReg))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 등록된 로봇 ID");
    }

    @Test
    @DisplayName("deregister — enabled=false 로 저장 후 disconnectRobot 호출됨")
    void deregister_disablesAndDisconnects() {
        when(repository.findById("robot-01")).thenReturn(Optional.of(sampleReg));
        when(repository.save(any())).thenReturn(sampleReg);

        service.deregister("robot-01");

        assertThat(sampleReg.isEnabled()).isFalse();
        verify(rosBridgeManager).disconnectRobot("robot-01");
    }

    @Test
    @DisplayName("update — displayName 필드가 변경됨")
    void update_displayName_isUpdated() {
        when(repository.findById("robot-01")).thenReturn(Optional.of(sampleReg));
        when(repository.save(any())).thenReturn(sampleReg);

        RobotRegistration patch = new RobotRegistration();
        patch.setDisplayName("수정된 이름");

        service.update("robot-01", patch);

        assertThat(sampleReg.getDisplayName()).isEqualTo("수정된 이름");
    }

    @Test
    @DisplayName("update — rosbridgeUri 필드가 변경됨")
    void update_rosbridgeUri_isUpdated() {
        when(repository.findById("robot-01")).thenReturn(Optional.of(sampleReg));
        when(repository.save(any())).thenReturn(sampleReg);

        RobotRegistration patch = new RobotRegistration();
        patch.setRosbridgeUri("ws://192.168.1.100:9090");

        service.update("robot-01", patch);

        assertThat(sampleReg.getRosbridgeUri()).isEqualTo("ws://192.168.1.100:9090");
    }
}
