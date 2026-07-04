package org.mitre.synthea.br.condition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Catalog of clinical target conditions supported in the Synthea-br MVP.
 *
 * <p>To add a new condition post-MVP, register it here with the GMF disease module path and the
 * keep-module resource path (relative to {@code keep_modules/}).
 */
public final class SupportedConditions {

  /** SNOMED-CT code for malignant neoplasm of breast used by the upstream disease module. */
  public static final String BREAST_CANCER_SNOMED = "254837009";

  private static final Map<String, ConditionDefinition> CONDITIONS = new LinkedHashMap<>();

  static {
    register(
        "breast_cancer",
        "breast_cancer",
        "br/breast_cancer.json");
  }

  private SupportedConditions() {
  }

  /**
   * Immutable definition of a supported target condition.
   */
  public static final class ConditionDefinition {
    /**
     * Config key value (e.g. {@code breast_cancer}).
     */
    public final String conditionKey;
    /**
     * Relative GMF disease module path without {@code .json} (e.g. {@code breast_cancer}).
     */
    public final String diseaseModulePath;
    /**
     * Keep-module path relative to {@code keep_modules/} (e.g. {@code br/breast_cancer.json}).
     */
    public final String keepModuleRelativePath;

    ConditionDefinition(String conditionKey, String diseaseModulePath,
        String keepModuleRelativePath) {
      this.conditionKey = conditionKey;
      this.diseaseModulePath = diseaseModulePath;
      this.keepModuleRelativePath = keepModuleRelativePath;
    }
  }

  /**
   * Register a supported condition. Intended for tests in the same package only.
   *
   * @param conditionKey config value for {@code br.target_condition}
   * @param diseaseModulePath GMF disease module path
   * @param keepModuleRelativePath keep module path relative to {@code keep_modules/}
   */
  static void registerForTest(String conditionKey, String diseaseModulePath,
      String keepModuleRelativePath) {
    register(conditionKey, diseaseModulePath, keepModuleRelativePath);
  }

  /**
   * Remove a test-only condition registration.
   *
   * @param conditionKey config value to remove
   */
  static void unregisterForTest(String conditionKey) {
    CONDITIONS.remove(conditionKey);
  }

  /**
   * Restore the default MVP catalog after test mutations.
   */
  static void resetToDefaultsForTest() {
    CONDITIONS.clear();
    register("breast_cancer", "breast_cancer", "br/breast_cancer.json");
  }

  private static void register(String conditionKey, String diseaseModulePath,
      String keepModuleRelativePath) {
    CONDITIONS.put(conditionKey, new ConditionDefinition(
        conditionKey, diseaseModulePath, keepModuleRelativePath));
  }

  /**
   * Look up a condition definition by config key.
   *
   * @param conditionKey value of {@code br.target_condition}
   * @return definition or {@code null} if unknown
   */
  public static ConditionDefinition get(String conditionKey) {
    return CONDITIONS.get(conditionKey);
  }

  /**
   * Returns the unmodifiable set of supported condition keys for error messages.
   *
   * @return unmodifiable set of supported condition keys for error messages
   */
  public static Set<String> supportedKeys() {
    return Collections.unmodifiableSet(CONDITIONS.keySet());
  }
}
