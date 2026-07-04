package org.mitre.synthea.br.lifecycle;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.modules.DeathModule;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Unit tests for {@link BrDeathCertification}.
 */
public class BrDeathCertificationTest {

  private String previousProfile;

  @Before
  public void setUp() {
    previousProfile = Config.get("br.profile");
  }

  @After
  public void tearDown() {
    if (previousProfile != null) {
      Config.set("br.profile", previousProfile);
    } else {
      Config.remove("br.profile");
    }
  }

  @Test
  public void testUpstreamDisplaysWhenProfileInactive() {
    Config.set("br.profile", "");
    assertEquals("Death Certification",
        BrDeathCertification.deathCertification().display);
    assertEquals("Cause of Death [US Standard Certificate of Death]",
        BrDeathCertification.causeOfDeathCode().display);
    assertEquals("U.S. standard certificate of death - 2003 revision",
        BrDeathCertification.deathCertificate().display);
  }

  @Test
  public void testBrazilianDisplaysWhenProfileActive() {
    Config.set("br.profile", "br");
    assertEquals("Certificação de óbito",
        BrDeathCertification.deathCertification().display);
    assertEquals("Causa da morte (DO)",
        BrDeathCertification.causeOfDeathCode().display);
    assertEquals("Declaração de Óbito (DO)",
        BrDeathCertification.deathCertificate().display);
  }

  @Test
  public void testPreservesUpstreamCodes() {
    Config.set("br.profile", "br");
    assertEquals(DeathModule.DEATH_CERTIFICATION.code,
        BrDeathCertification.deathCertification().code);
    assertEquals(DeathModule.CAUSE_OF_DEATH_CODE.code,
        BrDeathCertification.causeOfDeathCode().code);
    assertEquals(DeathModule.DEATH_CERTIFICATE.code,
        BrDeathCertification.deathCertificate().code);
  }
}
