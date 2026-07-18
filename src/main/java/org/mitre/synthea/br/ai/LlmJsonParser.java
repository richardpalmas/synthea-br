package org.mitre.synthea.br.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Robust JSON extraction and optional LLM cleanup for MAI-DxO persona replies.
 */
public final class LlmJsonParser {

  private static final String CLEAN_SYSTEM =
      "You repair malformed model output into a single valid JSON object. "
          + "Return ONLY the JSON object, no markdown fences, no commentary. "
          + "The JSON must include an \"action\" field for a MAI-DxO persona decision.";

  private LlmJsonParser() {
  }

  /**
   * Result of a local parse attempt (no LLM).
   */
  public static final class ParseResult {
    private final JsonObject object;
    private final String errorMessage;

    private ParseResult(JsonObject object, String errorMessage) {
      this.object = object;
      this.errorMessage = errorMessage;
    }

    /**
     * Whether a JSON object was parsed successfully.
     *
     * @return true on success
     */
    public boolean isSuccess() {
      return object != null;
    }

    /**
     * Parsed JSON object, or null on failure.
     *
     * @return object or null
     */
    public JsonObject getObject() {
      return object;
    }

    /**
     * Parser error message when unsuccessful.
     *
     * @return error or null
     */
    public String getErrorMessage() {
      return errorMessage;
    }
  }

  /**
   * Strips markdown fences and extracts the outermost JSON object, then parses it.
   *
   * @param raw model text
   * @return parse result
   */
  public static ParseResult parseLocal(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
      return new ParseResult(null, "empty response");
    }
    String stripped = stripMarkdownFence(raw.trim());
    String json = extractJsonObject(stripped);
    if (json == null) {
      return new ParseResult(null, "no JSON object delimiters found");
    }
    try {
      return new ParseResult(JsonParser.parseString(json).getAsJsonObject(), null);
    } catch (Exception expected) {
      return new ParseResult(null, expected.getMessage());
    }
  }

  /**
   * Attempts local parse; on failure, asks the same LLM client to clean the output.
   *
   * @param client LLM client (same BYOK client as the debate)
   * @param raw original model text
   * @param maxRetries maximum clean-with-LLM attempts (0 = local only)
   * @param stats robustness counters (may be null)
   * @return parse result after local + optional LLM cleanup
   */
  public static ParseResult parseWithFallback(LlmClient client, String raw, int maxRetries,
      RobustnessStats stats) {
    ParseResult local = parseLocal(raw);
    if (local.isSuccess()) {
      return local;
    }
    if (client == null || maxRetries < 1) {
      return local;
    }
    String lastError = local.getErrorMessage();
    String currentRaw = raw;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
      if (stats != null) {
        stats.incrementJsonParseRetries();
      }
      try {
        String cleaned = client.complete(CLEAN_SYSTEM, buildCleanUserPrompt(currentRaw, lastError));
        ParseResult cleanedResult = parseLocal(cleaned);
        if (cleanedResult.isSuccess()) {
          return cleanedResult;
        }
        lastError = cleanedResult.getErrorMessage();
        currentRaw = cleaned;
      } catch (LlmException e) {
        lastError = e.getMessage();
      }
    }
    return new ParseResult(null, lastError);
  }

  /**
   * Removes a leading/trailing markdown code fence when present.
   *
   * @param text raw text
   * @return text without outer fence
   */
  static String stripMarkdownFence(String text) {
    String trimmed = text.trim();
    if (!trimmed.startsWith("```")) {
      return trimmed;
    }
    int firstNewline = trimmed.indexOf('\n');
    if (firstNewline < 0) {
      return trimmed;
    }
    String withoutOpen = trimmed.substring(firstNewline + 1);
    int close = withoutOpen.lastIndexOf("```");
    if (close >= 0) {
      return withoutOpen.substring(0, close).trim();
    }
    return withoutOpen.trim();
  }

  /**
   * Extracts substring from the first opening brace to the last closing brace.
   *
   * @param text text possibly containing a JSON object
   * @return JSON substring or null
   */
  static String extractJsonObject(String text) {
    int start = text.indexOf('{');
    int end = text.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return text.substring(start, end + 1);
    }
    return null;
  }

  private static String buildCleanUserPrompt(String raw, String parseError) {
    return "The following model output is not valid JSON for a MAI-DxO persona decision.\n"
        + "Parser error: " + (parseError == null ? "unknown" : parseError) + "\n\n"
        + "Raw output:\n" + raw + "\n\n"
        + "Return ONLY a valid JSON object with fields such as "
        + "action, query, rationale, operations.";
  }
}
