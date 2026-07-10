package org.mitre.synthea.br.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allowed trajectory-focused generation values for the web UI (Story 7.2).
 */
public final class TrajectoryWebConstants {

  public static final String HTML_MODE_AUTO = "auto";
  public static final String HTML_MODE_ORIENTADOR = "orientador";
  public static final String HTML_MODE_PESQUISADOR = "pesquisador";
  public static final String HTML_MODE_FULL = "full";

  public static final String MODULE_PROFILE_FULL = "full";
  public static final String MODULE_PROFILE_PATHWAY_MINIMAL = "pathway_minimal";

  public static final String TRAJECTORY_MODE_LIFESPAN = "lifespan";
  public static final String TRAJECTORY_MODE_EPISODIC = "episodic";

  public static final String SIMULATION_WINDOW_FULL = "full_lifespan";
  public static final String SIMULATION_WINDOW_PRE_ONSET_10 = "pre_onset_years:10";

  public static final String ARCHETYPE_AUTO = "auto";
  public static final String ARCHETYPE_REMISSION = "remission";
  public static final String ARCHETYPE_PROGRESSION = "progression";

  private static final List<String> ARCHETYPE_MODES = Collections.unmodifiableList(
      Arrays.asList(ARCHETYPE_AUTO, ARCHETYPE_REMISSION, ARCHETYPE_PROGRESSION));

  private static final Pattern PRE_ONSET_PATTERN =
      Pattern.compile("^pre_onset_years:(\\d+)$", Pattern.CASE_INSENSITIVE);

  private static final int BREAST_CANCER_MIN_YEARS = 5;
  private static final int BREAST_CANCER_MAX_YEARS = 15;

  private static final List<String> HTML_MODES = Collections.unmodifiableList(
      Arrays.asList(HTML_MODE_AUTO, HTML_MODE_ORIENTADOR, HTML_MODE_PESQUISADOR, HTML_MODE_FULL));

  private TrajectoryWebConstants() {
  }

  /**
   * Supported HTML pathway mode values from the web form.
   *
   * @return immutable list of mode keys
   */
  public static List<String> htmlModes() {
    return HTML_MODES;
  }

  /**
   * Normalizes a simulation window value from the request.
   *
   * @param value raw value or null
   * @return trimmed value or {@link #SIMULATION_WINDOW_FULL}
   */
  public static String normalizedSimulationWindow(String value) {
    if (value == null || value.isBlank()) {
      return SIMULATION_WINDOW_FULL;
    }
    return value.trim().toLowerCase();
  }

  /**
   * Normalizes HTML pathway mode from the request.
   *
   * @param value raw value or null
   * @return trimmed lower-case mode or {@link #HTML_MODE_AUTO}
   */
  public static String normalizedHtmlPathwayMode(String value) {
    if (value == null || value.isBlank()) {
      return HTML_MODE_AUTO;
    }
    return value.trim().toLowerCase();
  }

  /**
   * Validates trajectory-related fields on a web request.
   *
   * @param request generation request
   * @param errors mutable error list
   */
  public static void validateTrajectoryFields(GenerationRequest request, List<String> errors) {
    String htmlMode = normalizedHtmlPathwayMode(request.htmlPathwayMode);
    if (!HTML_MODES.contains(htmlMode)) {
      errors.add("Modo HTML inválido. Use auto, orientador, pesquisador ou full.");
    }

    String moduleProfile = normalizedModuleProfile(request.moduleProfile);
    if (!MODULE_PROFILE_FULL.equals(moduleProfile)
        && !MODULE_PROFILE_PATHWAY_MINIMAL.equals(moduleProfile)) {
      errors.add("Perfil de módulos inválido. Use full ou pathway_minimal.");
    }

    String trajectoryMode = normalizedTrajectoryMode(request.trajectoryMode);
    if (!TRAJECTORY_MODE_LIFESPAN.equals(trajectoryMode)
        && !TRAJECTORY_MODE_EPISODIC.equals(trajectoryMode)) {
      errors.add("Modo de trajetória inválido. Use lifespan ou episodic.");
    }

    String target = normalizedTargetCondition(request.targetCondition);
    boolean needsTarget = request.pathwayFocus
        || MODULE_PROFILE_PATHWAY_MINIMAL.equals(moduleProfile)
        || TRAJECTORY_MODE_EPISODIC.equals(trajectoryMode);
    if (needsTarget && target == null) {
      errors.add("Trajetória focada, perfil pathway_minimal ou modo episodic "
          + "requerem condição clínica alvo.");
    }

    if (TRAJECTORY_MODE_EPISODIC.equals(trajectoryMode)) {
      if (target == null) {
        errors.add("Modo episodic requer condição clínica alvo.");
      } else if (!"breast_cancer".equals(target)) {
        errors.add("Modo episodic suporta apenas breast_cancer no MVP.");
      }
    }

    validateSimulationWindow(request, errors, target);
    validatePathwayArchetype(request, errors, target);
  }

