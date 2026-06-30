package org.mitre.synthea.br.geography;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.geography.Location;

/**
 * End-to-end geography tests via {@link Generator} (Story 3.2 AC #1, #3).
 */
public class BrGeographyIntegrationTest {

  private static final Pattern CEP_PATTERN = Pattern.compile("\\d{5}-\\d{3}");
  private static final int SAMPLE_SIZE = 100;
  private static final long FIXED_SEED = 20260702L;

  private static Set<String> usStates;
  private static Set<String> usCities;
  private static Set<String> usZips;
  private static Set<String> brUfNames;

  private String previousDefaultState;
  private String previousCountryCode;

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrGeographyResolver.resetCacheForTest();
    Config.set("br.profile", "");
    previousDefaultState = Generator.DEFAULT_STATE;
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    previousCountryCode = Config.get("generate.geography.country_code");
    loadUsReferenceValues();
  }

  /**
   * Reset configuration after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.profile", "");
    BrGeographyResolver.resetCacheForTest();
    Generator.DEFAULT_STATE = previousDefaultState;
    if (previousCountryCode != null) {
      Config.set("generate.geography.country_code", previousCountryCode);
    } else {
      Config.remove("generate.geography.country_code");
    }
  }

  @Test
  public void testGeneratorCreatePersonProducesBrazilianGeography() throws Exception {
    Config.set("br.profile", "br");
    BrGeographyResolver resolver = BrGeographyResolver.load();
    Generator generator = new Generator(new GeneratorOptions());
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);

    for (int i = 0; i < SAMPLE_SIZE; i++) {
      Map<String, Object> demo = generator.randomDemographics(random);
      Person person = generator.createPerson(random.randLong(), demo);

      String state = (String) person.attributes.get(Person.STATE);
      String city = (String) person.attributes.get(Person.CITY);
      String zip = (String) person.attributes.get(Person.ZIP);
      String county = (String) person.attributes.get(Person.COUNTY);
      Point2D.Double coordinate = (Point2D.Double) person.attributes.get(Person.COORDINATE);

      assertTrue("BR state must be a known UF name: " + state, brUfNames.contains(state));
      assertFalse("BR state must not match US state name: " + state, usStates.contains(state));
      assertFalse("BR city must not match US city name: " + city, usCities.contains(city));
      assertTrue("CEP must match NNNNN-NNN: " + zip, CEP_PATTERN.matcher(zip).matches());
      assertFalse("CEP must not match a US ZIP code: " + zip, usZips.contains(zip));
      assertFalse("CEP prefix must not match a US ZIP code: " + zip,
          usZips.contains(zip.substring(0, 5)));
      assertNotNull("County (UF sigla) must be set", county);
      assertTrue("County must be a 2-letter UF sigla: " + county, county.length() == 2);
      assertNotNull("Coordinate must be set", coordinate);

      BrGeographyResolver.UfData uf = resolver.getUfsBySigla().get(county);
      assertNotNull("UF sigla must exist in data pack: " + county, uf);
      assertTrue("Coordinate must be within UF bounding box",
          BrGeographyResolver.isWithinUfBoundingBox(coordinate, uf));
    }
  }

  @Test
  public void testBrProfileActiveDuringRandomDemographics() {
    Config.set("br.profile", "br");
    assertTrue(BrProfile.isActive());
  }

  private static void loadUsReferenceValues() throws Exception {
    if (usStates != null) {
      return;
    }
    usStates = new HashSet<>();
    usCities = new HashSet<>();
    usZips = new HashSet<>();
    brUfNames = new HashSet<>();

    String csv = Utilities.readResource("geography/zipcodes.csv", true, true);
    for (Map<String, String> row : SimpleCSV.parse(csv)) {
      String state = row.get("ST");
      String city = row.get("NAME");
      String zip = row.get("ZCTA5");
      if (state != null && !state.isEmpty()) {
        String fullName = Location.getStateName(state);
        if (fullName != null) {
          usStates.add(fullName);
        }
      }
      if (city != null && !city.isEmpty()) {
        usCities.add(city);
      }
      if (zip != null && !zip.isEmpty()) {
        usZips.add(zip);
      }
    }

    BrGeographyResolver resolver = BrGeographyResolver.load();
    for (BrGeographyResolver.UfData uf : resolver.getUfsBySigla().values()) {
      brUfNames.add(uf.nome);
    }
  }
}
