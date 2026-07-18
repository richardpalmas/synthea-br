package org.mitre.synthea.br.pathway;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.helpers.Config;

/**
 * Loads aggregated timing priors and injects GMF Delay distributions into the episodic
 * trajectory module JSON before {@code Module} construction (Story 9.8, AD-3).
 *
 * <p>Does not mutate a live {@code Module} master between patients — transformation happens
 * on the JSON string at load time so {@code ModuleSupplier} clones remain consistent (AD-2).
 */
public final class PathwayTimingLoader {

  public static final String PROPERTY_KEY = "br.pathway.timing_priors";
  public static final String VALUE_OFF = "off";
  public static final String VALUE_DEFAULT = "default";

  public static final String EPISODIC_MODULE_FILE = "breast_cancer_trajectory_br.json";
  public static final String DEFAULT_RESOURCE = "/br/pathways/breast_cancer_timing_priors.json";

  /** Stable Delay state names keyed by catalog transition {@code from->to}. */
  public static final Map<String, String> TRANSITION_TO_DELAY_STATE;

  private static final Pattern PHI_PATTERN = Pattern.compile(
      "(?i)\\b(cpf|rg\\b|cns\\b|paciente\\s+[a-záéíóú]\\w+|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2})");

  private static final Gson GSON = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

  static {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("screening->diagnosis", "delay_screening_to_diagnosis");
    map.put("diagnosis->staging", "delay_diagnosis_to_staging");
    map.put("staging->treatment", "delay_staging_to_treatment");
    map.put("treatment->follow_up", "delay_treatment_to_follow_up");
    TRANSITION_TO_DELAY_STATE = Collections.unmodifiableMap(map);
  }

  private PathwayTimingLoader() {
  }

  /**
   * Whether timing priors should be applied (not {@code off}).
   *
   * @return {@code true} when property is {@code default} or a custom classpath resource
   */
  public static boolean isEnabled() {
    String value = Config.get(PROPERTY_KEY, VALUE_DEFAULT);
    if (value == null || value.isBlank()) {
      return true;
    }
    return !VALUE_OFF.equalsIgnoreCase(value.trim());
  }

  /**
   * Effective property value for documentation/manifest.
   *
   * @return configured value or {@link #VALUE_DEFAULT}
   */
  public static String getConfiguredValue() {
    String value = Config.get(PROPERTY_KEY, VALUE_DEFAULT);
    if (value == null || value.isBlank()) {
      return VALUE_DEFAULT;
    }
    return value.trim();
  }

  /**
   * Applies timing priors to episodic module JSON when enabled and path matches.
   *
   * <p>Does not require {@code trajectory_mode=episodic} at load time — the module is only
   * executed in episodic mode, but {@link Module} suppliers cache on first load. Set
   * {@code br.pathway.timing_priors} before JVM start for generation runs.
   *
   * @param modulePath path passed to {@code Module.loadFile} (may include {@code modules/})
   * @param jsonString raw module JSON
   * @return possibly transformed JSON
   */
  public static String maybeApplyPriors(String modulePath, String jsonString) {
    if (!isEnabled() || modulePath == null || jsonString == null) {
      return jsonString;
    }
    String normalized = modulePath.replace('\\', '/');
    if (!normalized.endsWith(EPISODIC_MODULE_FILE)
        && !normalized.endsWith("modules/" + EPISODIC_MODULE_FILE)) {
      return jsonString;
    }
    PathwayTimingPack pack = loadConfiguredPack();
    return applyPriorsToModuleJson(jsonString, pack);
  }

  /**
   * Loads the pack selected by {@link #PROPERTY_KEY}.
   *
   * @return parsed timing pack
   */
  public static PathwayTimingPack loadConfiguredPack() {
    String configured = getConfiguredValue();
    if (VALUE_OFF.equalsIgnoreCase(configured)) {
      throw new IllegalStateException(
          "PathwayTimingLoader.loadConfiguredPack chamado com br.pathway.timing_priors=off");
    }
    if (VALUE_DEFAULT.equalsIgnoreCase(configured)) {
      return loadFromClasspath(DEFAULT_RESOURCE);
    }
    String resource = configured.startsWith("/") ? configured : "/" + configured;
    return loadFromClasspath(resource);
  }

