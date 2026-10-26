package com.workshop.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import com.google.gson.JsonObject;

public class Step1_RawCall {

    // Set your Gemini API key here or via env variable
    private static final String API_KEY = "";
    private static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions?key=" + API_KEY;

    public static void main(String[] args) throws Exception {
    	
    	//we create our system instruction. a normal string 
    	//String systemInstruction = "You are a grumpy tier-1 IT support agent. Answer tersely and always ask if they tried restarting.";
    	
    	String systemInstruction = "You are professional";
    	while(true) {
    		//we wait for user input from console
    		String userInput = Helper.getUserInput(">");
    		
    		String response = chat(userInput, systemInstruction);
    		
    		System.out.println("AI:"+ response);
    	}
    }
    
    public static String chat(String userPrompt, String systemInstruction) {
    	
    	try {
        HttpClient client = HttpClient.newHttpClient();
        
       JsonObject payload = new JsonObject();
       payload.addProperty("model", "models/gemini-3.8-flash");
       payload.addProperty("system_instruction", systemInstruction);
       payload.addProperty("input", userPrompt);
        
       String jsonPayload = payload.toString();
        // 1. Build the payload
        // 2. Send the HTTP POST
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 3. Inspect the response
        JSONObject responseJson = new JSONObject(response.body());
        
        String answer = responseJson
            .getJSONArray("steps")
            .getJSONObject(1)
            .getJSONArray("content")
            .getJSONObject(0)
            .getString("text");

        
        return answer;
    	}catch(Exception e) {
    		throw new RuntimeException(e);
    	}
    }
}
