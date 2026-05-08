package com.amr.dashboard.service;

import com.amr.dashboard.domain.Role;
import com.amr.dashboard.domain.User;
import com.amr.dashboard.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User buildUser(String username, Role role, boolean enabled) {
        User user = User.builder()
                .username(username)
                .passwordHash("encoded_pw")
                .role(role)
                .build();
        user.setEnabled(enabled);
        return user;
    }

    @Test
    @DisplayName("loadUserByUsername — 활성 사용자 정상 반환")
    void loadUserByUsername_activeUser_returnsUserDetails() {
        User user = buildUser("admin", Role.ADMIN, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails result = authService.loadUserByUsername("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("loadUserByUsername — 비활성 사용자면 disabled=true")
    void loadUserByUsername_disabledUser_returnsDisabledDetails() {
        User user = buildUser("viewer", Role.VIEWER, false);
        when(userRepository.findByUsername("viewer")).thenReturn(Optional.of(user));

        UserDetails result = authService.loadUserByUsername("viewer");

        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername — 존재하지 않는 사용자이면 UsernameNotFoundException 발생")
    void loadUserByUsername_notFound_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("createUser — 정상적으로 사용자 생성 및 저장")
    void createUser_success_savesUser() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("rawpw")).thenReturn("encoded_pw");
        User expected = buildUser("newuser", Role.OPERATOR, true);
        when(userRepository.save(any(User.class))).thenReturn(expected);

        User result = authService.createUser("newuser", "rawpw", Role.OPERATOR);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getRole()).isEqualTo(Role.OPERATOR);
        verify(passwordEncoder).encode("rawpw");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser — 중복 사용자명이면 IllegalArgumentException 발생")
    void createUser_duplicateUsername_throwsException() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> authService.createUser("admin", "pw", Role.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 사용자명");
    }

    @Test
    @DisplayName("changePassword — 새 비밀번호가 인코딩되어 저장됨")
    void changePassword_encodesAndSaves() {
        User user = buildUser("operator", Role.OPERATOR, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("new_encoded");

        authService.changePassword(1L, "newpw");

        assertThat(user.getPasswordHash()).isEqualTo("new_encoded");
    }

    @Test
    @DisplayName("changeRole — 역할이 변경됨")
    void changeRole_updatesRole() {
        User user = buildUser("operator", Role.OPERATOR, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.changeRole(1L, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("toggleEnabled — 활성 상태가 반전됨 (true → false)")
    void toggleEnabled_trueToFalse() {
        User user = buildUser("viewer", Role.VIEWER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.toggleEnabled(1L);

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("toggleEnabled — 비활성 상태가 반전됨 (false → true)")
    void toggleEnabled_falseToTrue() {
        User user = buildUser("viewer", Role.VIEWER, false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.toggleEnabled(1L);

        assertThat(user.isEnabled()).isTrue();
    }
}
