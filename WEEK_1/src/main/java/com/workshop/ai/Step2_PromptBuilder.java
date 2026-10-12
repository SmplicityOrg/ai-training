package com.workshop.ai;

import java.util.HashMap;
import java.util.Map;

import groovy.text.SimpleTemplateEngine;

public class Step2_PromptBuilder {


    public static void main(String[] args) throws Exception {
        // 1. Simulate data fetched from Salesforce / Database (SOQL Query)
        Map<String, Object> crmRecord = new HashMap<>();
        crmRecord.put("accountName", "Acme Corp");
        crmRecord.put("contactName", "Marc Benioff");
        crmRecord.put("contractValue", "$120,000");
        crmRecord.put("expirationDate", "Nov 15, 2026");

        // 2. Define a template (This is literally what Salesforce Prompt Builder creates)
        String promptTemplate = 
            "Draft a courteous contract renewal reminder email.\n" +
        
            		
            "Account: ${accountName}\n" +
            "Recipient: ${contactName}\n" +
            "Value: ${contractValue}\n" +
            "Renewal Date: ${expirationDate}\n" +
            "Tone: Professional and encouraging.";

        // 3. Merge data into the template using Groovy engine
        SimpleTemplateEngine engine = new SimpleTemplateEngine();
        String groundedPrompt = engine.createTemplate(promptTemplate).make(crmRecord).toString();

        System.out.println("=== Merged Grounded Prompt ===");
        System.out.println(groundedPrompt);
        System.out.println("================================\n");

        String systemInstruction = "You are a professional assistant";
        // 4. Send to the LLM
       String answer = Step1_RawCall.chat(groundedPrompt, systemInstruction);

        System.out.println("=== Step 2 Output ===");
        System.out.println(answer);
    }
}