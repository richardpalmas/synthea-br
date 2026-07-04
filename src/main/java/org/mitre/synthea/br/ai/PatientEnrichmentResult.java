package org.mitre.synthea.br.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result of AI enrichment for a single patient.
 */
public final class PatientEnrichmentResult {

  private final String patientId;
  private final List<Map<String, Object>> appliedOperations;
  private final List<Map<String, Object>> flags;
  private final String debateLog;
  private final boolean finalized;
  private final String narrativeSummary;

  /**
   * Creates a patient enrichment result.
   *
   * @param patientId patient identifier
   * @param appliedOperations successfully applied corrections
   * @param flags unfixable issues flagged by personas
   * @param debateLog full debate transcript
   * @param finalized whether FinalizePatient was reached
   */
  public PatientEnrichmentResult(String patientId,
      List<Map<String, Object>> appliedOperations,
      List<Map<String, Object>> flags,
      String debateLog,
      boolean finalized) {
    this(patientId, appliedOperations, flags, debateLog, finalized, null);
  }

  /**
   * Creates a patient enrichment result with an optional narrative summary.
   *
   * @param patientId patient identifier
   * @param appliedOperations successfully applied corrections
   * @param flags unfixable issues flagged by personas
   * @param debateLog full debate transcript
   * @param finalized whether FinalizePatient was reached
   * @param narrativeSummary human-readable PT-BR summary for HTML export
   */
  public PatientEnrichmentResult(String patientId,
      List<Map<String, Object>> appliedOperations,
      List<Map<String, Object>> flags,
      String debateLog,
      boolean finalized,
      String narrativeSummary) {
    this.patientId = patientId;
    this.appliedOperations = appliedOperations == null ? new ArrayList<>() : appliedOperations;
    this.flags = flags == null ? new ArrayList<>() : flags;
    this.debateLog = debateLog == null ? "" : debateLog;
    this.finalized = finalized;
    this.narrativeSummary = narrativeSummary;
  }

  /**
   * Returns a copy of this result with a narrative summary attached.
   *
   * @param summary human-readable PT-BR summary
   * @return result with narrative summary
   */
  public PatientEnrichmentResult withNarrativeSummary(String summary) {
    return new PatientEnrichmentResult(
        patientId, appliedOperations, flags, debateLog, finalized, summary);
  }

  /**
   * Returns the patient identifier.
   *
   * @return patient id
   */
  public String getPatientId() {
    return patientId;
  }

  /**
   * Returns successfully applied correction operations.
   *
   * @return applied correction audit rows
   */
  public List<Map<String, Object>> getAppliedOperations() {
    return appliedOperations;
  }

  /**
   * Returns issues flagged as unfixable by the persona panel.
   *
   * @return flagged unfixable issues
   */
  public List<Map<String, Object>> getFlags() {
    return flags;
  }

  /**
   * Returns the full debate transcript for diagnostics.
   *
   * @return debate transcript
   */
  public String getDebateLog() {
    return debateLog;
  }

  /**
   * Returns whether the debate reached FinalizePatient before exhausting iterations.
   *
   * @return true when debate finalized cleanly
   */
  public boolean isFinalized() {
    return finalized;
  }

  /**
   * Returns the PT-BR narrative summary for HTML export, when generated.
   *
   * @return narrative summary or null
   */
  public String getNarrativeSummary() {
    return narrativeSummary;
  }
}
