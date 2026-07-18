package org.mitre.synthea.br.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.mitre.synthea.helpers.Config;

/**
 * Embedded HTTP server for the Synthea-br generation web UI.
 */
public final class WebServer {

  private static final Gson GSON = new Gson();
  private static final String STATIC_PREFIX = "br/web/";

  private final HttpServer server;
  private final GenerationJobManager jobManager = GenerationJobManager.getInstance();

  private WebServer(HttpServer server) {
    this.server = server;
  }

  /**
   * Start the web server using configuration properties.
   *
   * @return started server wrapper
   * @throws IOException when the server cannot bind
   */
  public static WebServer startFromConfig() throws IOException {
    String bind = Config.get("br.web.bind", "127.0.0.1");
    int port = Config.getAsInteger("br.web.port", 8080);
    return start(bind, port);
  }

  /**
   * Start the web server on the given bind address and port.
   *
   * @param bind host address (use 127.0.0.1 for localhost-only)
   * @param port TCP port (0 for ephemeral in tests)
   * @return started server wrapper
   * @throws IOException when the server cannot bind
   */
  public static WebServer start(String bind, int port) throws IOException {
    InetSocketAddress address = new InetSocketAddress(bind, port);
    HttpServer httpServer = HttpServer.create(address, 0);
    WebServer webServer = new WebServer(httpServer);
    webServer.registerHandlers();
    httpServer.setExecutor(Executors.newFixedThreadPool(4));
    httpServer.start();
    return webServer;
  }

  /**
   * @return actual bound port (useful when port 0 was requested)
   */
  public int getBoundPort() {
    return server.getAddress().getPort();
  }

  /**
   * Stop the HTTP server.
   */
  public void stop() {
    server.stop(0);
  }

  private void registerHandlers() {
    server.createContext("/", new StaticHandler());
    server.createContext("/api/config/options", new OptionsHandler());
    server.createContext("/api/generate", new GenerateHandler());
    server.createContext("/api/generate/status", new StatusHandler());
  }

  private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
    byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(statusCode, bytes.length);
    try (OutputStream os = exchange.getResponseBody()) {
      os.write(bytes);
    }
  }

  private void writeError(HttpExchange exchange, int statusCode, String message) throws IOException {
    JsonObject error = new JsonObject();
    error.addProperty("error", message);
    writeJson(exchange, statusCode, error);
  }

  private final class StaticHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      String path = exchange.getRequestURI().getPath();
      if (path.equals("/")) {
        path = "/index.html";
      }
      String resourcePath = STATIC_PREFIX + path.substring(1);
      try (InputStream stream = WebServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
        if (stream == null) {
          exchange.sendResponseHeaders(404, -1);
          return;
        }
        byte[] bytes = stream.readAllBytes();
        String contentType = contentTypeFor(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(bytes);
        }
      }
    }

    private String contentTypeFor(String path) {
      if (path.endsWith(".css")) {
        return "text/css; charset=utf-8";
      }
      if (path.endsWith(".js")) {
        return "application/javascript; charset=utf-8";
      }
      return "text/html; charset=utf-8";
    }
  }

  private final class OptionsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      WebUiOptions options = new WebUiOptions();
      options.defaultSeed = Config.getAsLong("br.web.default_seed", 42L);
      options.defaultPopulation = Config.getAsInteger("br.web.default_population", 10);
      writeJson(exchange, 200, options);
    }
  }

  private final class GenerateHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      if (jobManager.isRunning()) {
        writeError(exchange, 409, "Geração em andamento");
        return;
      }
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      GenerationRequest request;
      try {
        request = GSON.fromJson(body, GenerationRequest.class);
        if (request == null) {
          writeError(exchange, 400, "Corpo JSON inválido.");
          return;
        }
      } catch (Exception ex) {
        writeError(exchange, 400, "Corpo JSON inválido.");
        return;
      }
      List<String> errors = request.validate();
      if (!errors.isEmpty()) {
        JsonObject payload = new JsonObject();
        payload.addProperty("error", String.join(" ", errors));
        writeJson(exchange, 400, payload);
        return;
      }
      try {
        String jobId = jobManager.startJob(request);
        JsonObject payload = new JsonObject();
        payload.addProperty("jobId", jobId);
        payload.addProperty("status", "running");
        writeJson(exchange, 202, payload);
      } catch (IllegalStateException ex) {
        writeError(exchange, 409, ex.getMessage());
      } catch (Exception ex) {
        ex.printStackTrace();
        writeError(exchange, 500, "Erro interno ao iniciar a geração.");
      }
    }
  }

  private final class StatusHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        exchange.sendResponseHeaders(405, -1);
        return;
      }
      writeJson(exchange, 200, jobManager.getStatus());
    }
  }
}
