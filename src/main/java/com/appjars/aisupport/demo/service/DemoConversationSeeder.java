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

import com.appjars.aisupport.business.service.LlmExchangeService;
import com.appjars.aisupport.business.service.MessageAttachmentService;
import com.appjars.aisupport.business.service.MessageService;
import com.appjars.aisupport.business.service.ParticipantService;
import com.appjars.aisupport.business.service.SessionService;
import com.appjars.aisupport.common.ContentStoreType;
import com.appjars.aisupport.common.ContentType;
import com.appjars.aisupport.common.SessionStatus;
import com.appjars.aisupport.common.SessionType;
import com.appjars.aisupport.demo.tools.ZephyrOneSupportTools;
import com.appjars.aisupport.model.AssistantDto;
import com.appjars.aisupport.model.LlmExchangeDto;
import com.appjars.aisupport.model.MessageAttachmentDto;
import com.appjars.aisupport.model.MessageDto;
import com.appjars.aisupport.model.ParticipantDto;
import com.appjars.aisupport.model.PromptDto;
import com.appjars.aisupport.model.SenderType;
import com.appjars.aisupport.model.SessionDto;
import com.appjars.aisupport.model.util.EmbeddingChunk;
import com.appjars.aisupport.model.util.OfferedTool;
import com.appjars.aisupport.model.util.ToolCall;
import com.appjars.service.HasLogger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Seeds a couple of pre-built conversations so the demo is not empty on first run and so the chat
 * visibility model and the LLM Inspector have something to show. Nothing here calls an LLM — every
 * message and exchange is hardcoded.
 *
 * <ul>
 * <li><b>Support conversation</b> — customer <b>Leo</b> asks the customer Support Assistant about
 * error E-27; the assistant answers (grounded on the Zephyr One guide), admin <b>Olivia</b>
 * steps in with a public follow-up, Leo attaches a CSV diagnostics log and a voice note (with its
 * transcript pre-filled) showcasing the chat's attachment types, then Olivia leaves an
 * <b>admin-only</b> internal note. A matching {@link LlmExchangeDto} carries the retrieved document
 * chunk, so the Inspector shows RAG was used.</li>
 * <li><b>General conversation</b> — customer <b>Sophia</b> chats with the General Assistant (no
 * documents), backed by a non-RAG exchange for contrast in the Inspector.</li>
 * </ul>
 *
 * Result: admins see both conversations (and the admin-only note); Leo sees only the support chat
 * (without the note); Sophia sees only the general chat.
 */
@Component
@RequiredArgsConstructor
public class DemoConversationSeeder implements HasLogger {

    private static final String CUSTOMER = "Leo";
    private static final String ADMIN = "Olivia";
    private static final String GENERAL_CUSTOMER = "Sophia";
    private static final String TOOL_CUSTOMER = "Maya";

    // Showcases tool calling: the Support Assistant resolves the request on its own, via
    // ZephyrOneSupportTools, with no human admin involved.
    private static final String COVERED_SERIAL = "ZO-88421-07";
    private static final String REPLACEMENT_PART_CODE = "BK-14";
    private static final String REPLACEMENT_ORDER_ID = "ORD-65790";

    // Showcases CSV attachments (2.2.0): the content is read as text, no file ever touches disk.
    private static final String DIAGNOSTICS_LOG_RESOURCE = "documents/zephyr-one-e27-log.csv";
    private static final String DIAGNOSTICS_LOG_NAME = "zephyr-one-e27-log.csv";

    // Showcases voice notes and audio transcription (2.2.0): a short recorded clip with its transcript.
    private static final String VOICE_NOTE_RESOURCE = "attachments/leo-voice-note.wav";
    private static final String VOICE_NOTE_NAME = "voice-note.wav";
    private static final String VOICE_NOTE_TRANSCRIPT =
        "It happened again this morning, same spot near the dock.";

    private final SessionService sessionService;
    private final MessageService messageService;
    private final ParticipantService participantService;
    private final LlmExchangeService llmExchangeService;
    private final MessageAttachmentService attachmentService;

