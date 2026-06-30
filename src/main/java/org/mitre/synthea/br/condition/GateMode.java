package org.mitre.synthea.br.condition;

import org.mitre.synthea.helpers.Config;

/**
 * Gate behaviour for targeted cohort generation.
 */
public enum GateMode {
  /**
   * Retry generation until the keep module matches or max attempts is exceeded.
   */
  RETRY("retry"),
  /**
   * Generate once per slot and skip export for non-conforming patients.
   */
  EXCLUDE("exclude");

  private final String configValue;

  GateMode(String configValue) {
    this.configValue = configValue;
  }

  /**
   * @return property value written to {@code br.target_condition.gate_mode}
   */
  public String getConfigValue() {
    return configValue;
  }

  /**
   * Parse {@code br.target_condition.gate_mode} from configuration.
   *
   * @return parsed gate mode, defaulting to {@link #RETRY}
   */
  public static GateMode fromConfig() {
    String raw = Config.get("br.target_condition.gate_mode", RETRY.configValue);
    if (raw == null || raw.isBlank()) {
      return RETRY;
    }
    String normalized = raw.trim().toLowerCase();
    for (GateMode mode : values()) {
      if (mode.configValue.equals(normalized)) {
        return mode;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Valor inválido para br.target_condition.gate_mode: '%s'. "
            + "Valores aceitos: retry, exclude",
        raw));
  }
}
