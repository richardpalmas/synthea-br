package org.mitre.synthea.br.ai;

/**
 * Factory for LLM clients based on configuration.
 */
public final class LlmClientFactory {

  private LlmClientFactory() {
  }

  /**
   * Builds an LLM client from current {@link AiEnrichmentConfig}.
   *
   * @return configured client
   */
  public static LlmClient create() {
    String apiKey = AiEnrichmentConfig.resolveApiKey();
    String provider = AiEnrichmentConfig.getProvider();
    String model = AiEnrichmentConfig.getModel();
    double temperature = AiEnrichmentConfig.getTemperature();
    int timeout = AiEnrichmentConfig.getTimeoutSeconds();

    if ("gemini".equals(provider)) {
      return new GeminiClient(apiKey, model, temperature, timeout);
    }
    if ("medgemma".equals(provider)) {
      return new HuggingFaceInferenceClient(apiKey, model, temperature, timeout);
    }
    return new OpenAiClient(apiKey, model, temperature, timeout);
  }
}
