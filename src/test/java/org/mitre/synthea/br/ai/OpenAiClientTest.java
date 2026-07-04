package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;

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
 * HTTP mock smoke tests for {@link OpenAiClient}.
 */
public class OpenAiClientTest {

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
      String response = "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";
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
  public void testCompleteSendsGpt55ModelInBody() throws Exception {
    OpenAiClient client = new OpenAiClient(
        "sk-test", "gpt-5.5", 0.1, 10, HttpClient.newHttpClient(), endpointUrl);
    String result = client.complete("system", "user");
    assertEquals("ok", result);

    JsonObject body = JsonParser.parseString(lastRequestBody).getAsJsonObject();
    assertEquals("gpt-5.5", body.get("model").getAsString());
  }
}
