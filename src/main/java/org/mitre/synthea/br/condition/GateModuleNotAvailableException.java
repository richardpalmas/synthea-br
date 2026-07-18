package org.mitre.synthea.br.condition;

/**
 * Thrown when the keep-module gate file for a condition is not yet available.
 */
public class GateModuleNotAvailableException extends IllegalStateException {

  /**
   * Create an exception for a missing gate module.
   *
   * @param conditionKey configured condition key
   */
  public GateModuleNotAvailableException(String conditionKey) {
    super(String.format(
        "Módulo de gate para a condição '%s' ainda não disponível — ver Story 2.2",
        conditionKey));
  }
}