    /** Seeds the demo conversations once, only if no session exists yet (so restarts are safe). */
    public void seed(AssistantDto generalAssistant, AssistantDto customerSupportAssistant) {
        if (!sessionService.findAll().isEmpty()) {
            return;
        }
        seedSupportConversation(customerSupportAssistant);
        seedGeneralConversation(generalAssistant);
        seedToolConversation(customerSupportAssistant);
        logger().info("Seeded demo conversations.");
    }

    private void seedSupportConversation(AssistantDto assistant) {
        Instant start = Instant.now().minus(Duration.ofHours(2)).truncatedTo(ChronoUnit.MILLIS);

        SessionDto session = createSession(CUSTOMER, "Zephyr One error E-27 — mower stopped mid-mow",
            assistant);
        participantService.saveAll(List.of(
            participant(session, CUSTOMER, false, false, start.minusSeconds(60)),
            participant(session, ADMIN, false, true, start.minusSeconds(60)),
            participant(session, assistant.getName(), true, true, start.minusSeconds(60))));

        MessageDto question = save(message(session, CUSTOMER, SenderType.USER, start,
            "My Zephyr One stopped mid-mow and shows **error E-27**. What does that mean and what should I do?")
                .processed(true).build());

        MessageDto answer = save(message(session, assistant.getName(), SenderType.AUTOMATIC,
            start.plusSeconds(5),
            """
                Sorry to hear that! **Error E-27** means the **boundary wire is disconnected**.

                Please check the boundary-wire connectors at the **HomeBase Z** dock and make sure they are \
                seated firmly, then press **Start** — the Zephyr One should resume. If E-27 keeps coming \
                back, contact Lumen Robotics support; this may be covered under your warranty.""")
                .processed(true).build());
        linkExchange(question, answer, supportExchange(session, answer, assistant));

        save(message(session, ADMIN, SenderType.ADMINISTRATOR, start.plusSeconds(300),
            """
                Hi Leo, Olivia from Lumen Robotics support here 👋. I've checked your details and you're \
                covered under warranty — I'm shipping a replacement connector today at no cost. You'll get \
                a tracking link by email shortly.""")
                .build());

        attachCsvLog(save(message(session, CUSTOMER, SenderType.USER, start.plusSeconds(360),
            "Here's the export from the mower's diagnostics panel, in case it helps.")
                .processed(true).build()));

        attachVoiceNote(save(message(session, CUSTOMER, SenderType.USER, start.plusSeconds(420),
            "(voice note)")
                .processed(true).build()));

        // Admin-only internal note: visible to admins, hidden from Leo. Mirrors the internal playbook.
        save(message(session, ADMIN, SenderType.ADMINISTRATOR, start.plusSeconds(360),
            "Internal: recurring E-27 points to a B12 boundary module. Opened RMA-2231, shipping " + "connector kit CK-27, logged a €40 goodwill credit.")
                .adminOnly(true).build());
    }

    private void seedGeneralConversation(AssistantDto assistant) {
        Instant start = Instant.now().minus(Duration.ofHours(1)).truncatedTo(ChronoUnit.MILLIS);

        SessionDto session = createSession(GENERAL_CUSTOMER, "Drafting a friendly out-of-office email",
            assistant);
        participantService.saveAll(List.of(
            participant(session, GENERAL_CUSTOMER, false, false, start.minusSeconds(60)),
            participant(session, assistant.getName(), true, true, start.minusSeconds(60))));

        MessageDto question = save(message(session, GENERAL_CUSTOMER, SenderType.USER, start,
            "Can you help me write a short, friendly out-of-office email for next week?")
                .processed(true).build());

        MessageDto answer = save(message(session, assistant.getName(), SenderType.AUTOMATIC,
            start.plusSeconds(4),
            """
                Of course! Here's a short, friendly draft:

                > **Subject:** Out of office
                >
                > Hi, thanks for your message! I'm out of the office until Monday with limited access to \
                email. I'll reply as soon as I'm back. For anything urgent, please contact my colleague.

                Want me to adjust the tone or the dates?""")
                .processed(true).build());
        linkExchange(question, answer, generalExchange(session, answer, assistant));
    }

