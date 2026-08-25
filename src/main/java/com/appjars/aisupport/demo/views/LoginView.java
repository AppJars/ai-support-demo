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
package com.appjars.aisupport.demo.views;

import com.appjars.aisupport.business.service.request.UserSearchRequest;
import com.appjars.aisupport.demo.security.SecurityConfiguration;
import com.appjars.aisupport.demo.service.DemoUserDetailsService;
import com.appjars.aisupport.model.UserDto;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Comparator;

/**
 * Login screen of the demo. Instead of a username/password form it shows one card per demo account,
 * so the evaluator signs in with a single click. Each card submits the standard Spring Security form
 * login (with the CSRF token) under the hood, so authentication, the post-login redirect and any
 * pending guided tour keep working unchanged. The demo convention is password == username.
 */
@Route(SecurityConfiguration.LOGIN_URL)
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {

  private static final String KEY_PREFIX = "appjars.aisupport.demo.login.";

  private final transient DemoUserDetailsService securityService;

  public LoginView(DemoUserDetailsService securityService) {
    this.securityService = securityService;

    addClassName("login-view");
    setSizeFull();
    setPadding(true);
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);

    H1 title = new H1(t("title"));
    title.addClassName("login-title");
    Paragraph intro = new Paragraph(t("intro"));
    intro.addClassName("login-intro");

    Image logo = new Image("icons/icon.png", "AppJars");

    Div cards = new Div();
    cards.addClassName("login-card-row");
    cards.getStyle().setMarginBottom("var(--lumo-space-m)");
    // One card per account; administrators first, then alphabetically.
    securityService.findUsers(UserSearchRequest.caseInsensitive("")).stream()
      .sorted(Comparator.comparing(UserDto::isAdmin)
        .reversed()
        .thenComparing(UserDto::getUsername))
      .forEach(user -> cards.add(accountCard(user)));

    add(new Div(logo, title, intro, cards));
  }

  private Div accountCard(UserDto user) {
    String username = user.getUsername();

    Div header = new Div(avatar(username), new Span(username), roleBadge(user.isAdmin()));
    header.addClassName("login-card-header");

    Paragraph desc = new Paragraph(t("persona." + username.toLowerCase() + ".desc"));
    desc.addClassName("login-card-desc");

    Button loginBtn = new Button(getTranslation(KEY_PREFIX + "button", username),
      e -> loginAs(username));
    loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    loginBtn.setWidthFull();

    Div card = new Div(header, desc, loginBtn);
    card.addClassName("login-card");
    return card;
  }

  private Avatar avatar(String username) {
    Avatar avatar = new Avatar(username);
    securityService.getProfilePictureUrlByUsername(username).ifPresent(avatar::setImage);
    return avatar;
  }

  private Span roleBadge(boolean admin) {
    Span badge = new Span(t(admin ? "role.admin" : "role.user"));
    badge.getElement().getThemeList().add("badge " + (admin ? "success" : "contrast"));
    badge.addClassName("login-card-badge");
    return badge;
  }

  /**
   * Submits the standard Spring Security form login for the chosen account by posting a hidden form
   * (with the CSRF token) to the login processing URL, exactly as a login form would.
   */
  private void loginAs(String username) {
    CsrfToken csrf = (CsrfToken) VaadinRequest.getCurrent().getAttribute(CsrfToken.class.getName());
    String csrfParam = csrf != null ? csrf.getParameterName() : "_csrf";
    String csrfToken = csrf != null ? csrf.getToken() : "";
    getUI().ifPresent(ui -> ui.getPage().executeJs(
      """
        const f = document.createElement('form');
        f.method = 'POST';
        f.action = 'login';
        const add = (n, v) => {
          const i = document.createElement('input');
          i.type = 'hidden';
          i.name = n;
          i.value = v;
          f.appendChild(i);
        };
        add('username', $0);
        add('password', $0);
        if ($1) { add($1, $2); }
        document.body.appendChild(f);
        f.submit();
        """,
      username, csrfParam, csrfToken));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (securityService.isUserAuthenticated()) {
      event.forwardTo("");
      return;
    }
    // Spring Security redirects failed logins back to /login?error.
    if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
      Notification notification = Notification.show(t("error"));
      notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
  }

  @Override
  public String getPageTitle() {
    return t("title");
  }

  private String t(String key) {
    return getTranslation(KEY_PREFIX + key);
  }
}
