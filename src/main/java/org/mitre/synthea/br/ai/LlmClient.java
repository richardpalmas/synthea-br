package org.mitre.synthea.br.ai;

/**
 * Minimal LLM completion contract for AI enrichment personas.
 */
public interface LlmClient {

  /**
   * Sends a chat-style completion request.
   *
   * @param systemPrompt system instructions
   * @param userPrompt user content
   * @return model text response
   * @throws LlmException when the provider returns an error
   */
  String complete(String systemPrompt, String userPrompt) throws LlmException;
}
