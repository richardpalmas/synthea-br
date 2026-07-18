package org.mitre.synthea.br.plausibility;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mitre.synthea.br.plausibility.rules.Plaus001TreatmentWithoutDiagnosis;
import org.mitre.synthea.br.plausibility.rules.Plaus002TreatmentBeforeDiagnosticExam;
import org.mitre.synthea.br.plausibility.rules.Plaus003MedicationDiagnosisCompatibility;
import org.mitre.synthea.world.agents.Person;

/**
 * Executable registry of plausibility rules backed by the versioned JSON catalog.
 */
public final class PlausibilityCatalog {

  private static final PlausibilityCatalog DEFAULT_INSTANCE = new PlausibilityCatalog();

  private final PlausibilityCatalogLoader loader;
  private final List<PlausibilityRule> rules;

  /**
   * Create a catalog with the default breast cancer rule set.
   */
  public PlausibilityCatalog() {
    this.loader = new PlausibilityCatalogLoader();
    this.rules = Collections.unmodifiableList(buildDefaultRules(loader));
  }

  /**
   * Return the default singleton catalog instance.
   *
   * @return shared catalog
   */
  public static PlausibilityCatalog getInstance() {
    return DEFAULT_INSTANCE;
  }

  /**
   * Return metadata for a catalog rule by stable ID.
   *
   * @param ruleId rule identifier (e.g. {@code PLAUS-001})
   * @return rule metadata from JSON catalog
   */
  public RuleMetadata getRuleMetadata(String ruleId) {
    return loader.getRule(ruleId);
  }

  /**
   * Return the catalog version string.
   *
   * @return version from JSON catalog
   */
  public String getVersion() {
    return loader.getVersion();
  }

  /**
   * Return registered rule implementations.
   *
   * @return immutable list of rules
   */
  public List<PlausibilityRule> getRules() {
    return rules;
  }

  /**
   * Evaluate all registered rules for the given person.
   *
   * @param person patient to evaluate (read-only)
   * @return aggregated violations from all rules
   */
  public List<Violation> evaluateAll(Person person) {
    List<Violation> violations = new ArrayList<>();
    for (PlausibilityRule rule : rules) {
      violations.addAll(rule.evaluate(person));
    }
    return violations;
  }

  BreastCancerCodeSets getCodeSets() {
    return loader.getCodeSets();
  }

  private static List<PlausibilityRule> buildDefaultRules(PlausibilityCatalogLoader catalogLoader) {
    List<PlausibilityRule> ruleList = new ArrayList<>();
    ruleList.add(new Plaus001TreatmentWithoutDiagnosis(catalogLoader));
    ruleList.add(new Plaus002TreatmentBeforeDiagnosticExam(catalogLoader));
    ruleList.add(new Plaus003MedicationDiagnosisCompatibility(catalogLoader));
    return ruleList;
  }

  static JsonObject readCatalogJson(String resourcePath) {
    try (InputStream stream = PlausibilityCatalog.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException("Catalog resource not found: " + resourcePath);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        return JsonParser.parseReader(reader).getAsJsonObject();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read catalog: " + resourcePath, e);
    }
  }
}
