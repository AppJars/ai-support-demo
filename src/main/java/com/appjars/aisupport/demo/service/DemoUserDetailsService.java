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
package com.appjars.aisupport.demo.service;

import com.appjars.aisupport.business.service.AuthenticatedUserProvider;
import com.appjars.aisupport.business.service.UserProfilePictureProvider;
import com.appjars.aisupport.business.service.request.UserSearchRequest;
import com.appjars.aisupport.demo.security.SecurityConfiguration;
import com.appjars.aisupport.model.UserDto;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoUserDetailsService implements UserProfilePictureProvider, AuthenticatedUserProvider {

  // Public URL under which the pictures are served, and the matching classpath location that backs
  // it. The files live in META-INF/resources/images/... so Vaadin serves them at /images/... (a path
  // already permitted anonymously by SecurityConfiguration), which is what avatar.setImage(url) needs.
  private static final String PICTURE_URL_PREFIX = "/images/profile-pictures/";
  private static final String PICTURE_CLASSPATH_PREFIX = "META-INF/resources" + PICTURE_URL_PREFIX;

  private final Set<String> usernames = Set.of("Olivia", "Marcus", "Sophia", "Leo");
  private final Map<String, String> usersProfilesPics = Map.of(
    "Marcus", "admin-profile-pic1.png",
    "Olivia", "admin-profile-pic2.png",
    "Leo", "user-profile-pic1.png",
    "Sophia", "user-profile-pic2.png"
  );

  private final AuthenticationContext authenticationContext;
  private final UserDetailsService userDetailsService;

  public void logout() {
    authenticationContext.logout();
  }

  public boolean isUserAuthenticated() {
    return authenticationContext.isAuthenticated();
  }

  @Override
  public Optional<UserDto> getAuthenticatedUser() {
    Optional<UserDetails> userDetails = authenticationContext.getAuthenticatedUser(UserDetails.class);
    return userDetails.map(details -> UserDto.builder()
      .username(details.getUsername())
      .isAdmin(authenticationContext.getGrantedRoles().contains(SecurityConfiguration.ADMIN_ROLE))
      .build()
    );
  }

  @Override
  public Set<UserDto> findUsers(UserSearchRequest request) {
    return usernames.stream()
      .filter(name -> matchesPrefix(name, request))
      .map(userDetailsService::loadUserByUsername)
      .map(u -> UserDto.builder()
        .username(u.getUsername())
        .isAdmin(u.getAuthorities().stream()
          .map(auth -> auth.getAuthority().substring(5)
          )
          .anyMatch(auth -> auth.equals(SecurityConfiguration.ADMIN_ROLE))
        )
        .build()
      ).collect(Collectors.toSet());
  }

  private static boolean matchesPrefix(String username, UserSearchRequest request) {
    String prefix = request.prefix() != null ? request.prefix() : "";
    return request.caseInsensitive()
      ? username.toLowerCase().startsWith(prefix.toLowerCase())
      : username.startsWith(prefix);
  }

  @Override
  public Optional<byte[]> getProfilePictureByUsername(String username) {
    String file = usersProfilesPics.get(username);
    if (file == null) {
      return Optional.empty();
    }
    try (InputStream is =
      getClass().getClassLoader().getResourceAsStream(PICTURE_CLASSPATH_PREFIX + file)) {
      return is == null ? Optional.empty() : Optional.of(is.readAllBytes());
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<String> getProfilePictureUrlByUsername(String username) {
    return Optional.ofNullable(usersProfilesPics.get(username)).map(file -> PICTURE_URL_PREFIX + file);
  }

}
