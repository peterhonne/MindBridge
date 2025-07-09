package com.mindbridge.ai.agent.orchestrator.service;

import com.mindbridge.ai.agent.orchestrator.enums.TherapyStatus;
import com.mindbridge.ai.agent.orchestrator.models.dto.UserProfileDto;
import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.repository.UserRepository;
import com.mindbridge.ai.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;

    public User getUser(String userId) {
        return userRepository.findByKeycloakUserId(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }


    public UserProfileDto getUserProfile(String userId) {
        User user = userRepository.findByKeycloakUserId(userId)
                .orElseGet(() -> saveUser(userId)); // if not exit, save one

        return UserProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .therapyStatus(user.getTherapyStatus())
                .build();
    }

    private User saveUser(String userId) {
        User user = User.builder()
                .keycloakUserId(userId)
                .email(SecurityUtils.getEmail())
                .username(SecurityUtils.getUsername())
                .therapyStatus(TherapyStatus.NONE)
                .build();
        return userRepository.save(user);
    }

    // TODO update user profile, update password, register account, delete account. Call keycloak API

}
