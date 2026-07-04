package org.mitre.synthea.br.demographics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Table;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.demographics.BrDemographicsLoader.BrDemographicsData;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.geography.Demographics;

/**
 * Statistical and regression tests for BR national demographics (Story 3.1 AC #2, #6, #7).
 */
public class BrDemographicsIntegrationTest {

  private static final int SAMPLE_SIZE = 1000;
  private static final long FIXED_SEED = 20260630L;
  /** Max absolute difference between observed and reference proportion (AC #7). */
  private static final double PROPORTION_THRESHOLD = 0.05;

  private static final List<String> AGE_GROUPS = Arrays.asList(
      "0..4", "5..9", "10..14", "15..19", "20..24", "25..29",
      "30..34", "35..39", "40..44", "45..49", "50..54",
      "55..59", "60..64", "65..69", "70..74", "75..79", "80..84", "85..110");

  private static final List<String> RACE_KEYS = Arrays.asList(
      "white", "black", "asian", "native", "other");

  private String previousDefaultState;

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrDemographicsLoader.resetCacheForTest();
    Config.set("br.profile", "");
    previousDefaultState = Generator.DEFAULT_STATE;
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
  }

  /**
   * Reset BR profile, loader cache, and static defaults after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.profile", "");
    BrDemographicsLoader.resetCacheForTest();
    Generator.DEFAULT_STATE = previousDefaultState;
  }

  @Test
  public void testBrProfileSampleMatchesReferenceWithinThreshold() throws Exception {
    Config.set("br.profile", "br");
    BrDemographicsData reference = BrDemographicsLoader.load();
    Generator generator = new Generator(new GeneratorOptions());

    Map<String, Integer> ageCounts = initCounts(AGE_GROUPS);
    Map<String, Integer> genderCounts = initCounts(Arrays.asList("M", "F"));
    Map<String, Integer> raceCounts = initCounts(RACE_KEYS);

    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      int age = (Integer) demo.get("target_age");
      ageCounts.merge(ageToGroup(age), 1, Integer::sum);
      genderCounts.merge((String) demo.get(Person.GENDER), 1, Integer::sum);
      raceCounts.merge((String) demo.get(Person.RACE), 1, Integer::sum);
    }

    assertProportionsWithinThreshold(ageCounts, reference.getAges(), "age group");
    Map<String, Double> expectedGender = new HashMap<>();
    expectedGender.put("M", reference.getGender().get("male"));
    expectedGender.put("F", reference.getGender().get("female"));
    assertProportionsWithinThreshold(genderCounts, expectedGender, "gender");
    assertProportionsWithinThreshold(raceCounts, reference.getRace(), "race");
  }

  @Test
  public void testBrSampleCloserToIbgeThanToUsDefault() throws Exception {
    Config.set("br.profile", "br");
    BrDemographicsData brReference = BrDemographicsLoader.load();
    Map<String, Double> usReference = buildPopulationWeightedUsReference(
        Generator.DEFAULT_STATE);

    Map<String, Integer> ageCounts = initCounts(AGE_GROUPS);
    Map<String, Integer> genderCounts = initCounts(Arrays.asList("M", "F"));

    Generator generator = new Generator(new GeneratorOptions());
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      ageCounts.merge(ageToGroup((Integer) demo.get("target_age")), 1, Integer::sum);
      genderCounts.merge((String) demo.get(Person.GENDER), 1, Integer::sum);
    }

    Map<String, Double> observedAge = toProportions(ageCounts);
    Map<String, Double> observedGender = toGenderProportions(genderCounts);

    double distanceToBr = l1Distance(observedAge, brReference.getAges())
        + l1GenderDistance(observedGender, brReference.getGender());
    double distanceToUs = l1Distance(observedAge, usReference)
        + l1GenderDistance(observedGender, extractUsGender(usReference));

    assertTrue(String.format(
        "BR sample should be closer to IBGE reference (distBr=%.3f) than US default (distUs=%.3f)",
        distanceToBr, distanceToUs),
        distanceToBr < distanceToUs);
  }

  @Test
  public void testInactiveProfilePreservesDeterministicUpstreamOutput() throws Exception {
    Config.set("br.profile", "");
    assertTrue(!BrProfile.isActive());

    List<String> baseline = sampleDemographicSignature(null);
    List<String> repeat = sampleDemographicSignature(null);

    assertEquals("Upstream demographics must be deterministic when br.profile is unset",
        baseline, repeat);
  }

  @Test
  public void testBrProfileChangesDistributionFromUpstream() throws Exception {
    List<String> upstream = sampleDemographicSignature(null);

    Config.set("br.profile", "br");
    BrDemographicsLoader.resetCacheForTest();
    List<String> brSample = sampleDemographicSignature("br");

    assertTrue("BR profile should produce different demographics than upstream default",
        !upstream.equals(brSample));
  }

  @Test
  public void testBrProfileSocioeconomicAndLanguageFromDataPack() throws Exception {
    Config.set("br.profile", "br");
    BrDemographicsData reference = BrDemographicsLoader.load();
    Generator generator = new Generator(new GeneratorOptions());

    Map<String, Integer> languageCounts = initCounts(
        Arrays.asList("portuguese", "spanish", "indigenous", "english", "other"));
    Map<String, Integer> educationCounts = initCounts(
        Arrays.asList("less_than_hs", "hs_degree", "some_college", "bs_degree"));
    Map<String, Integer> raceIbgeCounts = initCounts(
        Arrays.asList("branca", "preta", "parda", "amarela", "indigena"));
    int portugueseCount = 0;

    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      languageCounts.merge((String) demo.get(Person.FIRST_LANGUAGE), 1, Integer::sum);
      educationCounts.merge((String) demo.get(Person.EDUCATION), 1, Integer::sum);
      raceIbgeCounts.merge((String) demo.get(Person.RACE_IBGE), 1, Integer::sum);
      assertEquals("ETHNICITY must match IBGE raça/cor when br.profile=br",
          demo.get(Person.RACE_IBGE), demo.get(Person.ETHNICITY));
      if ("portuguese".equals(demo.get(Person.FIRST_LANGUAGE))) {
        portugueseCount++;
      }
    }

    assertTrue("Portuguese should dominate first language in BR profile",
        portugueseCount > SAMPLE_SIZE * 0.85);
    assertProportionsWithinThreshold(languageCounts, reference.getLanguage(), "language");
    assertProportionsWithinThreshold(educationCounts, reference.getEducation(), "education");
    assertProportionsWithinThreshold(raceIbgeCounts, reference.getRaceIbge(), "race_ibge");
  }

  @Test
  public void testBrProfileEthnicityIsNotUsOmbAxis() throws Exception {
    Config.set("br.profile", "br");
    Generator generator = new Generator(new GeneratorOptions());
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);

    for (int i = 0; i < 100; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      String ethnicity = (String) demo.get(Person.ETHNICITY);
      assertTrue("BR ethnicity must be IBGE category, not US OMB value: " + ethnicity,
          Arrays.asList("branca", "preta", "parda", "amarela", "indigena").contains(ethnicity));
    }
  }

  @Test
  public void testBrProfileIncomeDistributionDiffersFromUsCity() throws Exception {
    Config.set("br.profile", "");
    Generator upstreamGenerator = new Generator(new GeneratorOptions());
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    long upstreamLowIncome = 0;
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = upstreamGenerator.randomDemographics(random);
      if ((Integer) demo.get(Person.INCOME) < 25000) {
        upstreamLowIncome++;
      }
    }

    Config.set("br.profile", "br");
    BrDemographicsLoader.resetCacheForTest();
    Generator brGenerator = new Generator(new GeneratorOptions());
    random = new DefaultRandomNumberGenerator(FIXED_SEED);
    long brLowIncome = 0;
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = brGenerator.randomDemographics(random);
      if ((Integer) demo.get(Person.INCOME) < 25000) {
        brLowIncome++;
      }
    }

    assertTrue("BR profile should skew income lower than default US city demographics",
        brLowIncome > upstreamLowIncome);
  }

  private List<String> sampleDemographicSignature(String profileValue) throws Exception {
    if (profileValue == null) {
      Config.set("br.profile", "");
    } else {
      Config.set("br.profile", profileValue);
    }
    BrDemographicsLoader.resetCacheForTest();
    Generator generator = new Generator(new GeneratorOptions());
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    StringBuilder signature = new StringBuilder();
    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      signature.append(demo.get("target_age")).append('|');
      signature.append(demo.get(Person.GENDER)).append('|');
      signature.append(demo.get(Person.RACE)).append(';');
    }
    return Arrays.asList(signature.toString());
  }

  private static Map<String, Double> buildPopulationWeightedUsReference(String state)
      throws IOException {
    Table<String, String, Demographics> allDemographics = Demographics.load(state);
    Map<String, Demographics> cities = allDemographics.row(state);
    long totalPopulation = cities.values().stream().mapToLong(d -> d.population).sum();

    Map<String, Double> ages = new HashMap<>();
    Map<String, Double> gender = new HashMap<>();

    for (Demographics city : cities.values()) {
      double weight = city.population / (double) totalPopulation;
      for (Map.Entry<String, Double> entry : city.ages.entrySet()) {
        ages.merge(entry.getKey(), entry.getValue() * weight, Double::sum);
      }
      for (Map.Entry<String, Double> entry : city.gender.entrySet()) {
        gender.merge(entry.getKey(), entry.getValue() * weight, Double::sum);
      }
    }

    Map<String, Double> combined = new HashMap<>(ages);
    combined.put("male", gender.getOrDefault("male", 0.0));
    combined.put("female", gender.getOrDefault("female", 0.0));
    return combined;
  }

  private static Map<String, Double> extractUsGender(Map<String, Double> usReference) {
    Map<String, Double> gender = new HashMap<>();
    gender.put("male", usReference.getOrDefault("male", 0.0));
    gender.put("female", usReference.getOrDefault("female", 0.0));
    return gender;
  }

  private static Map<String, Double> toProportions(Map<String, Integer> counts) {
    Map<String, Double> proportions = new HashMap<>();
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      proportions.put(entry.getKey(), entry.getValue() / (double) SAMPLE_SIZE);
    }
    return proportions;
  }

  private static Map<String, Double> toGenderProportions(Map<String, Integer> counts) {
    Map<String, Double> proportions = new HashMap<>();
    proportions.put("male", counts.getOrDefault("M", 0) / (double) SAMPLE_SIZE);
    proportions.put("female", counts.getOrDefault("F", 0) / (double) SAMPLE_SIZE);
    return proportions;
  }

  private static double l1Distance(Map<String, Double> observed, Map<String, Double> reference) {
    double distance = 0.0;
    for (Map.Entry<String, Double> entry : reference.entrySet()) {
      if ("male".equals(entry.getKey()) || "female".equals(entry.getKey())) {
        continue;
      }
      double obs = observed.getOrDefault(entry.getKey(), 0.0);
      distance += Math.abs(obs - entry.getValue());
    }
    return distance;
  }

  private static double l1GenderDistance(Map<String, Double> observedGender,
      Map<String, Double> referenceGender) {
    double distance = 0.0;
    distance += Math.abs(observedGender.getOrDefault("male", 0.0)
        - referenceGender.getOrDefault("male", 0.0));
    distance += Math.abs(observedGender.getOrDefault("female", 0.0)
        - referenceGender.getOrDefault("female", 0.0));
    return distance;
  }

  private static Map<String, Integer> initCounts(List<String> keys) {
    Map<String, Integer> counts = new HashMap<>();
    for (String key : keys) {
      counts.put(key, 0);
    }
    return counts;
  }

  private static void assertProportionsWithinThreshold(Map<String, Integer> observedCounts,
      Map<String, Double> referenceFractions, String label) {
    for (Map.Entry<String, Double> entry : referenceFractions.entrySet()) {
      String key = entry.getKey();
      double expected = entry.getValue();
      double observed = observedCounts.getOrDefault(key, 0) / (double) SAMPLE_SIZE;
      double delta = Math.abs(observed - expected);
      assertTrue(String.format(
          "%s '%s': observed=%.3f expected=%.3f delta=%.3f exceeds threshold %.3f",
          label, key, observed, expected, delta, PROPORTION_THRESHOLD),
          delta <= PROPORTION_THRESHOLD);
    }
  }

  private static String ageToGroup(int age) {
    if (age <= 4) {
      return "0..4";
    }
    if (age <= 9) {
      return "5..9";
    }
    if (age <= 14) {
      return "10..14";
    }
    if (age <= 19) {
      return "15..19";
    }
    if (age <= 24) {
      return "20..24";
    }
    if (age <= 29) {
      return "25..29";
    }
    if (age <= 34) {
      return "30..34";
    }
    if (age <= 39) {
      return "35..39";
    }
    if (age <= 44) {
      return "40..44";
    }
    if (age <= 49) {
      return "45..49";
    }
    if (age <= 54) {
      return "50..54";
    }
    if (age <= 59) {
      return "55..59";
    }
    if (age <= 64) {
      return "60..64";
    }
    if (age <= 69) {
      return "65..69";
    }
    if (age <= 74) {
      return "70..74";
    }
    if (age <= 79) {
      return "75..79";
    }
    if (age <= 84) {
      return "80..84";
    }
    return "85..110";
  }
}
