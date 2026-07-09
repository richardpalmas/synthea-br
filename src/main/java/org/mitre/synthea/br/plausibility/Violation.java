package org.mitre.synthea.br.plausibility;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable structured output of a plausibility rule evaluation.
 *
 * <p>Designed for deterministic aggregation in post-generation reports (Story 4.2).
 */
public final class Violation {

  private final String ruleId;
  private final String severity;
  private final String patientId;
  private final String description;
  private final Map<String, Long> eventTimestamps;

  /**
   * Create a violation record.
   *
   * @param ruleId stable catalog rule identifier (e.g. {@code PLAUS-001})
   * @param severity severity level from catalog ({@code alta}, {@code média}, {@code baixa})
   * @param patientId patient identifier
   * @param description human-readable description of the specific violation found
   * @param eventTimestamps relevant event timestamps keyed by context label
   */
  public Violation(String ruleId, String severity, String patientId, String description,
      Map<String, Long> eventTimestamps) {
    this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.patientId = Objects.requireNonNull(patientId, "patientId");
    this.description = Objects.requireNonNull(description, "description");
    Map<String, Long> timestamps = eventTimestamps == null
        ? Collections.emptyMap()
        : new LinkedHashMap<>(eventTimestamps);
    this.eventTimestamps = Collections.unmodifiableMap(timestamps);
  }

  public String getRuleId() {
    return ruleId;
  }

  public String getSeverity() {
    return severity;
  }

  public String getPatientId() {
    return patientId;
  }

  public String getDescription() {
    return description;
  }

  public Map<String, Long> getEventTimestamps() {
    return eventTimestamps;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Violation other = (Violation) obj;
    return ruleId.equals(other.ruleId)
        && severity.equals(other.severity)
        && patientId.equals(other.patientId)
        && description.equals(other.description)
        && eventTimestamps.equals(other.eventTimestamps);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleId, severity, patientId, description, eventTimestamps);
  }
}
