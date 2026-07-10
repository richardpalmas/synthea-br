package org.mitre.synthea.br.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;

/**
 * HTTP integration tests for {@link WebServer}.
 */
public class WebServerTest {

  private WebServer server;
  private GenerationJobManager jobManager;
  private HttpClient client;

  /**
   * Start ephemeral web server on localhost.
   *
   * @throws Exception on setup errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    jobManager = GenerationJobManager.getInstance();
    jobManager.resetForTest();
    server = WebServer.start("127.0.0.1", 0);
    client = HttpClient.newHttpClient();
  }

  /**
   * Stop server and reset job manager.
   */
  @After
  public void tearDown() {
    if (server != null) {
      server.stop();
    }
    jobManager.resetForTest();
  }

  @Test
  public void testGetIndexHtml() throws Exception {
    int port = server.getBoundPort();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/"))
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("Synthea-br"));
    assertTrue(response.body().contains("Gerar cohort"));
  }

  @Test
  public void testGetConfigOptions() throws Exception {
    int port = server.getBoundPort();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/config/options"))
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("breast_cancer"));
    assertTrue(response.body().contains("medgemma"));
    assertTrue(response.body().contains("gpt-5.5"));
    assertTrue(response.body().contains("gemini-3.5-flash"));
    assertTrue(response.body().contains("google/medgemma-4b-it"));
    assertTrue(response.body().contains("\"label\""));
  }

  @Test
  public void testPostInvalidRequestReturns400() throws Exception {
    int port = server.getBoundPort();
    String body = "{\"population\":0,\"seed\":42}";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("População"));
  }

  @Test
  public void testConcurrentJobReturns409() throws Exception {
    jobManager.markRunningForTest();
    int port = server.getBoundPort();
    String body = "{\"population\":1,\"seed\":42,\"exportFhir\":false,"
        + "\"exportCsv\":false,\"exportHtml\":false}";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(409, response.statusCode());
    assertTrue(response.body().contains("Geração em andamento"));
  }

  @Test
  public void testGetConfigOptionsIncludesTrajectory() throws Exception {
    int port = server.getBoundPort();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/config/options"))
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"trajectory\""));
    assertTrue(response.body().contains("pathway_minimal"));
    assertTrue(response.body().contains("pre_onset_years:10"));
  }

  @Test
  public void testPostFocusedTrajectoryInvalidReturns400() throws Exception {
    int port = server.getBoundPort();
    String body = "{\"population\":5,\"seed\":42,\"pathwayFocus\":true,"
        + "\"exportFhir\":false,\"exportCsv\":false,\"exportHtml\":false}";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("condição clínica alvo"));
  }

  @Test
  public void testPostFocusedTrajectoryValidAccepted() throws Exception {
    int port = server.getBoundPort();
    String body = "{\"population\":1,\"seed\":42,\"brProfile\":true,"
        + "\"targetCondition\":\"breast_cancer\",\"gender\":\"F\","
        + "\"minAge\":45,\"maxAge\":75,\"pathwayFocus\":true,"
        + "\"moduleProfile\":\"pathway_minimal\",\"trajectoryMode\":\"episodic\","
        + "\"simulationWindow\":\"pre_onset_years:10\","
        + "\"exportFhir\":false,\"exportCsv\":false,\"exportHtml\":false}";
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/generate"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertTrue("Accepted job start", response.statusCode() == 200 || response.statusCode() == 202);
  }

  @Test
  public void testStatusEndpointIdleByDefault() throws Exception {
    int port = server.getBoundPort();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://127.0.0.1:" + port + "/api/generate/status"))
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.body().contains("\"state\":\"idle\""));
  }
}
