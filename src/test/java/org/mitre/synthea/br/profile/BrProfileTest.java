package org.mitre.synthea.br.profile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.demographics.BrDemographicsLoader;
import org.mitre.synthea.helpers.Config;

public class BrProfileTest {

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
  }

  /**
   * Clear BR profile override after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.profile", "");
    BrDemographicsLoader.resetCacheForTest();
  }

  @Test
  public void testIsActiveWhenProfileIsBr() {
    Config.set("br.profile", "br");
    assertTrue(BrProfile.isActive());
  }

  @Test
  public void testIsActiveCaseInsensitive() {
    Config.set("br.profile", "BR");
    assertTrue(BrProfile.isActive());
  }

  @Test
  public void testIsInactiveWhenUnset() {
    Config.set("br.profile", "");
    assertFalse(BrProfile.isActive());
  }

  @Test
  public void testIsInactiveWhenEmpty() {
    Config.set("br.profile", "");
    assertFalse(BrProfile.isActive());
  }

  @Test
  public void testIsInactiveForOtherValues() {
    Config.set("br.profile", "us");
    assertFalse(BrProfile.isActive());
  }
}
