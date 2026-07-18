package org.mitre.synthea.br.ai;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Random;

/**
 * Assigns a {@link NarrativeWritingPersona} per patient from seed + patient id.
 */
public final class NarrativePersonaAssigner {

  public static final String MODE_DETERMINISTIC = "deterministic";
  public static final String MODE_RANDOM = "random";

  private NarrativePersonaAssigner() {
  }

  /**
   * Assigns a writing persona for one patient.
   *
   * @param patientId synthetic patient id
   * @param seed generator or population seed
   * @param mode {@code deterministic} or {@code random}
   * @return assigned persona
   */
  public static NarrativeWritingPersona assign(String patientId, long seed, String mode) {
    NarrativeWritingPersona[] all = NarrativeWritingPersona.all();
    String normalized = mode == null ? MODE_DETERMINISTIC
        : mode.trim().toLowerCase(Locale.ROOT);
    if (MODE_RANDOM.equals(normalized)) {
      // Per-patient variation: mix patientId into the run seed (still reproducible).
      long patientMix = patientId == null ? 0L : patientId.hashCode();
      Random random = new Random(seed ^ (patientMix * 0x9E3779B97F4A7C15L));
      return all[random.nextInt(all.length)];
    }
    // deterministic: stable hash of patientId + seed
    String material = (patientId == null ? "" : patientId) + "|" + seed;
    int hash = material.hashCode();
    // Mix with UTF-8 bytes for slightly better distribution across short ids
    byte[] bytes = material.getBytes(StandardCharsets.UTF_8);
    int mixed = hash;
    for (byte b : bytes) {
      mixed = 31 * mixed + (b & 0xff);
    }
    return all[Math.floorMod(mixed, all.length)];
  }
}
