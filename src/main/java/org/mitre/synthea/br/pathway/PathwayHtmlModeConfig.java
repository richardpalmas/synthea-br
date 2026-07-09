package org.mitre.synthea.br.pathway;

import org.mitre.synthea.helpers.Config;

/**
 * Resolves {@code exporter.html.pathway_mode} for pathway-aware HTML export (Story 9.4).
 */
public final class PathwayHtmlModeConfig {

  /** Only pathway events grouped by phase; demographics always visible. */
  public static final String MODE_ORIENTADOR = "orientador";
  /** Pathway timeline plus collapsible out-of-pathway section. */
  public static final String MODE_PESQUISADOR = "pesquisador";
  /** Epic 6 flat HTML export without pathway grouping. */
  public static final String MODE_FULL = "full";

  private PathwayHtmlModeConfig() {
  }

  /**
   * Active HTML pathway mode from configuration.
   *
   * @return one of {@link #MODE_ORIENTADOR}, {@link #MODE_PESQUISADOR}, {@link #MODE_FULL}
   */
  public static String resolveMode() {
    String configured = Config.get("exporter.html.pathway_mode");
    if (configured == null || configured.isBlank()) {
      return PathwayFocusConfig.isEnabled() ? MODE_ORIENTADOR : MODE_FULL;
    }
    String normalized = configured.trim().toLowerCase();
    if (MODE_ORIENTADOR.equals(normalized)
        || MODE_PESQUISADOR.equals(normalized)
        || MODE_FULL.equals(normalized)) {
      return normalized;
    }
    throw new IllegalArgumentException(String.format(
        "exporter.html.pathway_mode='%s' invalido. Valores suportados: orientador, pesquisador, full.",
        configured));
  }

  /**
   * Whether the resolved mode uses pathway phase grouping in the HTML timeline.
   *
   * @return {@code true} for orientador and pesquisador
   */
  public static boolean usesPathwayTimeline(String mode) {
    return MODE_ORIENTADOR.equals(mode) || MODE_PESQUISADOR.equals(mode);
  }

  /**
   * Whether clinical rows outside the pathway allowlist should be hidden.
   *
   * @return {@code true} only for orientador mode
   */
  public static boolean hidesOutOfPathwayClinicalData(String mode) {
    return MODE_ORIENTADOR.equals(mode);
  }
}