  /**
   * Loads timing priors for the configured target condition default pack.
   *
   * @return pack for {@code breast_cancer} when condition matches
   */
  public static PathwayTimingPack loadForConfiguredCondition() {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null) {
      return loadFromClasspath(DEFAULT_RESOURCE);
    }
    String resource = String.format("/br/pathways/%s_timing_priors.json",
        resolved.definition.conditionKey);
    return loadFromClasspath(resource);
  }

  /**
   * Loads and validates a timing priors pack from the classpath.
   *
   * @param resourcePath classpath resource (leading slash)
   * @return validated pack
   */
  public static PathwayTimingPack loadFromClasspath(String resourcePath) {
    try (InputStream stream = PathwayTimingLoader.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException(
            "Data pack de timing priors nao encontrado: " + resourcePath);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        PathwayTimingPack pack = GSON.fromJson(reader, PathwayTimingPack.class);
        validatePack(pack, resourcePath);
        return pack;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Falha ao ler timing priors: " + resourcePath, e);
    }
  }

  /**
   * Replaces Delay distributions in episodic module JSON using pack transitions.
   *
   * @param moduleJson original GMF JSON
   * @param pack timing priors
   * @return transformed JSON string
   */
  public static String applyPriorsToModuleJson(String moduleJson, PathwayTimingPack pack) {
    JsonObject root = JsonParser.parseString(moduleJson).getAsJsonObject();
    JsonObject states = root.getAsJsonObject("states");
    if (states == null) {
      throw new IllegalStateException("Modulo episodico sem objeto states");
    }
    String unit = pack.unit == null || pack.unit.isBlank() ? "days" : pack.unit;
    for (Map.Entry<String, PathwayTimingPrior> entry : pack.transitions.entrySet()) {
      String delayState = TRANSITION_TO_DELAY_STATE.get(entry.getKey());
      if (delayState == null) {
        throw new IllegalArgumentException(String.format(
            "Transicao de timing '%s' desconhecida. Chaves esperadas: %s",
            entry.getKey(), TRANSITION_TO_DELAY_STATE.keySet()));
      }
      JsonObject state = states.getAsJsonObject(delayState);
      if (state == null) {
        throw new IllegalStateException(
            "Estado Delay ausente no modulo episodico: " + delayState);
      }
      if (!"Delay".equals(state.get("type").getAsString())) {
        throw new IllegalStateException("Estado " + delayState + " nao e Delay");
      }
      state.add("distribution", toGmfDistributionJson(entry.getValue()));
      state.addProperty("unit", unit);
    }
    return root.toString();
  }

  /**
   * Converts a prior into a GMF 2.0 distribution JSON object.
   *
   * <p><b>Approximation (documented):</b> GMF {@code TRIANGULAR} uses {@code mode} (density peak),
   * while the ADR schema exposes {@code median}. This loader maps {@code median → mode}. For
   * asymmetric ranges the simulated P50 will differ from the cited median — acceptable for MVP
   * phase-marker spacing; refine with a true median-preserving sampler in a future ADR revision.
   *
   * @param prior transition prior
   * @return distribution JSON
   */
  public static JsonObject toGmfDistributionJson(PathwayTimingPrior prior) {
    if (prior.hasTriangularFields()) {
      JsonObject distribution = new JsonObject();
      distribution.addProperty("kind", "TRIANGULAR");
      JsonObject parameters = new JsonObject();
      parameters.addProperty("min", prior.min);
      // median (ADR schema) → mode (GMF TRIANGULAR); see method javadoc.
      parameters.addProperty("mode", prior.median);
      parameters.addProperty("max", prior.max);
      distribution.add("parameters", parameters);
      distribution.addProperty("round", true);
      return distribution;
    }
    if (prior.hasBuckets()) {
      return bucketsToTriangular(prior.buckets);
    }
    throw new IllegalArgumentException(
        "Prior deve ter min/max/median ou buckets com probabilities somando ~1.0");
  }

  private static JsonObject bucketsToTriangular(List<TimingBucket> buckets) {
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double weightedMid = 0.0;
    double probSum = 0.0;
    for (TimingBucket bucket : buckets) {
      min = Math.min(min, bucket.rangeMin);
      max = Math.max(max, bucket.rangeMax);
      double mid = (bucket.rangeMin + bucket.rangeMax) / 2.0;
      weightedMid += mid * bucket.probability;
      probSum += bucket.probability;
    }
    if (Math.abs(probSum - 1.0) > 0.01) {
      throw new IllegalArgumentException(
          "Probabilidades de buckets devem somar 1.0 (obtido " + probSum + ")");
    }
    JsonObject distribution = new JsonObject();
    distribution.addProperty("kind", "TRIANGULAR");
    JsonObject parameters = new JsonObject();
    parameters.addProperty("min", min);
    parameters.addProperty("mode", weightedMid);
    parameters.addProperty("max", max);
    distribution.add("parameters", parameters);
    distribution.addProperty("round", true);
    return distribution;
  }

  static void validatePack(PathwayTimingPack pack, String resourcePath) {
    if (pack == null) {
      throw new IllegalStateException("Timing priors vazio: " + resourcePath);
    }
    if (pack.priorsVersion == null || pack.priorsVersion.isBlank()) {
      throw new IllegalStateException("priors_version obrigatorio em " + resourcePath);
    }
    if (pack.transitions == null || pack.transitions.isEmpty()) {
      throw new IllegalStateException("transitions obrigatorio em " + resourcePath);
    }
    assertNoPhi(pack);
    for (Map.Entry<String, PathwayTimingPrior> entry : pack.transitions.entrySet()) {
      if (!TRANSITION_TO_DELAY_STATE.containsKey(entry.getKey())) {
        throw new IllegalArgumentException(
            "Chave de transicao invalida (use phase_id do catalogo 9.2): " + entry.getKey());
      }
      PathwayTimingPrior prior = entry.getValue();
      if (prior == null || (!prior.hasTriangularFields() && !prior.hasBuckets())) {
        throw new IllegalArgumentException(
            "Distribuicao invalida para transicao " + entry.getKey());
      }
      if (prior.hasTriangularFields()
          && (prior.min > prior.median || prior.median > prior.max)) {
        throw new IllegalArgumentException(
            "min <= median <= max requerido para " + entry.getKey());
      }
    }
  }

  static void assertNoPhi(PathwayTimingPack pack) {
    if (pack.referenceNotes != null) {
      for (String note : pack.referenceNotes) {
        if (note != null && PHI_PATTERN.matcher(note).find()) {
          throw new IllegalStateException(
              "Possivel PHI detectado em reference_notes — NFR5: " + note);
        }
      }
    }
    String serialized = GSON.toJson(pack).toLowerCase(Locale.ROOT);
    if (PHI_PATTERN.matcher(serialized).find()) {
      throw new IllegalStateException(
          "Possivel PHI detectado no data pack de timing priors (NFR5)");
    }
  }

  /**
   * Root object for {@code *_timing_priors.json}.
   */
  public static final class PathwayTimingPack {
    public String priorsVersion;
    public String condition;
    public String unit;
    public List<String> referenceNotes;
    public Map<String, PathwayTimingPrior> transitions;

    public String getPriorsVersion() {
      return priorsVersion;
    }

    public List<String> getReferenceNotes() {
      return referenceNotes == null ? List.of() : List.copyOf(referenceNotes);
    }
  }

  /**
   * Single transition prior — triangular fields or probability buckets.
   */
  public static final class PathwayTimingPrior {
    public Double min;
    public Double max;
    public Double median;
    public List<TimingBucket> buckets;

    boolean hasTriangularFields() {
      return min != null && max != null && median != null;
    }

    boolean hasBuckets() {
      return buckets != null && !buckets.isEmpty();
    }
  }

  /**
   * Probability bucket for a transition duration.
   */
  public static final class TimingBucket {
    public double rangeMin;
    public double rangeMax;
    public double probability;
  }
}