    /**
     * A conversation the Support Assistant resolves entirely on its own by calling
     * {@link com.appjars.aisupport.demo.tools.ZephyrOneSupportTools}: no human admin steps in. The
     * matching {@link LlmExchangeDto} carries the offered tool definitions and the calls the model
     * made, so the Inspector shows tool usage distinct from the RAG and plain exchanges above.
     */
    private void seedToolConversation(AssistantDto assistant) {
        Instant start = Instant.now().minus(Duration.ofMinutes(30)).truncatedTo(ChronoUnit.MILLIS);

        SessionDto session = createSession(TOOL_CUSTOMER,
            "Warranty check and replacement blade kit for " + COVERED_SERIAL, assistant);
        participantService.saveAll(List.of(
            participant(session, TOOL_CUSTOMER, false, false, start.minusSeconds(60)),
            participant(session, assistant.getName(), true, true, start.minusSeconds(60))));

        MessageDto question = save(message(session, TOOL_CUSTOMER, SenderType.USER, start,
            "My Zephyr One's serial number is " + COVERED_SERIAL
                + ". Is it still under warranty, and can you send me a replacement blade kit ("
                + REPLACEMENT_PART_CODE + ")?")
                .processed(true).build());

        MessageDto answer = save(message(session, assistant.getName(), SenderType.AUTOMATIC,
            start.plusSeconds(6),
            """
                Good news — serial %s is covered under warranty until **2027-03-14**. I've placed an \
                order for replacement blade kit %s (order **%s**); it should arrive within 4 business \
                days. Let us know if you need anything else!"""
                .formatted(COVERED_SERIAL, REPLACEMENT_PART_CODE, REPLACEMENT_ORDER_ID))
                .processed(true).build());
        linkExchange(question, answer, toolExchange(session, answer, assistant));
    }

    /**
     * Persists the exchange and wires it up the way the LLM Inspector expects: the exchange's
     * messageId and the assistant answer's exchangeId both reference the answer, and the question
     * points at its answer, so the whole turn (question, prompt, LLM, RAG chunks) shows in the tree.
     */
    private void linkExchange(MessageDto question, MessageDto answer, LlmExchangeDto exchange) {
        Long exchangeId = llmExchangeService.save(exchange);
        answer.setExchangeId(exchangeId);
        save(answer);
        question.setAnsweredBy(answer);
        save(question);
    }

    // --- builders --------------------------------------------------------------------------------

    private SessionDto createSession(String owner, String summary, AssistantDto assistant) {
        SessionDto session = SessionDto.builder()
            .userId(owner)
            .summary(summary)
            .type(SessionType.MANUAL)
            .status(SessionStatus.ACTIVE)
            .assistant(assistant)
            .build();
        session.setId(sessionService.createSession(session));
        return session;
    }

    private ParticipantDto participant(SessionDto session, String userId, boolean isAssistant,
        boolean isAdmin, Instant joinDate) {
        return ParticipantDto.builder()
            .session(session)
            .userId(userId)
            .isAssistant(isAssistant)
            .isAdmin(isAdmin)
            .isActive(true)
            .joinDate(joinDate)
            // The participant converter dereferences lastAssistantSeen without a null check, so set it
            // to the session's assistant.
            .lastAssistantSeen(session.getAssistant())
            .build();
    }

    private MessageDto.MessageDtoBuilder message(SessionDto session, String userId,
        SenderType senderType, Instant eventDate, String content) {
        return MessageDto.builder()
            .session(session)
            .userId(userId)
            .senderType(senderType)
            .eventDate(eventDate)
            .content(content)
            // Seed a token count so the LLM Inspector shows a value instead of "Unknown".
            .contentTokenCount(estimateTokens(content));
    }

    /** Rough token estimate (~4 characters per token) for the seeded messages. */
    private int estimateTokens(String content) {
        return Math.max(1, content.length() / 4);
    }

    private MessageDto save(MessageDto message) {
        message.setId(messageService.save(message));
        return message;
    }