  private static void validatePathwayArchetype(GenerationRequest request, List<String> errors,
      String target) {
    String archetype = normalizedPathwayArchetype(request.pathwayArchetype);
    if (!ARCHETYPE_MODES.contains(archetype)) {
      errors.add("Arquétipo de trajetória inválido. Use auto, remission ou progression.");
      return;
    }
    if (!ARCHETYPE_AUTO.equals(archetype) && !"breast_cancer".equals(target)) {
      errors.add("Arquétipo forçado suporta apenas breast_cancer no MVP.");
    }
  }

  private static void validateSimulationWindow(GenerationRequest request, List<String> errors,
      String target) {
    String window = normalizedSimulationWindow(request.simulationWindow);
    if (SIMULATION_WINDOW_FULL.equalsIgnoreCase(window)) {
      return;
    }
    Matcher matcher = PRE_ONSET_PATTERN.matcher(window);
    if (!matcher.matches()) {
      errors.add("Janela de simulação inválida. Use full_lifespan ou pre_onset_years:N "
          + "(ex.: pre_onset_years:10).");
      return;
    }
    if (request.minAge == null || request.maxAge == null) {
      errors.add("Janela pre_onset_years requer idade mínima e máxima juntas.");
      return;
    }
    int preOnsetYears;
    try {
      preOnsetYears = Integer.parseInt(matcher.group(1));
    } catch (NumberFormatException ex) {
      errors.add("Janela pre_onset_years inválida. N deve ser um inteiro suportado.");
      return;
    }
    if (preOnsetYears <= 0) {
      errors.add("pre_onset_years deve ser positivo.");
      return;
    }
    if (preOnsetYears >= request.minAge) {
      errors.add(String.format(
          "pre_onset_years:%d incompatível com idade mínima %d — N deve ser menor que a idade alvo.",
          preOnsetYears, request.minAge));
      return;
    }
    if ("breast_cancer".equals(target)
        && (preOnsetYears < BREAST_CANCER_MIN_YEARS || preOnsetYears > BREAST_CANCER_MAX_YEARS)) {
      errors.add(String.format(
          "pre_onset_years:%d fora do intervalo piloto para câncer de mama (%d–%d anos).",
          preOnsetYears, BREAST_CANCER_MIN_YEARS, BREAST_CANCER_MAX_YEARS));
    }
  }

  /**
   * Builds a PT-BR summary of active trajectory settings for status display.
   *
   * @param request generation request
   * @return human-readable summary
   */
  public static String buildTrajectorySummary(GenerationRequest request) {
    String htmlMode = normalizedHtmlPathwayMode(request.htmlPathwayMode);
    String htmlLabel;
    if (HTML_MODE_AUTO.equals(htmlMode)) {
      htmlLabel = request.pathwayFocus ? "orientador (auto)" : "full (auto)";
    } else {
      htmlLabel = htmlMode;
    }
    StringBuilder summary = new StringBuilder();
    summary.append("Trajetória focada: ").append(request.pathwayFocus ? "sim" : "não");
    summary.append(" · Perfil: ").append(normalizedModuleProfile(request.moduleProfile));
    summary.append(" · Trajetória: ").append(normalizedTrajectoryMode(request.trajectoryMode));
    String window = normalizedSimulationWindow(request.simulationWindow);
    if (!SIMULATION_WINDOW_FULL.equalsIgnoreCase(window)) {
      summary.append(" · Janela: ").append(window);
    }
    String archetype = normalizedPathwayArchetype(request.pathwayArchetype);
    if (!ARCHETYPE_AUTO.equals(archetype)) {
      summary.append(" · Arquétipo: ").append(archetype);
    }
    summary.append(" · HTML: ").append(htmlLabel);
    return summary.toString();
  }

  static String normalizedModuleProfile(String value) {
    if (value == null || value.isBlank()) {
      return MODULE_PROFILE_FULL;
    }
    return value.trim().toLowerCase();
  }

  static String normalizedTrajectoryMode(String value) {
    if (value == null || value.isBlank()) {
      return TRAJECTORY_MODE_LIFESPAN;
    }
    return value.trim().toLowerCase();
  }

  static String normalizedPathwayArchetype(String value) {
    if (value == null || value.isBlank()) {
      return ARCHETYPE_AUTO;
    }
    return value.trim().toLowerCase();
  }

  private static String normalizedTargetCondition(String targetCondition) {
    if (targetCondition == null || targetCondition.trim().isEmpty()) {
      return null;
    }
    return targetCondition.trim();
  }
}
