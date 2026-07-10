package org.mitre.synthea.br.plausibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
   * Record violations for a patient, replacing any previous entry for the same ID.
   * Empty lists remove any prior entry. Used so multi-record export does not duplicate.
   *
   * @param patientId patient identifier
   * @param violations violations found for this patient
   */
  public void recordPatientViolations(String patientId, List<Violation> violations) {
    if (patientId == null || patientId.isEmpty()) {
      patientId = "unknown";
    }
    if (violations == null || violations.isEmpty()) {
      violationsByPatient.remove(patientId);
      return;
    }
    violationsByPatient.put(patientId, Collections.unmodifiableList(new ArrayList<>(violations)));
  }

  /**
   * Return a deterministic snapshot sorted by patient ID.
   *
   * @return unmodifiable map of patient ID to violation lists
   */
  public Map<String, List<Violation>> getViolationsByPatientSorted() {
    return snapshot().violationsByPatient;
  }

  /**
   * Count patients with at least one violation per severity level.
   *
   * @return map of severity label to patient count
   */
  public Map<String, Integer> countPatientsBySeverity() {
    return snapshot().severityCounts;
  }

  /**
   * Atomic snapshot of sorted violations and severity counts from the same map copy.
   *
   * @return consistent report data
   */
  public ReportSnapshot snapshot() {
    Map<String, List<Violation>> copy = new LinkedHashMap<>();
    for (Map.Entry<String, List<Violation>> entry : violationsByPatient.entrySet()) {
      List<Violation> violations = entry.getValue();
      if (violations == null) {
        continue;
      }
      copy.put(entry.getKey(), new ArrayList<>(violations));
    }

    List<String> patientIds = new ArrayList<>(copy.keySet());
    Collections.sort(patientIds);

    Map<String, List<Violation>> sorted = new LinkedHashMap<>();
    Map<String, Integer> counts = new LinkedHashMap<>();
    counts.put("alta", 0);
    counts.put("média", 0);
    counts.put("baixa", 0);

    for (String patientId : patientIds) {
      List<Violation> violations = copy.get(patientId);
      sorted.put(patientId, Collections.unmodifiableList(violations));
      Set<String> severities = new LinkedHashSet<>();
      for (Violation violation : violations) {
        severities.add(violation.getSeverity());
      }
      for (String severity : severities) {
        counts.merge(severity, 1, Integer::sum);
      }
    }

    return new ReportSnapshot(
        Collections.unmodifiableMap(sorted),
        Collections.unmodifiableMap(counts));
  }

  /**
   * Immutable view of accumulator state for report serialization.
   */
  public static final class ReportSnapshot {
    public final Map<String, List<Violation>> violationsByPatient;
    public final Map<String, Integer> severityCounts;

    ReportSnapshot(Map<String, List<Violation>> violationsByPatient,
        Map<String, Integer> severityCounts) {
      this.violationsByPatient = violationsByPatient;
      this.severityCounts = severityCounts;
    }
  }
}
