package org.mitre.synthea.br.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mitre.synthea.br.ai.AiEnrichmentConfig;
import org.mitre.synthea.br.ai.AiModelCatalog;
import org.mitre.synthea.br.condition.SupportedConditions;

/**
 * JSON payload for {@code GET /api/config/options}.
 */
public class WebUiOptions {

  public long defaultSeed = 42L;
  public int defaultPopulation = 10;
  public List<String> supportedConditions = new ArrayList<>(SupportedConditions.supportedKeys());
  public List<String> gateModes = Arrays.asList("retry", "exclude");
  public List<ExportOption> exportOptions = Arrays.asList(
      new ExportOption("exportFhir", "FHIR R4", "exporter.fhir.export"),
      new ExportOption("exportCsv", "CSV", "exporter.csv.export"),
      new ExportOption("exportHtml", "HTML narrativo", "exporter.html.export")
  );
  public BreastCancerPreset breastCancerPreset = new BreastCancerPreset();
  public AiEnrichmentOptions aiEnrichment = new AiEnrichmentOptions();

  /**
   * AI enrichment form metadata.
   */
  public static final class AiEnrichmentOptions {
    public int maxPatients = AiEnrichmentConfig.getMaxPatients();
    public List<AiProviderOption> providers = buildProviders();
    public String helpText =
        "Enriquecimento opcional via orquestração MAI-DxO. Requer credencial BYOK. "
            + "Camada não-determinística — seed governa apenas a geração inicial.";

    private static List<AiProviderOption> buildProviders() {
      List<AiProviderOption> result = new ArrayList<>();
      for (AiModelCatalog.ProviderDefinition def : AiModelCatalog.getProviders()) {
        List<AiModelOption> models = new ArrayList<>();
        for (AiModelCatalog.ModelOption model : def.getModels()) {
          models.add(new AiModelOption(model.getId(), model.getLabel()));
        }
        result.add(new AiProviderOption(
            def.getId(),
            def.getLabel(),
            def.getDefaultModel(),
            def.getApiKeyHint(),
            def.getProviderHelpText(),
            models));
      }
      return result;
    }
  }

  /**
   * LLM provider and model choices for the web form.
   */
  public static final class AiProviderOption {
    public final String id;
    public final String label;
    public final String defaultModel;
    public final String apiKeyHint;
    public final String providerHelpText;
    public final List<AiModelOption> models;

    AiProviderOption(String id, String label, String defaultModel, String apiKeyHint,
        String providerHelpText, List<AiModelOption> models) {
      this.id = id;
      this.label = label;
      this.defaultModel = defaultModel;
      this.apiKeyHint = apiKeyHint;
      this.providerHelpText = providerHelpText;
      this.models = models;
    }
  }

  /**
   * Model id and display label for dropdowns.
   */
  public static final class AiModelOption {
    public final String id;
    public final String label;

    AiModelOption(String id, String label) {
      this.id = id;
      this.label = label;
    }
  }

  /**
   * Suggested demographic filters for the breast cancer pilot cohort.
   */
  public static final class BreastCancerPreset {
    public String gender = "F";
    public int minAge = 45;
    public int maxAge = 75;
    public String helpText =
        "Para câncer de mama, recomenda-se gênero F e idade 45–75 para reduzir tentativas do gate.";
  }

  /**
   * Export toggle metadata for the web form.
   */
  public static final class ExportOption {
    public final String field;
    public final String label;
    public final String configKey;

    ExportOption(String field, String label, String configKey) {
      this.field = field;
      this.label = label;
      this.configKey = configKey;
    }
  }
}
