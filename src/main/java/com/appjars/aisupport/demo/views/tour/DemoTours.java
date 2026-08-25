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
package com.appjars.aisupport.demo.views.tour;

import com.appjars.aisupport.flow.util.TestIds;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.vaadin.addons.antlerflow.tour.EngineType;
import org.vaadin.addons.antlerflow.tour.Tour;
import org.vaadin.addons.antlerflow.tour.TourButton;
import org.vaadin.addons.antlerflow.tour.TourButtonType;
import org.vaadin.addons.antlerflow.tour.TourStep;

/**
 * Factory of the guided tours offered by the demo.
 *
 * <p>Anchored steps target a {@code data-antler-target} marker instead of their real selector: a
 * plain selector also matches hidden, zero-size duplicates (the chat bubble, closed dialogs) and the
 * step would anchor to one of those.
 */
public final class DemoTours {

  /** Session attribute used to start a tour after navigating (and logging in) to its view. */
  public static final String PENDING_TOUR_ATTRIBUTE = DemoTours.class.getName() + ".pendingTour";

  static final String KEY_PREFIX = "appjars.aisupport.demo.tour.";

  /** Attribute the resolver puts on the first visible element matching a step's real selector. */
  private static final String TARGET_ATTR = "data-antler-target";

  /**
   * Tags the first <em>visible</em> element matching each step's selector with {@code TARGET_ATTR}
   * (so the tour popover anchors to it and never to a hidden zero-size duplicate) and keeps the tags
   * in sync as the view renders. {@code $0} is a JSON map of {@code stepId -> css selector}.
   */
  private static final String RESOLVE_TARGETS_JS =
    """
      const MAP = JSON.parse($0);
      const ATTR = 'data-antler-target';
      const resolve = () => {
        Object.keys(MAP).forEach(id => {
          let pick = null;
          for (const el of document.querySelectorAll(MAP[id])) {
            const r = el.getBoundingClientRect();
            if (r.width > 4 && r.height > 4) { pick = el; break; }
          }
          document.querySelectorAll("[" + ATTR + "='" + id + "']")
              .forEach(el => { if (el !== pick) { el.removeAttribute(ATTR); } });
          if (pick && pick.getAttribute(ATTR) !== id) { pick.setAttribute(ATTR, id); }
        });
      };
      if (window.__antlerResolver) { window.__antlerResolver.stop(); }
      let scheduled = false;
      const schedule = () => {
        if (scheduled) { return; }
        scheduled = true;
        requestAnimationFrame(() => { scheduled = false; resolve(); });
      };
      resolve();
      const obs = new MutationObserver(schedule);
      obs.observe(document.body,
          {childList: true, subtree: true, attributes: true, attributeFilter: ['hidden', 'style', 'class']});
      window.__antlerResolver = {
        stop() {
          obs.disconnect();
          document.querySelectorAll('[' + ATTR + ']').forEach(el => el.removeAttribute(ATTR));
          window.__antlerResolver = null;
        }
      };
      """;

