package org.mitre.synthea.br.plausibility;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * A clinical code reference (system + code) from the catalog.
 */
public final class ClinicalCode {

  private final String system;
  private final String code;
  private final String display;

  /**
   * Create a clinical code reference.
   *
   * @param system code system URI or name
   * @param code code value
   * @param display human-readable display
   */
  public ClinicalCode(String system, String code, String display) {
    this.system = system;
    this.code = code;
    this.display = display;
  }

  /**
   * Parse a clinical code from catalog JSON.
   *
   * @param json code object
   * @return parsed code
   */
  public static ClinicalCode fromJson(JsonObject json) {
    return new ClinicalCode(
        json.get("system").getAsString(),
        normalizeCode(json.get("code")),
        json.has("display") ? json.get("display").getAsString() : "");
  }

  private static String normalizeCode(JsonElement element) {
    if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
      return String.valueOf(element.getAsLong());
    }
    return element.getAsString();
  }

  public String getSystem() {
    return system;
  }

  public String getCode() {
    return code;
  }

  public String getDisplay() {
    return display;
  }
}
