package org.mitre.synthea.br.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.helpers.Config;

/**
 * Resolves AI enrichment configuration from {@code synthea.properties} and CLI overrides.
 */
public final class AiEnrichmentConfig {

  private static final String PREFIX = "br.ai.";
  private static final String ENABLED_KEY = PREFIX + "enrichment.enabled";
  private static final String PROVIDER_KEY = PREFIX + "provider";
  private static final String MODEL_KEY = PREFIX + "model";
  private static final String API_KEY_KEY = PREFIX + "api_key";
  private static final String API_KEY_FILE_KEY = PREFIX + "api_key_file";
  private static final String MAX_ITERATIONS_KEY = PREFIX + "max_iterations";
  private static final String MAX_PATIENTS_KEY = PREFIX + "max_patients";
  private static final String TEMPERATURE_KEY = PREFIX + "temperature";
  private static final String TIMEOUT_KEY = PREFIX + "timeout_seconds";
  private static final String JSON_PARSE_RETRIES_KEY = PREFIX + "json_parse_retries";
  private static final String TRUNCATION_CONTINUATION_MAX_KEY =
      PREFIX + "truncation_continuation_max";
  private static final String NARRATIVE_PERSONA_MODE_KEY = PREFIX + "narrative.persona_mode";
  private static final String BIAS_TEST_ENABLED_KEY = PREFIX + "bias_test.enabled";
  private static final String BIAS_TEST_LENGTH_THRESHOLD_KEY =
      PREFIX + "bias_test.length_threshold";

  private AiEnrichmentConfig() {
  }

  /**
   * Whether AI enrichment is enabled and prerequisites are met.
   *
   * @return true when enabled and BR profile is active
   */
  public static boolean isEnabled() {
    return Config.getAsBoolean(ENABLED_KEY, false) && BrProfile.isActive();
  }

