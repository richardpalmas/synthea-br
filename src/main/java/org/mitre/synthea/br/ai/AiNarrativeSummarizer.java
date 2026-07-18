package org.mitre.synthea.br.ai;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
   * Summarizes enrichment actions for one patient using the default narrative style.
   *
   * @param client LLM client from the enrichment run
   * @param result patient enrichment audit
   * @return narrative summary in Portuguese
   */
  public static String summarizePatient(LlmClient client, PatientEnrichmentResult result) {
    return summarizePatient(client, result, NarrativeWritingPersona.NARRATIVE, null);
  }

  /**
   * Summarizes enrichment actions for one patient with a writing persona.
   *
   * @param client LLM client from the enrichment run
   * @param result patient enrichment audit
   * @param persona writing style persona
   * @return narrative summary in Portuguese
   */
  public static String summarizePatient(LlmClient client, PatientEnrichmentResult result,
      NarrativeWritingPersona persona) {
    return summarizePatient(client, result, persona, null);
  }

  /**
   * Summarizes enrichment with optional demographic context (for bias testing).
   *
   * @param client LLM client
   * @param result patient enrichment audit
   * @param persona writing style
   * @param demographicBlock optional demographic prompt fragment (may be null)
   * @return narrative summary in Portuguese
   */
  public static String summarizePatient(LlmClient client, PatientEnrichmentResult result,
      NarrativeWritingPersona persona, String demographicBlock) {
    if (result.getAppliedOperations().isEmpty() && result.getFlags().isEmpty()) {
      return "Nenhuma inconsistência clínica, regional ou de perfil exigiu correção neste paciente.";
    }
    if (result.getAppliedOperations().isEmpty()) {
      return fallbackPatientSummary(result);
    }
    NarrativeWritingPersona style = persona == null
        ? NarrativeWritingPersona.NARRATIVE : persona;
    String userPrompt = "";
    if (demographicBlock != null && !demographicBlock.isEmpty()) {
      userPrompt += demographicBlock + "\n";
    }
    userPrompt += "Paciente: " + result.getPatientId() + "\n"
        + "Correções aplicadas: " + GSON.toJson(result.getAppliedOperations()) + "\n"
        + "Limitações sinalizadas: " + GSON.toJson(result.getFlags()) + "\n"
        + "Debate finalizado: " + result.isFinalized();
    try {
      String raw = client.complete(composeSystemPrompt("narrative_patient_summary", style),
          userPrompt);
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
    return summarizeCohort(client, log, NarrativeWritingPersona.NARRATIVE);
  }

  /**
   * Summarizes cohort-level enrichment with a writing persona.
   *
   * @param client LLM client
   * @param log cohort log
   * @param persona writing style
   * @return cohort narrative summary
   */
  public static String summarizeCohort(LlmClient client, CohortEnrichmentLog log,
      NarrativeWritingPersona persona) {
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

    if (appliedTotal == 0) {
      return fallbackCohortSummary(log, appliedTotal, flagTotal);
    }

    NarrativeWritingPersona style = persona == null
        ? NarrativeWritingPersona.NARRATIVE : persona;
    String userPrompt = "Metadados: " + GSON.toJson(log.getMetadata()) + "\n"
        + "Pacientes enriquecidos: " + patients.size() + "\n"
        + "Total de correções aplicadas: " + appliedTotal + "\n"
        + "Total de limitações sinalizadas: " + flagTotal + "\n"
        + "Resumo por paciente: " + GSON.toJson(patients);
    try {
      String raw = client.complete(composeSystemPrompt("narrative_cohort_summary", style),
          userPrompt);
      return sanitizeNarrative(raw, fallbackCohortSummary(log, appliedTotal, flagTotal));
    } catch (LlmException ex) {
      System.err.println("AiNarrativeSummarizer: falha no resumo da cohort: " + ex.getMessage());
      return fallbackCohortSummary(log, appliedTotal, flagTotal);
    }
  }

  /**
   * Composes base narrative instructions with a writing-style persona overlay.
   *
   * @param basePromptName resource name without path/extension
   * @param persona writing persona
   * @return combined system prompt
   */
  static String composeSystemPrompt(String basePromptName, NarrativeWritingPersona persona) {
    NarrativeWritingPersona style = persona == null
        ? NarrativeWritingPersona.NARRATIVE : persona;
    String base = loadPrompt(basePromptName);
    return base + "\n\nInstrução de estilo (" + style.getId() + "):\n"
        + style.loadStylePrompt();
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
    List<String> reasons = extractFlagReasons(result.getFlags());
    if (!reasons.isEmpty()) {
      sb.append(" Limitações: ").append(String.join("; ", reasons)).append('.');
    }
    return sb.toString();
  }

  private static String fallbackCohortSummary(CohortEnrichmentLog log, int appliedTotal,
      int flagTotal) {
    int patientCount = log.getPatients().size();
    int patientsWithCorrections = 0;
    int patientsWithFlags = 0;
    int patientsWithoutIssues = 0;
    for (Map<String, Object> row : log.getPatients()) {
      int applied = intValue(row.get("appliedCount"));
      int flags = intValue(row.get("flagCount"));
      if (applied > 0) {
        patientsWithCorrections++;
      }
      if (flags > 0) {
        patientsWithFlags++;
      }
      if (applied == 0 && flags == 0) {
        patientsWithoutIssues++;
      }
    }
    return "A cohort teve " + patientCount + " paciente(s) revisado(s) por IA (MAI-DxO), "
        + "com " + appliedTotal + " correção(ões) aplicada(s) e " + flagTotal
        + " limitação(ões) sinalizada(s) como não corrigíveis. "
        + patientsWithCorrections + " paciente(s) recebeu(ram) correções, "
        + patientsWithFlags + " paciente(s) com limitações e "
        + patientsWithoutIssues + " paciente(s) sem inconsistências detectadas.";
  }

  private static List<String> extractFlagReasons(List<Map<String, Object>> flags) {
    List<String> reasons = new ArrayList<>();
    for (Map<String, Object> flag : flags) {
      Object reason = flag.get("reason");
      if (reason != null && !reason.toString().trim().isEmpty()) {
        reasons.add(reason.toString().trim());
      }
    }
    return reasons;
  }

  private static String sanitizeNarrative(String raw, String fallback) {
    if (raw == null) {
      return fallback;
    }
    String text = raw.trim();
    if (text.startsWith("{") && text.endsWith("}")) {
      return fallback;
    }
    // Preserve newlines for bullet_points / abcde personas; collapse spaces/tabs only.
    text = text.replaceAll("[ \\t\\x0B\\f\\r]+", " ");
    text = text.replaceAll(" *\\n *", "\n");
    text = text.replaceAll("\\n{3,}", "\n\n").trim();
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
