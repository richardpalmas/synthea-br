package org.mitre.synthea.br.terminology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Unit tests for {@link BrTerminologyResolver}.
 */
public class BrTerminologyResolverTest {

  private String previousProfile;

  /**
   * Saves config state before each test.
   */
  @Before
  public void setUp() {
    previousProfile = Config.get("br.profile");
    BrTerminologyResolver.resetCacheForTest();
  }

  /**
   * Restores config state after each test.
   */
  @After
  public void tearDown() {
    BrTerminologyResolver.resetCacheForTest();
    if (previousProfile != null) {
      Config.set("br.profile", previousProfile);
    } else {
      Config.remove("br.profile");
    }
  }

  @Test
  public void testResolveDisplaySnomedWhenProfileActive() {
    Config.set("br.profile", "br");
    Code code = new Code("SNOMED-CT", "254837009",
        "Malignant neoplasm of breast (disorder)");
    assertEquals("Neoplasia maligna da mama", BrTerminologyResolver.resolveDisplay(code));
  }

  @Test
  public void testResolveDisplayFallbackEnglishWhenUnmapped() {
    Config.set("br.profile", "br");
    Code code = new Code("SNOMED-CT", "999999999", "Unknown disorder (disorder)");
    assertEquals("Unknown disorder (disorder)", BrTerminologyResolver.resolveDisplay(code));
  }

  @Test
  public void testResolveDisplayUpstreamWhenProfileInactive() {
    Config.set("br.profile", "");
    Code code = new Code("SNOMED-CT", "254837009",
        "Malignant neoplasm of breast (disorder)");
    assertEquals("Malignant neoplasm of breast (disorder)",
        BrTerminologyResolver.resolveDisplay(code));
  }

  @Test
  public void testResolvePayerLabel() {
    Config.set("br.profile", "br");
    assertEquals("Sem cobertura", BrTerminologyResolver.resolvePayerLabel("NO_INSURANCE"));
  }

  @Test
  public void testResolveRxNormMedication() {
    Config.set("br.profile", "br");
    Code code = new Code("RxNorm", "198240", "Tamoxifen 10 MG Oral Tablet");
    assertEquals("Tamoxifeno 10 mg — comprimido oral",
        BrTerminologyResolver.resolveDisplay(code));
  }

  @Test
  public void testHasMapping() {
    Config.set("br.profile", "br");
    assertTrue(BrTerminologyResolver.hasMapping("SNOMED-CT", "162673000"));
    assertFalse(BrTerminologyResolver.hasMapping("SNOMED-CT", "999999999"));
  }

  @Test
  public void testResolveSocioeconomicKey() {
    Config.set("br.profile", "br");
    assertEquals("Ensino fundamental incompleto",
        BrTerminologyResolver.resolveSocioeconomicKey("less_than_hs"));
  }

  @Test
  public void testResolveDisplayFallbackByLabelText() {
    Config.set("br.profile", "br");
    Code code = new Code("SNOMED-CT", "741062008", "Not in labor force (finding)");
    assertEquals("Fora da força de trabalho", BrTerminologyResolver.resolveDisplay(code));
  }

  @Test
  public void testResolveDisplayLoincCbcPanel() {
    Config.set("br.profile", "br");
    Code code = new Code("LOINC", "58410-2", "CBC panel - Blood by Automated count");
    assertEquals("Hemograma completo — sangue (contagem automatizada)",
        BrTerminologyResolver.resolveDisplay(code));
  }
}
