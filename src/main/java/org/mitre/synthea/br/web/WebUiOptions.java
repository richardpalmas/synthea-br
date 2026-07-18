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
  public int maxPopulation = GenerationRequest.MAX_POPULATION;
  /** Default checked state for {@code br.profile=br} on the web form. */
  public boolean defaultBrProfile = true;
  public List<String> supportedConditions = new ArrayList<>(SupportedConditions.supportedKeys());
  public List<String> gateModes = Arrays.asList("retry", "exclude");
  public List<ExportOption> exportOptions = Arrays.asList(
      new ExportOption("exportFhir", "FHIR R4", "exporter.fhir.export"),
      new ExportOption("exportCsv", "CSV", "exporter.csv.export"),
      new ExportOption("exportHtml", "HTML narrativo", "exporter.html.export")
  );
  public BreastCancerPreset breastCancerPreset = new BreastCancerPreset();
  public TrajectoryOptions trajectory = new TrajectoryOptions();
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
        "Para câncer de mama, recomenda-se gênero F e idade 45–75 para reduzir tentativas do gate. "
            + "Perfil BR e trajetória focada já vêm pré-aplicados na UI; este preset preenche a demografia.";
    public FocusedTrajectoryPreset focusedTrajectory = new FocusedTrajectoryPreset();
    public boolean defaultExportHtml = true;
  }

  /**
   * Epic 9 trajectory form metadata and preset values (Story 7.2).
   */
  public static final class TrajectoryOptions {
    public boolean defaultPathwayFocus = true;
    public String defaultHtmlPathwayMode = TrajectoryWebConstants.HTML_MODE_ORIENTADOR;
    public String defaultModuleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    public String defaultTrajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC;
    public String defaultSimulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;
    public String defaultPathwayArchetype = TrajectoryWebConstants.ARCHETYPE_AUTO;
    public List<SelectOption> htmlPathwayModes = buildHtmlModes();
    public List<SelectOption> moduleProfiles = buildModuleProfiles();
    public List<SelectOption> trajectoryModes = buildTrajectoryModes();
    public List<SelectOption> simulationWindows = buildSimulationWindows();
    public List<SelectOption> pathwayArchetypes = buildPathwayArchetypes();
    public String sectionHelpText =
        "Overrides opcionais da receita padrão de relatório (focus, orientador, pathway_minimal, "
            + "episodic, pre_onset_years:10, archetype auto). Requer condição clínica alvo para "
            + "episodic. Arquétipo forçado (remission/progression) — apenas breast_cancer.";
    public String archetypeHelpText =
        "Epic 10 — <code>br.pathway.archetype</code>: auto usa ramos probabilísticos; "
            + "remission e progression alinham marcos de eventos aos prontuários example1/example2.";
    public FocusedTrajectoryPreset focusedTrajectoryPreset = new FocusedTrajectoryPreset();
    public ArchetypeExamplePreset remissionExample = ArchetypeExamplePreset.remission();
    public ArchetypeExamplePreset progressionExample = ArchetypeExamplePreset.progression();

    private static List<SelectOption> buildHtmlModes() {
      return Arrays.asList(
          new SelectOption(TrajectoryWebConstants.HTML_MODE_AUTO,
              "auto — orientador se focus ligado, senão full"),
          new SelectOption(TrajectoryWebConstants.HTML_MODE_ORIENTADOR, "orientador"),
          new SelectOption(TrajectoryWebConstants.HTML_MODE_PESQUISADOR, "pesquisador"),
          new SelectOption(TrajectoryWebConstants.HTML_MODE_FULL, "full (Epic 6)"));
    }

    private static List<SelectOption> buildModuleProfiles() {
      return Arrays.asList(
          new SelectOption(TrajectoryWebConstants.MODULE_PROFILE_FULL, "full — todos os módulos"),
          new SelectOption(TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL,
              "pathway_minimal — geração enxuta (Epic 9D)"));
    }

    private static List<SelectOption> buildTrajectoryModes() {
      return Arrays.asList(
          new SelectOption(TrajectoryWebConstants.TRAJECTORY_MODE_LIFESPAN,
              "lifespan — simulação vida inteira"),
          new SelectOption(TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC,
              "episodic — marcador de fases GMF (Epic 9E)"));
    }

    private static List<SelectOption> buildSimulationWindows() {
      return Arrays.asList(
          new SelectOption(TrajectoryWebConstants.SIMULATION_WINDOW_FULL,
              "full_lifespan — desde o nascimento"),
          new SelectOption(TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10,
              "pre_onset_years:10 — 10 anos antes do início (piloto mama)"));
    }

    private static List<SelectOption> buildPathwayArchetypes() {
      return Arrays.asList(
          new SelectOption(TrajectoryWebConstants.ARCHETYPE_AUTO,
              "auto — ramos probabilísticos do GMF"),
          new SelectOption(TrajectoryWebConstants.ARCHETYPE_REMISSION,
              "remission — luminal / desfecho favorável (example1)"),
          new SelectOption(TrajectoryWebConstants.ARCHETYPE_PROGRESSION,
              "progression — metástase / paliativo / óbito (example2)"));
    }
  }

  /**
   * Preset values for breast cancer focused trajectory (GUIA receita H).
   */
  public static final class FocusedTrajectoryPreset {
    public boolean pathwayFocus = true;
    public String htmlPathwayMode = TrajectoryWebConstants.HTML_MODE_ORIENTADOR;
    public String moduleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    public String trajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC;
    public String simulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;
    public String pathwayArchetype = TrajectoryWebConstants.ARCHETYPE_AUTO;
    public boolean exportHtml = true;
  }

  /**
   * Epic 10 preset aligned to example1 (remission) or example2 (progression) event milestones.
   */
  public static final class ArchetypeExamplePreset {
    public String label;
    public String helpText;
    public String pathwayArchetype;
    public boolean pathwayFocus = true;
    public String htmlPathwayMode = TrajectoryWebConstants.HTML_MODE_ORIENTADOR;
    public String moduleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    public String trajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_LIFESPAN;
    public String simulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;
    public boolean exportHtml = true;

    static ArchetypeExamplePreset remission() {
      ArchetypeExamplePreset preset = new ArchetypeExamplePreset();
      preset.label = "Example1 — remission";
      preset.helpText =
          "Luminal, desfecho favorável (neoadjuvância, mastectomia, quimio/radio, seguimento).";
      preset.pathwayArchetype = TrajectoryWebConstants.ARCHETYPE_REMISSION;
      return preset;
    }

    static ArchetypeExamplePreset progression() {
      ArchetypeExamplePreset preset = new ArchetypeExamplePreset();
      preset.label = "Example2 — progression";
      preset.helpText =
          "Metástase, paliativo e óbito (estadiamento por imagem, hospice, terminal).";
      preset.pathwayArchetype = TrajectoryWebConstants.ARCHETYPE_PROGRESSION;
      return preset;
    }
  }

  /**
   * Select option for trajectory dropdowns.
   */
  public static final class SelectOption {
    public final String value;
    public final String label;

    SelectOption(String value, String label) {
      this.value = value;
      this.label = label;
    }
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
