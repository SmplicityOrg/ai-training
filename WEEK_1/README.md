Here is a comprehensive `README.md` crafted to match the exact tone, progressive narrative, and practical code walkthrough from your workshop session.

---

# 🤖 Salesforce Tech Exchange: Understanding Agentic AI from First Principles

> **Workshop Session Notes & Implementation Lab**  
> *Speaker: Kureem Rossaye*  
> *Target Audience: Salesforce Developers, Apex Architects, and Technical Consultants*

---

## 🎯 Executive Summary & Objective

In this hands-on workshop, we demystify **Generative AI and Agentic AI**. We strip away marketing buzzwords (Agentforce, assistants, autonomous agents) and inspect how these systems work under the hood using foundational software engineering concepts you already use every day: **HTTP REST calls, JSON serialization, state management, and deterministic code execution.**

While this repository implements the patterns in **Java 17**, every concept directly maps 1:1 to **Salesforce Apex, Flow, and Prompt Builder**.

---

## 💡 The Core Mental Model

```
+-----------------------------------------------------------------------------------+
|                                  THE GOLDEN RULE                                   |
|                                                                                   |
|  LLMs do NOT touch databases, do NOT modify code, and do NOT perform actions.    |
|  An LLM is merely a stateless text-in, text-out web service.                      |
|  Any action, lookup, or mutation is orchestrated and executed by OUR backend code.|
+-----------------------------------------------------------------------------------+
```

---

## 🗺️ Progressive Architecture Roadmap

The workshop progresses through five sequential milestones:

```
Step 1: The Raw REST Call
   │   (LLMs are just web services with endpoint + headers + payload)
   ▼
Step 2: Grounding & Prompt Builder
   │   (Injecting deterministic CRM data into templates)
   ▼
Step 3: Conversational Memory
   │   (Solving LLM statelessness by accumulating message history)
   ▼
Step 4: Structured Output (JSON Contract)
   │   (Enforcing machine-parseable data over unstructured text)
   ▼
Step 5: The Agentic Tool-Calling Loop
       (LLM decides intent/parameters -> Java/Apex executes -> Return results)
```

---

## Step 1: The Raw Web Service Call (`Step1_RawCall.java`)

### The Concept
Whether using Google Gemini, OpenAI ChatGPT, Anthropic Claude, or DeepSeek, every interaction is a standard **HTTP POST request**:
1. An authentication key / header.
2. A payload containing the `model`, `system_instruction` (persona & constraints), and the `input` (prompt).
3. A JSON response returning metadata, token counts, and the generated content.

In Salesforce Apex, this is nothing more than `HttpRequest`, `Http.send()`, and JSON parsing.

### How It Works in Code
```java
JsonObject payload = new JsonObject();
payload.addProperty("model", "models/gemini-2.5-flash"); // or target model
payload.addProperty("system_instruction", systemInstruction);
payload.addProperty("input", userPrompt);

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(ENDPOINT))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
    .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
```

### Key Takeaways
* **System Instruction:** Sets persistent persona, role, and output guardrails without having to re-declare it inside the message.
* **No magic:** You do not need dedicated vendor SDKs; a plain REST client is sufficient.

---

## Step 2: Grounding & Prompt Builder (`Step2_PromptBuilder.java`)

### The Concept
When an LLM produces a vague or hallucinated answer, it is usually because it lacks business context.
* If you ask an LLM: *"Draft a courteous contract renewal reminder email"*, it produces a generic template because it doesn't know who, what, or how much.
* In Salesforce, **Prompt Builder** solves this through **Grounding**: injecting data from SOQL queries, Apex classes, or Flows into placeholder variables in a prompt template before invoking the model.

### Grounding Flow
```
[ SOQL / CRM Record ] ────┐
                          ├───► [ Prompt Template ] ───► [ Grounded Prompt ] ───► [ LLM Call ]
[ Dynamic Context   ] ────┘
```

### How It Works in Code
We simulate SOQL / CRM record retrieval using a map and merge it into a template using Groovy's `SimpleTemplateEngine`:

