package com.workshop.ai.mcp;

import lombok.Data;

@Data
public class McpResponse {
	private String jsonrpc = "2.0";
    private Object id;
    private Object result;
	public McpResponse(Object id, Object result) {
		super();
		this.id = id;
		this.result = result;
	}
    
    
    
}
