package com.workshop.ai.mcp;

import java.util.Map;

import lombok.Data;

@Data
public class McpRequest {
	private String jsonrpc = "2.0";
	private Object id; // Can be String or Integer
	private String method; // "initialize", "tools/list", or "tools/call"
	private Map<String, Object> params;

	
}