package org.mitre.synthea.br.providers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.geography.Location;

/**
 * Tests for BR provider loading (Story 3.4 AC #5, #7).
 */
public class BrProviderConfigTest {

  private static final DefaultRandomNumberGenerator PROVIDER_RANDOM =
      new DefaultRandomNumberGenerator(4242L);

  private static Set<String> usProviderIds;
  private static Set<String> usProviderNames;

  private Location testLocation;

  /**
   * Parse US default provider CSVs for reference IDs/names (AC #5).
   *
   * @throws Exception on IO errors
   */
  @BeforeClass
  public static void loadUsProviderReference() throws Exception {
    usProviderIds = new HashSet<>();
    usProviderNames = new HashSet<>();
    collectUsProviders("providers/hospitals.csv");
    collectUsProviders("providers/primary_care_facilities.csv");
  }

  private static void collectUsProviders(String resource) throws IOException {
    String csv = Utilities.readResource(resource, true, false);
    Iterator<? extends Map<String, String>> rows = SimpleCSV.parseLineByLine(csv);
    while (rows.hasNext()) {
      Map<String, String> row = rows.next();
      if (row.get("provider_num") != null) {
        usProviderIds.add(row.get("provider_num").replace("\"", ""));
      }
      if (row.get("name") != null) {
        usProviderNames.add(row.get("name").replace("\"", ""));
      }
    }
  }

  private static void assertNoUsProviderLeak(List<Provider> providers) {
    for (Provider provider : providers) {
      assertTrue("Provider id should be BR-prefixed: " + provider.cmsProviderNum,
          provider.cmsProviderNum != null && provider.cmsProviderNum.startsWith("BR-"));
      assertFalse("US provider id leaked: " + provider.cmsProviderNum,
          usProviderIds.contains(provider.cmsProviderNum));
      assertFalse("US provider name leaked: " + provider.name,
          usProviderNames.contains(provider.name));
    }
  }

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    Provider.clear();
    Config.set("br.profile", "");
    String testState = Config.get("test_state.default", "Massachusetts");
    testLocation = new Location(testState, null);
  }

  /**
   * Reset providers and BR profile after each test.
   */
  @After
  public void tearDown() {
    Provider.clear();
    Config.set("br.profile", "");
  }

  @Test
  public void testBrProfileLoadsOnlyBrazilianProviders() {
    Config.set("br.profile", "br");
    assertTrue(BrProfile.isActive());

    BrProviderLoader.load(testLocation, PROVIDER_RANDOM);

    List<Provider> providers = Provider.getProviderList();
    assertFalse("Expected BR providers to be loaded", providers.isEmpty());
    assertNoUsProviderLeak(providers);
  }

  @Test
  public void testInactiveProfileLoadsUsProviders() {
    Config.set("br.profile", "");
    assertFalse(BrProfile.isActive());

    Provider.loadProviders(testLocation, PROVIDER_RANDOM);

    List<Provider> providers = Provider.getProviderList();
    assertFalse(providers.isEmpty());
    boolean anyUs = false;
    for (Provider provider : providers) {
      if (usProviderIds.contains(provider.cmsProviderNum)) {
        anyUs = true;
        break;
      }
    }
    assertTrue("Expected at least one US provider when br.profile is inactive", anyUs);
  }

  @Test
  public void testGeneratorWithBrProfileUsesBrazilianProviders() throws Exception {
    Config.set("br.profile", "br");
    GeneratorOptions options = new GeneratorOptions();
    options.population = 1;
    options.seed = 999L;
    new Generator(options);

    List<Provider> providers = Provider.getProviderList();
    assertFalse(providers.isEmpty());
    assertNoUsProviderLeak(providers);
  }
}
