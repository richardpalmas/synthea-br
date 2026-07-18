package org.mitre.synthea.br.pathway.generation;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;

/**
 * Parses {@code br.generation.simulation_window} and computes simulation start time (Story 9.6).
 *
 * <p>When active, simulation begins at {@code target_age - N} years instead of birth, while
 * demographic attributes remain coherent with {@code -a}/IBGE age constraints.
 */
public final class SimulationWindowConfig {

  /** Full lifespan — upstream default (no window). */
  public static final String FULL_LIFESPAN = "full_lifespan";

  private static final Pattern PRE_ONSET_PATTERN =
      Pattern.compile("^pre_onset_years:(\\d+)$", Pattern.CASE_INSENSITIVE);

  /** Documented pilot range for breast cancer (ADR-008). */
  private static final int BREAST_CANCER_MIN_YEARS = 5;
  private static final int BREAST_CANCER_MAX_YEARS = 15;

  private SimulationWindowConfig() {
  }

  /**
   * Effective configuration value for manifest traceability.
   *
   * @return configured value or {@link #FULL_LIFESPAN}
   */
  public static String getEffectiveValue() {
    String raw = Config.get("br.generation.simulation_window", FULL_LIFESPAN);
    if (raw == null || raw.isBlank()) {
      return FULL_LIFESPAN;
    }
    return raw.trim();
  }

  /**
   * Whether a pre-onset simulation window is configured.
   *
   * @return {@code true} when not {@link #FULL_LIFESPAN}
   */
  public static boolean isActive() {
    String value = getEffectiveValue();
    return !FULL_LIFESPAN.equalsIgnoreCase(value);
  }

  /**
   * Validates configuration and CLI age constraints before generation starts (fail-fast).
   *
   * @param options generator options with age range
   */
  public static void validateForGeneration(GeneratorOptions options) {
    if (!isActive()) {
      return;
    }
    int preOnsetYears = parsePreOnsetYears(getEffectiveValue());
    validatePreOnsetYears(preOnsetYears, options);
  }

  /**
   * Simulation start timestamp for a person with the given birthdate and target age.
   *
   * @param birthdateMillis person birth timestamp
   * @param targetAgeYears resolved target age in years
   * @return start timestamp for {@code person.lastUpdated}
   */
  public static long simulationStartTime(long birthdateMillis, int targetAgeYears) {
    if (!isActive()) {
      return birthdateMillis;
    }
    int preOnsetYears = parsePreOnsetYears(getEffectiveValue());
    long offsetMillis = TimeUnit.DAYS.toMillis((long) (targetAgeYears - preOnsetYears) * 365L);
    return birthdateMillis + offsetMillis;
  }

  private static int parsePreOnsetYears(String value) {
    Matcher matcher = PRE_ONSET_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException(String.format(
          "br.generation.simulation_window='%s' invalido. "
              + "Valores suportados: full_lifespan, pre_onset_years:N (ex.: pre_onset_years:10).",
          value));
    }
    return Integer.parseInt(matcher.group(1));
  }

  private static void validatePreOnsetYears(int preOnsetYears, GeneratorOptions options) {
    if (preOnsetYears <= 0) {
      throw new IllegalArgumentException(String.format(
          "br.generation.simulation_window=pre_onset_years:%d invalido — N deve ser positivo.",
          preOnsetYears));
    }
    // Fail-fast: without -a, pickAge() can yield ages < N and corrupt lastUpdated < birthdate.
    if (!options.ageSpecified) {
      throw new IllegalArgumentException(
          "br.generation.simulation_window=pre_onset_years requer -a (idade alvo). "
              + "Ex.: -a 45-75 com pre_onset_years:10.");
    }
    if (preOnsetYears >= options.minAge) {
      throw new IllegalArgumentException(String.format(
          "br.generation.simulation_window=pre_onset_years:%d incompativel com idade alvo minima %d "
              + "— N deve ser menor que a idade alvo.",
          preOnsetYears, options.minAge));
    }
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved != null && "breast_cancer".equals(resolved.definition.conditionKey)) {
      if (preOnsetYears < BREAST_CANCER_MIN_YEARS || preOnsetYears > BREAST_CANCER_MAX_YEARS) {
        throw new IllegalArgumentException(String.format(
            "br.generation.simulation_window=pre_onset_years:%d fora do intervalo piloto "
                + "para cancer de mama (%d–%d anos).",
            preOnsetYears, BREAST_CANCER_MIN_YEARS, BREAST_CANCER_MAX_YEARS));
      }
    }
  }
}
