package org.mitre.synthea.br.coding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.coding.BrCodeMapper.Cid10Mapping;

/**
 * Unit tests for {@link BrCodeMapper} (Story 3.3 AC #1, #5).
 */
public class BrCodeMapperTest {

  private static final String BREAST_CANCER_SNOMED = "254837009";

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrCodeMapper.resetCacheForTest();
  }

  /**
   * Reset mapper cache after each test.
   */
  @After
  public void tearDown() {
    BrCodeMapper.resetCacheForTest();
  }

  @Test
  public void testCanMapBreastCancerPilotCode() {
    assertTrue(BrCodeMapper.canMap(BREAST_CANCER_SNOMED));
  }

  @Test
  public void testLookupReturnsC50PilotMapping() {
    Cid10Mapping mapping = BrCodeMapper.lookup(BREAST_CANCER_SNOMED);
    assertNotNull(mapping);
    assertEquals("C50.9", mapping.getCode());
    assertEquals("CID-10", mapping.getSystem());
    assertEquals("Neoplasia maligna da mama, não especificada", mapping.getDisplay());
  }

  @Test
  public void testLookupReturnsNullForUnmappedCode() {
    assertFalse(BrCodeMapper.canMap("999999999"));
    assertNull(BrCodeMapper.lookup("999999999"));
  }

  @Test
  public void testLookupReturnsNullForNullInput() {
    assertNull(BrCodeMapper.lookup(null));
  }
}
