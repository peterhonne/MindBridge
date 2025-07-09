package com.mindbridge.ai.agent.orchestrator.controller;


import com.mindbridge.ai.agent.orchestrator.models.dto.UserProfileDto;
import com.mindbridge.ai.agent.orchestrator.models.entity.User;
import com.mindbridge.ai.agent.orchestrator.service.UserProfileService;
import com.mindbridge.ai.common.annotation.SysLog;
import com.mindbridge.ai.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @SysLog("Get user profile")
    @GetMapping
    public ResponseEntity<UserProfileDto> getUserProfile() {
        UserProfileDto profile = userProfileService.getUserProfile(SecurityUtils.getKeycloakUserId());
        return ResponseEntity.ok(profile);
    }

    @SysLog("Init user profile")
    @PostMapping
    public ResponseEntity<UserProfileDto> initUserProfile() {
        User user = userProfileService.saveUser(SecurityUtils.getKeycloakUserId());
        UserProfileDto userProfile = UserProfileDto.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .therapyStatus(user.getTherapyStatus())
                .build();
        return ResponseEntity.ok(userProfile);
    }


}
