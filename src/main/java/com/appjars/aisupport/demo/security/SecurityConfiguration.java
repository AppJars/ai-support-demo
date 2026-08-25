/*-
 * #%L
 * AI Support - Demo
 * %%
 * Copyright (C) 2023 - 2026 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.appjars.aisupport.demo.security;

import com.appjars.aisupport.demo.views.LoginView;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SecurityConfiguration {

    public static final String LOGOUT_URL = "/";
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String USER_ROLE = "USER";
    public static final String LOGIN_URL = "/login";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth
            // Static resources the anonymous landing page needs (icons, images, root stylesheet).
            .requestMatchers(HttpMethod.GET, "/*.png", "/*.css", "/images/**")
            .permitAll()
            // SVG icons (SvgIcon("/icons/*.svg")) are served from META-INF/resources/icons and are
            // fetched as standalone browser requests; permit them so they aren't redirected to login.
            // VaadinWebSecurity auto-permitted these in V24; VaadinSecurityConfigurer does not.
            .requestMatchers(HttpMethod.GET, "/icons/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/webhooks/**")
            .permitAll()
            .requestMatchers(HttpMethod.POST, "/api/webhooks/**")
            .permitAll()
            .requestMatchers(HttpMethod.GET, "/api/attachments/**")
            .permitAll()
        ).csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/webhooks/**", "/api/attachments/**") // Disable CSRF for webhooks
        );

        http.with(vaadin(), configurer -> configurer.loginView(LoginView.class, LOGOUT_URL));

        return http.build();
    }

    /**
     * The four demo accounts. Passwords equal the username ({@code {noop}} = plain text): the login
     * screen is a one-click account picker that submits them for the user, so they are never typed.
     * Olivia and Marcus are the Lumen Robotics support team (admins); Sophia and Leo are customers.
     */
    @Bean
    public UserDetailsService users() {
        UserDetails olivia = User.builder()
            .username("Olivia")
            .password("{noop}Olivia")
            .roles(USER_ROLE, ADMIN_ROLE)
            .build();
        UserDetails marcus = User.builder()
            .username("Marcus")
            .password("{noop}Marcus")
            .roles(USER_ROLE, ADMIN_ROLE)
            .build();
        UserDetails sophia = User.builder()
            .username("Sophia")
            .password("{noop}Sophia")
            .roles(USER_ROLE)
            .build();
        UserDetails leo = User.builder()
            .username("Leo")
            .password("{noop}Leo")
            .roles(USER_ROLE)
            .build();
        return new InMemoryUserDetailsManager(olivia, marcus, sophia, leo);
    }
}
