package org.mitre.synthea.br.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Orchestrates cohort-level AI enrichment after generation and before export.
 */
public final class AiEnrichmentService {

  private static final String LOG_RELATIVE = "br/ai/enrichment_log.json";
  private static volatile CohortEnrichmentLog lastLog;

  private AiEnrichmentService() {
  }

  /**
   * Returns the last enrichment log from the most recent run (for manifest extension).
   *
   * @return last log or null
   */
  public static CohortEnrichmentLog getLastLog() {
    return lastLog;
  }

  /**
   * Enriches all generated patients when AI enrichment is enabled.
   *
   * @param generator completed generator with patient store
   */
  public static void enrichCohort(Generator generator) {
    if (!AiEnrichmentConfig.isEnabled()) {
      return;
    }

    try {
      AiEnrichmentConfig.validateWhenEnabled();

      List<Person> patients = generator.getGeneratedPatients();
      if (patients == null || patients.isEmpty()) {
        System.out.println(
            "AiEnrichment: nenhum paciente no internalStore — enriquecimento ignorado.");
        return;
      }

      int cap = Math.min(patients.size(), AiEnrichmentConfig.getMaxPatients());
      if (patients.size() > cap) {
        System.out.println(
            "AiEnrichment: limitando a " + cap + " pacientes (br.ai.max_patients).");
      }

      LlmClient client = LlmClientFactoryHolder.resolve();
      MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(
          client, AiEnrichmentConfig.getMaxIterations());

      CohortEnrichmentLog log = new CohortEnrichmentLog();
      log.setMetadata(
          AiEnrichmentConfig.getProvider(),
          AiEnrichmentConfig.getModel(),
          false);

      AiEnrichmentProgress progress = AiEnrichmentProgress.getInstance();
      progress.start(cap);

      System.out.printf("AiEnrichment: iniciando MAI-DxO para %d paciente(s)...%n", cap);

      long runSeed = generator.options != null ? generator.options.seed : 0L;
      String personaMode = AiEnrichmentConfig.getNarrativePersonaMode();
      boolean biasTest = AiEnrichmentConfig.isBiasTestEnabled();
      double biasThreshold = AiEnrichmentConfig.getBiasTestLengthThreshold();
      Map<String, Object> biasReport = biasTest
          ? BiasReportWriter.newReport(biasThreshold) : null;

      List<Person> slice = new ArrayList<>(patients.subList(0, cap));
      RobustnessStats cohortStats = new RobustnessStats();
      for (Person person : slice) {
        try {
          PatientEnrichmentResult result = orchestrator.enrichPatient(person);
          cohortStats.add(result.getRobustnessStats());

          long personaSeed = person.populationSeed != 0L ? person.populationSeed : runSeed;
          String patientId = person.attributes.getOrDefault(Person.ID, "unknown").toString();
          NarrativeWritingPersona persona = NarrativePersonaAssigner.assign(
              patientId, personaSeed, personaMode);

          // Demographic block only when bias test is on (baseline + swaps share context).
          String demoBlock = biasTest
              ? DemographicBiasSwapper.buildDemographicBlock(person) : null;
          String patientSummary = AiNarrativeSummarizer.summarizePatient(
              client, result, persona, demoBlock);
          PatientEnrichmentResult enriched = result
              .withNarrativeSummary(patientSummary)
              .withWritingPersona(persona.getId());
          log.addPatient(enriched);

          if (biasTest) {
            runBiasSwaps(client, person, result, persona, patientSummary, biasThreshold,
                biasReport);
          }

          System.out.printf("AiEnrichment: paciente %s — %d correção(ões), %d flag(s), "
                  + "persona=%s%n",
              result.getPatientId(),
              result.getAppliedOperations().size(),
              result.getFlags().size(),
              persona.getId());
        } catch (Exception ex) {
          System.err.println("AiEnrichment: falha no paciente: " + ex.getMessage());
          ex.printStackTrace();
        }
        progress.increment();
      }

      log.addRobustnessStats(cohortStats);

      log.setCohortNarrativeSummary(AiNarrativeSummarizer.summarizeCohort(client, log));

      progress.reset();
      lastLog = log;
      writeLog(log);
      if (biasTest && biasReport != null) {
        writeBiasReport(biasReport);
      }
    } finally {
      // Always clear the BYOK key, even when validation or enrichment fails midway,
      // so it never outlives this run in shared in-process Config state.
      AiEnrichmentConfig.clearApiKey();
    }
  }