  /**
   * Validates configuration when enrichment is enabled.
   *
   * @throws IllegalStateException when required settings are missing or invalid
   */
  public static void validateWhenEnabled() {
    if (!Config.getAsBoolean(ENABLED_KEY, false)) {
      return;
    }
    if (!BrProfile.isActive()) {
      throw new IllegalStateException(
          "br.ai.enrichment.enabled requer br.profile=br.");
    }
    String provider = getProvider();
    if (!AiModelCatalog.isSupportedProvider(provider)) {
      throw new IllegalStateException(
          "br.ai.provider deve ser um de: " + String.join(", ", AiModelCatalog.getProviderIds())
              + " (valor: " + provider + ").");
    }
    String apiKey = resolveApiKey();
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalStateException(
          "br.ai.api_key ou br.ai.api_key_file é obrigatório quando enriquecimento IA está ativo.");
    }
    validateModel();
    if (getMaxIterations() < 1) {
      throw new IllegalStateException("br.ai.max_iterations deve ser >= 1.");
    }
    if (getMaxPatients() < 1) {
      throw new IllegalStateException("br.ai.max_patients deve ser >= 1.");
    }
    if (getJsonParseRetries() < 0) {
      throw new IllegalStateException("br.ai.json_parse_retries deve ser >= 0.");
    }
    if (getTruncationContinuationMax() < 0) {
      throw new IllegalStateException("br.ai.truncation_continuation_max deve ser >= 0.");
    }
  }

  /**
   * Validates configured model against {@link AiModelCatalog}.
   *
   * @throws IllegalStateException when model is not in the catalog for the provider
   */
  public static void validateModel() {
    String provider = getProvider();
    String model = getModel();
    if (!AiModelCatalog.isSupportedModel(provider, model)) {
      throw new IllegalStateException(
          "br.ai.model inválido para " + provider + ": " + model
              + ". Valores aceitos: " + AiModelCatalog.supportedModelsList(provider));
    }
  }

  /**
   * LLM provider id: {@code openai}, {@code gemini}, or {@code medgemma}.
   *
   * @return provider name
   */
  public static String getProvider() {
    String value = Config.get(PROVIDER_KEY, "openai");
    return value == null ? "openai" : value.trim().toLowerCase();
  }

  /**
   * Model name for the selected provider.
   *
   * @return model id
   */
  public static String getModel() {
    String configured = Config.get(MODEL_KEY);
    if (configured != null && !configured.trim().isEmpty()) {
      return configured.trim();
    }
    return AiModelCatalog.defaultModel(getProvider());
  }

  /**
   * Maximum debate iterations per patient.
   *
   * @return iteration cap
   */
  public static int getMaxIterations() {
    return Config.getAsInteger(MAX_ITERATIONS_KEY, 5);
  }

  /**
   * Safety cap on patients enriched per run.
   *
   * @return patient cap
   */
  public static int getMaxPatients() {
    return Config.getAsInteger(MAX_PATIENTS_KEY, 10);
  }

  /**
   * LLM temperature.
   *
   * @return temperature between 0 and 2
   */
  public static double getTemperature() {
    return Config.getAsDouble(TEMPERATURE_KEY, 0.2);
  }

  /**
   * HTTP timeout per LLM call in seconds.
   *
   * @return timeout seconds
   */
  public static int getTimeoutSeconds() {
    return Config.getAsInteger(TIMEOUT_KEY, 120);
  }

  /**
   * Maximum LLM cleanup attempts after local JSON parse failure (Story 8.1).
   *
   * @return retry count (default 1)
   */
  public static int getJsonParseRetries() {
    return Config.getAsInteger(JSON_PARSE_RETRIES_KEY, 1);
  }

  /**
   * Maximum truncation continuation calls per persona turn (Story 8.1).
   *
   * @return continuation cap (default 1)
   */
  public static int getTruncationContinuationMax() {
    return Config.getAsInteger(TRUNCATION_CONTINUATION_MAX_KEY, 1);
  }

  /**
   * Narrative writing persona assignment mode (Story 8.2).
   *
   * @return {@code deterministic} or {@code random}
   */
  public static String getNarrativePersonaMode() {
    String value = Config.get(NARRATIVE_PERSONA_MODE_KEY, "deterministic");
    return value == null ? "deterministic" : value.trim().toLowerCase();
  }

  /**
   * Whether demographic bias testing runs after enrichment (Story 8.2).
   *
   * @return true when bias test is enabled
   */
  public static boolean isBiasTestEnabled() {
    return Config.getAsBoolean(BIAS_TEST_ENABLED_KEY, false);
  }

  /**
   * Relative length delta threshold for flagging bias (Story 8.2).
   *
   * @return threshold between 0 and 1 (default 0.20)
   */
  public static double getBiasTestLengthThreshold() {
    return Config.getAsDouble(BIAS_TEST_LENGTH_THRESHOLD_KEY, 0.20);
  }

  /**
   * Resolves API key from config or key file. Never log the return value.
   *
   * @return API key or null
   */
  public static String resolveApiKey() {
    String direct = Config.get(API_KEY_KEY);
    if (direct != null && !direct.trim().isEmpty()) {
      return direct.trim();
    }
    String filePath = Config.get(API_KEY_FILE_KEY);
    if (filePath == null || filePath.trim().isEmpty()) {
      return null;
    }
    try {
      return Files.readString(Path.of(filePath.trim()), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      throw new IllegalStateException(
          "Não foi possível ler br.ai.api_key_file: " + e.getMessage(), e);
    }
  }

  /**
   * Temporarily sets API key in memory (web UI). Cleared after job via {@link #clearApiKey()}.
   *
   * @param apiKey user's API key
   */
  public static void setTransientApiKey(String apiKey) {
    if (apiKey == null || apiKey.trim().isEmpty()) {
      Config.remove(API_KEY_KEY);
    } else {
      Config.set(API_KEY_KEY, apiKey.trim());
    }
  }

  /**
   * Removes transient API key from config after a run.
   */
  public static void clearApiKey() {
    Config.remove(API_KEY_KEY);
  }
}
