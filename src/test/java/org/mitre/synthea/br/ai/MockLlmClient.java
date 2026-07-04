package org.mitre.synthea.br.ai;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double that returns canned LLM responses in order.
 */
public final class MockLlmClient implements LlmClient {

  private final List<String> responses = new ArrayList<>();
  private int index;

  /**
   * Creates a mock with ordered responses.
   *
   * @param cannedResponses responses to return sequentially
   */
  public MockLlmClient(String... cannedResponses) {
    if (cannedResponses != null) {
      for (String response : cannedResponses) {
        responses.add(response);
      }
    }
  }

  @Override
  public String complete(String systemPrompt, String userPrompt) {
    if (responses.isEmpty()) {
      return "{\"action\":\"FinalizePatient\"}";
    }
    String response = responses.get(Math.min(index, responses.size() - 1));
    index++;
    return response;
  }

  /**
   * Returns the number of completed calls.
   *
   * @return number of completed calls
   */
  public int getCallCount() {
    return index;
  }
}
