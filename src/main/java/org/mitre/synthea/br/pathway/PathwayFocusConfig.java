package org.mitre.synthea.br.pathway;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.helpers.Config;

/**
 * Reads {@code br.pathway.focus} and validates prerequisites for pathway-focused export (Story 9.3).
 */
public final class PathwayFocusConfig {

  private PathwayFocusConfig() {
  }

  /**
   * Whether pathway-focused export filtering is active.
   *
   * @return {@code true} when {@code br.pathway.focus=true}
   */
  public static boolean isEnabled() {
    return Config.getAsBoolean("br.pathway.focus", false);
  }

  /**
   * Validates that focus mode has a configured target condition before loading a catalog.
   *
   * @throws IllegalStateException when focus is enabled but {@code br.target_condition} is unset
   */
  public static void validateFocusPrerequisites() {
    if (!isEnabled()) {
      return;
    }
    if (TargetConditionConfig.resolveConfigured() == null) {
      throw new IllegalStateException(
          "br.pathway.focus=true requer br.target_condition configurado — "
              + "impossivel resolver catalogo de trajetoria.");
    }
  }
}
