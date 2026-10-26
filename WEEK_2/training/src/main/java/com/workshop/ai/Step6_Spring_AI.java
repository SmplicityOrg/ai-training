package com.workshop.ai;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@AllArgsConstructor
public class Step6_Spring_AI /*implements CommandLineRunner*/ {

	private final ChatClient chatClient;
	private final CrmTools crmTools;
	
	public static void main(String[] args) {
		SpringApplication.run(Step6_Spring_AI.class, args);
	}
	

    //@Override
    public void run(String... args) {
        System.out.println("\n=======================================================");
        System.out.println("        SPRING AI: RE-CREATING SESSION 1 LIVE          ");
        System.out.println("=======================================================\n");

        step1_RawCallAndSystemPrompt();
        step2_PromptBuilderGrounding();
        step3_ConversationalMemory();
        step4_StructuredOutput();
        step5_NativeToolCalling();
    }

    // -------------------------------------------------------------
    // STEP 1: Basic Call + System Instruction
    // -------------------------------------------------------------
    private void step1_RawCallAndSystemPrompt() {
        System.out.println("--- STEP 1: System Instruction & Call ---");

        String response = chatClient.prompt()
            .system("You are a grumpy tier-1 IT support agent. Answer in one sentence and ask if they tried restarting.")
            .user("My mouse cursor is frozen.")
            .call()
            .content();

        System.out.println("AI Response:\n" + response + "\n");
    }

    // -------------------------------------------------------------
    // STEP 2: Grounding / Dynamic Prompt (Salesforce Prompt Builder)
    // -------------------------------------------------------------
    private void step2_PromptBuilderGrounding() {
        System.out.println("--- STEP 2: Grounding (Prompt Builder Analogy) ---");

        String template = """
            Draft a renewal reminder email.
            Account: {accountName}
            Contact: {contactName}
            Value: {contractValue}
            Renewal Date: {expirationDate}
            """;

        Map<String, Object> crmRecord = Map.of(
            "accountName", "Acme Corp",
            "contactName", "Marc Benioff",
            "contractValue", "$120,000",
            "expirationDate", "Nov 15, 2026"
        );

        // Spring AI merges the map into {variables} automatically
        String email = chatClient.prompt()
            .user(u -> u.text(template).params(crmRecord))
            .call()
            .content();

        System.out.println("AI Grounded Email:\n" + email + "\n");
    }

    // -------------------------------------------------------------
    // STEP 3: Chat Memory (No Manual Array Appending!)
    // -------------------------------------------------------------
    private void step3_ConversationalMemory() {
        System.out.println("--- STEP 3: Conversational Memory (Statelessness Solved) ---");

        // We attach an in-memory chat memory advisor with a conversation ID
        ChatClient memoryClient = chatClient.mutate()
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
            .build();

        String conversationId = "user-session-123";

        // Turn 1
        memoryClient.prompt()
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user("Hi, my favorite city is Tokyo.")
            .call()
            .content();

        // Turn 2: It remembers Tokyo automatically without manual serializeAsText()!
        String turn2Response = memoryClient.prompt()
        		.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user("What is my favorite city?")
            .call()
            .content();

        System.out.println("AI Response to Turn 2: " + turn2Response + "\n");
    }

    // -------------------------------------------------------------
    // STEP 4: Structured Output (Direct to Java List<Country>)
    // -------------------------------------------------------------
    private void step4_StructuredOutput() {
        System.out.println("--- STEP 4: Structured Output (Entity POJO Mapping) ---");

        // Spring AI automatically generates the schema and parses JSON into Country objects
        List<Country> countries = chatClient.prompt()
            .user("Give me 3 tropical island countries.")
            .call()
            .entity(new ParameterizedTypeReference<List<Country>>() {});

        System.out.println("Parsed Java List<Country>:");
        for (Country c : countries) {
            System.out.println("  • " + c.getCountryName() + " (" + c.getCountryCode() + ") - Pop: " + c.getEstimatedPopulation());
        }
        System.out.println();
    }

    // -------------------------------------------------------------
    // STEP 5: Native Tool Calling Loop (Fully Automated)
    // -------------------------------------------------------------
    private void step5_NativeToolCalling() {
        System.out.println("--- STEP 5: Native Tool Calling (Agent Loop) ---");

        // In Session 1, this required 100 lines of while-loop and regex parsing.
        // In Spring AI, passing .tools(crmTools) handles discovery, dispatch, and final answer!
        String finalAnswer = chatClient.prompt()
            .tools(crmTools)
            .user("What is the status of opportunity OPP-101 and what is today's date?")
            .call()
            .content();

        System.out.println("\nFinal Answer:\n" + finalAnswer);
        System.out.println("=======================================================\n");
    }

}
