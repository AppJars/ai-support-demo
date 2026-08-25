# Zephyr One — Internal Support Playbook

**Audience:** Lumen Robotics support agents (internal only)
**Product:** Zephyr One robotic lawn mower
**Document owner:** Lumen Robotics Support Operations

> This is a **fictional** internal playbook created for the AI Support demo. It intentionally contains
> different, agent-facing facts than the customer knowledge base, so the two support assistants ground
> their answers on different documents and reply differently to the same question.

---

## 1. Escalation tiers

| Tier | Handled by | Target first response | Scope |
|---|---|---|---|
| **T1** | Front-line agent | 2 hours | FAQs, resets, error-code guidance, schedule help |
| **T2** | Senior agent | 4 hours | Repeat faults, firmware issues, warranty claims |
| **T3** | Support Lead (Olivia) | 1 business day | RMA approvals, goodwill above limit, safety reports |

Always resolve at the lowest possible tier. Escalate to **T3** immediately for any report of fire,
smoke, or injury.

## 2. RMA policy (internal)

- Open an RMA only after a **T2** diagnosis confirms a hardware fault.
- RMA ticket format: `RMA-XXXX` (4-digit sequence).
- Agents may approve a replacement part up to **€60** without sign-off.
- Replacements between **€60 and €250** require **Support Lead** approval.
- Anything above **€250** (e.g. a full unit swap) requires **Operations Manager** approval.

## 3. Goodwill credits

- Front-line agents may issue a goodwill credit of up to **€40** per case.
- Credits above €40 require Support Lead approval and a note in the ticket.
- Never offer goodwill before confirming the customer is inside the return or warranty window.

## 4. Known hardware issues

| Batch | Symptom | Root cause | Fix |
|---|---|---|---|
| **B12 boundary module** | Recurring **E-27** (boundary wire disconnected) | Connector corrosion on early B12 boundary modules | Replace with connector kit **CK-27**; log `RMA-XXXX`; offer €40 goodwill |
| **PowerCell L2 (pre-2023)** | Premature **E-40** (low battery) after ~30 min | Cell balancing firmware defect | Push **ZephyrOS 3.2**; if it persists, RMA the battery |

The B12 boundary-module issue is the single most common escalation. If a customer reports E-27 more
than once, assume a B12 unit and go straight to the **CK-27** replacement path.

## 5. Internal error-code notes

| Code | Internal note |
|---|---|
| **E-27** | If it recurs, it is almost always the B12 boundary module (see §4), not customer wiring. Ship **CK-27**. |
| **E-40** | Check the PowerCell L2 batch before advising a battery RMA; a firmware push often resolves it. |
| **E-61** | Usually a dock power-supply fault; ship dock PSU **PSU-Z** rather than a full HomeBase Z. |

## 6. Internal contacts

- **Firmware team:** `#zephyr-firmware` (for suspected ZephyrOS defects).
- **Logistics / RMA fulfilment:** `logistics@lumen.example` (dispatch parts, track RMAs).
- **Support Lead on call:** Olivia (T3 approvals, safety escalations).

## 7. Tone and disclosure

- This playbook is **internal**. Never quote batch numbers, root-cause details, or approval limits to
  customers.
- To customers, describe an E-27 fix as "replacing a connector under warranty", not "a known B12 defect".
