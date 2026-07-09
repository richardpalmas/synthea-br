package org.mitre.synthea.br.plausibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe accumulator for per-patient plausibility violations during generation.
 *
 * <p>Singleton pattern aligned with {@link org.mitre.synthea.export.CDWExporter#getInstance()}.
 */
public final class PlausibilityReportAccumulator {

  private static final PlausibilityReportAccumulator INSTANCE = new PlausibilityReportAccumulator();

  private final ConcurrentHashMap<String, List<Violation>> violationsByPatient =
      new ConcurrentHashMap<>();

  private PlausibilityReportAccumulator() {
  }

  /**
   * Return the shared accumulator instance.
   *
   * @return singleton accumulator
   */
  public static PlausibilityReportAccumulator getInstance() {
    return INSTANCE;
  }

  /**
   * Clear all accumulated violations (typically after report write).
   */
  public void reset() {
    violationsByPatient.clear();
  }

  /**
   * Record violations for a patient. Empty lists are not stored.
   *
   * @param patientId patient identifier
   * @param violations violations found for this patient
   */
  public void recordPatientViolations(String patientId, List<Violation> violations) {
    if (violations == null || violations.isEmpty()) {
      return;
    }
    violationsByPatient.compute(patientId, (id, existing) -> {
      List<Violation> merged = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
      merged.addAll(violations);
      return merged;
    });
  }

  /**
   * Return a deterministic snapshot sorted by patient ID.
   *
   * @return unmodifiable map of patient ID to violation lists
   */
  public Map<String, List<Violation>> getViolationsByPatientSorted() {
    List<String> patientIds = new ArrayList<>(violationsByPatient.keySet());
    Collections.sort(patientIds);
    Map<String, List<Violation>> sorted = new LinkedHashMap<>();
    for (String patientId : patientIds) {
      List<Violation> violations = violationsByPatient.get(patientId);
      sorted.put(patientId, Collections.unmodifiableList(new ArrayList<>(violations)));
    }
    return Collections.unmodifiableMap(sorted);
  }

  /**
   * Count patients with at least one violation per severity level.
   *
   * @return map of severity label to patient count
   */
  public Map<String, Integer> countPatientsBySeverity() {
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("alta", 0);
    counts.put("média", 0);
    counts.put("baixa", 0);
    for (List<Violation> violations : violationsByPatient.values()) {
      java.util.Set<String> severities = new java.util.LinkedHashSet<>();
      for (Violation violation : violations) {
        severities.add(violation.getSeverity());
      }
      for (String severity : severities) {
        counts.merge(severity, 1, Integer::sum);
      }
    }
    return counts;
  }
}
