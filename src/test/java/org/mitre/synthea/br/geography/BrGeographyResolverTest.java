package org.mitre.synthea.br.geography;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.geography.Demographics;
import org.mitre.synthea.world.geography.Location;

/**
 * Tests for Brazilian geography resolver (Story 3.2 AC #1–#4, #7).
 */
public class BrGeographyResolverTest {

  private static final Pattern CEP_PATTERN = Pattern.compile("\\d{5}-\\d{3}");
  private static final int SAMPLE_SIZE = 200;
  private static final long FIXED_SEED = 20260701L;

  private static Set<String> usStates;
  private static Set<String> usCities;
  private static Set<String> usZips;
  private static Set<String> brMunicipioNames;
  private static Set<String> brUfNames;

  private String previousCountryCode;

  /**
   * Load test configuration and US reference values before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrGeographyResolver.resetCacheForTest();
    Config.set("br.profile", "");
    previousCountryCode = Config.get("generate.geography.country_code");
    loadUsReferenceValues();
  }

  /**
   * Reset caches after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.profile", "");
    BrGeographyResolver.resetCacheForTest();
    if (previousCountryCode != null) {
      Config.set("generate.geography.country_code", previousCountryCode);
    } else {
      Config.remove("generate.geography.country_code");
    }
  }

  @Test
  public void testCepFormatMatchesBrazilianPattern() throws Exception {
    BrGeographyResolver resolver = BrGeographyResolver.load();
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);

    for (int i = 0; i < SAMPLE_SIZE; i++) {
      BrGeographyResolver.MunicipioSelection selection = resolver.selectMunicipio(random);
      String cep = BrGeographyResolver.generateCep(selection.getMunicipio(), random);
      assertTrue("CEP must match NNNNN-NNN: " + cep, CEP_PATTERN.matcher(cep).matches());
    }
  }

  @Test
  public void testCoordinatesWithinUfBoundingBoxForAllPilotUfs() throws Exception {
    BrGeographyResolver resolver = BrGeographyResolver.load();
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);

    Map<String, BrGeographyResolver.UfData> ufsBySigla = resolver.getUfsBySigla();
    Set<String> pilotUfSiglas = new HashSet<>();
    for (BrGeographyResolver.MunicipioData municipio : resolver.getMunicipios()) {
      pilotUfSiglas.add(municipio.ufSigla);
    }

    for (String ufSigla : pilotUfSiglas) {
      BrGeographyResolver.UfData uf = ufsBySigla.get(ufSigla);
      BrGeographyResolver.MunicipioData municipio = findMunicipioForUf(resolver, uf.sigla);
      assertNotNull("Pilot data must include at least one municipality for UF " + uf.sigla,
          municipio);
      for (int i = 0; i < 50; i++) {
        Point2D.Double coordinate =
            BrGeographyResolver.assignCoordinate(municipio, uf, random);
        assertTrue("Coordinate must be within UF " + uf.sigla + " bounding box: " + coordinate,
            BrGeographyResolver.isWithinUfBoundingBox(coordinate, uf));
      }
    }
  }

  @Test
  public void testBrProfileSampleDoesNotMatchUsGeography() throws Exception {
    Config.set("br.profile", "br");
    BrGeographyResolver resolver = BrGeographyResolver.load();
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);

    for (int i = 0; i < SAMPLE_SIZE; i++) {
      BrGeographyResolver.MunicipioSelection selection = resolver.selectMunicipio(random);
      String state = selection.getUfNome();
      String city = selection.getNome();

      assertTrue("BR state must be a known UF name: " + state, brUfNames.contains(state));
      assertTrue("BR city must be a pilot municipality: " + city, brMunicipioNames.contains(city));
      assertFalse("BR state must not match US state name: " + state, usStates.contains(state));
      assertFalse("BR city must not match US city name: " + city, usCities.contains(city));

      Person person = new Person(random.randLong());
      person.attributes.put(Person.CITY, city);
      person.attributes.put(Person.STATE, state);
      resolver.completePersonGeography(person);

      String zip = (String) person.attributes.get(Person.ZIP);
      assertTrue("CEP must match BR format", CEP_PATTERN.matcher(zip).matches());
      assertFalse("CEP must not match a US ZIP code: " + zip, usZips.contains(zip));
      Point2D.Double coordinate = (Point2D.Double) person.attributes.get(Person.COORDINATE);
      assertNotNull(coordinate);
      assertTrue("Coordinate must be within UF bounding box",
          BrGeographyResolver.isWithinUfBoundingBox(coordinate, selection.getUf()));
    }
  }

  @Test
  public void testEffectiveCountryCodeIsBrWhenProfileActive() {
    Config.set("br.profile", "br");
    Config.set("generate.geography.country_code", "US");
    assertEquals("BR", BrProfile.getEffectiveCountryCode());
  }

  @Test
  public void testInactiveProfilePreservesUpstreamGeography() throws Exception {
    Config.set("br.profile", "");
    assertFalse(BrProfile.isActive());

    String testState = Config.get("test_state.default", "Massachusetts");
    Location location = new Location(testState, null);

    List<String> baseline = sampleUpstreamGeography(location,
        new DefaultRandomNumberGenerator(FIXED_SEED));
    List<String> repeat = sampleUpstreamGeography(location,
        new DefaultRandomNumberGenerator(FIXED_SEED));

    assertEquals("Upstream geography must be deterministic when br.profile is unset",
        baseline, repeat);
    assertTrue("Upstream state should be a known US state",
        usStates.contains(baseline.get(0)));
  }

  @Test
  public void testCompletePersonGeographyPreservesBirthplaceOnRelocation() throws Exception {
    BrGeographyResolver resolver = BrGeographyResolver.load();
    RandomNumberGenerator random = new DefaultRandomNumberGenerator(FIXED_SEED);
    BrGeographyResolver.MunicipioSelection selection = resolver.selectMunicipio(random);

    Person person = new Person(random.randLong());
    person.attributes.put(Person.CITY, selection.getNome());
    person.attributes.put(Person.STATE, selection.getUfNome());
    resolver.completePersonGeography(person);

    String birthCity = (String) person.attributes.get(Person.BIRTH_CITY);
    String birthState = (String) person.attributes.get(Person.BIRTH_STATE);

    person.attributes.put(Person.CITY, "Outra Cidade");
    person.attributes.put(Person.STATE, selection.getUfNome());
    resolver.completePersonGeography(person, false);

    assertEquals("Birth city must be preserved on relocation",
        birthCity, person.attributes.get(Person.BIRTH_CITY));
    assertEquals("Birth state must be preserved on relocation",
        birthState, person.attributes.get(Person.BIRTH_STATE));
    assertNotNull("ZIP must be updated on relocation", person.attributes.get(Person.ZIP));
  }

  @Test
  public void testAllUfsPresentInDataPack() throws Exception {
    BrGeographyResolver resolver = BrGeographyResolver.load();
    assertEquals(27, resolver.getUfsBySigla().size());
  }

  private static BrGeographyResolver.MunicipioData findMunicipioForUf(
      BrGeographyResolver resolver, String ufSigla) {
    for (BrGeographyResolver.MunicipioData municipio : resolver.getMunicipios()) {
      if (ufSigla.equals(municipio.ufSigla)) {
        return municipio;
      }
    }
    return null;
  }

  private static List<String> sampleUpstreamGeography(Location location,
      RandomNumberGenerator random) {
    Demographics city = location.randomCity(random);
    return java.util.Arrays.asList(city.state, city.city);
  }

  private static void loadUsReferenceValues() throws Exception {
    if (usStates != null) {
      return;
    }
    usStates = new HashSet<>();
    usCities = new HashSet<>();
    usZips = new HashSet<>();
    brMunicipioNames = new HashSet<>();
    brUfNames = new HashSet<>();

    String csv = Utilities.readResource("geography/zipcodes.csv", true, true);
    List<? extends Map<String, String>> rows = SimpleCSV.parse(csv);
    for (Map<String, String> row : rows) {
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
    for (BrGeographyResolver.MunicipioData municipio : resolver.getMunicipios()) {
      brMunicipioNames.add(municipio.nome);
    }
    for (BrGeographyResolver.UfData uf : resolver.getUfsBySigla().values()) {
      brUfNames.add(uf.nome);
    }
  }
}
