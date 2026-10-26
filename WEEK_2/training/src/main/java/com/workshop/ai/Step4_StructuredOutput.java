package com.workshop.ai;

public class Step4_StructuredOutput {

	public static void main(String[] args) {
		
		String systemInstruction = """
				
				You are a professional
				
				Your task is to provide me a list of countries based on the user input.
				
				You should strictly provide your response in the following json format. and nothing else
				
				[
					{
						countryName:'country name',
						countryCode:'code',
						estimatedPopulation: number
					}
				]
				
				
				""";

		Step3_Conversational.chat(systemInstruction);
		
		
	}

}
