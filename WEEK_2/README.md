```
========================================================================================
SESSION 2: DECOUPLING TOOLS VIA MODEL CONTEXT PROTOCOL (MCP)
Architecture, Bidirectional Integration (Spring AI <-> Salesforce), & Agentforce Tools
========================================================================================
```

---

## 1. Executive Summary & Architectural Overview

In Session 1, tool execution was tightly coupled to our local application code: our reasoning engine and tool handlers lived in the exact same JVM. 

In enterprise environments, however, forcing AI agents to maintain bespoke, point-to-point integrations with dozens of external platforms (ERP, Logistics, CRMs, Issue Trackers) results in an unmaintainable $M \times N$ integration sprawl. 

**Session 2 introduces the Model Context Protocol (MCP)**—an open JSON-RPC 2.0 specification designed to decouple tool providers from tool consumers.

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          MCP ARCHITECTURE PATTERN                         │
└───────────────────────────────────────────────────────────────────────────┘

 1. External MCP Tool Ingestion (Cloudflare Tunnel)
 ┌──────────────────────┐          JSON-RPC (HTTP)          ┌─────────────────────┐
 │ Salesforce Org       │ ────────────────────────────────► │ Spring Boot App     │
 │ (Agentforce Runtime) │ ◄──────────────────────────────── │ (Logistics Server)  │
 └──────────────────────┘     Via Cloudflare / ngrok        └─────────────────────┘
     (MCP Client / Host)                                       (MCP Server)


 2. Salesforce as an MCP Server (OAuth 2.0)
 ┌──────────────────────┐          JSON-RPC (HTTP)          ┌─────────────────────┐
 │ Spring AI Client     │ ────────────────────────────────► │ Salesforce Org      │
 │ (Java Console App)   │ ◄──────────────────────────────── │ (Salesforce MCP)    │
 └──────────────────────┘       Bearer Token (OAuth)        └─────────────────────┘
     (MCP Client / Host)                                       (MCP Server)
```

---

## 2. Core Concepts Covered

### The Evolution from Local Tool Calling to MCP
* **Local Function Calling:** The LLM client holds both the model connection and the execution logic. The model outputs a typed token structure (`tool_calls`), and local application code executes the tool.
* **The MCP Standard:** A universal, transport-agnostic interface (stdio, HTTP/SSE, Streamable HTTP POST) standardizing three primitives:
  1. **`initialize`**: Protocol handshake, capability negotiation, and metadata exchange.
  2. **`tools/list`**: Dynamic schema discovery returning standard JSON Schema tool declarations.
  3. **`tools/call`**: Type-safe dispatch of tool execution commands.

---

## 3. Code Implementations & Demos Walkthrough

### Part A: Building the Local MCP Server (Spring Boot)

Two implementations were explored:
1. **Low-Level Protocol Implementation (`McpServerController.java`):** A manual JSON-RPC 2.0 dispatcher implementing the raw `initialize`, `tools/list`, and `tools/call` RPC methods.
2. **Declarative Spring AI MCP Starter (`LogisticsMcpServer.java`):** Utilizing declarative annotations to automatically derive tool schemas and manage JSON-RPC routing.

#### Key Code: `LogisticsMcpServer.java`
```java
package com.workshop.ai.mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;

@Service
@SpringBootApplication
public class LogisticsMcpServer {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsMcpServer.class, args);
    }

    @McpTool(description = "Queries FedEx and Warehouse ERP for real-time tracking of an order.")
    public String checkShipmentStatus(
        @McpToolParam(description = "The customer order number, e.g. ORD-8821") String orderId
    ) {
        System.out.println("  --> [MCP SERVER EXECUTING] checkShipmentStatus for: " + orderId);

        if ("ORD-8821".equalsIgnoreCase(orderId)) {
            return "ORDER #ORD-8821 STATUS: CRITICAL_DELAY. Current Location: Chicago Distribution Hub. Reason: Forklift pallet damage. Estimated delay: 4 days.";
        }
        return "Order " + orderId + " not found in logistics database.";
    }

    @McpTool(description = "Triggers warehouse to ship an emergency overnight replacement package.")
    public String dispatchExpeditedReplacement(
        @McpToolParam(description = "The original order number being replaced") String orderId,
        @McpToolParam(description = "Destination postal/zip code") String destinationZip
    ) {
        System.out.println("  --> [MCP SERVER EXECUTING] dispatchExpeditedReplacement for: " + orderId + " to " + destinationZip);

        return "SUCCESS: Replacement package REPL-9901 dispatched via FedEx Priority Overnight. Guaranteed delivery: Tomorrow 10:30 AM.";
    }
}
```

#### Server Configuration (`application.properties`)
```properties
server.port=8080
spring.ai.mcp.server.name=warehouse-logistics-mcp
spring.ai.mcp.server.version=1.0.0

