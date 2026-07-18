package org.mitre.synthea.br.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * OpenAI Chat Completions client (BYOK).
 */
public final class OpenAiClient implements LlmClient {

  private static final String DEFAULT_URL = "https://api.openai.com/v1/chat/completions";

  private final String apiKey;
  private final String model;
  private final double temperature;
  private final HttpClient httpClient;
  private final int timeoutSeconds;
  private final String endpointUrl;

  /**
   * Creates a client with the given settings.
   *
   * @param apiKey API key (BYOK)
   * @param model model id
   * @param temperature sampling temperature
   * @param timeoutSeconds HTTP timeout
   */
  public OpenAiClient(String apiKey, String model, double temperature, int timeoutSeconds) {
    this.apiKey = apiKey;
    this.model = model;
    this.temperature = temperature;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
    this.timeoutSeconds = timeoutSeconds;
    this.endpointUrl = DEFAULT_URL;
  }

  /**
   * Test hook: custom endpoint URL (same package tests only).
   */
  OpenAiClient(String apiKey, String model, double temperature, int timeoutSeconds,
      HttpClient httpClient, String endpointUrl) {
    this.apiKey = apiKey;
    this.model = model;
    this.temperature = temperature;
    this.httpClient = httpClient;
    this.timeoutSeconds = timeoutSeconds;
    this.endpointUrl = endpointUrl;
  }

  @Override
  public String complete(String systemPrompt, String userPrompt) throws LlmException {
    JsonObject body = new JsonObject();
    body.addProperty("model", model);
    body.addProperty("temperature", temperature);
    JsonArray messages = new JsonArray();
    JsonObject system = new JsonObject();
    system.addProperty("role", "system");
    system.addProperty("content", systemPrompt);
    messages.add(system);
    JsonObject user = new JsonObject();
    user.addProperty("role", "user");
    user.addProperty("content", userPrompt);
    messages.add(user);
    body.add("messages", messages);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(endpointUrl))
          .timeout(Duration.ofSeconds(timeoutSeconds))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
          .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new LlmException("OpenAI HTTP " + response.statusCode() + ": " + response.body());
      }
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      return json.getAsJsonArray("choices").get(0).getAsJsonObject()
          .getAsJsonObject("message").get("content").getAsString();
    } catch (LlmException e) {
      throw e;
    } catch (Exception e) {
      throw new LlmException("OpenAI request failed: " + e.getMessage(), e);
    }
  }
}
