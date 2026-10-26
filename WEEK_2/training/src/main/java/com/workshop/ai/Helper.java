package com.workshop.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

import org.json.JSONObject;

public class Helper {

	private final static HttpClient client = HttpClient.newHttpClient();

	private final static Scanner scanner = new Scanner(System.in);

	public static String httpPost(String endpoint, String payload) {

		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(endpoint))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String extractAnswer(String llmResponse) {
		JSONObject responseJson = new JSONObject(llmResponse);

		String answer = responseJson.getJSONArray("steps").getJSONObject(1).getJSONArray("content").getJSONObject(0)
				.getString("text");
		return answer;
	}

	public static String getUserInput(String prompt) {
		System.out.print(prompt == null || prompt.length() == 1? ">":prompt.length()> 1? prompt + ": ":prompt);
		String input = scanner.nextLine(); // Pauses and waits for user input
		return input;
	}

}
