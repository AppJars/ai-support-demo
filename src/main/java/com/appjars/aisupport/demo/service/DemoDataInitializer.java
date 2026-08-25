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

import com.appjars.aisupport.business.service.AssistantService;
import com.appjars.aisupport.business.service.CategoryService;
import com.appjars.aisupport.business.service.DocumentService;
import com.appjars.aisupport.business.service.LlmService;
import com.appjars.aisupport.business.service.PromptService;
import com.appjars.aisupport.common.LlmType;
import com.appjars.aisupport.model.AssistantDto;
import com.appjars.aisupport.model.CategoryDto;
import com.appjars.aisupport.model.DocumentDto;
import com.appjars.aisupport.model.LlmDto;
import com.appjars.aisupport.model.PromptDto;
import com.appjars.aisupport.model.filters.AssistantFilter;
import com.appjars.aisupport.model.filters.DocumentFilter;
import com.appjars.service.HasLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Seeds the demo data on first startup so the app is usable and compelling immediately. It creates a
 * small but complete catalog:
 * <ul>
 * <li>two <b>LLMs</b>: a working local Ollama model and a disabled <b>OpenAI</b> placeholder the
 * evaluator can complete with their own key;</li>
 * <li>three <b>prompts</b> written in Markdown, each with a couple of revisions to showcase the
 * revision history;</li>
 * <li>two knowledge-base <b>categories</b>, each with its own fictional <b>document</b>;</li>
 * <li>three <b>assistants</b>: a General Assistant (plain chat) and two document-grounded support
 * assistants — one customer-facing, one internal — bound to different categories so their
 * answers differ.</li>
 * </ul>
 * Once the catalog exists, {@link DemoConversationSeeder} adds a couple of pre-built conversations
 * and a matching LLM Inspector exchange. Every entity is created only if missing, so restarts are
 * safe. No LLM is ever called to produce the data (only the documents are embedded at startup).
 */
