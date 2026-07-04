package org.mitre.synthea.br.ai;

/**
 * Test hook to override {@link LlmClientFactory} behavior in unit tests.
 */
public final class LlmClientFactoryHolder {

  private static LlmClient override;

  private LlmClientFactoryHolder() {
  }

  /**
   * Sets a client used instead of provider REST calls.
   *
   * @param client mock or stub client
   */
  public static void setClient(LlmClient client) {
    override = client;
  }

  /**
   * Clears test override.
   */
  public static void reset() {
    override = null;
  }

  /**
   * Resolves client for production or test override.
   *
   * @return LLM client
   */
  public static LlmClient resolve() {
    if (override != null) {
      return override;
    }
    return LlmClientFactory.create();
  }
}
