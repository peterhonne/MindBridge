package com.mindbridge.ai.agent.orchestrator.controller;


import com.mindbridge.ai.agent.orchestrator.models.dto.UserProfileDto;
import com.mindbridge.ai.agent.orchestrator.service.UserProfileService;
import com.mindbridge.ai.common.annotation.SysLog;
import com.mindbridge.ai.common.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;

    @SysLog("Get user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getUserProfile() {
        UserProfileDto profile = userProfileService.getUserProfile(SecurityUtils.getKeycloakUserId());
        return ResponseEntity.ok(profile);
    }


}
