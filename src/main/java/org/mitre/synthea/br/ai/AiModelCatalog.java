package org.mitre.synthea.br.ai;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Single source of truth for supported LLM providers and models (Epic 8 enrichment).
 * Catalog curated as of 2026-07.
 */
public final class AiModelCatalog {

  private static final List<ProviderDefinition> PROVIDERS = List.of(
      new ProviderDefinition(
          "openai",
          "OpenAI",
          "gpt-4o-mini",
          "Chave de API OpenAI (sk-...)",
          null,
          List.of(
              model("gpt-5.5", "GPT-5.5 (frontier)"),
              model("gpt-5.5-pro", "GPT-5.5 Pro"),
              model("gpt-5.4", "GPT-5.4"),
              model("gpt-5.4-mini", "GPT-5.4 Mini"),
              model("gpt-4o", "GPT-4o"),
              model("gpt-4o-mini", "GPT-4o Mini (padrão)")
          )),
      new ProviderDefinition(
          "gemini",
          "Google Gemini",
          "gemini-2.5-flash",
          "Chave de API Gemini (AI Studio)",
          null,
          List.of(
              model("gemini-3.5-flash", "Gemini 3.5 Flash"),
              model("gemini-3.1-pro-preview", "Gemini 3.1 Pro (preview)"),
              model("gemini-2.5-flash", "Gemini 2.5 Flash (padrão)"),
              model("gemini-2.5-flash-lite", "Gemini 2.5 Flash-Lite"),
              model("gemini-2.5-pro", "Gemini 2.5 Pro")
          )),
      new ProviderDefinition(
          "medgemma",
          "Google MedGemma",
          "google/medgemma-4b-it",
          "Token Hugging Face (hf_...) com permissão Inference Providers",
          "MedGemma via Hugging Face Inference API. O modelo pode ainda não ter "
              + "provider serverless no Hub — verifique disponibilidade antes de usar.",
          List.of(
              model("google/medgemma-4b-it", "MedGemma 4B (texto, recomendado)"),
              model("google/medgemma-27b-text-it", "MedGemma 27B Text")
          ))
  );

  private AiModelCatalog() {
  }

  /**
   * Returns all supported provider definitions.
   *
   * @return immutable provider list
   */
  public static List<ProviderDefinition> getProviders() {
    return PROVIDERS;
  }

  /**
   * Returns supported provider ids.
   *
   * @return provider id list
   */
  public static List<String> getProviderIds() {
    return PROVIDERS.stream().map(ProviderDefinition::getId).toList();
  }

  /**
   * Whether the provider id is supported.
   *
   * @param provider provider id
   * @return true when known
   */
  public static boolean isSupportedProvider(String provider) {
    return findProvider(provider).isPresent();
  }

  /**
   * Default model id for a provider.
   *
   * @param provider provider id
   * @return default model or gpt-4o-mini fallback
   */
  public static String defaultModel(String provider) {
    return findProvider(provider)
        .map(ProviderDefinition::getDefaultModel)
        .orElse("gpt-4o-mini");
  }

  /**
   * BYOK hint shown in the web UI for the provider's credential field.
   *
   * @param provider provider id
   * @return hint text
   */
  public static String apiKeyHint(String provider) {
    return findProvider(provider)
        .map(ProviderDefinition::getApiKeyHint)
        .orElse("API key (BYOK)");
  }

  /**
   * Whether the model is in the catalog for the given provider.
   *
   * @param provider provider id
   * @param model model id
   * @return true when supported
   */
  public static boolean isSupportedModel(String provider, String model) {
    if (model == null || model.trim().isEmpty()) {
      return false;
    }
    Optional<ProviderDefinition> def = findProvider(provider);
    if (def.isEmpty()) {
      return false;
    }
    String normalized = model.trim();
    return def.get().getModels().stream()
        .anyMatch(m -> m.getId().equals(normalized));
  }

  /**
   * Comma-separated list of supported models for error messages.
   *
   * @param provider provider id
   * @return model ids joined
   */
  public static String supportedModelsList(String provider) {
    return findProvider(provider)
        .map(p -> p.getModels().stream().map(ModelOption::getId).reduce((a, b) -> a + ", " + b)
            .orElse(""))
        .orElse("");
  }

  private static Optional<ProviderDefinition> findProvider(String provider) {
    if (provider == null) {
      return Optional.empty();
    }
    String normalized = provider.trim().toLowerCase(Locale.ROOT);
    return PROVIDERS.stream()
        .filter(p -> p.getId().equals(normalized))
        .findFirst();
  }

  private static ModelOption model(String id, String label) {
    return new ModelOption(id, label);
  }

  /**
   * Provider with models and UI metadata.
   */
  public static final class ProviderDefinition {
    private final String id;
    private final String label;
    private final String defaultModel;
    private final String apiKeyHint;
    private final String providerHelpText;
    private final List<ModelOption> models;

    ProviderDefinition(String id, String label, String defaultModel, String apiKeyHint,
        String providerHelpText, List<ModelOption> models) {
      this.id = id;
      this.label = label;
      this.defaultModel = defaultModel;
      this.apiKeyHint = apiKeyHint;
      this.providerHelpText = providerHelpText;
      this.models = Collections.unmodifiableList(models);
    }

    /**
     * @return provider id
     */
    public String getId() {
      return id;
    }

    /**
     * @return display label
     */
    public String getLabel() {
      return label;
    }

    /**
     * @return default model id
     */
    public String getDefaultModel() {
      return defaultModel;
    }

    /**
     * @return API key field hint
     */
    public String getApiKeyHint() {
      return apiKeyHint;
    }

    /**
     * @return optional provider-specific help (may be null)
     */
    public String getProviderHelpText() {
      return providerHelpText;
    }

    /**
     * @return supported models
     */
    public List<ModelOption> getModels() {
      return models;
    }
  }

  /**
   * Model id and friendly label for UI dropdowns.
   */
  public static final class ModelOption {
    private final String id;
    private final String label;

    ModelOption(String id, String label) {
      this.id = id;
      this.label = label;
    }

    /**
     * @return model id sent to the API
     */
    public String getId() {
      return id;
    }

    /**
     * @return human-readable label
     */
    public String getLabel() {
      return label;
    }
  }
}
