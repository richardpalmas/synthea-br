package org.mitre.synthea.br.ai;

import java.util.Locale;

/**
 * Detects truncated LLM replies and requests continuation before parsing.
 *
 * <p>Continuation runs only when local JSON parse fails <em>and</em> a known
 * truncation marker is present — avoiding false positives on valid clinical text.
 */
public final class LlmResponseGuard {

  /**
   * Canonical truncation markers (case-insensitive substring match).
   */
  static final String[] DEFAULT_TRUNCATION_MARKERS = {
      "for brevity i have stopped",
      "would you like me to continue",
      "i have been cut off",
      "continuar?",
      "deseja que eu continue"
  };

  private static final String CONTINUATION_SYSTEM =
      "Continue the previous incomplete reply. Output only the continuation text "
          + "needed to finish the JSON or answer. Do not restart from scratch.";

  private LlmResponseGuard() {
  }

  /**
   * Whether the response text contains a known truncation marker.
   *
   * @param response model text
   * @return true when a marker is present
   */
  public static boolean looksTruncated(String response) {
    if (response == null || response.isEmpty()) {
      return false;
    }
    String lower = response.toLowerCase(Locale.ROOT);
    for (String marker : DEFAULT_TRUNCATION_MARKERS) {
      if (lower.contains(marker)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Completes a chat turn. Requests continuation only when the reply is not yet
   * valid JSON <em>and</em> a truncation marker is present.
   *
   * @param client LLM client
   * @param systemPrompt system prompt for the original turn
   * @param userPrompt user prompt for the original turn
   * @param maxContinuations max continuation calls (0 = no continuation)
   * @param stats robustness counters (may be null)
   * @return response text (markers stripped only after a real continuation path)
   * @throws LlmException when the provider fails
   */
  public static String completeWithContinuation(LlmClient client, String systemPrompt,
      String userPrompt, int maxContinuations, RobustnessStats stats) throws LlmException {
    String response = client.complete(systemPrompt, userPrompt);
    if (response == null) {
      return "";
    }
    if (LlmJsonParser.parseLocal(response).isSuccess()) {
      return response;
    }
    if (!looksTruncated(response) || maxContinuations < 1) {
      return response;
    }

    StringBuilder combined = new StringBuilder(response);
    int remaining = maxContinuations;
    while (remaining > 0
        && !LlmJsonParser.parseLocal(combined.toString()).isSuccess()
        && looksTruncated(combined.toString())) {
      if (stats != null) {
        stats.incrementTruncationContinuations();
      }
      String withoutMarkers = stripTruncationMarkers(combined.toString());
      String continuation = client.complete(CONTINUATION_SYSTEM,
          "Previous incomplete output:\n" + withoutMarkers
              + "\n\nContinue from where you left off.");
      if (continuation == null || continuation.isEmpty()) {
        return withoutMarkers;
      }
      combined.setLength(0);
      combined.append(withoutMarkers).append(continuation);
      remaining--;
    }
    if (LlmJsonParser.parseLocal(combined.toString()).isSuccess()) {
      return stripTruncationMarkers(combined.toString());
    }
    return stripTruncationMarkers(combined.toString());
  }

  /**
   * Removes known truncation marker phrases from text (case-insensitive).
   *
   * @param text response text
   * @return text without truncation markers
   */
  static String stripTruncationMarkers(String text) {
    if (text == null || text.isEmpty()) {
      return text == null ? "" : text;
    }
    String result = text;
    for (String marker : DEFAULT_TRUNCATION_MARKERS) {
      result = replaceIgnoreCase(result, marker, "");
    }
    return result.trim();
  }

  private static String replaceIgnoreCase(String haystack, String needle, String replacement) {
    String lowerHay = haystack.toLowerCase(Locale.ROOT);
    String lowerNeedle = needle.toLowerCase(Locale.ROOT);
    StringBuilder out = new StringBuilder();
    int idx = 0;
    int found;
    while ((found = lowerHay.indexOf(lowerNeedle, idx)) >= 0) {
      out.append(haystack, idx, found);
      out.append(replacement);
      idx = found + needle.length();
    }
    out.append(haystack, idx, haystack.length());
    return out.toString();
  }
}
