package org.mitre.synthea.br.plausibility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loader for the versioned breast cancer plausibility catalog.
 */
public final class PlausibilityCatalogLoader {

  private static final String CATALOG_RESOURCE = "/br/plausibility/catalog_breast_cancer.json";

  private final String version;
  private final String catalogName;
  private final Map<String, RuleMetadata> rulesById;
  private final BreastCancerCodeSets codeSets;

  /**
   * Load the default breast cancer catalog from classpath resources.
   */
  public PlausibilityCatalogLoader() {
    JsonObject root = PlausibilityCatalog.readCatalogJson(CATALOG_RESOURCE);
    this.version = root.get("version").getAsString();
    this.catalogName = root.get("catalog").getAsString();
    this.rulesById = parseRules(root.getAsJsonArray("rules"));
    this.codeSets = new BreastCancerCodeSets(root.getAsJsonObject("clinicalCodes"));
  }

  public String getVersion() {
    return version;
  }

  public String getCatalogName() {
    return catalogName;
  }

  /**
   * Return metadata for a rule by stable ID.
   *
   * @param ruleId catalog rule ID
   * @return rule metadata
   */
  public RuleMetadata getRule(String ruleId) {
    RuleMetadata metadata = rulesById.get(ruleId);
    if (metadata == null) {
      throw new IllegalArgumentException("Unknown rule id: " + ruleId);
    }
    return metadata;
  }

  public BreastCancerCodeSets getCodeSets() {
    return codeSets;
  }

  private static Map<String, RuleMetadata> parseRules(JsonArray rulesArray) {
    Map<String, RuleMetadata> rules = new LinkedHashMap<>();
    for (JsonElement element : rulesArray) {
      RuleMetadata metadata = RuleMetadata.fromJson(element.getAsJsonObject());
      if (rules.containsKey(metadata.getId())) {
        throw new IllegalStateException("Duplicate rule id in plausibility catalog: "
            + metadata.getId());
      }
      rules.put(metadata.getId(), metadata);
    }
    return Collections.unmodifiableMap(rules);
  }
}
