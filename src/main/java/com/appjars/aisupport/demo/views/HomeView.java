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

import com.appjars.aisupport.demo.utils.Utils;
import com.appjars.aisupport.demo.views.layouts.MainLayout;
import com.appjars.aisupport.flow.view.AssistantView;
import com.appjars.aisupport.flow.view.ChatView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/** Public landing page: the features, how to sign in, the license model and the guided tours. */
@AnonymousAllowed
@Route(value = "", layout = MainLayout.class)
public class HomeView extends VerticalLayout implements HasDynamicTitle {

  private static final String KEY_PREFIX = "appjars.aisupport.demo.home.";

  private static final String APPJARS_SITE_URL = "https://www.appjars.com";
  private static final String APPJARS_LICENSE_URL = "https://docs.appjars.com/free-mode/";
  private static final String GITHUB_ORG_URL = "https://github.com/AppJars";
  private static final String DOCS_URL = "https://docs.appjars.com/ai-support/overview/";

  public HomeView() {
    addClassName("home-view");
    setAlignItems(Alignment.STRETCH);
    setSizeFull();
    add(
      createHero(),
      Utils.divider(),
      createFeaturesSection(),
      Utils.divider(),
      createTryItSection(),
      Utils.divider(),
      createLicenseSection(),
      Utils.divider(),
      createLinksSection()
    );
  }

  private Component createHero() {
    H1 title = new H1(t("hero.title"));
    Paragraph tagline = new Paragraph(t("hero.tagline"));
    tagline.addClassName("home-tagline");

    Anchor anchor = new Anchor(APPJARS_SITE_URL);
    Image logo = new Image("images/logo-color.png", "AppJars Logo");
    logo.setHeight("92px");
    logo.setWidth("auto");
    Tooltip.forComponent(logo).setText(getTranslation("appjars.aisupport.demo.home.links.appjars"));
    anchor.setTarget("_blank");
    anchor.add(logo);

    Div hero = new Div(anchor, title, tagline);
    hero.setId("home-hero");
    hero.addClassName("home-hero");
    return hero;
  }

  private Component createFeaturesSection() {
    Div cards = new Div(
      featureCard(VaadinIcon.COMMENTS, "features.chat"),
      featureCard(VaadinIcon.MAGIC, "features.assistants"),
      featureCard(VaadinIcon.BOOK, "features.rag"),
      featureCard(VaadinIcon.SERVER, "features.llms"),
      featureCard(VaadinIcon.CLIPBOARD_TEXT, "features.prompts"),
      featureCard(VaadinIcon.SEARCH, "features.inspector"),
      featureCard(VaadinIcon.CONNECT, "features.channels")
    );
    cards.addClassName("home-features");

    return section("home-features", t("features.title"), cards);
  }

  private Card featureCard(VaadinIcon icon, String key) {
    Card card = new Card();
    card.addClassName("home-feature-card");
    Icon prefix = icon.create();
    prefix.addClassName("home-feature-icon");
    card.setHeaderPrefix(prefix);
    card.setTitle(t(key + ".title"));
    card.add(new Paragraph(t(key + ".desc")));
    return card;
  }

  private Component createTryItSection() {
    Paragraph intro = new Paragraph(t("tryit.intro"));

    Button chat = new Button(t("tryit.chat"),
      e -> getUI().ifPresent(ui -> ui.navigate(ChatView.class))
    );
    chat.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    Button admin = new Button(t("tryit.admin"),
      e -> getUI().ifPresent(ui -> ui.navigate(AssistantView.class))
    );

    Div actions = new Div(chat, Utils.divider(true), admin);
    actions.addClassName("home-actions");

    return section("home-tryit", t("tryit.title"), intro, Utils.divider(true), actions);
  }

  private Component createLicenseSection() {
    Paragraph desc = new Paragraph(t("license.desc"));
    Anchor link = new Anchor(APPJARS_LICENSE_URL, t("license.link"));
    link.setTarget("_blank");
    return section("home-license", t("license.title"), desc, new Paragraph(link));
  }

  private Component createLinksSection() {
    Anchor github = new Anchor(GITHUB_ORG_URL, t("links.github"));
    github.setTarget("_blank");
    Anchor readme = new Anchor(DOCS_URL, t("links.readme"));
    readme.setTarget("_blank");
    Div links = new Div(github, Utils.divider(true), readme);
    links.addClassName("home-links");
    links.getStyle().setMarginBottom("var(--lumo-space-m)");
    return section("home-links", t("links.title"), links);
  }

  private VerticalLayout section(String id, String title, Component... content) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing("var(--lumo-space-s)");
    section.setId(id);
    section.addClassName("home-section");
    section.add(new H3(title));
    section.add(content);
    return section;
  }

  private String t(String key) {
    return getTranslation(KEY_PREFIX + key);
  }

  @Override
  public String getPageTitle() {
    return t("title");
  }
}
