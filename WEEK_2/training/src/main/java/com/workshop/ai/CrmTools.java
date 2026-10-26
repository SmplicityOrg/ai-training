package com.workshop.ai;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class CrmTools {

    // Mock CRM database
    private static final Map<String, String> OPPORTUNITY_DB = new HashMap<>();

    static {
        OPPORTUNITY_DB.put("OPP-101", "Stage: Closed-Won, Amount: $150,000 USD, Account: Acme Corp");
        OPPORTUNITY_DB.put("OPP-102", "Stage: Negotiation, Amount: $45,000 USD, Account: Globex Inc");
    }

    /**
     * Tool 1: Lookup opportunity details by Opportunity ID
     */
    @Tool(description = "Lookup opportunity status, amount, and account by Opportunity ID")
    public String getOpportunityDetails(String oppId) {
        System.out.println("\n[JAVA TOOL EXECUTED] getOpportunityDetails called with oppId: " + oppId);
        return OPPORTUNITY_DB.getOrDefault(oppId, "Opportunity not found.");
    }

    /**
     * Tool 2: Check current system date
     */
    @Tool(description = "Returns the current system date")
    public String getCurrentDate() {
        System.out.println("\n[JAVA TOOL EXECUTED] getCurrentDate called");
        return LocalDate.now().toString();
    }
}