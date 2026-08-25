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
package com.appjars.aisupport.demo.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.function.SerializableFunction;

/** Navbar control that switches the color scheme between system, light and dark. */
public class ThemeSelector extends MenuBar {

  /**
   * Applies the stored choice before the first paint, so the page never flashes the wrong scheme.
   * Mirrors what {@code Page.setColorScheme} does, which only runs once the UI has attached.
   * Registered from {@code Application.configurePage}.
   */
  public static final String BOOTSTRAP_SCRIPT =
    """
      (() => {
        const stored = localStorage.getItem('theme');
        const scheme = stored === 'dark' || stored === 'light' ? stored : 'light dark';
        document.documentElement.setAttribute('theme', scheme.replace(' ', '-'));
        document.documentElement.style.colorScheme = scheme;
      })();
      """;

  private static final String KEY_PREFIX = "appjars.aisupport.demo.layout.theme.";

  public ThemeSelector(SerializableFunction<String, String> translator) {
    addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
    String label = translator.apply(KEY_PREFIX + "label");

    var trigger = addItem(VaadinIcon.ADJUST.create());
    trigger.setAriaLabel(label);
    Tooltip.forComponent(this).setText(label);

    var schemes = trigger.getSubMenu();
    schemes.addItem(item(translator, "system", VaadinIcon.DESKTOP.create()),
      e -> apply(ColorScheme.Value.SYSTEM));
    schemes.addItem(item(translator, "light", VaadinIcon.SUN_O.create()),
      e -> apply(ColorScheme.Value.LIGHT));
    schemes.addItem(item(translator, "dark", VaadinIcon.MOON_O.create()),
      e -> apply(ColorScheme.Value.DARK));
  }

  private Component item(SerializableFunction<String, String> translator, String key,
    AbstractIcon<?> icon) {
    return Utils.menuItem(translator.apply(KEY_PREFIX + key), icon);
  }

  // SYSTEM resolves to the CSS "light dark" pair, so the browser follows the OS setting on its own
  // and only an explicit choice is worth storing.
  private void apply(ColorScheme.Value scheme) {
    getUI().ifPresent(ui -> {
      ui.getPage().setColorScheme(scheme);
      ui.getPage().executeJs(
        """
          if ($0) {
            localStorage.setItem('theme', $0);
          } else {
            localStorage.removeItem('theme');
          }
          """,
        scheme == ColorScheme.Value.SYSTEM ? null : scheme.getValue());
    });
  }
}
