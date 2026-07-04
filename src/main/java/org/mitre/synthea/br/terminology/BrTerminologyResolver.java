package org.mitre.synthea.br.terminology;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.mitre.synthea.br.coding.BrCodeMapper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Resolves clinical and upstream labels to Brazilian Portuguese ({@code display_pt}) at export
 * time when {@code br.profile=br} is active. Does not mutate {@link HealthRecord}.
 */
public final class BrTerminologyResolver {

  private static final String[] DATA_PACKS = {
      "br/terminology/labels_upstream.json",
      "br/terminology/labels_death_br.json",
      "br/terminology/snomed_wellness_common.json",
      "br/terminology/snomed_breast_cancer_pilot.json",
      "br/terminology/snomed_sdoh_extended.json",
      "br/terminology/snomed_dental_extended.json",
      "br/terminology/snomed_pain_infections.json",
      "br/terminology/loinc_wellness_labs.json",
      "br/terminology/loinc_screening_scores.json",
      "br/terminology/rxnorm_pilot.json",
      "br/terminology/rxnorm_cohort_common.json"
  };

  private static volatile Map<String, String> cachedBySystemCode;
  private static volatile Map<String, String> cachedByCodeOnly;
  private static volatile Map<String, String> cachedLabels;

  private BrTerminologyResolver() {
  }

  /**
   * Resolves a clinical code display for export. Returns the original display when the BR profile
   * is inactive or no mapping exists.
   *
   * @param code clinical code from {@link HealthRecord}
   * @return localized display or upstream fallback
   */
  public static String resolveDisplay(Code code) {
    if (code == null) {
      return null;
    }
    String fallback = code.display != null ? code.display : code.code;
    if (!BrProfile.isActive()) {
      return fallback;
    }
    String mapped = lookupClinical(code.system, code.code);
    if (mapped != null) {
      return mapped;
    }
    if (code.system != null && code.system.contains("SNOMED") && code.code != null) {
      BrCodeMapper.Cid10Mapping cid10 = BrCodeMapper.lookup(code.code);
      if (cid10 != null && cid10.getDisplay() != null && !cid10.getDisplay().isEmpty()) {
        return cid10.getDisplay();
      }
    }
    if (code.display != null && !code.display.isEmpty()) {
      String byText = resolveDisplayText(code.display);
      if (!byText.equals(code.display)) {
        return byText;
      }
    }
    return fallback;
  }

  /**
   * Resolves a raw display string (e.g. encounter reason without structured code).
   *
   * @param display upstream English display
   * @return localized label when mapped, otherwise original
   */
  public static String resolveDisplayText(String display) {
    if (display == null || display.isEmpty()) {
      return display;
    }
    if (!BrProfile.isActive()) {
      return display;
    }
    String mapped = cachedLabels().get(display);
    return mapped != null ? mapped : display;
  }

  /**
   * Resolves payer/plan upstream keys (e.g. {@code NO_INSURANCE}).
   *
   * @param payerKey upstream payer name or status key
   * @return localized label
   */
  public static String resolvePayerLabel(String payerKey) {
    if (payerKey == null || payerKey.isEmpty()) {
      return payerKey;
    }
    if (!BrProfile.isActive()) {
      return payerKey;
    }
    String mapped = lookupClinical("PAYER", payerKey);
    if (mapped != null) {
      return mapped;
    }
    mapped = cachedLabels().get(payerKey);
    return mapped != null ? mapped : payerKey;
  }

  /**
   * Resolves socioeconomic upstream keys (education, income bands, employment).
   *
   * @param key upstream attribute key
   * @return localized label
   */
  public static String resolveSocioeconomicKey(String key) {
    if (key == null || key.isEmpty()) {
      return key;
    }
    if (!BrProfile.isActive()) {
      return key;
    }
    String mapped = lookupClinical("SOCIOECONOMIC", key);
    if (mapped != null) {
      return mapped;
    }
    mapped = cachedLabels().get(key);
    return mapped != null ? mapped : key.replace('_', ' ');
  }

