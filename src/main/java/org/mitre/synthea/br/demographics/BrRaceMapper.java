package org.mitre.synthea.br.demographics;

import java.util.HashMap;
import java.util.Map;

import org.mitre.synthea.helpers.RandomNumberGenerator;

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

  private static final Map<String, String> IBGE_DISPLAY_NAMES = Map.of(
      "branca", "Branca",
      "preta", "Preta",
      "parda", "Parda",
      "amarela", "Amarela",
      "indigena", "Indigena");

  private static final Map<String, String> INTERNAL_TO_PT_BR = Map.of(
      "white", "branca",
      "black", "preta",
      "asian", "amarela",
      "native", "indigena",
      "other", "outro",
      "hawaiian", "hawaiana");

  private static final Map<String, String> PT_BR_TO_INTERNAL = Map.of(
      "branca", "white",
      "preta", "black",
      "amarela", "asian",
      "indigena", "native",
      "outro", "other",
      "hawaiana", "hawaiian");

  private BrRaceMapper() {
  }

  /**
   * Human-readable label for an IBGE raça/cor category (export / FHIR text).
   *
   * @param ibgeCategory IBGE key (e.g. {@code parda})
   * @return display label
   */
  public static String getIbgeDisplayName(String ibgeCategory) {
    return IBGE_DISPLAY_NAMES.getOrDefault(ibgeCategory, ibgeCategory);
  }

  /**
   * Portuguese (pt-BR) label for an internal Synthea race key (CSV / human-readable export).
   *
   * @param internalRace internal key ({@code white}, {@code black}, etc.)
   * @return pt-BR category ({@code branca}, {@code preta}, etc.)
   */
  public static String toBrazilianDisplayRace(String internalRace) {
    return INTERNAL_TO_PT_BR.getOrDefault(internalRace, internalRace);
  }

  /**
   * Resolve an internal Synthea race key from either English or pt-BR stored values.
   *
   * @param raceOrDisplay internal or pt-BR race value
   * @return internal race key
   */
  public static String toInternalRaceKey(String raceOrDisplay) {
    if (raceOrDisplay == null) {
      return null;
    }
    if (INTERNAL_TO_PT_BR.containsKey(raceOrDisplay)) {
      return raceOrDisplay;
    }
    return PT_BR_TO_INTERNAL.getOrDefault(raceOrDisplay, raceOrDisplay);
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

  /**
   * Map a single IBGE raça/cor category to an internal Synthea race key for one person.
   *
   * @param ibgeCategory IBGE category ({@code branca}, {@code preta}, etc.)
   * @param random source of randomness for {@code parda} split
   * @return internal race key ({@code white}, {@code black}, etc.)
   */
  public static String toInternalRace(String ibgeCategory, RandomNumberGenerator random) {
    switch (ibgeCategory) {
      case "branca":
        return "white";
      case "preta":
        return "black";
      case "amarela":
        return "asian";
      case "indigena":
        return "native";
      case "parda":
        return random.rand() < PARDA_TO_BLACK_RATIO ? "black" : "other";
      default:
        throw new IllegalArgumentException("Unknown IBGE race category: " + ibgeCategory);
    }
  }
}