```java
// 1. Fetch CRM Record (Simulation of SOQL query)
Map<String, Object> crmRecord = new HashMap<>();
crmRecord.put("accountName", "Acme Corp");
crmRecord.put("contactName", "Marc Benioff");
crmRecord.put("contractValue", "$120,000");
crmRecord.put("expirationDate", "Nov 15, 2026");

// 2. Define Template (What Prompt Builder manages)
String promptTemplate = 
    "Draft a courteous contract renewal reminder email.\n" +
    "Account: ${accountName}\n" +
    "Recipient: ${contactName}\n" +
    "Value: ${contractValue}\n" +
    "Renewal Date: ${expirationDate}\n" +
    "Tone: Professional and encouraging.";

// 3. Ground the prompt
String groundedPrompt = engine.createTemplate(promptTemplate).make(crmRecord).toString();
```

---

## Step 3: Conversational Memory & State Management (`Step3_Conversational.java`)

### The Trap: LLMs Are Stateless
If you run an interactive loop and ask:
1. `User:` *"My favorite city is Port Louis."*  
   `AI:` *"Port Louis is vibrant and beautiful!"*
2. `User:` *"What is yours?"*  
   `AI:` *"I don't know what you mean. Could you please clarify?"*

The model fails because **HTTP is stateless, and the model maintains no session, thread, or memory on its server.**

### The Solution: Re-sending History
To build a chat experience, our backend must maintain the conversation history and re-send the full transcript with every subsequent turn:

```
Turn 1 Request:
  User: "My favorite city is Port Louis."
Turn 1 Response:
  AI: "Port Louis is wonderful."

Turn 2 Request (Full Transcript):
  User: "My favorite city is Port Louis."
  AI: "Port Louis is wonderful."
  User: "What is yours?"
Turn 2 Response:
  AI: "If I had to choose, I would say Tokyo!"
```

