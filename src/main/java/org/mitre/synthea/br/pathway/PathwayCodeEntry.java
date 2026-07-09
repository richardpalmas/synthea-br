package org.mitre.synthea.br.pathway;

/**
 * A single clinical code (SNOMED-CT/LOINC/RxNorm/CPT) belonging to a {@link PathwayPhase}
 * allowlist. Immutable data-pack entry parsed from JSON (AD-3) — never hardcoded in Java.
 */
public final class PathwayCodeEntry {

  private String system;
  private String code;
  private String display;

  /**
   * Terminology system of this code (e.g. {@code SNOMED-CT}, {@code LOINC}, {@code RxNorm}).
   *
   * @return the code system identifier
   */
  public String getSystem() {
    return system;
  }

  /**
   * Code value within {@link #getSystem()}.
   *
   * @return the code value
   */
  public String getCode() {
    return code;
  }

  /**
   * Human-readable display text for the code.
   *
   * @return the display text
   */
  public String getDisplay() {
    return display;
  }

  /**
   * Unified key combining system and code, used for allowlist lookups.
   *
   * @return {@code "{system}|{code}"}
   */
  public String toUnifiedKey() {
    return system + "|" + code;
  }
}
