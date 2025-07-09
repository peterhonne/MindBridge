package com.mindbridge.ai.common.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class KeycloakResourceServerSecurityConfiguration {


    @Value("${keycloak.client-id:MindBridgeClient}")
    private String clientId;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize ->
                        authorize.requestMatchers("/actuator/**","/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/mindbridge/**").permitAll()
                                .anyRequest().authenticated())
                .csrf(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2-> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter = jwt -> {

            var resourceAccess = jwt.getClaimAsMap("resource_access");
            if (resourceAccess == null || resourceAccess.get(clientId) == null) {
                return List.of();
            }
            var clientRoles = (List<String>) ((java.util.Map<String, Object>)resourceAccess.get(clientId)).get("roles");
            if (clientRoles == null) {
                return List.of();
            }
            return clientRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtConverter;
    }


}
