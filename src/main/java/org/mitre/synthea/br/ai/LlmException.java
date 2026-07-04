package org.mitre.synthea.br.ai;

/**
 * Raised when an LLM provider call fails.
 */
public class LlmException extends Exception {

  /**
   * Constructs with message.
   *
   * @param message error detail
   */
  public LlmException(String message) {
    super(message);
  }

  /**
   * Constructs with message and cause.
   *
   * @param message error detail
   * @param cause underlying cause
   */
  public LlmException(String message, Throwable cause) {
    super(message, cause);
  }
}
