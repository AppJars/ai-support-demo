# AI Support Demo

A ready-to-run demonstration of the **AppJars AI Support** framework — a full-stack, AI-powered
support chat you can drop into a Vaadin application. It runs entirely on your machine using a local
LLM (Ollama) and a vector database (PostgreSQL + pgvector), so you can try every feature without any
API key.

The demo opens on a public landing page that explains the features and offers **guided tours** of
each view, and it signs you in with a single click — no credentials to type.

Visit the official [AppJars documentation](https://docs.appjars.com) for more information.

---

## What you get out of the box

When the application starts for the first time, a `DemoDataInitializer` creates all the demo data
automatically (and skips anything that already exists on later restarts):

| Entity | Value | Purpose |
|--------|-------|---------|
| **LLMs** | `llama3.2:3b` (Ollama) + a **disabled OpenAI placeholder** | The working local model, plus a ready-to-fill OpenAI entry |
| **Prompts** | `General Prompt`, `Support Prompt — Customer`, `Support Prompt — Internal` | Markdown system prompts, each seeded with **revision history** |
| **Categories** | `Product Knowledge Base`, `Internal Support Playbook` | Two separate knowledge bases |
| **Documents** | `Zephyr One Support Guide`, `Zephyr One Internal Playbook` | Fictional docs embedded as vectors for RAG |
| **Assistants** | `General Assistant` *(default)*, `Support Assistant`, `Support Assistant (Internal)` | Plain chat + two document-grounded assistants |
| **Conversations** | A customer support chat + a general chat | Pre-built so the chat and LLM Inspector are populated on first run |

### Assistants and knowledge bases: seeing RAG clearly

The demo ships three assistants on purpose:

- The **General Assistant** talks about anything, like any chatbot (no documents).
- The **Support Assistant** (customer-facing) and **Support Assistant (Internal)** (agent-facing) are
  each grounded on a **different** knowledge base about a **fictional** product — the *Zephyr One*
  robotic mower by *Lumen Robotics*. Because the product is made up, its facts (error codes, warranty
  terms, escalation rules…) exist **only** in the documents, so a correct answer proves it came from
  Retrieval-Augmented Generation (RAG), not model training. Ask both the same question (e.g. *"How do
  I handle error E-27?"*) and they answer differently, each from its own document.

The knowledge bases live at `src/main/resources/documents/zephyr-one-support.md` (customer) and
`zephyr-one-internal-playbook.md` (internal) — open them to see the facts each assistant can ground on.

### Pre-seeded conversations

So the app is not empty on first run, two conversations are seeded (no LLM is called to produce them —
the messages are fixed):

- A **support chat**: *Leo* (customer) asks the Support Assistant about error E-27, the assistant
  answers from the guide, and *Olivia* (admin) steps in — including one **admin-only** internal note.
- A **general chat**: *Sophia* (customer) chats with the General Assistant.

This shows the visibility model (admins see every conversation and the admin-only note; each customer
sees only their own), and it populates the **LLM Inspector** — open the support conversation there to
see the retrieved document chunk that grounded the answer.

---

## Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Docker** and **Docker Compose**

---

## 1. Start the infrastructure

The `docker-compose.yaml` file starts three services:

- **`vectorial_db`** — PostgreSQL 18 with the `pgvector` extension (port `5432`)
- **`ollama`** — Ollama server (port `11434`)
- **`ollama-init`** — a one-shot container that pulls the `llama3.2:3b` model into Ollama, then exits

```bash
docker compose up -d
```

> The first run downloads the `llama3.2:3b` model (~2 GB). Wait for the `ollama-init` container to
> finish before starting the application:
>
> ```bash
> docker compose logs ollama-init --follow
> ```

---

## 2. Run the application

```bash
mvn
```

(`spring-boot:run` is the default goal.) Then open **http://localhost:8080**.

On first startup you will see log lines like:

```
INFO  DemoDataInitializer  - Created LLM 'llama3.2:3b'.
INFO  DemoDataInitializer  - Created disabled OpenAI placeholder LLM.
INFO  DemoDataInitializer  - Created prompt 'Support Prompt — Customer' with 2 revisions.
INFO  DemoDataInitializer  - Created category 'Internal Support Playbook'.
INFO  DemoDataInitializer  - Embedded document 'Zephyr One Support Guide'.
INFO  DemoDataInitializer  - Created assistant 'Support Assistant (Internal)'.
INFO  DemoConversationSeeder - Seeded demo conversations.
```

Embedding the document may take a moment while it is chunked and stored as vectors.

---

## Using the demo

### Guided tours

http://localhost:8080 opens on a public landing page (no login required) that summarizes the
features and carries a **Guided tour** menu, also available from the navbar once you are signed in.

Each tour highlights the real elements of a screen and explains them step by step, one popover at a
time. Ten tours are available:

| Tour | Covers |
|---|---|
| **This page** | What the demo contains, how to sign in, and free mode |
| **AI chat** | Conversation list, starting a chat, search and filter, writing and sending, choosing an assistant, and seeing RAG in action |
| **Chat bubble** | The floating assistant: switching conversations, sending, moving and resizing it |
| **Assistants** | The assistant list, creating one, and what the default assistant does |
| **Prompts** | The prompt list, creating one, and what makes a good prompt |
| **Language models** | Adding an Ollama or OpenAI provider, and its caveats |
| **Documents** | The knowledge base, uploading a document, and connecting it to an assistant |
| **Categories** | Grouping documents and wiring them to assistants |
| **LLM Inspector** | Picking a conversation, the exchange breakdown, and digging into a message |
| **Channels** | The channel list, creating one, and connecting it to Meta |

Starting a view tour while signed out takes you to the sign-in screen first; the tour is remembered
for the session and resumes by itself once you pick an account, so you can launch any tour straight
from the landing page. If you are already signed in it just navigates there.

Tours are driven by [Driver.js](https://driverjs.com). Leave a tour at any step with `Esc` or by
clicking outside the popover.

### Signing in

Click **Sign in** and pick any of the four demo accounts — no password needed:

| Account | Role | What it shows |
|---------|------|---------------|
| **Olivia** | Administrator | Support Lead — sees **every** customer conversation and can post internal, admin-only notes |
| **Marcus** | Administrator | AI Manager — configures assistants, models, prompts and the knowledge base |
| **Sophia** | Customer | Chats with support and the AI assistant; sees only her own conversation |
| **Leo** | Customer | A second customer identity, handy for trying a multi-user conversation |

Sign in as an administrator to see the whole picture, or as a customer to see the end-user
experience. Open the app in two browsers (e.g. Olivia and Sophia) to try a multi-user conversation.

### Chatting, and proving RAG

Start chatting from the **AI chat** view or the floating chat bubble. Use the assistant selector to
switch between the two assistants:

- With the **Support Assistant**, ask questions about the Zephyr One:
  - *"What does error code E-27 mean?"*
  - *"How long is the Zephyr One battery warranty?"*
  - *"What is Lumen Robotics' return policy?"*

  The answers come straight from the knowledge base. Now ask something that isn't in the guide and
  it will honestly tell you it doesn't have that information.

- With the **General Assistant**, ask anything — it behaves like a normal chatbot.

You can also attach files to a message (up to 3 per message, 10 MB each). Images, audio, video and
PDFs are passed to the model when it is multimodal.

### The admin panel

The navigation menu opens the management views: **Assistants**, **Prompts**, **Language models
(LLMs)**, **Categories**, **Documents**, the **LLM Inspector** (to see the exact prompt, retrieved
chunks and token usage of each exchange) and **Channels** (WhatsApp / Facebook / Instagram).

---

## A note on response times

The default setup uses `llama3.2:3b` running locally via Ollama — a small model running on CPU, so
**responses can take from a few seconds to a few minutes** depending on your hardware.

If it is too slow, you have two options:

### Use a different Ollama model

```bash
docker exec ollama ollama pull llama3.1:8b
```

Then go to **LLMs**, edit the `llama3.2:3b` entry and update the `modelName` in its configuration.

### Use OpenAI instead

For much faster, higher-quality responses, use the pre-seeded **OpenAI (add your key)** entry:

1. Go to **LLMs** and edit **OpenAI (add your key)** — it starts *disabled*.
2. Replace `<YOUR_API_KEY>` and `<YOUR_OPENAI_MODEL>` in its configuration (e.g. `gpt-4o-mini`), then enable it.
3. Go to **Assistants**, edit an assistant and switch its LLM to the OpenAI entry.

---

## Token estimation

A HuggingFace tokenizer for `llama3.2:3b` is bundled in the `tokenizers/` folder and pre-configured
in `application.properties`, so token estimation works out of the box:

```properties
appjars.aisupport.tokenizer.llama3=tokenizers/llama3-tokenizer.json
```

---

## Configuration

All configuration lives in `src/main/resources/application.properties`, organized into sections. It
lists **every** property the appjar supports, so you can see what is available at a glance —
properties the demo does not need are left with an empty value.
Every property is documented in the
[AI Support documentation](https://docs.appjars.com/ai-support/overview/):

| Section | What it configures |
|---|---|
| **Spring / Database** | Datasource URL, credentials and the Hibernate DDL strategy |
| **Vaadin** | Server push mode for the UI |
| **Database Connection** | Host, port and name of the PostgreSQL instance |
| **PgVector / Embedding Store** | Vector dimensions and the embeddings table name |
| **External Embedding Model** | Optional ONNX embedding model and tokenizer paths |
| **Retrieval (RAG)** | Chunks per query, similarity threshold and aggregation limits |
| **RAG Scoring Model** | Optional ONNX re-ranker used to score retrieved chunks |
| **Advanced RAG** | Search-query guidance file, queries generated per message and length limits |
| **Prompts and Moderation** | Summary and moderation guidance files, moderation toggle and reply |
| **LLM Models and Tokenizers** | Request timeout, moderation model, Ollama context window and tokenizer paths |
| **Chat Attachments** | Storage directory and the per-message file count and size limits |
| **Tooling** | Packages scanned for `@Tool`-annotated beans exposed to the LLM |
| **Channels** | WhatsApp / Facebook / Instagram toggles, webhook secrets and verify tokens |
| **Date and Time Formatting** | Date, time and datetime display patterns |
| **Scheduled Maintenance** | Schedules and rules for closing idle sessions and pruning old records |
| **View Routes** | URL paths for each view |

---

## Stopping the infrastructure

```bash
docker compose down
```

To also remove stored data (models, database):

```bash
docker compose down -v
```
