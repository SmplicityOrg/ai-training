package com.workshop.ai;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

@Component
@SpringBootApplication
public class Step7_Spring_AI_MCP implements CommandLineRunner {

	
	
	@Autowired
	private  ChatClient client;
	
	private SyncMcpToolCallbackProvider mcpTools;
	
	
	private final String baseUrl = "https://api.salesforce.com";
    private final String mcpEndpoint = "/platform/mcp/v1/platform/sobject-all";
	
	private String jwtToken = "eyJ0bmsiOiJjb3JlL3Byb2QvMDBEYm0wMDAwMHZYZGJGRUFTIiwidmVyIjoiMS4wIiwia2lkIjoiQ09SRV9BVEpXVC4wMERibTAwMDAwdlhkYkYuMTc4NzQyMzA0Mzc3NiIsInR0eSI6InNmZGMtY29yZS10b2tlbiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0.eyJzY3AiOiJyZWZyZXNoX3Rva2VuIG1jcF9hcGkiLCJzdWIiOiJ1aWQ6MDA1Ym0wMDAwMFZPZW5GQUFUIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczovL29yZ2Zhcm0tYjdmMjZjNzEwZS1kZXYtZWQuZGV2ZWxvcC5teS5zYWxlc2ZvcmNlLmNvbSIsImNsaWVudF9pZCI6IjNNVkc5MW9xdmlxSktvRUhMNHFqZEhhZ3d2czFXdUxNVFA4WUFKV0hXcDBFdlByTUd3TTF5SmNnbDB3ZzR0MUMzNTJBeldSdXUxQ3drRUI1bE1BVDkiLCJhdWQiOlsiaHR0cHM6Ly9vcmdmYXJtLWI3ZjI2YzcxMGUtZGV2LWVkLmRldmVsb3AubXkuc2FsZXNmb3JjZS5jb20iLCJodHRwczovL2FwaS5zYWxlc2ZvcmNlLmNvbSJdLCJuYmYiOjE3OTI1NjUzOTYsIm10eSI6Im9hdXRoIiwic2ZpIjoiZDU2NjdhYWYzODVlZGVmM2M5ODE2MjI5ZTlkMDRmNmY2NGI3ZWQ4NzBjOWViMDFkZTRjM2M1MTkyZTI2OTAyNSIsInNmYXBfb3AiOiJFaW5zdGVpbkhhd2tpbmdDMkNFbmFibGVkLEVHcHRGb3JEZXZzQXZhaWxhYmxlLEVpbnN0ZWluR2VuZXJhdGl2ZVNlcnZpY2UsVGFibGVhdU1ldHJpY0Jhc2ljcyxFaW5zdGVpbkdQVE5DUCxNQ1BTZXJ2aWNlIiwiaHNjIjpmYWxzZSwiZXhwIjoxNzkyNTcyNjExLCJpYXQiOjE3OTI1NjU0MTF9.K-mGRqrEsTxtPlFTGvb1BoZtGTt-w07_IZyAcGdSgJpNiQ4D6fl4MRDn3x5MuN8hnYFFNv6lL7KfTOeNQpGlw6ALUa6u5rzmqaf_Y6KDmEZxeKJn9apK8lJNPlcymRyjYqdEswr6vMjGW4ueaBESuHtQBRF46iR-NJ2A336KWPIexftA4dDVZfmDvyHlThvSniKD5uEP1FSNMyEMF706Jhco9465DxPnWkfAajsDoravUuJ1pTUA7r2nG3ayUtUzMjKF7DztH6Yf8WEeueBfKV8A9tjO1-_-N13a8AWdUlvADAI73igjLGFAinIqZqjmI8zq-Ewq8kcE0O-rVeoiLw";
	
	
	public static void main(String[] args) {
		SpringApplication.run(Step7_Spring_AI_MCP.class, args);
	}
	
	
	@Override
	public void run(String... args) throws Exception {
		// TODO Auto-generated method stub
		chat("Provide me the name and address of 2 contacts");
	}
	
	
	private void initMCP() {
		if(mcpTools == null) {
			HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
		            .builder(baseUrl)
		            .endpoint(mcpEndpoint)
		            // Use clientBuilder instead of the deprecated .client(...)
		            .clientBuilder(HttpClient.newBuilder()
		                .connectTimeout(Duration.ofSeconds(30)))
		            // Use customizeRequest instead of requestCustomizer
		            .httpRequestCustomizer((builder, method, endpoint, body, context) -> {
		                builder.header("Authorization", "Bearer " + jwtToken);
		                builder.header("Accept", "application/json, text/event-stream");
		                builder.header("Content-Type", "application/json");
		            })
		            .build();

			        // Build the sync MCP client
			        McpSyncClient client = McpClient.sync(transport)
			            .requestTimeout(Duration.ofSeconds(30))
			            .clientInfo(new McpSchema.Implementation("salesforce-pipeline-advisor", "2.0.0"))
			            .build();

			        // Perform the MCP protocol handshake (initialize + notifications/initialized)
			        client.initialize();
		        
		        mcpTools = new SyncMcpToolCallbackProvider.Builder().addMcpClient(client).build();
		}
	}
	
	public String chat(String userPrompt) {
		initMCP();
		
		this.client = client.mutate()
	            .defaultTools(mcpTools)
	            .defaultSystem("""
	                You are an executive Sales Intelligence & Pipeline Health Advisor.
	                You have access to live Salesforce CRM data through the MCP `soqlQuery` tool.
	                
	                WORKFLOW:
	                1. Translate user requests about deals, opportunities, or revenue into accurate SOQL queries.
	                2. Execute the SOQL query using the `soqlQuery` tool.
	                3. Audit the returned records:
	                   - Identify deals where CloseDate is approaching but the Stage is still early (e.g. Prospecting, Qualification).
	                   - Group deals by stage and calculate projected vs. at-risk pipeline.
	                   - Provide prioritized, actionable deal-coaching steps.
	                4. Present data in clean markdown tables followed by risk assessments.
	                """)
	            .build();
		
		String content =  this.client.prompt(userPrompt).call().content();
		
		System.out.print(content);
		return content;
	}

	
	
}
