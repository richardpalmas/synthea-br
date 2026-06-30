package org.mitre.synthea.br.coding;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mitre.synthea.helpers.Utilities;

/**
 * Loads SNOMED-CT → CID-10 BR mappings from {@code br/coding/} data packs.
 *
 * <p>Uses the same JSON schema as {@code org.mitre.synthea.export.rif.CodeMapper} (source code →
 * array of target entries with {@code code}, optional {@code display}/{@code description},
 * optional {@code system}, optional {@code weight}) without coupling to {@code exporter.bfd.*}.
 */
public final class BrCodeMapper {

  static final String BREAST_CANCER_MAPPING = "br/coding/snomed_to_cid10_breast_cancer.json";

  private static volatile Map<String, List<Map<String, String>>> cached;

  private BrCodeMapper() {
  }

  /**
   * Immutable CID-10 target resolved from a SNOMED source code.
   */
  public static final class Cid10Mapping {
    private final String code;
    private final String system;
    private final String display;

    Cid10Mapping(String code, String system, String display) {
      this.code = code;
      this.system = system;
      this.display = display;
    }

    /**
     * CID-10 code (e.g. {@code C50.9}).
     *
     * @return ICD-10/CID-10 code value
     */
    public String getCode() {
      return code;
    }

    /**
     * Terminology system short name (e.g. {@code CID-10}).
     *
     * @return system identifier from the data pack
     */
    public String getSystem() {
      return system;
    }

    /**
     * Human-readable display for the CID-10 code.
     *
     * @return display text, may be null
     */
    public String getDisplay() {
      return display;
    }
  }

  /**
   * Whether a CID-10 mapping exists for the supplied SNOMED code.
   *
   * @param snomedCode SNOMED-CT code string
   * @return true when mapped in the loaded data pack
   */
  public static boolean canMap(String snomedCode) {
    return lookup(snomedCode) != null;
  }

  /**
   * Resolve the first CID-10 mapping for a SNOMED code (pilot subset uses weight 1.0).
   *
   * @param snomedCode SNOMED-CT code string
   * @return mapping or null when absent
   */
  public static Cid10Mapping lookup(String snomedCode) {
    if (snomedCode == null) {
      return null;
    }
    List<Map<String, String>> entries = load().get(snomedCode);
    if (entries == null || entries.isEmpty()) {
      return null;
    }
    Map<String, String> entry = entries.get(0);
    if (entry == null) {
      return null;
    }
    String code = entry.get("code");
    if (code == null || code.isEmpty()) {
      return null;
    }
    String system = entry.get("system");
    if (system == null || system.isEmpty()) {
      system = "CID-10";
    }
    String display = entry.get("display");
    if (display == null) {
      display = entry.get("description");
    }
    return new Cid10Mapping(code, system, display);
  }

  /**
   * Clears cached mappings — test use only.
   */
  public static void resetCacheForTest() {
    cached = null;
  }

  private static Map<String, List<Map<String, String>>> load() {
    Map<String, List<Map<String, String>>> local = cached;
    if (local != null) {
      return local;
    }
    synchronized (BrCodeMapper.class) {
      if (cached != null) {
        return cached;
      }
      cached = Collections.unmodifiableMap(readMapping(BREAST_CANCER_MAPPING));
      return cached;
    }
  }

  private static Map<String, List<Map<String, String>>> readMapping(String resourcePath) {
    try {
      String json = Utilities.readResource(resourcePath);
      Gson gson = new Gson();
      JsonObject root = gson.fromJson(json, JsonObject.class);
      if (root == null) {
        return Collections.emptyMap();
      }
      Type listType = new TypeToken<List<Map<String, String>>>() {
      }.getType();
      HashMap<String, List<Map<String, String>>> filtered = new HashMap<>();
      for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
        if (entry.getKey().startsWith("_")) {
          continue;
        }
        filtered.put(entry.getKey(), gson.fromJson(entry.getValue(), listType));
      }
      return filtered;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read BR code mapping: " + resourcePath, e);
    } catch (JsonParseException e) {
      throw new IllegalStateException("Unable to parse BR code mapping: " + resourcePath, e);
    }
  }
}
