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
 * Google Gemini generateContent client (BYOK).
 */
public final class GeminiClient implements LlmClient {

  private final String apiKey;
  private final String model;
  private final double temperature;
  private final int timeoutSeconds;
  private final HttpClient httpClient;

  /**
   * Creates a client with the given settings.
   *
   * @param apiKey API key (BYOK)
   * @param model model id
   * @param temperature sampling temperature
   * @param timeoutSeconds HTTP timeout
   */
  public GeminiClient(String apiKey, String model, double temperature, int timeoutSeconds) {
    this.apiKey = apiKey;
    this.model = model;
    this.temperature = temperature;
    this.timeoutSeconds = timeoutSeconds;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
  }

  @Override
  public String complete(String systemPrompt, String userPrompt) throws LlmException {
    // API key is sent via header (not query string) to avoid it leaking into proxy/access
    // logs or exception messages that may include the request URI.
    String url = "https://generativelanguage.googleapis.com/v1beta/models/"
        + model + ":generateContent";

    JsonObject body = new JsonObject();
    JsonObject generationConfig = new JsonObject();
    generationConfig.addProperty("temperature", temperature);
    body.add("generationConfig", generationConfig);

    JsonArray contents = new JsonArray();
    JsonObject content = new JsonObject();
    JsonArray parts = new JsonArray();
    JsonObject part = new JsonObject();
    part.addProperty("text", systemPrompt + "\n\n" + userPrompt);
    parts.add(part);
    content.add("parts", parts);
    contents.add(content);
    body.add("contents", contents);

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(timeoutSeconds))
          .header("Content-Type", "application/json")
          .header("x-goog-api-key", apiKey)
          .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
          .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new LlmException("Gemini HTTP " + response.statusCode() + ": " + response.body());
      }
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      return json.getAsJsonArray("candidates").get(0).getAsJsonObject()
          .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject()
          .get("text").getAsString();
    } catch (LlmException e) {
      throw e;
    } catch (Exception e) {
      throw new LlmException("Gemini request failed: " + e.getMessage(), e);
    }
  }
}