@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements HasLogger {

    // LLMs -------------------------------------------------------------------------------------
    private static final String LLM_NAME = "llama3.2:3b";
    private static final String LLM_CONFIGURATION =
        "{\"host\":\"http://localhost\",\"port\":\"11434\",\"modelName\":\"llama3.2:3b\"}";

    private static final String OPENAI_LLM_NAME = "OpenAI (add your key)";
    // Disabled placeholder: the evaluator fills in the key and model to switch the demo to OpenAI.
    private static final String OPENAI_CONFIGURATION =
        "{\"key\":\"<YOUR_API_KEY>\",\"modelName\":\"<YOUR_OPENAI_MODEL>\",\"url\":\"https://api.openai.com/v1\"}";

    // Knowledge base ---------------------------------------------------------------------------
    private static final String CUSTOMER_CATEGORY_NAME = "Product Knowledge Base";
    private static final String CUSTOMER_DOCUMENT_NAME = "Zephyr One Support Guide";
    private static final String CUSTOMER_DOCUMENT_RESOURCE = "documents/zephyr-one-support.md";

    private static final String INTERNAL_CATEGORY_NAME = "Internal Support Playbook";
    private static final String INTERNAL_DOCUMENT_NAME = "Zephyr One Internal Playbook";
    private static final String INTERNAL_DOCUMENT_RESOURCE = "documents/zephyr-one-internal-playbook.md";

    // Prompts are written in Markdown (it renders in the editor and in chat) and seeded with a second
    // revision so the revision history has something to show.
    private static final String GENERAL_PROMPT_NAME = "General Prompt";
    private static final String GENERAL_PROMPT_V1 = """
        You are a friendly, general-purpose AI assistant.

        - Answer clearly and concisely.
        - Use **Markdown** (lists, headings, `code`) when it makes the answer easier to read.""";
    private static final String GENERAL_PROMPT_V2 = GENERAL_PROMPT_V1 + """


        - If you are not sure about something, say so instead of guessing.""";

    private static final String CUSTOMER_PROMPT_NAME = "Support Prompt — Customer";
    private static final String CUSTOMER_PROMPT_V1 = """
        ## Role
        You are the customer-support assistant for the **Zephyr One** robotic lawn mower by **Lumen Robotics**.

        ## Rules
        - Answer using **only** the information in the retrieved documents.
        - If the answer is not in those documents, say you don't have that information and suggest
          contacting Lumen Robotics support.
        - Be concise and precise, and mention the specific figure, code or step when relevant.""";
    private static final String CUSTOMER_PROMPT_V2 = CUSTOMER_PROMPT_V1 + """


        - Greet the customer and keep a warm, reassuring tone.""";

    private static final String INTERNAL_PROMPT_NAME = "Support Prompt — Internal";
    private static final String INTERNAL_PROMPT_V1 = """
        ## Role
        You are the **internal support assistant** for Lumen Robotics agents handling Zephyr One cases.

        ## Rules
        - Answer using **only** the internal support playbook provided.
        - Give the agent the exact escalation tier, RMA rule, part code or approval limit that applies.
        - Be precise and terse. This guidance is internal — never phrase it as customer-facing copy.""";
    private static final String INTERNAL_PROMPT_V2 = INTERNAL_PROMPT_V1 + """


        - Call out explicitly when a case must be escalated to the Support Lead.""";

    // Assistants ---------------------------------------------------------------------------------
    private static final String GENERAL_ASSISTANT_NAME = "General Assistant";
    private static final String CUSTOMER_ASSISTANT_NAME = "Support Assistant";
    private static final String INTERNAL_ASSISTANT_NAME = "Support Assistant (Internal)";

    private final DocumentService documentService;
    private final CategoryService categoryService;
    private final LlmService llmService;
    private final PromptService promptService;
    private final AssistantService assistantService;
    private final DemoConversationSeeder conversationSeeder;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        LlmDto llm = getOrCreateLlm();
        getOrCreateOpenAiPlaceholder();

        PromptDto generalPrompt = getOrCreatePrompt(GENERAL_PROMPT_NAME, GENERAL_PROMPT_V1,
            GENERAL_PROMPT_V2, "Added a note about admitting uncertainty");
        PromptDto customerPrompt = getOrCreatePrompt(CUSTOMER_PROMPT_NAME, CUSTOMER_PROMPT_V1,
            CUSTOMER_PROMPT_V2, "Added a warmer customer tone");
        PromptDto internalPrompt = getOrCreatePrompt(INTERNAL_PROMPT_NAME, INTERNAL_PROMPT_V1,
            INTERNAL_PROMPT_V2, "Ask agents to flag Support Lead escalations");

        CategoryDto customerCategory = getOrCreateCategory(CUSTOMER_CATEGORY_NAME,
            "Fictional customer-facing Zephyr One documentation used to demonstrate RAG");
        CategoryDto internalCategory = getOrCreateCategory(INTERNAL_CATEGORY_NAME,
            "Fictional internal agent playbook, grounding a separate admin support assistant");
        getOrCreateDocument(CUSTOMER_DOCUMENT_NAME, CUSTOMER_DOCUMENT_RESOURCE, customerCategory);
        getOrCreateDocument(INTERNAL_DOCUMENT_NAME, INTERNAL_DOCUMENT_RESOURCE, internalCategory);

        // General Assistant is the default, so the first chat works instantly without depending on
        // the documents having finished embedding.
        AssistantDto general =
            getOrCreateAssistant(GENERAL_ASSISTANT_NAME, true, llm, generalPrompt, List.of());
        AssistantDto customerSupport = getOrCreateAssistant(CUSTOMER_ASSISTANT_NAME, false, llm,
            customerPrompt, List.of(customerCategory));
        getOrCreateAssistant(INTERNAL_ASSISTANT_NAME, false, llm, internalPrompt,
            List.of(internalCategory));

        // Pre-build a couple of conversations (and a matching inspector exchange) once the catalog is
        // ready. The customer support assistant grounds the RAG showcase; the general assistant backs
        // the second, non-RAG conversation.
        conversationSeeder.seed(general, customerSupport);
    }

    private LlmDto getOrCreateLlm() {
        return llmService.findAll().stream()
            .filter(l -> LLM_NAME.equals(l.getName()))
            .findFirst()
            .orElseGet(() -> {
                LlmDto llm = LlmDto.builder()
                    .name(LLM_NAME)
                    .enabled(true)
                    .system(false)
                    .type(LlmType.OLLAMA)
                    .configuration(LLM_CONFIGURATION)
                    .build();
                llm.setId(llmService.save(llm));
                logger().info("Created LLM '{}'.", LLM_NAME);
                return llm;
            });
    }

    /** Seeds a disabled OpenAI LLM so the evaluator can see how to switch the demo to OpenAI. */
    private void getOrCreateOpenAiPlaceholder() {
        boolean exists = llmService.findAll().stream()
            .anyMatch(l -> OPENAI_LLM_NAME.equals(l.getName()));
        if (exists) {
            return;
        }
        LlmDto llm = LlmDto.builder()
            .name(OPENAI_LLM_NAME)
            .enabled(false)
            .system(false)
            .type(LlmType.OPEN_AI)
            .configuration(OPENAI_CONFIGURATION)
            .build();
        llmService.save(llm);
        logger().info("Created disabled OpenAI placeholder LLM.");
    }

    /**
     * Gets or creates a prompt, seeding it with two revisions on first creation so the revision
     * history is populated. {@link PromptService#saveWithComment} records each revision via Envers.
     */
    private PromptDto getOrCreatePrompt(String name, String contentV1, String contentV2,
        String secondRevisionComment) {
        return promptService.findAll().stream()
            .filter(p -> name.equals(p.getName()))
            .findFirst()
            .orElseGet(() -> {
                PromptDto prompt = PromptDto.builder()
                    .name(name)
                    .content(contentV1)
                    .enabled(true)
                    .system(true)
                    .build();
                prompt = promptService.saveWithComment(prompt, "system", "Initial version");
                prompt.setContent(contentV2);
                prompt = promptService.saveWithComment(prompt, "Marcus", secondRevisionComment);
                logger().info("Created prompt '{}' with 2 revisions.", name);
                return prompt;
            });
    }

    private CategoryDto getOrCreateCategory(String name, String description) {
        return categoryService.findByName(name).orElseGet(() -> {
            CategoryDto category = CategoryDto.builder()
                .name(name)
                .description(description)
                .enabled(true)
                .build();
            category.setId(categoryService.save(category));
            logger().info("Created category '{}'.", name);
            return category;
        });
    }

    private AssistantDto getOrCreateAssistant(String name, boolean defaultAssistant, LlmDto llm,
        PromptDto prompt, List<CategoryDto> categories) {
        return assistantService.findByFilter(AssistantFilter.builder().name(name).build())
            .stream().findFirst()
            .orElseGet(() -> {
                AssistantDto assistant = AssistantDto.builder()
                    .name(name)
                    .enabled(true)
                    .defaultAssistant(defaultAssistant)
                    .llm(llm)
                    .prompt(prompt)
                    .categories(new ArrayList<>(categories))
                    .build();
                assistant.setId(assistantService.save(assistant));
                logger().info("Created assistant '{}'.", name);
                return assistant;
            });
    }

    private DocumentDto getOrCreateDocument(String name, String resource, CategoryDto category) {
        List<DocumentDto> documents =
            documentService.findByFilter(DocumentFilter.builder().nameLike(name).build());
        if (!documents.isEmpty()) {
            return documents.get(0);
        }
        try {
            // Copy the resource to a temp file: ClassPathResource.getFile() fails when the app runs
            // from a packaged jar (the resource is nested inside the jar, not on the file system),
            // and DocumentDto only accepts a File.
            File file = File.createTempFile("zephyr-", ".md");
            file.deleteOnExit();
            try (InputStream in = new ClassPathResource(resource).getInputStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            DocumentDto document = DocumentDto.builder()
                .name(name)
                .enabled(true)
                .file(file)
                .externalId(resource)
                .build();
            document.setId(documentService.addDocument(document, List.of(category)));
            logger().info("Embedded document '{}'.", name);
            return document;
        } catch (IOException e) {
            logger().error("Failed to load {} from classpath.", resource, e);
            return null;
        }
    }
}
