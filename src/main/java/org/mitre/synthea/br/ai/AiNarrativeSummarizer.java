package org.mitre.synthea.br.ai;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Generates human-readable PT-BR narrative summaries of AI enrichment for HTML export.
 */
public final class AiNarrativeSummarizer {

  private static final Gson GSON = new Gson();

  private AiNarrativeSummarizer() {
  }

  /**
   * Summarizes enrichment actions for one patient using the same LLM client.
   *
   * @param client LLM client from the enrichment run
   * @param result patient enrichment audit
   * @return narrative summary in Portuguese
   */
  public static String summarizePatient(LlmClient client, PatientEnrichmentResult result) {
    if (result.getAppliedOperations().isEmpty() && result.getFlags().isEmpty()) {
      return "Nenhuma inconsistência clínica, regional ou de perfil exigiu correção neste paciente.";
    }
    String userPrompt = "Paciente: " + result.getPatientId() + "\n"
        + "Correções aplicadas: " + GSON.toJson(result.getAppliedOperations()) + "\n"
        + "Limitações sinalizadas: " + GSON.toJson(result.getFlags()) + "\n"
        + "Debate finalizado: " + result.isFinalized();
    try {
      String raw = client.complete(loadPrompt("narrative_patient_summary"), userPrompt);
      return sanitizeNarrative(raw, fallbackPatientSummary(result));
    } catch (LlmException ex) {
      System.err.println("AiNarrativeSummarizer: falha no resumo do paciente "
          + result.getPatientId() + ": " + ex.getMessage());
      return fallbackPatientSummary(result);
    }
  }

  /**
   * Summarizes cohort-level enrichment outcomes for the narrative HTML index.
   *
   * @param client LLM client from the enrichment run
   * @param log cohort enrichment log with per-patient rows
   * @return cohort narrative summary in Portuguese
   */
  public static String summarizeCohort(LlmClient client, CohortEnrichmentLog log) {
    List<Map<String, Object>> patients = log.getPatients();
    if (patients.isEmpty()) {
      return "Nenhum paciente foi submetido ao enriquecimento por IA nesta execução.";
    }

    int appliedTotal = 0;
    int flagTotal = 0;
    for (Map<String, Object> row : patients) {
      appliedTotal += intValue(row.get("appliedCount"));
      flagTotal += intValue(row.get("flagCount"));
    }

    String userPrompt = "Metadados: " + GSON.toJson(log.getMetadata()) + "\n"
        + "Pacientes enriquecidos: " + patients.size() + "\n"
        + "Total de correções aplicadas: " + appliedTotal + "\n"
        + "Total de limitações sinalizadas: " + flagTotal + "\n"
        + "Resumo por paciente: " + GSON.toJson(patients);
    try {
      String raw = client.complete(loadPrompt("narrative_cohort_summary"), userPrompt);
      return sanitizeNarrative(raw, fallbackCohortSummary(log, appliedTotal, flagTotal));
    } catch (LlmException ex) {
      System.err.println("AiNarrativeSummarizer: falha no resumo da cohort: " + ex.getMessage());
      return fallbackCohortSummary(log, appliedTotal, flagTotal);
    }
  }

  private static String fallbackPatientSummary(PatientEnrichmentResult result) {
    int applied = result.getAppliedOperations().size();
    int flags = result.getFlags().size();
    if (applied == 0 && flags == 0) {
      return "Nenhuma inconsistência clínica, regional ou de perfil exigiu correção neste paciente.";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Foram aplicadas ").append(applied).append(" correção(ões)");
    if (flags > 0) {
      sb.append(" e ").append(flags).append(" limitação(ões) permaneceu(ram) sinalizada(s)");
    }
    sb.append(" após análise MAI-DxO.");
    if (!result.isFinalized()) {
      sb.append(" O painel não concluiu a revisão dentro do limite de iterações.");
    }
    return sb.toString();
  }

  private static String fallbackCohortSummary(CohortEnrichmentLog log, int appliedTotal,
      int flagTotal) {
    int patientCount = log.getPatients().size();
    return "A cohort teve " + patientCount + " paciente(s) revisado(s) por IA (MAI-DxO), "
        + "com " + appliedTotal + " correção(ões) aplicada(s) e " + flagTotal
        + " limitação(ões) sinalizada(s) como não corrigíveis.";
  }

  private static String sanitizeNarrative(String raw, String fallback) {
    if (raw == null) {
      return fallback;
    }
    String text = raw.trim();
    if (text.startsWith("{") && text.endsWith("}")) {
      return fallback;
    }
    text = text.replaceAll("\\s+", " ").trim();
    return text.isEmpty() ? fallback : text;
  }

  private static int intValue(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private static String loadPrompt(String name) {
    String path = "/br/ai/prompts/" + name + ".txt";
    try (InputStream in = AiNarrativeSummarizer.class.getResourceAsStream(path)) {
      if (in == null) {
        return "Resuma o enriquecimento clínico em português, de forma breve e objetiva.";
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      return "Resuma o enriquecimento clínico em português, de forma breve e objetiva.";
    }
  }
}
