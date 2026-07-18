package org.mitre.synthea.br.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cohort-level enrichment log written to {@code output/br/ai/enrichment_log.json}.
 */
public final class CohortEnrichmentLog {

  private final Map<String, Object> metadata = new LinkedHashMap<>();
  private final List<Map<String, Object>> patients = new ArrayList<>();
  private String cohortNarrativeSummary;

  /**
   * Sets run metadata.
   *
   * @param provider LLM provider
   * @param model model id
   * @param deterministic whether output is reproducible
   */
  public void setMetadata(String provider, String model, boolean deterministic) {
    metadata.put("provider", provider);
    metadata.put("model", model);
    metadata.put("deterministic", deterministic);
    metadata.put("orchestration", "MAI-DxO");
    metadata.put("json_parse_retries", 0);
    metadata.put("truncation_continuations", 0);
    metadata.put("persona_turns_skipped", 0);
  }

  /**
   * Adds cohort-level robustness counters into metadata (Story 8.1).
   *
   * @param stats aggregated counters
   */
  public void addRobustnessStats(RobustnessStats stats) {
    if (stats == null) {
      return;
    }
    metadata.put("json_parse_retries",
        asInt(metadata.get("json_parse_retries")) + stats.getJsonParseRetries());
    metadata.put("truncation_continuations",
        asInt(metadata.get("truncation_continuations")) + stats.getTruncationContinuations());
    metadata.put("persona_turns_skipped",
        asInt(metadata.get("persona_turns_skipped")) + stats.getPersonaTurnsSkipped());
  }

  private static int asInt(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }

  /**
   * Adds a patient result summary (no API keys, no full debate by default in summary).
   *
   * @param result patient enrichment result
   */
  public void addPatient(PatientEnrichmentResult result) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("patientId", result.getPatientId());
    row.put("appliedCount", result.getAppliedOperations().size());
    row.put("flagCount", result.getFlags().size());
    row.put("finalized", result.isFinalized());
    row.put("appliedOperations", result.getAppliedOperations());
    row.put("flags", result.getFlags());
    row.put("debateLog", result.getDebateLog());
    if (result.getNarrativeSummary() != null) {
      row.put("narrativeSummary", result.getNarrativeSummary());
    }
    if (result.getWritingPersona() != null) {
      row.put("writingPersona", result.getWritingPersona());
    }
    patients.add(row);
  }

  /**
   * Sets the cohort-level narrative summary for HTML export.
   *
   * @param summary human-readable PT-BR summary
   */
  public void setCohortNarrativeSummary(String summary) {
    this.cohortNarrativeSummary = summary;
  }

  /**
   * Returns the cohort-level narrative summary, when generated.
   *
   * @return cohort summary or null
   */
  public String getCohortNarrativeSummary() {
    return cohortNarrativeSummary;
  }

  /**
   * Returns the LLM model id from run metadata, when set.
   *
   * @return model id or null
   */
  public String getModel() {
    Object model = metadata.get("model");
    return model != null ? model.toString() : null;
  }

  /**
   * Returns run metadata.
   *
   * @return metadata map
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /**
   * Returns per-patient enrichment summaries.
   *
   * @return patient rows
   */
  public List<Map<String, Object>> getPatients() {
    return patients;
  }

  /**
   * Serializes to a map for JSON output.
   *
   * @return full log structure
   */
  public Map<String, Object> toMap() {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("metadata", metadata);
    root.put("patients", patients);
    if (cohortNarrativeSummary != null) {
      root.put("cohortNarrativeSummary", cohortNarrativeSummary);
    }
    return root;
  }
}
