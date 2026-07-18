package org.mitre.synthea.br.pathway;

import java.util.function.Predicate;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.helpers.Config;

/**
 * Parses {@code br.generation.trajectory_mode} for episodic vs lifespan simulation (Story 9.7).
 *
 * <p>MVP: the episodic module is a <strong>phase marker</strong> ({@code pathway_phase} attributes)
 * that runs in parallel with the upstream {@code breast_cancer} GMF module — it does not replace
 * clinical event generation (ADR-008 / GUIA-DE-USO).
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
   * @throws IllegalArgumentException when the configured value is not supported
   */
  public static String getActiveMode() {
    String value = Config.get("br.generation.trajectory_mode", MODE_LIFESPAN);
    if (value == null || value.isBlank()) {
      return MODE_LIFESPAN;
    }
    String trimmed = value.trim();
    if (MODE_LIFESPAN.equalsIgnoreCase(trimmed) || MODE_EPISODIC.equalsIgnoreCase(trimmed)) {
      return trimmed.toLowerCase();
    }
    throw new IllegalArgumentException(String.format(
        "br.generation.trajectory_mode='%s' invalido. Valores suportados: lifespan, episodic.",
        trimmed));
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
   * Module path predicate for episodic vs lifespan.
   *
   * <p>In {@code lifespan} mode the episodic marker module is excluded so default simulation
   * stays unchanged (AC #4). In {@code episodic} mode prerequisites are validated; inclusion is
   * forced via {@link #forceInclude(String)} alongside the module profile filter.
   *
   * @return predicate applied alongside module profile filtering
   */
  public static Predicate<String> buildPathPredicate() {
    if (!isEpisodic()) {
      return path -> !isEpisodicModulePath(path);
    }
    validateEpisodicPrerequisites();
    return path -> true;
  }

  /**
   * Whether the module path must be loaded even if the active module profile would exclude it.
   *
   * @param path module path
   * @return {@code true} when episodic mode forces this path
   */
  public static boolean forceInclude(String path) {
    return isEpisodic() && isEpisodicModulePath(path);
  }

  /**
   * Path of the episodic phase-marker module when mode is episodic.
   *
   * @return module path or {@code null} when lifespan mode is active
   */
  public static String episodicModulePath() {
    return isEpisodic() ? EPISODIC_MODULE_PATH : null;
  }

  private static boolean isEpisodicModulePath(String path) {
    return path != null
        && (EPISODIC_MODULE_PATH.equals(path) || path.startsWith(EPISODIC_MODULE_PATH + "/"));
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
