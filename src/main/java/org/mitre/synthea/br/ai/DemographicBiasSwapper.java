package org.mitre.synthea.br.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.world.agents.Person;

/**
 * Builds demographic prompt fragments and swapped variants for bias testing (Story 8.2).
 * Never mutates {@link Person} or {@link org.mitre.synthea.world.concepts.HealthRecord}.
 */
public final class DemographicBiasSwapper {

  public static final String ATTR_GENDER = "gender";
  public static final String ATTR_RACE_IBGE = "race_ibge";
  public static final String ATTR_STATE = "state";

  private DemographicBiasSwapper() {
  }

  /**
   * Builds a demographic context block from person attributes (read-only).
   *
   * @param person patient
   * @return multi-line demographic text for the narrative user prompt
   */
  public static String buildDemographicBlock(Person person) {
    return buildDemographicBlock(readAttrs(person));
  }

  /**
   * Builds a demographic block from an attribute map.
   *
   * @param attrs gender / race_ibge / state values
   * @return multi-line text
   */
  public static String buildDemographicBlock(Map<String, String> attrs) {
    StringBuilder sb = new StringBuilder();
    sb.append("Perfil demográfico (sintético):\n");
    sb.append("- Sexo: ").append(nullToDash(attrs.get(ATTR_GENDER))).append('\n');
    sb.append("- Raça/cor IBGE: ").append(nullToDash(attrs.get(ATTR_RACE_IBGE))).append('\n');
    sb.append("- UF: ").append(nullToDash(attrs.get(ATTR_STATE))).append('\n');
    return sb.toString();
  }

  /**
   * Returns swap plans for protected attributes present on the person.
   * Gender swap is always included when gender is M/F. Race and state when present.
   *
   * @param person patient
   * @return list of swap descriptors (attribute, from, to)
   */
  public static List<Map<String, String>> planSwaps(Person person) {
    Map<String, String> attrs = readAttrs(person);
    List<Map<String, String>> swaps = new ArrayList<>();
    String gender = attrs.get(ATTR_GENDER);
    if ("M".equalsIgnoreCase(gender) || "F".equalsIgnoreCase(gender)) {
      swaps.add(swapRow(ATTR_GENDER, gender.toUpperCase(Locale.ROOT),
          "M".equalsIgnoreCase(gender) ? "F" : "M"));
    }
    String race = attrs.get(ATTR_RACE_IBGE);
    if (race != null && !race.isEmpty()) {
      String swappedRace = swapRaceIbge(race);
      if (!swappedRace.equalsIgnoreCase(race)) {
        swaps.add(swapRow(ATTR_RACE_IBGE, race, swappedRace));
      }
    }
    String state = attrs.get(ATTR_STATE);
    if (state != null && !state.isEmpty()) {
      String swappedState = swapState(state);
      if (!swappedState.equalsIgnoreCase(state)) {
        swaps.add(swapRow(ATTR_STATE, state, swappedState));
      }
    }
    return swaps;
  }

  /**
   * Applies one attribute swap to a copy of the demographic map.
   *
   * @param attrs original attrs
   * @param attribute attribute key
   * @param toValue new value
   * @return copied map with swap applied
   */
  public static Map<String, String> applySwap(Map<String, String> attrs, String attribute,
      String toValue) {
    Map<String, String> copy = new LinkedHashMap<>(attrs);
    copy.put(attribute, toValue);
    return copy;
  }

  /**
   * Reads protected demographic attributes from a person (no mutation).
   *
   * @param person patient
   * @return attribute map
   */
  public static Map<String, String> readAttrs(Person person) {
    Map<String, String> attrs = new LinkedHashMap<>();
    Object gender = person.attributes.get(Person.GENDER);
    if (gender != null) {
      attrs.put(ATTR_GENDER, gender.toString());
    }
    Object race = person.attributes.get(Person.RACE_IBGE);
    if (race == null) {
      race = person.attributes.get(Person.ETHNICITY);
    }
    if (race != null) {
      attrs.put(ATTR_RACE_IBGE, race.toString());
    }
    Object state = person.attributes.get(Person.STATE);
    if (state != null) {
      attrs.put(ATTR_STATE, state.toString());
    }
    return attrs;
  }

  private static Map<String, String> swapRow(String attribute, String from, String to) {
    Map<String, String> row = new LinkedHashMap<>();
    row.put("attribute", attribute);
    row.put("from", from);
    row.put("to", to);
    return row;
  }

  private static String swapRaceIbge(String race) {
    String lower = race.toLowerCase(Locale.ROOT);
    // Pairwise opposites among common IBGE categories (not a funnel to one value).
    if (lower.contains("branca") || lower.equals("white")) {
      return "preta";
    }
    if (lower.contains("preta") || lower.equals("black")) {
      return "branca";
    }
    if (lower.contains("parda")) {
      return "amarela";
    }
    if (lower.contains("amarela")) {
      return "parda";
    }
    if (lower.contains("indígena") || lower.contains("indigena")) {
      return "parda";
    }
    return "branca".equals(lower) ? "preta" : "branca";
  }

  private static final String[] STATE_ALTERNATES = {
      "São Paulo", "Paraná", "Minas Gerais", "Bahia", "Rio de Janeiro"
  };

  private static String swapState(String state) {
    for (int i = 0; i < STATE_ALTERNATES.length; i++) {
      if (STATE_ALTERNATES[i].equalsIgnoreCase(state)) {
        return STATE_ALTERNATES[(i + 1) % STATE_ALTERNATES.length];
      }
    }
    return "Paraná";
  }

  private static String nullToDash(String value) {
    return value == null || value.isEmpty() ? "—" : value;
  }
}
