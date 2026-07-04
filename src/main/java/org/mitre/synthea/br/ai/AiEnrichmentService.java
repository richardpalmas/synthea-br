package org.mitre.synthea.br.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

      List<Person> slice = new ArrayList<>(patients.subList(0, cap));
      for (Person person : slice) {
        try {
          PatientEnrichmentResult result = orchestrator.enrichPatient(person);
          String patientSummary = AiNarrativeSummarizer.summarizePatient(client, result);
          log.addPatient(result.withNarrativeSummary(patientSummary));
          System.out.printf("AiEnrichment: paciente %s — %d correção(ões), %d flag(s)%n",
              result.getPatientId(),
              result.getAppliedOperations().size(),
              result.getFlags().size());
        } catch (Exception ex) {
          System.err.println("AiEnrichment: falha no paciente: " + ex.getMessage());
          ex.printStackTrace();
        }
        progress.increment();
      }

      log.setCohortNarrativeSummary(AiNarrativeSummarizer.summarizeCohort(client, log));

      progress.reset();
      lastLog = log;
      writeLog(log);
    } finally {
      // Always clear the BYOK key, even when validation or enrichment fails midway,
      // so it never outlives this run in shared in-process Config state.
      AiEnrichmentConfig.clearApiKey();
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
