package org.mitre.synthea.br.web;

import java.io.IOException;

import org.mitre.synthea.helpers.Config;

/**
 * Entry point for the Synthea-br web generation UI.
 */
public final class WebServerLauncher {

  private WebServerLauncher() {
  }

  /**
   * Start the web server and block until interrupted.
   *
   * @throws IOException when the server cannot start
   */
  public static void startAndBlock() throws IOException {
    WebServer server = WebServer.startFromConfig();
    String bind = Config.get("br.web.bind", "127.0.0.1");
    int port = server.getBoundPort();
    System.out.println("Synthea-br interface web disponível em http://" + bind + ":" + port);
    System.out.println("Pressione Ctrl+C para encerrar.");
    try {
      Thread.currentThread().join();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    } finally {
      server.stop();
    }
  }
}
