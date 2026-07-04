package org.mitre.synthea.br.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Structured correction proposal from persona debate.
 */
public final class CorrectionProposal {

  /** Supported operation types for MVP v1. */
  public static final List<String> SUPPORTED_OPS = List.of(
      "add_observation",
      "fix_encounter_date",
      "set_person_attribute",
      "add_procedure",
      "flag_unfixable");

  private final List<Map<String, Object>> operations;

  /**
   * Creates a proposal with operations.
   *
   * @param operations list of operation maps
   */
  public CorrectionProposal(List<Map<String, Object>> operations) {
    this.operations = operations == null ? new ArrayList<>() : new ArrayList<>(operations);
  }

  /**
   * Returns operations to apply or flag.
   *
   * @return operation list
   */
  public List<Map<String, Object>> getOperations() {
    return operations;
  }

  /**
   * Whether the proposal contains any actionable operations.
   *
   * @return true when non-empty
   */
  public boolean hasOperations() {
    return !operations.isEmpty();
  }
}