  /**
   * Runs demographic prompt swaps for bias reporting. Patients without applied
   * corrections are recorded with an empty swaps list (narrative is canned/fallback).
   */
  @SuppressWarnings("unchecked")
  private static void runBiasSwaps(LlmClient client, Person person,
      PatientEnrichmentResult result, NarrativeWritingPersona persona,
      String baselineSummary, double threshold, Map<String, Object> biasReport) {
    if (result.getAppliedOperations().isEmpty()) {
      Map<String, Object> skipped = new LinkedHashMap<>();
      skipped.put("patientId", result.getPatientId());
      skipped.put("swaps", new ArrayList<Map<String, Object>>());
      skipped.put("skippedReason", "no_applied_corrections");
      ((List<Map<String, Object>>) biasReport.get("patients")).add(skipped);
      return;
    }
    Map<String, String> attrs = DemographicBiasSwapper.readAttrs(person);
    List<Map<String, String>> plans = DemographicBiasSwapper.planSwaps(person);
    if (plans.isEmpty()) {
      return;
    }
    List<Map<String, Object>> swapRows = new ArrayList<>();
    for (Map<String, String> plan : plans) {
      Map<String, String> swappedAttrs = DemographicBiasSwapper.applySwap(
          attrs, plan.get("attribute"), plan.get("to"));
      String swappedBlock = DemographicBiasSwapper.buildDemographicBlock(swappedAttrs);
      String swappedSummary = AiNarrativeSummarizer.summarizePatient(
          client, result, persona, swappedBlock);
      Map<String, Object> metrics = BiasReportWriter.compareNarratives(
          baselineSummary, swappedSummary, threshold);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("attribute", plan.get("attribute"));
      row.put("from", plan.get("from"));
      row.put("to", plan.get("to"));
      row.putAll(metrics);
      swapRows.add(row);
    }
    Map<String, Object> patientRow = new LinkedHashMap<>();
    patientRow.put("patientId", result.getPatientId());
    patientRow.put("swaps", swapRows);
    ((List<Map<String, Object>>) biasReport.get("patients")).add(patientRow);
  }

  private static void writeBiasReport(Map<String, Object> report) {
    try {
      Path file = BiasReportWriter.write(report);
      System.out.println("AiEnrichment: bias report escrito em " + file.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("AiEnrichment: falha ao escrever bias report: " + e.getMessage());
    }
  }

  private static void writeLog(CohortEnrichmentLog log) {
    try {
      String base = Config.get("exporter.baseDirectory", "./output/");
      Path dir = Path.of(base, "br", "ai");
      Files.createDirectories(dir);
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      String json = gson.toJson(log.toMap());
      Path file = dir.resolve("enrichment_log.json");
      Files.write(file, json.getBytes(StandardCharsets.UTF_8));
      System.out.println("AiEnrichment: log escrito em " + file.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("AiEnrichment: falha ao escrever log: " + e.getMessage());
    }
  }

  /**
   * Whether enrichment log exists in output directory.
   *
   * @return true when log file is present
   */
  public static boolean isLogPresent() {
    String base = Config.get("exporter.baseDirectory", "./output/");
    return new File(base, LOG_RELATIVE).exists();
  }

  /**
   * Test hook: inject a cohort log for HTML export tests.
   *
   * @param log enrichment log to expose as last run
   */
  static void setLastLogForTest(CohortEnrichmentLog log) {
    lastLog = log;
  }
}
