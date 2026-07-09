package org.mitre.synthea.br.pathway;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mitre.synthea.br.condition.SupportedConditions;
import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;

/**
 * Classifies {@link HealthRecord} entries into pathway phases using {@link PathwayCatalog}
 * (Story 9.4). Read-only — never mutates clinical data (AD-2).
 */
public final class PathwayEventClassifier {

  private static final String SNOMED_URI = "http://snomed.info/sct";

  private final PathwayCatalog catalog;
  private final Map<String, String> unifiedKeyToPhaseId;
  private final Set<String> targetConditionCodes;

  private PathwayEventClassifier(PathwayCatalog catalog, Set<String> targetConditionCodes) {
    this.catalog = catalog;
    this.unifiedKeyToPhaseId = buildPhaseLookup(catalog);
    this.targetConditionCodes = targetConditionCodes;
  }

  /**
   * Builds a classifier for the configured target condition catalog.
   *
   * @return classifier backed by the active catalog
   */
  public static PathwayEventClassifier forConfiguredCatalog() {
    PathwayCatalog catalog = PathwayCatalog.loadForConfiguredCondition();
    return new PathwayEventClassifier(catalog, resolveTargetConditionCodes());
  }

  /**
   * Returns the catalog backing this classifier.
   *
   * @return pathway catalog
   */
  public PathwayCatalog getCatalog() {
    return catalog;
  }

  /**
   * Resolves the canonical phase id for an entry, or {@code null} when out of pathway.
   *
   * @param entry clinical entry
   * @return phase id or {@code null}
   */
  public String classifyEntry(Entry entry) {
    if (entry == null) {
      return null;
    }
    String phaseId = lookupPhase(entry);
    if (phaseId != null) {
      return phaseId;
    }
    if (entry.type != null && !entry.type.isBlank()) {
      return unifiedKeyToPhaseId.get("SNOMED-CT|" + entry.type);
    }
    return null;
  }

  /**
   * Resolves the phase id for an encounter reason or type code, or {@code null}.
   *
   * @param encounter encounter to classify
   * @return phase id or {@code null}
   */
  public String classifyEncounter(Encounter encounter) {
    if (encounter == null) {
      return null;
    }
    if (encounter.reason != null) {
      String phaseId = unifiedKeyToPhaseId.get(toAllowlistKey(
          encounter.reason.system, encounter.reason.code));
      if (phaseId != null) {
        return phaseId;
      }
    }
    if (encounter.codes != null) {
      for (Code code : encounter.codes) {
        String phaseId = unifiedKeyToPhaseId.get(toAllowlistKey(code.system, code.code));
        if (phaseId != null) {
          return phaseId;
        }
      }
    }
    return null;
  }

  /**
   * Whether the entry represents the configured target condition (visual highlight).
   *
   * @param entry clinical entry
   * @return {@code true} when entry codes match the target condition
   */
  public boolean isTargetConditionEntry(Entry entry) {
    if (entry == null || targetConditionCodes.isEmpty()) {
      return false;
    }
    for (Code code : entry.codes) {
      if (targetConditionCodes.contains(code.code)) {
        return true;
      }
    }
    return entry.type != null && targetConditionCodes.contains(entry.type);
  }

  /**
   * Whether any code on the entry is in the unified pathway allowlist.
   *
   * @param entry clinical entry
   * @return {@code true} when allowlisted
   */
  public boolean isPathwayEntry(Entry entry) {
    return classifyEntry(entry) != null;
  }

  private String lookupPhase(Entry entry) {
    for (Code code : entry.codes) {
      String phaseId = unifiedKeyToPhaseId.get(toAllowlistKey(code.system, code.code));
      if (phaseId != null) {
        return phaseId;
      }
    }
    return null;
  }

  private static Map<String, String> buildPhaseLookup(PathwayCatalog catalog) {
    Map<String, String> lookup = new HashMap<>();
    for (PathwayPhase phase : catalog.getPhasesInOrder()) {
      for (PathwayCodeEntry codeEntry : phase.getCodeAllowlist()) {
        lookup.putIfAbsent(codeEntry.toUnifiedKey(), phase.getPhaseId());
      }
    }
    return lookup;
  }

  private static Set<String> resolveTargetConditionCodes() {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null) {
      return Set.of();
    }
    if ("breast_cancer".equals(resolved.definition.conditionKey)) {
      return Set.of(SupportedConditions.BREAST_CANCER_SNOMED);
    }
    return Set.of();
  }

  private static String toAllowlistKey(String system, String code) {
    return normalizeSystem(system) + "|" + code;
  }

  private static String normalizeSystem(String system) {
    if (system == null) {
      return "";
    }
    if (SNOMED_URI.equals(system)) {
      return "SNOMED-CT";
    }
    return system;
  }
}
