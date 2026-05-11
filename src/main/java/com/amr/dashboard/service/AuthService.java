package com.amr.dashboard.service;

import com.amr.dashboard.domain.Role;
import com.amr.dashboard.domain.User;
import com.amr.dashboard.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole().name())
                .disabled(!user.isEnabled())
                .build();
    }

    @Transactional
    public User createUser(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다: " + username);
        }
        validatePassword(rawPassword);
        return userRepository.save(User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    @Transactional
    public void changePassword(Long userId, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        validatePassword(newRawPassword);
        user.changePassword(passwordEncoder.encode(newRawPassword));
    }

    /**
     * 비밀번호 정책: 최소 8자, 대문자 1개 이상, 숫자 1개 이상
     */
    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
        if (!rawPassword.chars().anyMatch(Character::isUpperCase)) {
            throw new IllegalArgumentException("비밀번호에 대문자가 최소 1개 포함되어야 합니다.");
        }
        if (!rawPassword.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("비밀번호에 숫자가 최소 1개 포함되어야 합니다.");
        }
    }

    @Transactional
    public void changeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.changeRole(role);
    }

    @Transactional
    public void toggleEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setEnabled(!user.isEnabled());
    }
}