# STATELESS enables synchronous HTTP POST interactions on /mcp (Postman & Agentforce friendly)
spring.ai.mcp.server.protocol=STATELESS
spring.ai.mcp.server.transport=webmvc
```

---

### Part B: Testing the Server Contract via Postman

All payloads are sent as `POST http://localhost:8080/mcp`.

#### 1. Capability Handshake (`initialize`)
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": { "name": "postman-client", "version": "1.0.0" }
  }
}
```

#### 2. Tool Introspection (`tools/list`)
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```
*Expected Result:* Spring AI introspects `@McpTool` parameters and returns the JSON Schema for `checkShipmentStatus` and `dispatchExpeditedReplacement`.

#### 3. Execution Verification (`tools/call`)
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "checkShipmentStatus",
    "arguments": {
      "orderId": "ORD-8821"
    }
  }
}
```

---

### Part C: Live End-to-End Integrations

#### 1. Ingesting External MCP into Salesforce Agentforce
1. **Public Ingress:** Tunnel the local Spring Boot service running on port 8080 using Cloudflare:
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```
2. **Salesforce Registration:** 
   * Navigate to **Setup $\rightarrow$ External Services** (or **Agentforce Tools / MCP Integration**).
   * Register the endpoint: `https://<your-subdomain>.trycloudflare.com/mcp`.
3. **Agentforce Configuration:**
   * Open **Agent Builder** $\rightarrow$ select your Service Agent.
   * Add/Navigate to the **Order Inquiries / Delivery Issues** Topic.
   * Add the imported MCP tool `checkShipmentStatus`.
4. **Live Execution:**
   * Query in Testing Console: *"Can you check the current status of order ORD-8821?"*
   * *Observed Result:* Agentforce triggers the HTTP tool call through Cloudflare; local Spring Boot console logs execution; Agentforce synthesizes the Chicago hub delay in chat.

#### 2. Consuming Salesforce as an MCP Server via Spring AI
1. **Expose Salesforce Tools:** Configure a Connected App or External Client App with client credentials flow and permissions to expose CRM tools over MCP.
2. **Acquire Access Token:** Fetch the OAuth Bearer token via Postman:
   ```http
   POST https://<your-domain>.my.salesforce.com/services/oauth2/token
   grant_type=client_credentials&client_id=...&client_secret=...
   ```
3. **Query Salesforce via Spring AI:** 
   * Configure a Spring AI client to discover native Salesforce Opportunity and Account tools exposed over the Salesforce MCP endpoint.
   * The Spring AI console outputs verified Opportunity data extracted via Salesforce's native MCP service.

---

## 4. Key Takeaways

1. **MCP is Protocol, Not Implementation:** MCP standardizes the communication pipe between clients (Agentforce, Claude Desktop, Cursor) and servers (Spring Boot, Python, Enterprise Gateways).
2. **Stateless HTTP Transport is Enterprise-Ready:** By using the HTTP POST pattern for JSON-RPC 2.0, standard API Gateways, OAuth middleware, and cloud tunnels can manage AI agent traffic without needing stateful SSE streams.
3. **Bidirectional Synergy:** Salesforce can act both as an **MCP Host** (consuming external enterprise microservices into Agentforce) and as an **MCP Server** (exposing its data layer to external AI applications).

---

## 5. Homework & Hands-On Exercise

### Objective
Create a new custom tool inside `LogisticsMcpServer.java` named `cancelDeliveryTicket`, expose it via Cloudflare, and configure Agentforce to call it.

### Specifications
1. **Java Implementation:**
   * Method signature: `public String cancelDeliveryTicket(String orderId, String reason)`
   * Logic: 
     * If `orderId` is `"ORD-8821"`, return: `"CANCEL_REJECTED: Package is already loaded on freight vehicle."`
     * If any other `orderId`, return: `"CANCEL_SUCCESS: Shipment halted for order " + orderId + ". Reason logged: " + reason`
2. **Postman Verification:**
   * Send a JSON-RPC `tools/call` for `cancelDeliveryTicket` with `orderId: "ORD-8821"` and assert the rejection response.
3. **Agentforce Verification:**
   * Test in Agent Builder: *"Please cancel the delivery for my order ORD-8821 because I am moving."*
   * Verify that Agentforce accurately reads the rejection message and informs the user that the item is already loaded on a vehicle.

---

