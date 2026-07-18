package org.mitre.synthea.br.demographics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.demographics.BrDemographicsLoader.BrDemographicsData;
import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.helpers.Utilities;

public class BrDemographicsLoaderTest {

  private static final List<String> AGE_GROUPS = Arrays.asList(
      "0..4", "5..9", "10..14", "15..19", "20..24", "25..29",
      "30..34", "35..39", "40..44", "45..49", "50..54",
      "55..59", "60..64", "65..69", "70..74", "75..79", "80..84", "85..110");

  /**
   * Load test configuration and reset loader cache before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrDemographicsLoader.resetCacheForTest();
  }

  /**
   * Reset loader cache after each test.
   */
  @After
  public void tearDown() {
    BrDemographicsLoader.resetCacheForTest();
  }

  @Test
  public void testRawCsvAgeFractionsSumToOneInSourceFile() throws Exception {
    String raw = Utilities.readResource("br/demographics/distribuicao_nacional.csv", true, false);
    double ageSum = 0.0;
    for (String line : raw.split("\\R")) {
      String trimmed = line.trim();
      if (trimmed.startsWith("#") || trimmed.isEmpty() || trimmed.startsWith("section,")) {
        continue;
      }
      String[] parts = trimmed.split(",");
      if (parts.length == 3 && "age".equals(parts[0])) {
        ageSum += Double.parseDouble(parts[2]);
      }
    }
    assertEquals("Raw age fractions in data pack should sum to 1.0", 1.0, ageSum, 0.001);
  }

  @Test
  public void testLoadCsvAgeGenderRaceFractionsSumToOne() throws Exception {
    BrDemographicsData data = BrDemographicsLoader.load();

    assertFractionsSumToOne(data.getAges(), "age");
    assertFractionsSumToOne(data.getGender(), "gender");
    assertFractionsSumToOne(data.getRaceIbge(), "race_ibge");
    assertFractionsSumToOne(data.getRace(), "internal race");
    assertFractionsSumToOne(data.getLanguage(), "language");
    assertFractionsSumToOne(data.getEducation(), "education");
    assertFractionsSumToOne(data.getIncome(), "income");

    for (String ageGroup : AGE_GROUPS) {
      assertTrue("Missing age group " + ageGroup, data.getAges().containsKey(ageGroup));
    }
    assertEquals(2, data.getGender().size());
    assertEquals(5, data.getRaceIbge().size());
    assertTrue("Portuguese must be dominant language",
        data.getLanguage().get("portuguese") > 0.9);
  }

  @Test
  public void testRaceMapperSingleCategoryMapping() {
    DefaultRandomNumberGenerator random = new DefaultRandomNumberGenerator(42L);
    assertEquals("white", BrRaceMapper.toInternalRace("branca", random));
    assertEquals("black", BrRaceMapper.toInternalRace("preta", random));
    assertEquals("asian", BrRaceMapper.toInternalRace("amarela", random));
    assertEquals("native", BrRaceMapper.toInternalRace("indigena", random));
  }

  @Test
  public void testRaceMapperBrazilianDisplayTranslation() {
    assertEquals("branca", BrRaceMapper.toBrazilianDisplayRace("white"));
    assertEquals("preta", BrRaceMapper.toBrazilianDisplayRace("black"));
    assertEquals("amarela", BrRaceMapper.toBrazilianDisplayRace("asian"));
    assertEquals("indigena", BrRaceMapper.toBrazilianDisplayRace("native"));
    assertEquals("outro", BrRaceMapper.toBrazilianDisplayRace("other"));
    assertEquals("hawaiana", BrRaceMapper.toBrazilianDisplayRace("hawaiian"));
  }

  @Test
  public void testRaceMapperInternalKeyRoundTrip() {
    assertEquals("white", BrRaceMapper.toInternalRaceKey("branca"));
    assertEquals("other", BrRaceMapper.toInternalRaceKey("outro"));
    assertEquals("black", BrRaceMapper.toInternalRaceKey("black"));
  }

  @Test
  public void testRaceMapperAppliesPardaSplit() {
    Map<String, Double> ibge = new HashMap<>();
    ibge.put("branca", 0.50);
    ibge.put("preta", 0.10);
    ibge.put("parda", 0.40);
    ibge.put("amarela", 0.0);
    ibge.put("indigena", 0.0);

    Map<String, Double> internal = BrRaceMapper.toInternalRaceDistribution(ibge);

    assertEquals(0.50, internal.get("white"), 0.0001);
    assertEquals(0.10 + 0.40 * BrRaceMapper.PARDA_TO_BLACK_RATIO,
        internal.get("black"), 0.0001);
    assertEquals(0.40 * BrRaceMapper.PARDA_TO_OTHER_RATIO,
        internal.get("other"), 0.0001);
  }

  private static void assertFractionsSumToOne(Map<String, Double> fractions, String label) {
    double sum = fractions.values().stream().mapToDouble(Double::doubleValue).sum();
    assertEquals("Fractions for " + label + " should sum to 1.0", 1.0, sum, 0.001);
  }
}
