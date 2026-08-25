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
package com.appjars.aisupport.demo.views.layouts;

import com.appjars.aisupport.business.service.UserProfilePictureProvider;
import com.appjars.aisupport.demo.service.DemoUserDetailsService;
import com.appjars.aisupport.demo.utils.ThemeSelector;
import com.appjars.aisupport.demo.utils.Utils;
import com.appjars.aisupport.demo.views.HomeView;
import com.appjars.aisupport.demo.views.LoginView;
import com.appjars.aisupport.demo.views.tour.DemoTours;
import com.appjars.aisupport.demo.views.tour.DemoTours.DemoTour;
import com.appjars.aisupport.flow.component.AISupportChatAssistant;
import com.appjars.aisupport.flow.util.RouteConfigurer;
import com.appjars.aisupport.flow.view.*;
import com.appjars.aisupport.model.UserDto;
import com.flowingcode.backendcore.exception.ServiceException;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Style.AlignItems;
import com.vaadin.flow.dom.Style.Display;
import com.vaadin.flow.dom.Style.TextAlign;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

// Anonymous so the public landing page (HomeView) can render inside this layout; beforeEnter still
// reroutes anonymous visitors to the login screen for every other view.
@FieldDefaults(level = AccessLevel.PRIVATE)
@PreserveOnRefresh
@AnonymousAllowed
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

  private static final String DOCS_URL =
    "https://docs.appjars.com/ai-support/overview/";

  H2 viewTitle;
  Class<?> currentView;

  final DemoUserDetailsService securityService;
  final UserProfilePictureProvider avatarProvider;
  final AISupportChatAssistant chatAssistant;

  @Value(RouteConfigurer.URL_CHAT)
  String chatViewPath;

  public MainLayout(DemoUserDetailsService securityService, UserProfilePictureProvider avatarProvider,
    AISupportChatAssistant chatAssistant) {
    this.securityService = securityService;
    this.avatarProvider = avatarProvider;
    this.chatAssistant = chatAssistant;
    setPrimarySection(Section.DRAWER);
    addDrawerContent();
    addHeaderContent();
  }

  // The floating chat assistant is only for signed-in users. It goes into the content slot next to
  // the routed view, which AppLayout leaves untouched when it swaps views on navigation.
  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    if (securityService.isUserAuthenticated() && getChildren().noneMatch(AISupportChatAssistant.class::isInstance)) {
      getElement().appendChild(chatAssistant.getElement());
    }
  }

  private void addHeaderContent() {
    DrawerToggle toggle = new DrawerToggle();
    toggle.getElement().setAttribute("aria-label", "Menu toggle");

    viewTitle = new H2();
    viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

    // The title takes the free space, pushing the tour and documentation actions to the end.
    HorizontalLayout header = new HorizontalLayout(toggle, viewTitle, createTourMenu(), createDocsButton(),
      new ThemeSelector(this::getTranslation));
    header.setWidthFull();
    header.setAlignItems(FlexComponent.Alignment.CENTER);
    header.expand(viewTitle);
    header.getStyle().set("padding-inline-end", "var(--lumo-space-m)");

    addToNavbar(true, header);
  }

  // Lets the user start a guided tour from any view: the current view's tour starts in place, the
  // others navigate to their view first (reusing the pending-tour mechanism).
  private MenuBar createTourMenu() {
    MenuBar menu = new MenuBar();
    menu.addThemeVariants(MenuBarVariant.LUMO_PRIMARY);
    menu.setOpenOnHover(true);

    HorizontalLayout mainItem = new HorizontalLayout(
      new Text(tourLabel("button")),
      VaadinIcon.MAP_MARKER.create()
    );
    mainItem.setPadding(false);
    mainItem.setSpacing(false);
    mainItem.getStyle().setGap("var(--lumo-space-xs)");
    mainItem.setAlignItems(Alignment.CENTER);

    MenuItem trigger = menu.addItem(mainItem);
    trigger.getElement().setAttribute("aria-label", tourLabel("button"));

    SubMenu tours = trigger.getSubMenu();
    // Shortcut for the view currently shown; the rest jump to a specific view's tour.
    tours.addItem(tourItem("thispage", VaadinIcon.BOOKMARK.create()), e -> startCurrentTour());
    tours.addSeparator();
    tours.addItem(tourItem("home", VaadinIcon.HOME.create()), e -> startTour(DemoTour.HOME));
    tours.addItem(tourItem("chat", VaadinIcon.COMMENTS.create()), e -> startTour(DemoTour.CHAT));
    tours.addItem(tourItem("bubble", new SvgIcon("icons/robot.svg")), e -> startTour(DemoTour.BUBBLE));
    tours.addItem(tourItem("assistants", VaadinIcon.USERS.create()), e -> startTour(DemoTour.ASSISTANTS));
    tours.addItem(tourItem("prompts", VaadinIcon.FILE_TEXT.create()), e -> startTour(DemoTour.PROMPTS));
    tours.addItem(tourItem("llms", VaadinIcon.AUTOMATION.create()), e -> startTour(DemoTour.LLMS));
    tours.addItem(tourItem("documents", VaadinIcon.BOOK.create()), e -> startTour(DemoTour.DOCUMENTS));
    tours.addItem(tourItem("categories", VaadinIcon.FOLDER.create()), e -> startTour(DemoTour.CATEGORIES));
    tours.addItem(tourItem("inspector", VaadinIcon.SEARCH.create()), e -> startTour(DemoTour.INSPECTOR));
    tours.addItem(tourItem("channels", VaadinIcon.CONNECT.create()), e -> startTour(DemoTour.CHANNELS));
    return menu;
  }

  private Button createDocsButton() {
    Button docs = new Button(
      VaadinIcon.BOOK.create(),
      e -> getUI().ifPresent(ui -> ui.getPage().open(DOCS_URL, "_blank"))
    );
    docs.addThemeVariants(ButtonVariant.LUMO_ICON);
    docs.setAriaLabel(getTranslation("appjars.aisupport.demo.layout.docs"));
    docs.setTooltipText(getTranslation("appjars.aisupport.demo.layout.docs"));
    return docs;
  }

  // Starts the tour of the view currently shown; if that view has no tour, tells the user.
  private void startCurrentTour() {
    DemoTour tour = currentTour();
    if (tour != null) {
      startTour(tour);
    } else {
      Notification.show(getTranslation("appjars.aisupport.demo.layout.tour.none"));
    }
  }

  private DemoTour currentTour() {
    if (currentView == null) {
      return null;
    }
    for (DemoTour tour : DemoTour.values()) {
      if (pendingTourView(tour).equals(currentView)) {
        return tour;
      }
    }
    return null;
  }

  private void startTour(DemoTour tour) {
    Class<? extends Component> target = pendingTourView(tour);
    if (target.equals(currentView)) {
      runTour(tour);
    } else {
      VaadinSession.getCurrent().setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, tour);
      getUI().ifPresent(ui -> ui.navigate(target));
    }
  }

  // Starts the tour on the current view. The chat-bubble tour additionally opens the floating chat
  // assistant while it runs (and closes it again when the tour ends) so its window can be explained.
  private void runTour(DemoTour tour) {
    if (tour == DemoTour.BUBBLE) {
      DemoTours.start(tour, this, this::getTranslation, chatAssistant::open, chatAssistant::close);
    } else {
      DemoTours.start(tour, this, this::getTranslation);
    }
  }

  private Div tourItem(String key, AbstractIcon<?> icon) {
    return Utils.menuItem(tourLabel(key), icon);
  }

  private String tourLabel(String key) {
    return getTranslation("appjars.aisupport.demo.layout.tour." + key);
  }

  private void addDrawerContent() {
    H1 appName = new H1(getTranslation("appjars.aisupport.demo.layout.drawertitle"));
    appName.addClassNames(LumoUtility.FontSize.LARGE);

    Header header = new Header(appName);
    header.getStyle().setPadding("var(--lumo-space-m)");

    Scroller scroller = new Scroller(createNavigation());

    addToDrawer(header, scroller, Utils.divider(), createFooter());
  }

  private SideNav createNavigation() {
    SideNav nav = new SideNav();

    SideNavItem homeItem =
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.home"), HomeView.class);
    homeItem.setPrefixComponent(VaadinIcon.HOME.create());

    SideNavItem aiSupportItem = new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.aiSupportItem"));
    aiSupportItem.setPrefixComponent(VaadinIcon.CHAT.create());
    aiSupportItem.setExpanded(true);

    aiSupportItem.addItem(
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.aichat"), ChatView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.assistants"), AssistantView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.prompts"), PromptsView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.llms"), LlmsView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.inspector"), LlmInspectorView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.categories"), CategoriesView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.documents"), DocumentsView.class),
      new SideNavItem(getTranslation("appjars.aisupport.demo.menuitem.channels"), ChannelsView.class));

    nav.addItem(homeItem, aiSupportItem);

    return nav;
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // The landing page is the public face of the demo, reachable without logging in; every other
    // view requires authentication.
    if (!securityService.isUserAuthenticated() && !HomeView.class.equals(event.getNavigationTarget())) {
      event.rerouteTo(LoginView.class);
    }
  }

  private Footer createFooter() {
    Footer footer = new Footer();

    Optional<Principal> userOpt = Optional.ofNullable(VaadinRequest.getCurrent().getUserPrincipal());
    Div layout = new Div();
    layout.getStyle()
      .setDisplay(Display.FLEX)
      .setAlignItems(AlignItems.CENTER)
      .setWidth("100%")
      .setHeight("100%")
      .setGap("var(--lumo-space-s)");

    if (userOpt.isPresent()) {
      UserDto user = securityService.getAuthenticatedUser().orElseThrow(() -> new ServiceException(
        "No authenticated user"));
      String rolePrefix = "appjars.aisupport.demo.login.role";
      Badge badge = new Badge(getTranslation(rolePrefix + (user.isAdmin() ? ".admin" : ".user")));
      badge.addThemeVariants(BadgeVariant.SMALL);
      if (!user.isAdmin()) {
        badge.addThemeVariants(BadgeVariant.CONTRAST);
      }

      Avatar avatar = new Avatar(user.getUsername());
      avatar.setThemeName("small");
      avatar.getElement().setAttribute("tabindex", "-1");
      avatarProvider.getProfilePictureUrlByUsername(user.getUsername()).ifPresent(avatar::setImage);

      MenuBar userMenu = new MenuBar();
      userMenu.setThemeName("tertiary");
      userMenu.getStyle()
        .setBackgroundColor("var(--lumo-contrast-5pct)")
        .setBorderRadius("var(--lumo-border-radius-l)")
        .setPadding("var(--lumo-space-xs) 0px");
      userMenu.setWidthFull();

      // The class propagates to the inner vaadin-menu-bar-button, which main-layout.css stretches to
      // full width (setWidthFull only stretches the menu-bar host, not its light-DOM button).
      MenuItem profile = userMenu.addItem("");
      profile.addClassNames("footer-user");

      Span username = new Span(user.getUsername());
      username.getStyle()
        .set("flex", "1")
        .setColor("var(--lumo-header-text-color)")
        .setTextAlign(TextAlign.LEFT);

      layout.add(avatar, username, badge);
      layout.getStyle().setCursor("pointer");

      profile.add(layout);
      profile.getSubMenu().addItem(
        Utils.menuItem(getTranslation("appjars.aisupport.demo.layout.signout"), VaadinIcon.SIGN_OUT.create()),
        e -> securityService.logout()
      );

      footer.add(userMenu);
    } else {
      Anchor loginLink = new Anchor("login", getTranslation("appjars.aisupport.demo.layout.signin"));
      Icon doorIcon = VaadinIcon.SIGN_IN.create();
      doorIcon.setSize("var(--lumo-icon-size-s)");
      doorIcon.getStyle().set("fill", "var(--lumo-primary-text-color)");

      layout.add(doorIcon, loginLink);
      footer.add(layout);
    }

    footer.getStyle().setPadding("var(--lumo-space-s)");
    return footer;
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    HasElement view = event.getActiveChain().stream().findFirst().orElse(null);
    currentView = view == null ? null : view.getClass();
    viewTitle.setText(titleOf(view));
    // We make the chat assistant visible only when not on the chat view
    boolean onChatView = event.getLocation().getPath().equals(chatViewPath);
    chatAssistant.getChildren().forEach(c -> c.setVisible(!onChatView));
    if (onChatView) {
      chatAssistant.close();
    }
    startPendingTour(event);
  }

  // A view tour requested from the landing page survives the login redirect (it is stored in the
  // session) and starts once its target view has actually rendered.
  private void startPendingTour(AfterNavigationEvent event) {
    VaadinSession session = VaadinSession.getCurrent();
    if (!(session.getAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE) instanceof DemoTour pending)) {
      return;
    }
    Class<? extends Component> target = pendingTourView(pending);
    boolean rendered = event.getActiveChain().stream().anyMatch(c -> target.equals(c.getClass()));
    if (rendered) {
      session.setAttribute(DemoTours.PENDING_TOUR_ATTRIBUTE, null);
      runTour(pending);
    }
  }

  private Class<? extends Component> pendingTourView(DemoTour tour) {
    return switch (tour) {
      case CHAT -> ChatView.class;
      case ASSISTANTS -> AssistantView.class;
      case PROMPTS -> PromptsView.class;
      case LLMS -> LlmsView.class;
      case DOCUMENTS -> DocumentsView.class;
      case CATEGORIES -> CategoriesView.class;
      case INSPECTOR -> LlmInspectorView.class;
      case CHANNELS -> ChannelsView.class;
      case HOME -> HomeView.class;
      // The floating chat bubble shows on any authenticated view except the full chat; the Assistants
      // view is a reliable place for evaluators to see it.
      case BUBBLE -> AssistantView.class;
    };
  }

  // Resolves the title of the routed view (the leaf of the navigation chain). The AI Support views
  // implement HasDynamicTitle.
  private String titleOf(HasElement view) {
    if (view instanceof HasDynamicTitle dynamicTitle) {
      return dynamicTitle.getPageTitle();
    }
    if (view != null) {
      PageTitle pageTitle = view.getClass().getAnnotation(PageTitle.class);
      if (pageTitle != null) {
        return pageTitle.value();
      }
    }
    return "";
  }
}
