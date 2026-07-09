package org.mitre.synthea.br.pathway;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mitre.synthea.br.condition.TargetConditionConfig;

/**
 * Versioned catalog of clinical pathway phases for a target condition (AD-3: data pack in
 * resources, no hardcoded clinical data in Java). Loaded from
 * {@code br/pathways/{condition}_phases.json} on the classpath.
 *
 * <p>Consumed by Stories 9.3 (export filter), 9.4 (HTML narrative), 9.7 (GMF episodic module)
 * and 9.8 (timing priors) as the single source of truth for the "relevant event" ontology
 * (docs/research/adr/ADR-008-trajetoria-clinica-focada.md).
 */
public final class PathwayCatalog {

  private static final String RESOURCE_PATTERN = "/br/pathways/%s_phases.json";

  private static final Gson GSON = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

  private String catalogVersion;
  private String condition;
  private List<PathwayPhase> phases;
  private AlwaysInclude alwaysInclude;

  private transient Map<String, PathwayPhase> phasesById;
  private transient List<PathwayPhase> phasesInOrder;
  private transient Set<String> unifiedCodes;
  private transient Set<String> alwaysIncludeAttributes;

  private PathwayCatalog() {
  }

  /**
   * Data pack section listing attributes always relevant regardless of pathway phase (e.g.
   * demographics, BR cohort metadata).
   */
  private static final class AlwaysInclude {
    private List<String> attributes;
  }

  /**
   * Loads the pathway catalog for the configured {@code br.target_condition}, when set.
   *
   * @return parsed catalog for the resolved condition key
   * @throws IllegalStateException when {@code br.target_condition} is unset or blank
   * @throws PathwayCatalogNotFoundException when no data pack exists for the configured condition
   */
  public static PathwayCatalog loadForConfiguredCondition() {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null) {
      throw new IllegalStateException(
          "br.target_condition nao configurado — impossivel resolver catalogo de trajetoria.");
    }
    return loadForCondition(resolved.definition.conditionKey);
  }

  /**
   * Loads and parses the pathway catalog for the given target condition from
   * {@code src/main/resources/br/pathways/{targetCondition}_phases.json}.
   *
   * @param targetCondition value of {@code br.target_condition} (e.g. {@code breast_cancer})
   * @return parsed, immutable-view catalog with phases in canonical order
   * @throws PathwayCatalogNotFoundException when no data pack exists for the condition
   */
  public static PathwayCatalog loadForCondition(String targetCondition) {
    String resourcePath = String.format(RESOURCE_PATTERN, targetCondition);
    try (InputStream stream = PathwayCatalog.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new PathwayCatalogNotFoundException(targetCondition);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        PathwayCatalog catalog = GSON.fromJson(reader, PathwayCatalog.class);
        catalog.finishInit();
        return catalog;
      }
    } catch (IOException e) {
      throw new IllegalStateException("Falha ao ler catalogo de trajetoria: " + resourcePath, e);
    }
  }

  private void finishInit() {
    phasesInOrder = phases.stream()
        .sorted(Comparator.comparingInt(PathwayPhase::getOrder))
        .collect(Collectors.toUnmodifiableList());
    phasesById = new LinkedHashMap<>();
    for (PathwayPhase phase : phasesInOrder) {
      phasesById.put(phase.getPhaseId(), phase);
    }
    unifiedCodes = new TreeSet<>();
    for (PathwayPhase phase : phasesInOrder) {
      for (PathwayCodeEntry entry : phase.getCodeAllowlist()) {
        unifiedCodes.add(entry.toUnifiedKey());
      }
    }
    alwaysIncludeAttributes = alwaysInclude == null || alwaysInclude.attributes == null
        ? Set.of()
        : Set.copyOf(alwaysInclude.attributes);
  }

  /**
   * Target condition key this catalog was loaded for (e.g. {@code breast_cancer}).
   *
   * @return the condition key
   */
  public String getCondition() {
    return condition;
  }

  /**
   * Version of this data pack, consumable by manifest writers for traceability (Story 9.3+).
   *
   * @return the catalog version string
   */
  public String getCatalogVersion() {
    return catalogVersion;
  }

  /**
   * All phases in this catalog, sorted by canonical {@code order} ascending.
   *
   * @return unmodifiable list of phases in canonical order
   */
  public List<PathwayPhase> getPhasesInOrder() {
    return phasesInOrder;
  }

  /**
   * Look up a phase by its stable {@code phase_id}.
   *
   * @param phaseId stable phase identifier (e.g. {@code screening})
   * @return the phase, or {@code null} if no phase with that ID exists
   */
  public PathwayPhase getPhase(String phaseId) {
    return phasesById.get(phaseId);
  }

  /**
   * Whether the given attribute/key is marked {@code always_include} in this catalog (relevant
   * regardless of pathway phase — e.g. demographics, BR cohort metadata).
   *
   * @param attribute attribute or config key name
   * @return {@code true} when the attribute is always included
   */
  public boolean isAlwaysIncludeAttribute(String attribute) {
    return alwaysIncludeAttributes.contains(attribute);
  }

  /**
   * Unified allowlist of {@code system|code} keys across all phases, for fast membership checks
   * by export filters (Story 9.3) independent of which phase a code belongs to.
   *
   * @return unmodifiable set of {@code "{system}|{code}"} keys
   */
  public Set<String> unifiedAllowlistCodes() {
    return unifiedCodes;
  }
}