    /** Attaches the CSV diagnostics log to a message, read as plain text straight from the classpath. */
    private void attachCsvLog(MessageDto message) {
        String csv = readClasspathResourceAsString(DIAGNOSTICS_LOG_RESOURCE);
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
            .message(message)
            .type(ContentType.TEXT)
            .filename(DIAGNOSTICS_LOG_NAME)
            .mimeType("text/csv")
            .contentStoreType(ContentStoreType.PLAIN_TEXT)
            .textContent(csv)
            .kbSize((int) Math.ceil(csv.getBytes(StandardCharsets.UTF_8).length / 1024.0))
            .build();
        attachment.setId(attachmentService.save(attachment));
        message.setAttachments(Set.of(attachment));
    }

    /** Attaches the sample voice note, base64-encoded inline, with its transcript pre-filled. */
    private void attachVoiceNote(MessageDto message) {
        byte[] wav = readClasspathResourceAsBytes(VOICE_NOTE_RESOURCE);
        MessageAttachmentDto attachment = MessageAttachmentDto.builder()
            .message(message)
            .type(ContentType.AUDIO)
            .filename(VOICE_NOTE_NAME)
            .mimeType("audio/wav")
            .contentStoreType(ContentStoreType.BASE64)
            .textContent(Base64.getEncoder().encodeToString(wav))
            .kbSize((int) Math.ceil(wav.length / 1024.0))
            .transcript(VOICE_NOTE_TRANSCRIPT)
            .build();
        attachment.setId(attachmentService.save(attachment));
        message.setAttachments(Set.of(attachment));
    }

    private String readClasspathResourceAsString(String resource) {
        return new String(readClasspathResourceAsBytes(resource), StandardCharsets.UTF_8);
    }