  /**
   * Lifts the Driver.js popover into the browser <em>top layer</em> for the chat-bubble tour. The
   * bubble is a {@code vaadin-popover}, and in Vaadin&nbsp;25 every overlay opens via the native
   * Popover API ({@code popover="manual"} + {@code showPopover()}), so it paints in the top layer
   * <em>above all z-indexed content</em> — the tour popover (a plain fixed-position element) would
   * otherwise be hidden behind the chat window no matter its placement or z-index. Promoting the
   * Driver popover to a manual popover puts it in the same top layer; because top-layer order follows
   * the last {@code showPopover()} call, a listener re-asserts the tour popover on top whenever the
   * chat window opens after the tour has started. Its {@code margin} is reset so the native
   * {@code :popover-open} centering ({@code margin:auto}) does not fight Driver's own inset styles.
   *
   * <p>The chat window can also be dragged and it slides into place with a transform when it opens.
   * Driver.js re-anchors the popover on scroll/resize but not on a plain transform move, so the step
   * would lag behind a moved window. A {@code requestAnimationFrame} loop dispatches a {@code resize}
   * event, which drives Driver to recompute the popover against the target's live position — keeping
   * it glued to its control as the window animates open or is dragged.
   */
  private static final String PROMOTE_TOP_LAYER_JS =
    """
      if (window.__demoTourTopLayer) { window.__demoTourTopLayer.stop(); }
      const promote = () => document.querySelectorAll('.driver-popover').forEach(el => {
        if (el.getAttribute('popover') !== 'manual') { el.setAttribute('popover', 'manual'); }
        el.style.margin = '0';
        try { if (!el.matches(':popover-open')) { el.showPopover(); } } catch (e) {}
      });
      // Re-enter the top layer last so we stack above a chat window that opened after us.
      const reassert = () => {
        const el = document.querySelector('.driver-popover');
        if (el && el.matches(':popover-open')) { try { el.hidePopover(); el.showPopover(); } catch (e) {} }
      };
      const onToggle = (e) => {
        const t = e.target;
        if (e.newState === 'open' && t && t.classList && !t.classList.contains('driver-popover')) {
          reassert();
        }
      };
      document.addEventListener('toggle', onToggle, true);
      const obs = new MutationObserver(promote);
      obs.observe(document.body, {childList: true, subtree: true});
      promote();
      // Keep the step anchored while the window animates open or is dragged (transform moves fire no
      // resize event, so nudge Driver to recompute against the target's current position).
      let frame = 0;
      const tick = () => { window.dispatchEvent(new Event('resize')); frame = requestAnimationFrame(tick); };
      frame = requestAnimationFrame(tick);
      window.__demoTourTopLayer = {
        stop() {
          cancelAnimationFrame(frame);
          obs.disconnect();
          document.removeEventListener('toggle', onToggle, true);
          document.querySelectorAll('.driver-popover[popover]').forEach(el => {
            try { el.hidePopover(); } catch (e) {}
            el.removeAttribute('popover');
          });
          window.__demoTourTopLayer = null;
        }
      };
      """;

  /**
   * Neutralizes a Driver.js rule that hides content around the highlighted element. Driver tags the
   * step target with {@code .driver-active-element}, and its stylesheet forces {@code overflow:hidden}
   * on that element's parent ({@code :not(body):has(> .driver-active-element)}). The message field and
   * the send button are siblings under one container, so when the send button is the step target that
   * clips the whole input row and the field vanishes. This override (higher specificity so it beats
   * Driver's {@code !important}) restores {@code overflow:visible} on those parents; it only matches
   * while a step is active, so it is inert once the tour ends. Removed again in {@link #STOP_JS}.
   */
  private static final String TOUR_CSS_JS =
    """
      if (!document.getElementById('demo-tour-css')) {
        const style = document.createElement('style');
        style.id = 'demo-tour-css';
        style.textContent =
            'body :not(body):has(> .driver-active-element) { overflow: visible !important; }';
        document.head.appendChild(style);
      }
      """;

  private static final String STOP_JS =
    "if (window.__antlerResolver) { window.__antlerResolver.stop(); }" + " if (window.__demoTourTopLayer) { window.__demoTourTopLayer.stop(); }" + " document.getElementById('demo-tour-css')?.remove();";

  public enum DemoTour {
    HOME, CHAT, ASSISTANTS, PROMPTS, LLMS, DOCUMENTS, CATEGORIES, INSPECTOR, CHANNELS, BUBBLE
  }


  /**
   * One tour step. {@code selector} is the <em>real</em> element selector (resolved to the first
   * visible match); {@code null} makes the step a centered/floating one.
   */
  private record StepDef(String key, String selector, String position, boolean first, boolean last) {
  }

  private DemoTours() {
  }

  /** Wraps a library {@link TestIds} value in the {@code data-testid} attribute selector steps use. */
  private static String testId(String id) {
    return "[data-testid='" + id + "']";
  }

