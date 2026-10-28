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
package com.appjars.aisupport.demo.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/**
 * Fictional Lumen Robotics back-office operations for the Zephyr One lawn mower, exposed as tools
 * so the Support Assistant can resolve a warranty and parts request without a human admin. Backed
 * by a small hardcoded catalog; nothing here calls a real system.
 */
@Component
public class ZephyrOneSupportTools {

  private static final String COVERED_SERIAL = "ZO-88421-07";

  @Tool("Checks whether a Zephyr One unit, identified by its serial number, is still under warranty")
  public String checkWarrantyStatus(@P("the unit's serial number, e.g. ZO-88421-07") String serialNumber) {
    if (COVERED_SERIAL.equals(serialNumber)) {
      return "{\"serialNumber\":\"" + serialNumber + "\",\"covered\":true,\"expiresOn\":\"2027-03-14\"}";
    }
    return "{\"serialNumber\":\"" + serialNumber + "\",\"covered\":false}";
  }

  @Tool("Places a replacement part order for a Zephyr One unit and returns the order id")
  public String createReplacementOrder(
      @P("the unit's serial number") String serialNumber,
      @P("the part code to ship, e.g. BK-14") String partCode) {
    String orderId = "ORD-" + Math.abs((serialNumber + partCode).hashCode() % 90000 + 10000);
    return "{\"orderId\":\"" + orderId + "\",\"partCode\":\"" + partCode
        + "\",\"estimatedDeliveryDays\":4}";
  }
}
