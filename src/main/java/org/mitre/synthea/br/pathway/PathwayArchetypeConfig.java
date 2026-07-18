package org.mitre.synthea.br.pathway;

import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Resolves {@code br.pathway.archetype} for deterministic trajectory branches (Epic 10).
 *
 * <p>Values: {@code auto} (default probabilistic GMF), {@code remission} (luminal favorable
 * outcome), {@code progression} (metastatic / palliative / death).
 */
public final class PathwayArchetypeConfig {

  /** Person attribute written at generation start when archetype is forced. */
  public static final String PERSON_ATTRIBUTE = "pathway_archetype";

  public static final String MODE_AUTO = "auto";
  public static final String MODE_REMISSION = "remission";
  public static final String MODE_PROGRESSION = "progression";

  private PathwayArchetypeConfig() {
  }

  /**
   * Active archetype mode from configuration.
   *
   * @return {@code auto}, {@code remission}, or {@code progression}
   */
  public static String getActiveMode() {
    String value = Config.get("br.pathway.archetype", MODE_AUTO);
    if (value == null || value.isBlank()) {
      return MODE_AUTO;
    }
    String trimmed = value.trim().toLowerCase();
    if (MODE_AUTO.equals(trimmed) || MODE_REMISSION.equals(trimmed)
        || MODE_PROGRESSION.equals(trimmed)) {
      return trimmed;
    }
    throw new IllegalArgumentException(String.format(
        "br.pathway.archetype='%s' invalido. Valores suportados: auto, remission, progression.",
        trimmed));
  }

  /**
   * Whether a forced archetype is active (not {@code auto}).
   *
   * @return {@code true} when remission or progression is configured
   */
  public static boolean isForced() {
    return !MODE_AUTO.equals(getActiveMode());
  }

  /**
   * Writes the configured archetype onto the person when forced.
   *
   * @param person newly created person
   */
  public static void applyToPerson(Person person) {
    if (!isForced()) {
      return;
    }
    person.attributes.put(PERSON_ATTRIBUTE, getActiveMode());
  }

  /**
   * Validates archetype prerequisites when forced.
   */
  public static void validateForcedPrerequisites() {
    if (!isForced()) {
      return;
    }
    String target = Config.get("br.target_condition");
    if (target == null || target.isBlank()) {
      throw new IllegalStateException(
          "br.pathway.archetype forçado requer br.target_condition configurado.");
    }
    if (!"breast_cancer".equals(target.trim())) {
      throw new IllegalStateException(String.format(
          "br.pathway.archetype forçado suporta apenas breast_cancer no MVP; condicao: '%s'.",
          target.trim()));
    }
  }
}
