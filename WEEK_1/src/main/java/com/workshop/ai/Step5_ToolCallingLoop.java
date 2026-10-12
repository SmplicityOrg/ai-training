package com.workshop.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonArray;

public class Step5_ToolCallingLoop {
	
	private final static List<Message> MESSAGES = new ArrayList<Message>();


    // 1. The Prompt defining tools and strict output contract
    private static final String SYSTEM_INSTRUCTION = """
        You are an intelligent assistant integrated into an enterprise CRM.
        
        You have access to the following tools:
        1. getOpportunityDetails(oppId: string) -> Returns stage, amount, and account name.
        2. getCurrentDate() -> Returns today's date.

        When the user asks a question, evaluate whether you need external data from a tool:
        
        - If you NEED a tool, return ONLY this JSON structure:
        {
          "type": "tool_call",
          "calls":[{
	          "toolName": "<name_of_the_tool>",
	          "parameters": {
	             "<parameter_name>": "<value>"
	          }
	       }]
        }

        - When you HAVE the necessary information to answer, return ONLY this JSON structure:
        {
          "type": "final_response",
          "message": "<your full textual answer to the user>"
        }

        STRICT RULE: Output pure JSON only. Do not wrap in markdown or backticks (no ```json).
        """;

    public static void main(String[] args) throws Exception {
        // User query requiring an external lookup
        String userQuery = "Can you give me the status and value of opportunity OPP-101, and what today's date is?";

        System.out.println("User Query: " + userQuery);
        System.out.println("--------------------------------------------------");

        // Maintain conversation history for the loop
        

        boolean resolved = false;
        int maxTurns = 5; // Guard against infinite loops

        while (!resolved && maxTurns-- > 0) {
        	
        	MESSAGES.add(new Message("User", userQuery));
        	
        	String msg = MESSAGES.stream().map(m->m.toString()).collect(Collectors.joining());
        	
        	
        	System.out.println("\n[User Input]\n" + msg);
            // 1. Ask LLM
            String rawJsonResponse = Step1_RawCall.chat(msg, SYSTEM_INSTRUCTION);
            System.out.println("\n[LLM RAW RESPONSE]\n" + rawJsonResponse);

            JSONObject responseObj = new JSONObject(cleanJson(rawJsonResponse));
            String responseType = responseObj.getString("type");

            // 2. Decide based on type
            if ("tool_call".equalsIgnoreCase(responseType)) {
            	StringBuilder result = new StringBuilder();
            	JSONArray tools = responseObj.getJSONArray("calls");
            	for(int i =0; i < tools.length(); i++) {
            		JSONObject tool = tools.getJSONObject(i);
	                String toolName = tool.getString("toolName");
	                JSONObject params = tool.optJSONObject("parameters");
	
	                // Execute the tool in Java
	                String toolResult = "";
	                if ("getOpportunityDetails".equalsIgnoreCase(toolName)) {
	                    String oppId = params.getString("oppId");
	                    toolResult = CrmTools.getOpportunityDetails(oppId);
	                } else if ("getCurrentDate".equalsIgnoreCase(toolName)) {
	                    toolResult = CrmTools.getCurrentDate();
	                } else {
	                    toolResult = "Error: Tool not found.";
	                }
	                
	                result.append("\n (" + toolName + "): " + toolResult).append("\n");
            	}

                MESSAGES.add(new Message( "Tool result", result.toString()));
                    

            } else if ("final_response".equalsIgnoreCase(responseType)) {
                System.out.println("\n================ FINAL ANSWER ================");
                System.out.println(responseObj.getString("message"));
                System.out.println("==============================================");
                resolved = true;
            }
        }
    }

    // Helper to sanitize markdown block fences if the model accidentally returns them
    private static String cleanJson(String raw) {
        return raw.trim()
                  .replaceAll("^```json", "")
                  .replaceAll("^```", "")
                  .replaceAll("```$", "")
                  .trim();
    }
}