### Architectural Implications: Token Compounding & Degradation
1. **Token Cost:** Token consumption does not grow linearly with each turn—it compounds, because every turn repeats the full transcript plus the new reply.
2. **Context Rotting / Attention Drift:** Long histories can cause the LLM to lose focus on instructions given at the beginning of the context window.
3. **Prompt Caching:** Modern models (such as Gemini's context caching for prompts > 32k tokens) mitigate cost if prefix tokens remain unchanged, but the client application must still manage and transmit the context.

---

## Step 4: Structured Output (`Step4_StructuredOutput.java`)

### Unstructured Text vs. Deterministic JSON
Natural language output (markdown, conversational text, lists) cannot be reliably consumed by downstream code. For autonomous systems to work without humans in the loop, models must return **predictable, typed JSON**.

```
Unstructured: "Here is a list: 1. Mauritius (approx 1.2M), 2. Madagascar..." ──► Fails to parse
Structured:   [{"countryName": "Mauritius", "countryCode": "MU", "estimatedPopulation": 1265740}] ──► Deterministic
```

### How It Works in Code
We inject an explicit output schema contract into the `system_instruction`:

```java
String systemInstruction = """
    You are a professional assistant.
    Your task is to provide a list of countries based on user input.
    You must strictly provide your response in the following JSON format and nothing else:
    [
        {
            "countryName": "string",
            "countryCode": "string",
            "estimatedPopulation": number
        }
    ]
    """;
```

Once returned, your backend simply deserializes the response into a strongly-typed model (`Country.java` or an Apex class via `(List<Country>) JSON.deserialize(...)`).

---

## Step 5: The Agentic Tool-Calling Loop (`Step5_ToolCallingLoop.java`)

This is the cornerstone of **Agentic AI** (including Salesforce Agentforce).

### How Tool Calling Works
The LLM does **not** call your database. Instead, you declare the tools that exist, and the LLM determines **which** tool to call and with **what** parameters.

```
       User Query: "Check OPP-101 and current date"
                           │
                           ▼
                  +─────────────────+
                  |  LLM Evaluates   |
                  +─────────────────+
                           │
     Returns JSON intent: {"type": "tool_call", "toolName": "getOpportunityDetails"}
                           │
                           ▼
                  +─────────────────+
                  | Backend Code    |  <-- Runs in Java / Apex
                  | Executes Tool   |
                  +─────────────────+
                           │
        Appends Tool Output: "Stage: Closed-Won, Amount: $150,000"
                           │
                           ▼
                  +─────────────────+
                  |  LLM Synthesizes |  <-- LLM sees tool output in context
                  +─────────────────+
                           │
     Returns JSON: {"type": "final_response", "message": "OPP-101 is Closed-Won..."}
```

### The System Prompt Contract
```json
{
  "type": "tool_call",
  "calls": [
    {
      "toolName": "getOpportunityDetails",
      "parameters": { "oppId": "OPP-101" }
    }
  ]
}
```
Or, when done:
```json
{
  "type": "final_response",
  "message": "<Full human-readable response>"
}
```

### Execution Loop (`Step5_ToolCallingLoop.java`)
```java
while (!resolved && maxTurns-- > 0) {
    // 1. Send conversation history to model
    String rawJsonResponse = Step1_RawCall.chat(msg, SYSTEM_INSTRUCTION);
    JSONObject responseObj = new JSONObject(cleanJson(rawJsonResponse));

    if ("tool_call".equalsIgnoreCase(responseObj.getString("type"))) {
        // 2. Dispatch local methods dynamically based on model instruction
        JSONArray tools = responseObj.getJSONArray("calls");
        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            String toolName = tool.getString("toolName");

            if ("getOpportunityDetails".equalsIgnoreCase(toolName)) {
                String result = CrmTools.getOpportunityDetails(tool.getJSONObject("parameters").getString("oppId"));
                MESSAGES.add(new Message("Tool result", result));
            } else if ("getCurrentDate".equalsIgnoreCase(toolName)) {
                String result = CrmTools.getCurrentDate();
                MESSAGES.add(new Message("Tool result", result));
            }
        }
    } else if ("final_response".equalsIgnoreCase(responseObj.getString("type"))) {
        // 3. Loop concludes when model outputs final answer
        System.out.println(responseObj.getString("message"));
        resolved = true;
    }
}
```

---

## 🛠️ Repository File Structure

```
├── pom.xml                                 # Maven dependencies (org.json, Groovy, etc.)
└── src/main/java/com/workshop/ai/
    ├── Step1_RawCall.java                  # Raw HTTP REST interaction with Gemini API
    ├── Step2_PromptBuilder.java            # Templating & Grounding demo using CRM record simulation
    ├── Step3_Conversational.java           # Multi-turn conversational memory loop
    ├── Step4_StructuredOutput.java          # Schema-enforced JSON output generation
    ├── Step5_ToolCallingLoop.java          # Complete agentic tool-calling cycle
    ├── CrmTools.java                       # Local mock implementations of business tools
    ├── Helper.java                         # HTTP client and console input utility methods
    ├── Message.java                        # Conversation memory transcript model
    └── Country.java                        # DTO for structured output parsing
```

---

## 🚀 Running the Project

### Prerequisites
* Java Development Kit (JDK 17+)
* Apache Maven 3.8+
* An active Gemini API key (or set up an alternative model endpoint)

### Build and Run
```bash
# Clone the repository
git clone <company-github-repo-url>
cd training

# Compile dependencies
mvn clean compile

# Run individual steps
mvn exec:java -Dexec.mainClass="com.workshop.ai.Step1_RawCall"
mvn exec:java -Dexec.mainClass="com.workshop.ai.Step2_PromptBuilder"
mvn exec:java -Dexec.mainClass="com.workshop.ai.Step3_Conversational"
mvn exec:java -Dexec.mainClass="com.workshop.ai.Step4_StructuredOutput"
mvn exec:java -Dexec.mainClass="com.workshop.ai.Step5_ToolCallingLoop"
```

---

## 🏋️ Homework: The Apex Porting Challenge

> **Challenge for the Team:**  
> Take what is implemented here in Java and build the exact same pattern in pure **Salesforce Apex**:
> 1. Create a Named Credential / External Credential pointing to the LLM endpoint.
> 2. Create an Apex class `LlmClient.cls` that makes an `HttpRequest` and parses the response.
> 3. Build an Apex controller that takes an Opportunity Record ID, constructs a grounded prompt, invokes the model, and parses a structured JSON response to update an Opportunity record or write a task.
>
> *No Agentforce license required—just pure Apex, standard REST integrations, and structured JSON contracts.*

---

## 🔮 What's Next in the Next Sync
* Native Function Calling protocols vs. Prompted JSON schemas
* Introduction to **Model Context Protocol (MCP)** clients and servers
* Deep-dive into Agentforce Actions and Apex Invocation under the hood
