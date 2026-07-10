package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link NarrativePersonaAssigner}.
 */
public class NarrativePersonaAssignerTest {

  @Test
  public void testDeterministicSameSeedSamePersona() {
    NarrativeWritingPersona a = NarrativePersonaAssigner.assign(
        "patient-1", 42L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    NarrativeWritingPersona b = NarrativePersonaAssigner.assign(
        "patient-1", 42L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    assertEquals(a, b);
  }

  @Test
  public void testDeterministicDifferentPatientsMayDiffer() {
    NarrativeWritingPersona a = NarrativePersonaAssigner.assign(
        "patient-A", 99L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    NarrativeWritingPersona b = NarrativePersonaAssigner.assign(
        "patient-B", 99L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    // Not guaranteed different, but with distinct ids usually differs; assert both valid.
    assertNotNull(a);
    assertNotNull(b);
    assertTrue(a.getId().length() > 0);
  }

  @Test
  public void testDifferentSeedsCanChangePersona() {
    NarrativeWritingPersona a = NarrativePersonaAssigner.assign(
        "same-patient", 1L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    NarrativeWritingPersona b = NarrativePersonaAssigner.assign(
        "same-patient", 2L, NarrativePersonaAssigner.MODE_DETERMINISTIC);
    // May or may not differ; at least both resolve
    assertNotNull(a);
    assertNotNull(b);
  }

  @Test
  public void testRandomModeReproducibleWithSameSeed() {
    NarrativeWritingPersona a = NarrativePersonaAssigner.assign(
        "p", 7L, NarrativePersonaAssigner.MODE_RANDOM);
    NarrativeWritingPersona b = NarrativePersonaAssigner.assign(
        "p", 7L, NarrativePersonaAssigner.MODE_RANDOM);
    assertEquals(a, b);
  }

  @Test
  public void testRandomModeVariesByPatientId() {
    long seed = 12345L;
    java.util.Set<NarrativeWritingPersona> seen = new java.util.HashSet<>();
    for (int i = 0; i < 20; i++) {
      seen.add(NarrativePersonaAssigner.assign(
          "patient-" + i, seed, NarrativePersonaAssigner.MODE_RANDOM));
    }
    assertTrue("random mode should vary personas across patients", seen.size() > 1);
  }

  @Test
  public void testAllPersonasLoadStylePrompts() {
    for (NarrativeWritingPersona persona : NarrativeWritingPersona.all()) {
      String prompt = persona.loadStylePrompt();
      assertNotNull(prompt);
      assertTrue(prompt.length() > 10);
      assertEquals(persona, NarrativeWritingPersona.fromId(persona.getId()));
    }
  }

  @Test
  public void testUnknownIdDefaultsToNarrative() {
    assertEquals(NarrativeWritingPersona.NARRATIVE,
        NarrativeWritingPersona.fromId("unknown-style"));
  }

  @Test
  public void testComposeSystemPromptIncludesStyle() {
    String composed = AiNarrativeSummarizer.composeSystemPrompt(
        "narrative_patient_summary", NarrativeWritingPersona.CONCISE);
    assertTrue(composed.contains("concise"));
    assertNotEquals(composed, "");
  }
}
