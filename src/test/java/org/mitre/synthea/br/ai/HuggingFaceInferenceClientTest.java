package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * HTTP mock tests for {@link HuggingFaceInferenceClient}.
 */
public class HuggingFaceInferenceClientTest {

  private HttpServer server;
  private String endpointUrl;
  private String lastRequestBody;

  /**
   * Starts a local HTTP server that records the request body.
   *
   * @throws IOException on server startup errors
   */
  @Before
  public void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    int port = server.getAddress().getPort();
    endpointUrl = "http://127.0.0.1:" + port + "/v1/chat/completions";
    server.createContext("/v1/chat/completions", exchange -> {
      lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      String response = "{\"choices\":[{\"message\":{\"content\":\"resposta hf\"}}]}";
      exchange.sendResponseHeaders(200, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });
    server.start();
  }

  /**
   * Stops the mock HTTP server.
   */
  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  public void testCompleteSendsMedgemmaModelInBody() throws Exception {
    HuggingFaceInferenceClient client = new HuggingFaceInferenceClient(
        "hf_test", "google/medgemma-4b-it", 0.2, 10,
        HttpClient.newHttpClient(), endpointUrl);
    String result = client.complete("system", "user");
    assertEquals("resposta hf", result);

    JsonObject body = JsonParser.parseString(lastRequestBody).getAsJsonObject();
    assertEquals("google/medgemma-4b-it", body.get("model").getAsString());
    assertEquals(0.2, body.get("temperature").getAsDouble(), 0.001);
  }

  @Test
  public void testHttp404ReturnsInferenceProviderMessage() throws Exception {
    server.stop(0);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    int port = server.getAddress().getPort();
    endpointUrl = "http://127.0.0.1:" + port + "/v1/chat/completions";
    server.createContext("/v1/chat/completions", exchange -> {
      String response = "model not deployed";
      exchange.sendResponseHeaders(404, response.length());
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes(StandardCharsets.UTF_8));
      }
    });
    server.start();

    HuggingFaceInferenceClient client = new HuggingFaceInferenceClient(
        "hf_test", "google/medgemma-4b-it", 0.2, 10,
        HttpClient.newHttpClient(), endpointUrl);
    try {
      client.complete("system", "user");
      assertTrue("expected LlmException", false);
    } catch (LlmException ex) {
      assertTrue(ex.getMessage().contains("Inference Provider"));
    }
  }
}
