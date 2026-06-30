package org.mitre.synthea.br.demographics;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.geography.Demographics;

/**
 * Loads Brazilian national demographic distributions from {@code br/demographics/}.
 */
public final class BrDemographicsLoader {

  private static final String DATA_FILE = "br/demographics/distribuicao_nacional.csv";

  private static final Set<String> REQUIRED_RACE_IBGE_CATEGORIES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList("branca", "preta", "parda", "amarela", "indigena")));

  private static volatile BrDemographicsData cached;

  private BrDemographicsLoader() {
  }

  /**
   * Immutable national demographic reference loaded from the data pack.
   */
  public static final class BrDemographicsData {
    private final Map<String, Double> ages;
    private final Map<String, Double> gender;
    private final Map<String, Double> race;
    private final Map<String, Double> raceIbge;

    BrDemographicsData(Map<String, Double> ages, Map<String, Double> gender,
        Map<String, Double> race, Map<String, Double> raceIbge) {
      this.ages = Collections.unmodifiableMap(ages);
      this.gender = Collections.unmodifiableMap(gender);
      this.race = Collections.unmodifiableMap(race);
      this.raceIbge = Collections.unmodifiableMap(raceIbge);
    }

    /**
     * Age range distribution (keys like {@code 0..4}).
     *
     * @return unmodifiable map of age range → fraction
     */
    public Map<String, Double> getAges() {
      return ages;
    }

    /**
     * Gender distribution ({@code male}, {@code female}).
     *
     * @return unmodifiable map of gender → fraction
     */
    public Map<String, Double> getGender() {
      return gender;
    }

    /**
     * Internal race distribution after IBGE mapping.
     *
     * @return unmodifiable map of internal race key → fraction
     */
    public Map<String, Double> getRace() {
      return race;
    }

    /**
     * Raw IBGE raça/cor fractions from the data pack.
     *
     * @return unmodifiable map of IBGE category → fraction
     */
    public Map<String, Double> getRaceIbge() {
      return raceIbge;
    }
  }

  /**
   * Load (or return cached) national BR demographic data.
   *
   * @return loaded data pack
   * @throws IOException if the CSV cannot be read or parsed
   */
  public static BrDemographicsData load() throws IOException {
    BrDemographicsData local = cached;
    if (local != null) {
      return local;
    }
    synchronized (BrDemographicsLoader.class) {
      if (cached == null) {
        cached = parseFromResource();
      }
      return cached;
    }
  }

  /**
   * Clear cached data (test use only).
   */
  public static void resetCacheForTest() {
    cached = null;
  }

  /**
   * Build a non-shared {@link Demographics} picker for age/gender/race using BR national data,
   * while preserving location-specific fields (city, income, education) from {@code location}.
   *
   * <p>Story 3.1 integration point: {@code Generator.pickDemographics} uses this when
   * {@code BrProfile.isActive()} without mutating shared {@link Demographics} instances in
   * {@code Location}. Story 3.2 (geografia) reuses the same {@code BrProfile.isActive()} branch
   * pattern for geography overrides.
   *
   * <p>Etnia, idioma e variáveis socioeconômicas permanecem da cidade US até Story 3.x (decisão
   * de review 2026-06-30).
   *
   * @param location demographics for the selected US city (location context only)
   * @return new demographics instance for picking age/gender/race
   * @throws IOException if the BR data pack cannot be loaded
   */
  public static Demographics createPickerForLocation(Demographics location) throws IOException {
    BrDemographicsData data = load();
    Demographics picker = new Demographics();
    picker.id = location.id;
    picker.city = location.city;
    picker.state = location.state;
    picker.county = location.county;
    picker.population = location.population;
    picker.ages = new HashMap<>(data.getAges());
    picker.gender = new HashMap<>(data.getGender());
    picker.race = new HashMap<>(data.getRace());
    picker.income = location.income;
    picker.education = location.education;
    picker.ethnicity = location.ethnicity;
    return picker;
  }

  private static BrDemographicsData parseFromResource() throws IOException {
    String raw = Utilities.readResource(DATA_FILE, true, false);
    String csv = stripCommentLines(raw);
    List<? extends Map<String, String>> rows;
    try {
      rows = SimpleCSV.parse(csv);
    } catch (RuntimeException e) {
      throw new IOException("Failed to parse BR demographics CSV", e);
    }

    Map<String, Double> ages = new HashMap<>();
    Map<String, Double> gender = new HashMap<>();
    Map<String, Double> raceIbge = new HashMap<>();

    for (Map<String, String> row : rows) {
      String section = row.get("section");
      String category = row.get("category");
      String fractionText = row.get("fraction");
      if (section == null || section.trim().isEmpty()) {
        throw new IOException("Missing section column in BR demographics CSV row: " + row);
      }
      if (category == null || category.trim().isEmpty()) {
        throw new IOException("Missing category column in BR demographics CSV row: " + row);
      }
      double fraction = parseFraction(fractionText, section, category);
      switch (section) {
        case "age":
          ages.put(category, fraction);
          break;
        case "gender":
          gender.put(category, fraction);
          break;
        case "race_ibge":
          raceIbge.put(category, fraction);
          break;
        default:
          throw new IOException("Unknown section in BR demographics CSV: " + section);
      }
    }

    validateRequiredRaceIbgeCategories(raceIbge);
    normalizeToUnitSum(ages, "age");
    normalizeToUnitSum(gender, "gender");
    normalizeToUnitSum(raceIbge, "race_ibge");

    Map<String, Double> race = BrRaceMapper.toInternalRaceDistribution(raceIbge);
    normalizeToUnitSum(race, "internal race");

    return new BrDemographicsData(ages, gender, race, raceIbge);
  }

  private static void validateRequiredRaceIbgeCategories(Map<String, Double> raceIbge)
      throws IOException {
    for (String required : REQUIRED_RACE_IBGE_CATEGORIES) {
      if (!raceIbge.containsKey(required)) {
        throw new IOException("Missing required IBGE race category in data pack: " + required);
      }
    }
  }

  private static double parseFraction(String fractionText, String section, String category)
      throws IOException {
    if (fractionText == null || fractionText.trim().isEmpty()) {
      throw new IOException("Missing fraction for " + section + "/" + category
          + " in BR demographics CSV");
    }
    try {
      return Double.parseDouble(fractionText.trim());
    } catch (NumberFormatException e) {
      throw new IOException("Invalid fraction '" + fractionText + "' for " + section + "/"
          + category + " in BR demographics CSV", e);
    }
  }

  private static String stripCommentLines(String raw) {
    StringBuilder builder = new StringBuilder();
    for (String line : raw.split("\\R")) {
      if (!line.trim().startsWith("#")) {
        builder.append(line).append('\n');
      }
    }
    return builder.toString();
  }

  private static void normalizeToUnitSum(Map<String, Double> map, String label)
      throws IOException {
    double sum = map.values().stream().mapToDouble(Double::doubleValue).sum();
    if (sum <= 0.0) {
      throw new IOException("Demographic fractions for " + label
          + " must sum to a positive value");
    }
    if (Math.abs(sum - 1.0) > 0.001) {
      map.replaceAll((key, value) -> value / sum);
    }
  }
}
