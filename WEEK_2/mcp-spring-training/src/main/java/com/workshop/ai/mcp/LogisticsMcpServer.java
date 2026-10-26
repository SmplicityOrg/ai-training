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
			@McpToolParam(description = "The customer order number, e.g. ORD-8821") String orderId) {

		System.out.println("  --> [MCP SERVER EXECUTING] checkShipmentStatus for: " + orderId);

		if ("ORD-8821".equalsIgnoreCase(orderId)) {
			return "ORDER #ORD-8821 STATUS: CRITICAL_DELAY. Current Location: Chicago Distribution Hub. Reason: Forklift pallet damage. Estimated delay: 4 days.";
		}
		return "Order " + orderId + " not found in logistics database.";

	}

	@McpTool(description = "Triggers warehouse to ship an emergency overnight replacement package.")
	public String dispatchExpeditedReplacement(
			@McpToolParam(description = "The original order number being replaced") String orderId,
			@McpToolParam(description = "Destination postal/zip code") String destinationZip) {
		System.out.println("  --> [MCP SERVER EXECUTING] dispatchExpeditedReplacement for: " + orderId + " to " + destinationZip);

		return "SUCCESS: Replacement package REPL-9901 dispatched via FedEx Priority Overnight. Guaranteed delivery: Tomorrow 10:30 AM.";
	}
}