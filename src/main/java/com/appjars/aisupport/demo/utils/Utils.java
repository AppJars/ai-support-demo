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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;

public final class Utils {

    private Utils() {
    }

    public static Div divider() {
        return divider(false);
    }

    public static Div divider(boolean vertical) {
        Div divider = new Div();
        if (vertical) {
            divider.getStyle()
                .setWidth("1px")
                .setHeight("100%")
                .set("border-right", "1px solid var(--lumo-contrast-10pct)");
        } else {
            divider.getStyle()
                .setWidth("100%")
                .setHeight("1px")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        }
        return divider;
    }

    public static Div menuItem(String title, AbstractIcon<?> icon) {
        Div menuItem = new Div();
        Span titleSpan = new Span(title);

        menuItem.getStyle()
            .set("display", "flex")
            .set("align-items", "center")
            .set("gap", "var(--lumo-space-s)");

        titleSpan.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("font-weight", "500")
            .set("flex", "1");

        icon.setSize("var(--lumo-icon-size-s)");
        icon.getStyle().set("opacity", "0.85");

        menuItem.add(icon, titleSpan);
        return menuItem;
    }

}
