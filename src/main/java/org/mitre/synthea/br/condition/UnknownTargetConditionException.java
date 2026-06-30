package org.mitre.synthea.br.condition;

/**
 * Thrown when {@code br.target_condition} references an unsupported condition key.
 */
public class UnknownTargetConditionException extends IllegalArgumentException {

  /**
   * Create an exception for an unknown condition key.
   *
   * @param conditionKey unsupported config value
   */
  public UnknownTargetConditionException(String conditionKey) {
    super(String.format(
        "Condição clínica alvo desconhecida: '%s'. Condições suportadas: %s",
        conditionKey,
        String.join(", ", SupportedConditions.supportedKeys())));
  }
}
