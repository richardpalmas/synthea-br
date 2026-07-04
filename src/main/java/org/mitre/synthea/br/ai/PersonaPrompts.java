package org.mitre.synthea.br.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads persona system prompts from {@code br/ai/prompts/}.
 */
public final class PersonaPrompts {

  private static final Map<String, String> CACHE = new HashMap<>();

  private PersonaPrompts() {
  }

  /**
   * Returns the system prompt for a persona role.
   *
   * @param persona one of hypothesis, test_chooser, stewardship, checklist, challenger
   * @return prompt text
   */
  public static String get(String persona) {
    return CACHE.computeIfAbsent(persona, PersonaPrompts::load);
  }

  private static String load(String persona) {
    String path = "/br/ai/prompts/" + persona + ".txt";
    try (InputStream in = PersonaPrompts.class.getResourceAsStream(path)) {
      if (in == null) {
        return defaultPrompt(persona);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return defaultPrompt(persona);
    }
  }

  private static String defaultPrompt(String persona) {
    return "Você é o Dr. " + persona + " no painel MAI-DxO do Synthea-br. "
        + "Responda SOMENTE em JSON válido com campos: action, query, rationale, operations. "
        + "actions: AskQuestion, RequestRecordSlice, ProposeCorrection, FinalizePatient.";
  }
}
