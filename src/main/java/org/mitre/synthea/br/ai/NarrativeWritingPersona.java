package org.mitre.synthea.br.ai;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writing-style personas for HTML narrative summaries (Story 8.2).
 * Distinct from MAI-DxO clinical debate personas in {@link PersonaPrompts}.
 */
public enum NarrativeWritingPersona {

  CONCISE("concise"),
  NARRATIVE("narrative"),
  BULLET_POINTS("bullet_points"),
  CLINICAL_SHORTHAND("clinical_shorthand"),
  ABCDE("abcde");

  private static final Map<String, String> CACHE = new HashMap<>();

  private final String resourceId;

  NarrativeWritingPersona(String resourceId) {
    this.resourceId = resourceId;
  }

  /**
   * Stable id used in config, logs, and resource filenames.
   *
   * @return resource id
   */
  public String getId() {
    return resourceId;
  }

  /**
   * Loads the PT-BR style instruction for this persona.
   *
   * @return style prompt text
   */
  public String loadStylePrompt() {
    return CACHE.computeIfAbsent(resourceId, NarrativeWritingPersona::load);
  }

  /**
   * Resolves a persona by id (case-insensitive).
   *
   * @param id persona id
   * @return matching persona or {@link #NARRATIVE} as default
   */
  public static NarrativeWritingPersona fromId(String id) {
    if (id == null || id.trim().isEmpty()) {
      return NARRATIVE;
    }
    String normalized = id.trim().toLowerCase(Locale.ROOT);
    for (NarrativeWritingPersona persona : values()) {
      if (persona.resourceId.equals(normalized)) {
        return persona;
      }
    }
    return NARRATIVE;
  }

  /**
   * Returns all personas in declaration order (stable for hashing).
   *
   * @return persona array
   */
  public static NarrativeWritingPersona[] all() {
    return values();
  }

  private static String load(String id) {
    String path = "/br/ai/prompts/writing_personas/" + id + ".txt";
    try (InputStream in = NarrativeWritingPersona.class.getResourceAsStream(path)) {
      if (in == null) {
        return defaultStyle(id);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      return defaultStyle(id);
    }
  }

  private static String defaultStyle(String id) {
    return "Estilo de escrita: " + id
        + ". Produza o resumo em português do Brasil sem inventar fatos.";
  }
}