  private static List<StepDef> steps(DemoTour tour) {
    return switch (tour) {
      // Section ids set by HomeView; unique and always laid out on the landing page.
      case HOME -> List.of(
        new StepDef("home.welcome", "#home-hero", "bottom", true, false),
        new StepDef("home.features", "#home-features", "top", false, false),
        new StepDef("home.tryit", "#home-tryit", "top", false, false),
        new StepDef("home.license", "#home-license", "top", false, true)
      );
      case CHAT -> List.of(
        new StepDef("chat.intro", null, null, true, false),
        new StepDef("chat.sessions", testId(TestIds.CHAT_SESSIONS_GRID), "right", false, false),
        new StepDef("chat.newsession", testId(TestIds.CHAT_NEW_SESSION_BUTTON), "bottom", false, false),
        new StepDef("chat.search", testId(TestIds.CHAT_SESSION_SEARCH_BUTTON), "bottom", false, false),
        new StepDef("chat.filter", testId(TestIds.CHAT_SESSION_FILTER_BUTTON), "bottom", false, false),
        new StepDef("chat.assistants", testId(TestIds.CHAT_ASSISTANT_SELECTOR), "bottom", false, false),
        new StepDef("chat.input", testId(TestIds.MESSAGE_INPUT), "top", false, false),
        new StepDef("chat.send", testId(TestIds.MESSAGE_BUTTON), "top", false, false),
        new StepDef("chat.rag", null, null, false, true)
      );
      case ASSISTANTS -> List.of(
        new StepDef("assistants.intro", null, null, true, false),
        new StepDef("assistants.grid", testId(TestIds.ASSISTANTS_GRID), "top", false, false),
        new StepDef("assistants.create", testId(TestIds.ASSISTANTS_NEW_BUTTON), "bottom", false, false),
        new StepDef("assistants.default", null, null, false, true)
      );
      case PROMPTS -> List.of(
        new StepDef("prompts.intro", null, null, true, false),
        new StepDef("prompts.grid", testId(TestIds.PROMPTS_GRID), "top", false, false),
        new StepDef("prompts.create", testId(TestIds.PROMPTS_NEW_BUTTON), "bottom", false, false),
        new StepDef("prompts.finish", null, null, false, true)
      );
      case LLMS -> List.of(
        new StepDef("llms.intro", null, null, true, false),
        new StepDef("llms.grid", testId(TestIds.LLMS_GRID), "top", false, false),
        new StepDef("llms.create", testId(TestIds.LLMS_NEW_BUTTON), "bottom", false, false),
        new StepDef("llms.finish", null, null, false, true)
      );
      case DOCUMENTS -> List.of(
        new StepDef("documents.intro", null, null, true, false),
        new StepDef("documents.grid", testId(TestIds.DOCUMENTS_GRID), "top", false, false),
        new StepDef("documents.upload", testId(TestIds.DOCUMENTS_NEW_BUTTON), "bottom", false, false),
        new StepDef("documents.categories", null, null, false, true)
      );
      case CATEGORIES -> List.of(
        new StepDef("categories.intro", null, null, true, false),
        new StepDef("categories.grid", testId(TestIds.CATEGORIES_GRID), "top", false, false),
        new StepDef("categories.create", testId(TestIds.CATEGORIES_NEW_BUTTON), "bottom", false, false),
        new StepDef("categories.finish", null, null, false, true)
      );
      case INSPECTOR -> List.of(
        new StepDef("inspector.intro", null, null, true, false),
        new StepDef("inspector.grid", testId(TestIds.INSPECTOR_SESSION_GRID), "left", false, false),
        new StepDef("inspector.tree", testId(TestIds.INSPECTOR_MESSAGE_TREE), "top", false, false),
        new StepDef("inspector.details", null, null, false, true)
      );
      case CHANNELS -> List.of(
        new StepDef("channels.intro", null, null, true, false),
        new StepDef("channels.grid", testId(TestIds.CHANNELS_GRID), "top", false, false),
        new StepDef("channels.create", testId(TestIds.CHANNELS_NEW_BUTTON), "bottom", false, false),
        new StepDef("channels.docs", null, null, false, true)
      );
      // The bubble is opened (server-side) at the start of this tour; the intro is a centered step
      // that gives the window time to open. The window is a vaadin-popover in the browser top layer,
      // so PROMOTE_TOP_LAYER_JS lifts the tour popover into the same layer (see start) — that lets
      // the following steps anchor to the controls inside the window without being hidden behind it.
      case BUBBLE -> List.of(
        new StepDef("bubble.intro", null, null, true, false),
        new StepDef("bubble.sessions", testId(TestIds.CHAT_BUBBLE_SESSION_SELECT), "bottom", false, false),
        new StepDef("bubble.input", testId(TestIds.MESSAGE_INPUT), "top", false, false),
        new StepDef("bubble.send", testId(TestIds.MESSAGE_BUTTON), "top", false, false),
        new StepDef("bubble.move", null, null, false, true)
      );
    };
  }

