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
  private final RobustnessStats robustnessStats;
  private final String writingPersona;

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
    this(patientId, appliedOperations, flags, debateLog, finalized, null, null, null);
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
    this(patientId, appliedOperations, flags, debateLog, finalized, narrativeSummary, null, null);
  }

  /**
   * Creates a patient enrichment result with narrative and robustness stats.
   *
   * @param patientId patient identifier
   * @param appliedOperations successfully applied corrections
   * @param flags unfixable issues flagged by personas
   * @param debateLog full debate transcript
   * @param finalized whether FinalizePatient was reached
   * @param narrativeSummary human-readable PT-BR summary for HTML export
   * @param robustnessStats parse/truncation counters for this patient
   */
  public PatientEnrichmentResult(String patientId,
      List<Map<String, Object>> appliedOperations,
      List<Map<String, Object>> flags,
      String debateLog,
      boolean finalized,
      String narrativeSummary,
      RobustnessStats robustnessStats) {
    this(patientId, appliedOperations, flags, debateLog, finalized, narrativeSummary,
        robustnessStats, null);
  }

  /**
   * Creates a full patient enrichment result including writing persona.
   *
   * @param patientId patient identifier
   * @param appliedOperations successfully applied corrections
   * @param flags unfixable issues flagged by personas
   * @param debateLog full debate transcript
   * @param finalized whether FinalizePatient was reached
   * @param narrativeSummary human-readable PT-BR summary for HTML export
   * @param robustnessStats parse/truncation counters for this patient
   * @param writingPersona narrative writing persona id (Story 8.2)
   */
  public PatientEnrichmentResult(String patientId,
      List<Map<String, Object>> appliedOperations,
      List<Map<String, Object>> flags,
      String debateLog,
      boolean finalized,
      String narrativeSummary,
      RobustnessStats robustnessStats,
      String writingPersona) {
    this.patientId = patientId;
    this.appliedOperations = appliedOperations == null ? new ArrayList<>() : appliedOperations;
    this.flags = flags == null ? new ArrayList<>() : flags;
    this.debateLog = debateLog == null ? "" : debateLog;
    this.finalized = finalized;
    this.narrativeSummary = narrativeSummary;
    this.robustnessStats = robustnessStats == null ? new RobustnessStats() : robustnessStats;
    this.writingPersona = writingPersona;
  }

  /**
   * Returns a copy of this result with a narrative summary attached.
   *
   * @param summary human-readable PT-BR summary
   * @return result with narrative summary
   */
  public PatientEnrichmentResult withNarrativeSummary(String summary) {
    return new PatientEnrichmentResult(
        patientId, appliedOperations, flags, debateLog, finalized, summary, robustnessStats,
        writingPersona);
  }

  /**
   * Returns a copy with writing persona id attached.
   *
   * @param personaId writing persona id
   * @return result with writing persona
   */
  public PatientEnrichmentResult withWritingPersona(String personaId) {
    return new PatientEnrichmentResult(
        patientId, appliedOperations, flags, debateLog, finalized, narrativeSummary,
        robustnessStats, personaId);
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

  /**
   * Returns parse/truncation robustness counters for this patient.
   *
   * @return robustness stats
   */
  public RobustnessStats getRobustnessStats() {
    return robustnessStats;
  }

  /**
   * Returns the writing persona id used for the narrative summary, when set.
   *
   * @return persona id or null
   */
  public String getWritingPersona() {
    return writingPersona;
  }
}
