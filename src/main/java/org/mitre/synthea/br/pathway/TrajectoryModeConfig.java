package org.mitre.synthea.br.pathway;

import java.util.function.Predicate;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.helpers.Config;

/**
 * Parses {@code br.generation.trajectory_mode} for episodic vs lifespan simulation (Story 9.7).
 */
public final class TrajectoryModeConfig {

  private static final String MODE_LIFESPAN = "lifespan";
  private static final String MODE_EPISODIC = "episodic";
  private static final String EPISODIC_MODULE_PATH = "breast_cancer_trajectory_br";

  private TrajectoryModeConfig() {
  }

  /**
   * Active trajectory mode from configuration.
   *
   * @return {@code lifespan} or {@code episodic}
   */
  public static String getActiveMode() {
    String value = Config.get("br.generation.trajectory_mode", MODE_LIFESPAN);
    if (value == null || value.isBlank()) {
      return MODE_LIFESPAN;
    }
    return value.trim();
  }

  /**
   * Whether episodic GMF trajectory mode is enabled.
   *
   * @return {@code true} when mode is {@code episodic}
   */
  public static boolean isEpisodic() {
    return MODE_EPISODIC.equalsIgnoreCase(getActiveMode());
  }

  /**
   * Module path predicate ensuring episodic marker module loads alongside upstream disease module.
   *
   * <p>Does not remove {@code breast_cancer} — clinical simulation remains in the upstream module;
   * episodic wrapper sets {@code pathway_phase} attributes in parallel (Story 9.7 AC #4).
   *
   * @return predicate applied alongside module profile filtering
   */
  public static Predicate<String> buildPathPredicate() {
    if (!isEpisodic()) {
      return path -> true;
    }
    validateEpisodicPrerequisites();
    return path -> true;
  }

  /**
   * Path of the episodic phase-marker module when mode is episodic.
   *
   * @return module path or {@code null} when lifespan mode is active
   */
  public static String episodicModulePath() {
    return isEpisodic() ? EPISODIC_MODULE_PATH : null;
  }

  private static void validateEpisodicPrerequisites() {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null) {
      throw new IllegalStateException(
          "br.generation.trajectory_mode=episodic requer br.target_condition configurado.");
    }
    if (!"breast_cancer".equals(resolved.definition.conditionKey)) {
      throw new IllegalStateException(String.format(
          "br.generation.trajectory_mode=episodic suporta apenas breast_cancer no MVP; "
              + "condicao configurada: '%s'.",
          resolved.definition.conditionKey));
    }
  }
}
