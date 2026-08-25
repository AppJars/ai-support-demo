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
package com.appjars.aisupport.demo;

import com.appjars.AppJarsAutoConfiguration;
import com.appjars.aisupport.AiSupportConfiguration;
import com.appjars.aisupport.demo.utils.ThemeSelector;
import com.appjars.aisupport.demo.views.layouts.MainLayout;
import com.appjars.aisupport.flow.util.RouteConfigurer;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Inline.Wrapping;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.lumo.Lumo;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@Push
@ComponentScan(basePackageClasses = {AiSupportConfiguration.class, AppJarsAutoConfiguration.class})
@EnableJpaRepositories(basePackageClasses = AiSupportConfiguration.class)
// Comment out Lumo to use Aura
// @StyleSheet(Aura.STYLESHEET)
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
@PWA(
  name = "AI Support Demo",
  shortName = "AI Support Demo",
  description = "A demonstration of the AI Support AppJar module in Vaadin 25."
)
@EnableScheduling
public class Application extends SpringBootServletInitializer implements AppShellConfigurator {

  final RouteConfigurer routeConfigurer;

  public Application(RouteConfigurer routeConfigurer) {
    this.routeConfigurer = routeConfigurer;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @PostConstruct
  public void configure() {
    routeConfigurer.setViewsRouterLayout(MainLayout.class);
  }

  @Override
  public void configurePage(AppShellSettings settings) {
    // Use the AppJars icon as the browser-tab favicon.
    settings.addFavIcon("icon", "icons/icon.png", "180x180");
    // Prepended to the head so the stored theme is applied before the first paint.
    settings.addInlineWithContents(TargetElement.HEAD, Position.PREPEND,
      ThemeSelector.BOOTSTRAP_SCRIPT, Wrapping.JAVASCRIPT);
  }
}
