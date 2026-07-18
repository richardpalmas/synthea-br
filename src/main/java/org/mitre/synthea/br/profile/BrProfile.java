package org.mitre.synthea.br.profile;

import org.mitre.synthea.helpers.Config;

/**
 * Master flag for the Brazilian localization profile ({@code br.profile}).
 *
 * <p>{@link #isActive()} is the <strong>only</strong> entry point for checking whether the BR
 * profile is enabled. Stories 3.2, 3.3, and 3.4 must reuse this method — do not re-read
 * {@code br.profile} elsewhere.
 *
 * <p>Accepted values: {@code br} (case-insensitive). Absent, empty, or any other value preserves
 * upstream US behavior.
 */
public final class BrProfile {

  private static final String PROPERTY_KEY = "br.profile";
  private static final String ACTIVE_VALUE = "br";

  private BrProfile() {
  }

  /**
   * Whether the Brazilian profile is active.
   *
   * @return {@code true} when {@code br.profile=br} (case-insensitive)
   */
  public static boolean isActive() {
    String value = Config.get(PROPERTY_KEY);
    return value != null && ACTIVE_VALUE.equalsIgnoreCase(value.trim());
  }

  /**
   * Effective country code for geography export (Story 3.2 AC #4).
   *
   * <p>When the BR profile is active, returns {@code BR} regardless of
   * {@code generate.geography.country_code}, logging a warning if the configured value differs.
   *
   * @return {@code BR} when profile active; otherwise {@code generate.geography.country_code}
   */
  public static String getEffectiveCountryCode() {
    if (isActive()) {
      String configured = Config.get("generate.geography.country_code");
      if (configured != null && !configured.trim().isEmpty()
          && !"BR".equalsIgnoreCase(configured.trim())) {
        System.err.println("WARNING: br.profile=br overrides generate.geography.country_code ('"
            + configured + "') with 'BR' for export.");
      }
      return "BR";
    }
    return Config.get("generate.geography.country_code");
  }
}
