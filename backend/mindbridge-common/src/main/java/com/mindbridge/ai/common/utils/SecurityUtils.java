package com.mindbridge.ai.common.utils;


import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;


@UtilityClass
public class SecurityUtils {

    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public String getUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }


    public String getUsername() {
        Authentication authentication = SecurityUtils.getAuthentication();
        Jwt jwt = (Jwt) authentication.getCredentials();
        return (String) jwt.getClaims().get("preferred_username");
    }

    public String getEmail() {
        Authentication authentication = SecurityUtils.getAuthentication();
        Jwt jwt = (Jwt) authentication.getCredentials();
        return (String) jwt.getClaims().get("email");
    }


}
