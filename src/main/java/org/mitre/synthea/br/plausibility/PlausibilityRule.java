package org.mitre.synthea.br.plausibility;

import java.util.List;

import org.mitre.synthea.world.agents.Person;

/**
 * Read-only plausibility rule evaluated against a patient's {@link Person#record}.
 *
 * <p><strong>Implementations MUST NOT mutate</strong> the {@code HealthRecord}, {@code Person},
 * or any clinical attribute. This interface is intentionally separate from
 * {@link org.mitre.synthea.engine.HealthRecordEditor}, which exists to inject realistic errors
 * into records by mutating them.
 *
 * <p>Rules return structured {@link Violation} objects for downstream reporting.
 */
public interface PlausibilityRule {

  /**
   * Evaluate this rule against the given person.
   *
   * @param person patient to evaluate (read-only)
   * @return zero or more violations; never {@code null}
   */
  List<Violation> evaluate(Person person);
}
