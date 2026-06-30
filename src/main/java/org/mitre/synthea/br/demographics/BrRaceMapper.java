package org.mitre.synthea.br.demographics;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps IBGE raça/cor categories to internal Synthea race keys (US Census / OMB).
 *
 * <p>Internal keys ({@code white}, {@code black}, {@code asian}, {@code native}, {@code other})
 * are preserved so FHIR export codification in {@code FhirR4.java} remains valid.
 *
 * <p><strong>Ambiguity — {@code parda}:</strong> the largest IBGE category (~45%) has no direct
 * US Census equivalent. Interim split (pending ADR-003 confirmation):
 * <ul>
 *   <li>40% of {@code parda} fraction → {@code black}</li>
 *   <li>60% of {@code parda} fraction → {@code other}</li>
 * </ul>
 * Direct 1:1 mappings: branca→white, preta→black, amarela→asian, indigena→native.
 */
public final class BrRaceMapper {

  /**
   * Fraction of {@code parda} mapped to {@code black} (interim; see ADR-003).
   */
  public static final double PARDA_TO_BLACK_RATIO = 0.40;

  /**
   * Fraction of {@code parda} mapped to {@code other} (interim; see ADR-003).
   */
  public static final double PARDA_TO_OTHER_RATIO = 0.60;

  private BrRaceMapper() {
  }

  /**
   * Convert IBGE raça/cor fractions to internal Synthea race distribution.
   *
   * @param ibgeFractions map of IBGE category → fraction (must sum to ~1.0)
   * @return map of internal race key → fraction summing to 1.0
   */
  public static Map<String, Double> toInternalRaceDistribution(Map<String, Double> ibgeFractions) {
    Map<String, Double> internal = new HashMap<>();
    internal.put("white", ibgeFractions.getOrDefault("branca", 0.0));
    internal.put("black", ibgeFractions.getOrDefault("preta", 0.0));
    internal.put("asian", ibgeFractions.getOrDefault("amarela", 0.0));
    internal.put("native", ibgeFractions.getOrDefault("indigena", 0.0));

    double parda = ibgeFractions.getOrDefault("parda", 0.0);
    internal.merge("black", parda * PARDA_TO_BLACK_RATIO, Double::sum);
    internal.merge("other", parda * PARDA_TO_OTHER_RATIO, Double::sum);

    internal.putIfAbsent("other", 0.0);
    internal.put("hawaiian", 0.0);

    return internal;
  }
}