    private byte[] readClasspathResourceAsBytes(String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load classpath resource " + resource, e);
        }
    }

    /** A RAG exchange whose retrieved chunk quotes the customer Zephyr One guide (shows RAG usage). */
    private LlmExchangeDto supportExchange(SessionDto session, MessageDto answer, AssistantDto a) {
        PromptDto prompt = a.getPrompt();
        String query = "Zephyr One error E-27 meaning and fix";
        EmbeddingChunk chunk = new EmbeddingChunk(
            "chunk-zephyr-e27", 0.86, "6", "zephyr-one-support", "Zephyr One Support Guide", null,
            null, "Product Knowledge Base",
            "E-27 — Boundary wire disconnected. Recommended action: check the boundary-wire connectors " + "at the HomeBase Z dock.",
            38, false, Map.of(query, 0.86), Map.of("section", "6. Error codes"));

        return baseExchange(session, answer, a)
            .promptTokenCount(prompt != null ? 90 : 0)
            .inputTokenCount(430)
            .outputTokenCount(150)
            .request(
                "[{\"type\":\"SYSTEM\",\"text\":" + jsonString(prompt != null ?
                    prompt.getContent() :
                    "") + "},{\"type\":\"USER\",\"text\":\"" + query + "\\n\\nContext: E-27 means the boundary wire is disconnected; check the HomeBase Z " + "connectors.\"}]")
            .response(
                "{\"type\":\"AI\",\"text\":\"Error E-27 means the boundary wire is disconnected. Check " + "the connectors at the HomeBase Z dock and press Start.\"}")
            .ragQueries(List.of(query))
            .ragRequestContent(query)
            .ragInputTokenCount(18)
            .ragOutputTokenCount(0)
            .ragTimeMs(320L)
            .embeddingChunks(List.of(chunk))
            .build();
    }

    /** A plain (non-RAG) exchange for the general assistant — no retrieved chunks, for contrast. */
    private LlmExchangeDto generalExchange(SessionDto session, MessageDto answer, AssistantDto a) {
        PromptDto prompt = a.getPrompt();
        return baseExchange(session, answer, a)
            .promptTokenCount(prompt != null ? 40 : 0)
            .inputTokenCount(60)
            .outputTokenCount(80)
            .request("[{\"type\":\"USER\",\"text\":\"Write a short friendly out-of-office email.\"}]")
            .response("{\"type\":\"AI\",\"text\":\"Subject: Out of office ...\"}")
            .build();
    }

    /**
     * A tool-driven exchange: the model is offered both {@link ZephyrOneSupportTools} tools and
     * calls each once before answering, so the Inspector shows what each call cost and returned.
     */
    private LlmExchangeDto toolExchange(SessionDto session, MessageDto answer, AssistantDto a) {
        PromptDto prompt = a.getPrompt();
        OfferedTool checkWarranty = new OfferedTool("checkWarrantyStatus", """
            {"name":"checkWarrantyStatus","description":"Checks whether a Zephyr One unit, identified \
            by its serial number, is still under warranty","parameters":{"type":"object","properties":\
            {"serialNumber":{"type":"string"}},"required":["serialNumber"]}}""", 32);
        OfferedTool createOrder = new OfferedTool("createReplacementOrder", """
            {"name":"createReplacementOrder","description":"Places a replacement part order for a \
            Zephyr One unit and returns the order id","parameters":{"type":"object","properties":\
            {"serialNumber":{"type":"string"},"partCode":{"type":"string"}},\
            "required":["serialNumber","partCode"]}}""", 38);

        ToolCall warrantyCall = new ToolCall(1, "checkWarrantyStatus",
            "{\"serialNumber\":\"" + COVERED_SERIAL + "\"}",
            "{\"serialNumber\":\"" + COVERED_SERIAL + "\",\"covered\":true,\"expiresOn\":\"2027-03-14\"}",
            false, 180L, 22, 14, false);
        ToolCall orderCall = new ToolCall(2, "createReplacementOrder",
            "{\"serialNumber\":\"" + COVERED_SERIAL + "\",\"partCode\":\"" + REPLACEMENT_PART_CODE + "\"}",
            "{\"orderId\":\"" + REPLACEMENT_ORDER_ID + "\",\"partCode\":\"" + REPLACEMENT_PART_CODE
                + "\",\"estimatedDeliveryDays\":4}",
            false, 240L, 28, 18, false);

        return baseExchange(session, answer, a)
            .promptTokenCount(prompt != null ? 90 : 0)
            .inputTokenCount(210)
            .outputTokenCount(95)
            .request("[{\"type\":\"USER\",\"text\":\"Is " + COVERED_SERIAL
                + " under warranty, and can you send a replacement " + REPLACEMENT_PART_CODE + "?\"}]")
            .response("{\"type\":\"AI\",\"text\":\"Serial " + COVERED_SERIAL
                + " is covered; order " + REPLACEMENT_ORDER_ID + " placed for " + REPLACEMENT_PART_CODE
                + ".\"}")
            .offeredTools(List.of(checkWarranty, createOrder))
            .toolCalls(List.of(warrantyCall, orderCall))
            // Each definition travels on both model calls; each call's result travels on the one
            // model call after it.
            .toolInputTokenCount((checkWarranty.inputTokenCount() + createOrder.inputTokenCount()) * 2
                + warrantyCall.inputTokenCount() + orderCall.inputTokenCount())
            .toolOutputTokenCount(warrantyCall.outputTokenCount() + orderCall.outputTokenCount())
            .modelCallCount(2)
            .build();
    }

    /** Shared snapshot fields (assistant / LLM / prompt / timing) for a seeded exchange. */
    private LlmExchangeDto.LlmExchangeDtoBuilder baseExchange(SessionDto session, MessageDto answer,
        AssistantDto a) {
        Instant when = answer.getEventDate();
        return LlmExchangeDto.builder()
            .sessionId(session.getId())
            .messageId(answer.getId())
            .assistantId(a.getId())
            .assistantName(a.getName())
            .advancedRag(false)
            .categoryNames(a.getCategories() == null ?
                List.of() :
                a.getCategories().stream().map(c -> c.getName()).toList())
            .llmId(a.getLlm().getId())
            .llmName(a.getLlm().getName())
            .llmType(a.getLlm().getType())
            .llmContent(a.getLlm().getConfiguration())
            .promptId(a.getPrompt() != null ? a.getPrompt().getId() : null)
            .promptName(a.getPrompt() != null ? a.getPrompt().getName() : null)
            .promptContent(a.getPrompt() != null ? a.getPrompt().getContent() : null)
            .requestDate(when.plusSeconds(1))
            .responseDate(when.plusSeconds(4));
    }

    /** Minimal JSON string escaping for embedding prompt content in the seeded request payload. */
    private String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "") + "\"";
    }
}