  public static Tour create(DemoTour tour, SerializableFunction<String, String> translator) {
    List<TourStep> tourSteps = steps(tour).stream().map(def -> toStep(def, translator)).toList();
    return Tour.builder()
      // The guided tours run on the Driver.js engine.
      .engineType(EngineType.DRIVER)
      .steps(tourSteps)
      .showCancelButton(true)
      .allowClose(true)
      .build();
  }

  /**
   * Creates the tour, attaches it to {@code host}, tags the visible targets and starts it — cleaning
   * everything up once the tour is completed or canceled.
   */
  public static void start(DemoTour tour, Component host,
    SerializableFunction<String, String> translator) {
    start(tour, host, translator, () -> {
    }, () -> {
    });
  }

  /**
   * Same as {@link #start(DemoTour, Component, SerializableFunction)}, but runs {@code onStart} right
   * after the tour starts and {@code onStop} when it is completed or canceled. Used by the chat-bubble
   * tour to open the floating assistant while the tour runs and close it again afterwards.
   */
  public static void start(DemoTour tour, Component host,
    SerializableFunction<String, String> translator, Runnable onStart, Runnable onStop) {
    List<StepDef> defs = steps(tour);
    Tour t = create(tour, translator);
    host.getElement().appendChild(t.getElement());
    host.getElement().executeJs(TOUR_CSS_JS);
    host.getElement().executeJs(RESOLVE_TARGETS_JS, targetJson(defs));
    t.addTourCompletedListener(e -> stop(t, host, onStop));
    t.addTourCanceledListener(e -> stop(t, host, onStop));
    t.start();
    // The bubble tour opens a top-layer chat window; lift the tour popover into that layer too so it
    // is never hidden behind the window. Injected before onStart so the listener is armed when the
    // window opens.
    if (tour == DemoTour.BUBBLE) {
      host.getElement().executeJs(PROMOTE_TOP_LAYER_JS);
    }
    onStart.run();
  }

  private static void stop(Tour t, Component host, Runnable onStop) {
    onStop.run();
    host.getElement().executeJs(STOP_JS);
    t.getElement().removeFromParent();
  }

  private static TourStep toStep(StepDef def, SerializableFunction<String, String> t) {
    String id = def.key().replace('.', '-');
    String attachTo = def.selector() == null ? null : "[" + TARGET_ATTR + "='" + id + "']";

    List<TourButton> buttons = new ArrayList<>();
    if (!def.first()) {
      buttons.add(TourButton.builder().label(t.apply(KEY_PREFIX + "btn.back")).secondary(true)
        .type(TourButtonType.PREVIOUS).build()
      );
    }
    buttons.add(TourButton.builder()
      .label(t.apply(KEY_PREFIX + (def.last() ? "btn.done" : "btn.next")))
      .type(TourButtonType.NEXT).build()
    );

    return TourStep.builder().id(id).attachTo(attachTo).position(def.position())
      .title(t.apply(KEY_PREFIX + def.key() + ".title"))
      .content(t.apply(KEY_PREFIX + def.key() + ".desc")).buttons(buttons).build();
  }

  /** Builds the {@code {stepId: selector}} JSON map for the anchored steps of a tour. */
  private static String targetJson(List<StepDef> defs) {
    return defs.stream().filter(def -> def.selector() != null)
      .map(def -> "\"" + def.key().replace('.', '-') + "\":\"" + def.selector() + "\"")
      .collect(Collectors.joining(",", "{", "}")
      );
  }
}
