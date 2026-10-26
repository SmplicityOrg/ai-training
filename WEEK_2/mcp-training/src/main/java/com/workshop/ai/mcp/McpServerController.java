package com.workshop.ai.mcp;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/mcp")
public class McpServerController {

    @PostMapping
    public McpResponse handleMcpRequest(@RequestBody McpRequest request) {
        String method = request.getMethod();

        return switch (method) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            default -> new McpResponse(request.getId(), Map.of("error", "Method not supported: " + method));
        };
    }

    // 1. Handshake
    private McpResponse handleInitialize(McpRequest req) {
        Map<String, Object> result = Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of("tools", Map.of()),
            "serverInfo", Map.of(
                "name", "salesforce-mock-mcp-server",
                "version", "1.0.0"
            )
        );
        return new McpResponse(req.getId(), result);
    }

    // 2. Tool Discovery (Returning JSON Schema)
    private McpResponse handleToolsList(McpRequest req) {
        // Schema for getOpportunityDetails
        Map<String, Object> oppTool = Map.of(
            "name", "getOpportunityDetails",
            "description", "Fetches stage, amount, and account name for an opportunity.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "oppId", Map.of(
                        "type", "string", 
                        "description", "The Salesforce Opportunity ID, e.g. OPP-101"
                    )
                ),
                "required", List.of("oppId")
            )
        );

        // Schema for getCurrentDate
        Map<String, Object> dateTool = Map.of(
            "name", "getCurrentDate",
            "description", "Returns today's system date.",
            "inputSchema", Map.of(
                "type", "object",
                "properties", Map.of()
            )
        );

        Map<String, Object> result = Map.of("tools", List.of(oppTool, dateTool));
        return new McpResponse(req.getId(), result);
    }

    // 3. Tool Execution
    private McpResponse handleToolsCall(McpRequest req) {
        Map<String, Object> params = req.getParams();
        String toolName = (String) params.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) params.get("arguments");

        String executionResult = "";

        // Dispatch to actual business logic
        if ("getOpportunityDetails".equals(toolName)) {
            String oppId = (String) args.get("oppId");
            executionResult = "OPP-101".equals(oppId)
                ? "Stage: Closed-Won, Amount: $150,000, Account: Acme Corp"
                : "Opportunity not found";
        } else if ("getCurrentDate".equals(toolName)) {
            executionResult = LocalDate.now().toString();
        } else {
            executionResult = "Unknown tool: " + toolName;
        }

        // MCP tool execution format expects a "content" array of text blocks
        Map<String, Object> result = Map.of(
            "content", List.of(
                Map.of("type", "text", "text", executionResult)
            ),
            "isError", false
        );

        return new McpResponse(req.getId(), result);
    }
}