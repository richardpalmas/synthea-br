package org.mitre.synthea.br.pathway;

import java.util.Collections;
import java.util.List;

/**
 * A single phase of a clinical pathway (e.g. {@code screening}, {@code diagnosis}) as defined in
 * a {@code br/pathways/*_phases.json} data pack (AD-3). Parsed by {@link PathwayCatalog} — never
 * constructed with hardcoded clinical data in Java.
 */
public final class PathwayPhase {

  private String phaseId;
  private int order;
  private String titlePtBr;
  private String descriptionPtBr;
  private List<String> encounterTypes;
  private List<PathwayCodeEntry> codeAllowlist;

  /**
   * Stable identifier for this phase (e.g. {@code screening}), referenced by Stories 9.3/9.4/9.7.
   *
   * @return the stable phase identifier
   */
  public String getPhaseId() {
    return phaseId;
  }

  /**
   * Canonical 1-indexed order of this phase relative to other phases in the same catalog.
   *
   * @return the canonical order
   */
  public int getOrder() {
    return order;
  }

  /**
   * Portuguese (Brazil) display title for this phase.
   *
   * @return the PT-BR title
   */
  public String getTitlePtBr() {
    return titlePtBr;
  }

  /**
   * Portuguese (Brazil) description for this phase.
   *
   * @return the PT-BR description
   */
  public String getDescriptionPtBr() {
    return descriptionPtBr;
  }

  /**
   * Encounter classes typically associated with this phase, if documented.
   *
   * @return unmodifiable list of encounter type strings, possibly empty
   */
  public List<String> getEncounterTypes() {
    return encounterTypes == null ? Collections.emptyList() : encounterTypes;
  }

  /**
   * Clinical codes (SNOMED-CT/LOINC/RxNorm/CPT) that classify a {@code HealthRecord} entry as
   * belonging to this phase.
   *
   * @return unmodifiable list of code allowlist entries, possibly empty
   */
  public List<PathwayCodeEntry> getCodeAllowlist() {
    return codeAllowlist == null ? Collections.emptyList() : codeAllowlist;
  }
}
