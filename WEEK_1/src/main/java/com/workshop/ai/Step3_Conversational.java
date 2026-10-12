package com.workshop.ai;

import java.util.ArrayList;
import java.util.List;

public class Step3_Conversational {
	
	private static List<Message> ALL_MESSAGES = new ArrayList<Message>();
	
	
	public static void main(String[] args)throws Exception {
		
		String systemInstruction = "You are cool";
		chat(systemInstruction);
	}
	
	
	
	public static void chat(String systemInstruction) {
		while(true) {
			String userInput = Helper.getUserInput("User");
			
			ALL_MESSAGES.add(new Message("User", userInput));
			
			String prompt = serializeAsText();
			
			
			System.err.println(prompt);
			
			String aiResponse = Step1_RawCall.chat(prompt, systemInstruction);
			System.out.println("AI:" + aiResponse);
			ALL_MESSAGES.add(new Message("AI", aiResponse));
		}
	}
	
	
	public static String serializeAsText() {
		StringBuilder b = new StringBuilder();
		for(Message m :  ALL_MESSAGES) {
			b.append(m.toString());
		}
		
		return b.toString();
	}

}
