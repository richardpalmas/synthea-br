package org.mitre.synthea.br.condition;

import org.mitre.synthea.engine.Module;
import org.mitre.synthea.engine.State;
import org.mitre.synthea.world.agents.Person;

/**
 * Shared evaluation of keep-module gate semantics for Synthea-br.
 */
public final class GateEvaluator {

  private GateEvaluator() {
  }

  /**
   * Evaluate whether a person satisfies a keep module at the given finish time.
   *
   * <p>Uses the same terminal-state convention as {@code Generator.checkCriteria}: the gate module
   * must end in state {@code Keep}.
   *
   * @param person simulated person
   * @param gateModule loaded keep module
   * @param finishTime simulation finish timestamp
   * @return true when the gate module terminates in {@code Keep}
   */
  public static boolean matchesCondition(Person person, Module gateModule, long finishTime) {
    gateModule.process(person, finishTime, false);
    State terminal = person.history.get(0);
    return "Keep".equals(terminal.name);
  }

  /**
   * Check whether the person has the breast cancer SNOMED code active in their record.
   *
   * @param person simulated person
   * @return true when SNOMED {@code 254837009} is present
   */
  public static boolean hasBreastCancer(Person person) {
    return person.record.present.containsKey(SupportedConditions.BREAST_CANCER_SNOMED);
  }
}