  /**
   * Whether a PT-BR mapping exists for the given clinical code.
   *
   * @param system terminology system
   * @param code code value
   * @return true when mapped
   */
  public static boolean hasMapping(String system, String code) {
    if (!BrProfile.isActive() || code == null) {
      return false;
    }
    return lookupClinical(system, code) != null;
  }

  /**
   * Clears cached data packs — test use only.
   */
  public static void resetCacheForTest() {
    cachedBySystemCode = null;
    cachedByCodeOnly = null;
    cachedLabels = null;
  }

  private static String lookupClinical(String system, String code) {
    if (code == null) {
      return null;
    }
    ensureLoaded();
    String normalizedSystem = normalizeSystem(system);
    String bySystem = cachedBySystemCode.get(normalizedSystem + "|" + code);
    if (bySystem != null) {
      return bySystem;
    }
    return cachedByCodeOnly.get(code);
  }

  private static Map<String, String> cachedLabels() {
    ensureLoaded();
    return cachedLabels;
  }

  private static void ensureLoaded() {
    if (cachedBySystemCode != null) {
      return;
    }
    synchronized (BrTerminologyResolver.class) {
      if (cachedBySystemCode != null) {
        return;
      }
      HashMap<String, String> bySystemCode = new HashMap<>();
      HashMap<String, String> byCodeOnly = new HashMap<>();
      HashMap<String, String> labels = new HashMap<>();
      Gson gson = new Gson();
      for (String packPath : DATA_PACKS) {
        try {
          mergePack(gson, packPath, bySystemCode, byCodeOnly, labels);
        } catch (IOException e) {
          throw new IllegalStateException("Unable to read terminology pack: " + packPath, e);
        } catch (JsonParseException e) {
          throw new IllegalStateException("Unable to parse terminology pack: " + packPath, e);
        }
      }
      cachedBySystemCode = Collections.unmodifiableMap(bySystemCode);
      cachedByCodeOnly = Collections.unmodifiableMap(byCodeOnly);
      cachedLabels = Collections.unmodifiableMap(labels);
    }
  }

  private static void mergePack(Gson gson, String packPath,
      HashMap<String, String> bySystemCode,
      HashMap<String, String> byCodeOnly,
      HashMap<String, String> labels) throws IOException {
    String json = Utilities.readResource(packPath);
    JsonObject root = gson.fromJson(json, JsonObject.class);
    if (root == null) {
      return;
    }
    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
      if (entry.getKey().startsWith("_")) {
        continue;
      }
      if (!entry.getValue().isJsonObject()) {
        continue;
      }
      JsonObject obj = entry.getValue().getAsJsonObject();
      String displayPt = readDisplayPt(obj);
      if (displayPt == null) {
        continue;
      }
      String code = entry.getKey();
      String system = obj.has("system") ? obj.get("system").getAsString() : null;
      if (system != null && isLabelSystem(system)) {
        labels.put(code, displayPt);
        String normalized = normalizeSystem(system);
        bySystemCode.put(normalized + "|" + code, displayPt);
      } else {
        String normalized = normalizeSystem(system);
        bySystemCode.put(normalized + "|" + code, displayPt);
        byCodeOnly.putIfAbsent(code, displayPt);
      }
    }
  }

  private static String readDisplayPt(JsonObject obj) {
    if (obj.has("display_pt")) {
      return obj.get("display_pt").getAsString();
    }
    return null;
  }

  private static boolean isLabelSystem(String system) {
    return "LABEL".equalsIgnoreCase(system)
        || "PAYER".equalsIgnoreCase(system)
        || "SOCIOECONOMIC".equalsIgnoreCase(system);
  }

  private static String normalizeSystem(String system) {
    if (system == null || system.isEmpty()) {
      return "UNKNOWN";
    }
    String upper = system.toUpperCase(Locale.ROOT);
    if (upper.contains("SNOMED")) {
      return "SNOMED";
    }
    if (upper.contains("RXNORM") || upper.equals("RXNORM")) {
      return "RXNORM";
    }
    if (upper.contains("LOINC")) {
      return "LOINC";
    }
    if (upper.contains("ICD")) {
      return "ICD";
    }
    if (isLabelSystem(system)) {
      return system.toUpperCase(Locale.ROOT);
    }
    return upper;
  }
}
