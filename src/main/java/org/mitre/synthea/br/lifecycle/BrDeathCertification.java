package org.mitre.synthea.br.lifecycle;

import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.modules.DeathModule;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Brazilian death certificate (Declaração de Óbito / SIM) labels when {@code br.profile=br}.
 * Preserves upstream LOINC/SNOMED codes; only localizes human-readable display text.
 */
public final class BrDeathCertification {

  private BrDeathCertification() {
  }

  /**
   * Returns a copy of {@code code} with Brazilian Portuguese display when the BR profile is active.
   *
   * @param code upstream death-related code
   * @return localized copy or the original reference when profile inactive or unmapped
   */
  public static Code localized(Code code) {
    if (code == null || !BrProfile.isActive()) {
      return code;
    }
    String displayPt = displayForCode(code);
    if (displayPt == null) {
      return code;
    }
    return new Code(code.system, code.code, displayPt);
  }

  /**
   * SNOMED code for the death certification encounter.
   *
   * @return localized or upstream code
   */
  public static Code deathCertification() {
    return localized(DeathModule.DEATH_CERTIFICATION);
  }

  /**
   * LOINC code for the cause-of-death observation type.
   *
   * @return localized or upstream code
   */
  public static Code causeOfDeathCode() {
    return localized(DeathModule.CAUSE_OF_DEATH_CODE);
  }

  /**
   * LOINC code for the death certificate report.
   *
   * @return localized or upstream code
   */
  public static Code deathCertificate() {
    return localized(DeathModule.DEATH_CERTIFICATE);
  }

  private static String displayForCode(Code code) {
    if (code.code == null) {
      return null;
    }
    switch (code.code) {
      case "308646001":
        return "Certificação de óbito";
      case "69453-9":
        return "Causa da morte (DO)";
      case "69409-1":
        return "Declaração de Óbito (DO)";
      default:
        return null;
    }
  }
}
