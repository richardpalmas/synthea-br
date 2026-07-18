package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.Utilities;

/**
 * Unit tests for {@link PathwayTimingLoader} (Story 9.8).
 */
public class PathwayTimingLoaderTest {

  private String previousTimingPriors;
  private String previousTrajectoryMode;
  private String previousTargetCondition;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    previousTimingPriors = Config.get(PathwayTimingLoader.PROPERTY_KEY);
    previousTrajectoryMode = Config.get("br.generation.trajectory_mode");
    previousTargetCondition = Config.get("br.target_condition");
  }

  @After
  public void tearDown() {
    restore(PathwayTimingLoader.PROPERTY_KEY, previousTimingPriors);
    restore("br.generation.trajectory_mode", previousTrajectoryMode);
    restore("br.target_condition", previousTargetCondition);
  }

  @Test
  public void loadDefaultPack_hasVersionAndCatalogTransitions() {
    PathwayTimingLoader.PathwayTimingPack pack =
        PathwayTimingLoader.loadFromClasspath(PathwayTimingLoader.DEFAULT_RESOURCE);
    assertEquals("1.0.0", pack.getPriorsVersion());
    assertEquals("breast_cancer", pack.condition);
    assertTrue(pack.transitions.containsKey("screening->diagnosis"));
    assertTrue(pack.transitions.containsKey("treatment->follow_up"));
    assertFalse(pack.getReferenceNotes().isEmpty());
  }

  @Test
  public void toGmfDistribution_triangularFromMinMedianMax() {
    PathwayTimingLoader.PathwayTimingPrior prior = new PathwayTimingLoader.PathwayTimingPrior();
    prior.min = 15.0;
    prior.median = 30.0;
    prior.max = 90.0;
    JsonObject distribution = PathwayTimingLoader.toGmfDistributionJson(prior);
    assertEquals("TRIANGULAR", distribution.get("kind").getAsString());
    assertEquals(15.0, distribution.getAsJsonObject("parameters").get("min").getAsDouble(), 0.001);
    assertEquals(30.0, distribution.getAsJsonObject("parameters").get("mode").getAsDouble(), 0.001);
    assertEquals(90.0, distribution.getAsJsonObject("parameters").get("max").getAsDouble(), 0.001);
  }

  @Test
  public void applyPriors_replacesDelayDistributionsDeterministically() throws Exception {
    String original = Utilities.readResource("modules/breast_cancer_trajectory_br.json");
    PathwayTimingLoader.PathwayTimingPack pack =
        PathwayTimingLoader.loadFromClasspath(PathwayTimingLoader.DEFAULT_RESOURCE);
    String once = PathwayTimingLoader.applyPriorsToModuleJson(original, pack);
    String twice = PathwayTimingLoader.applyPriorsToModuleJson(original, pack);
    assertEquals(once, twice);

    JsonObject states = JsonParser.parseString(once).getAsJsonObject().getAsJsonObject("states");
    JsonObject delay = states.getAsJsonObject("delay_screening_to_diagnosis");
    assertEquals("TRIANGULAR", delay.getAsJsonObject("distribution").get("kind").getAsString());
    assertEquals(15.0,
        delay.getAsJsonObject("distribution").getAsJsonObject("parameters").get("min").getAsDouble(),
        0.001);
  }

  @Test
  public void isEnabled_falseWhenOff() {
    Config.set(PathwayTimingLoader.PROPERTY_KEY, "off");
    assertFalse(PathwayTimingLoader.isEnabled());
  }

  @Test
  public void maybeApplyPriors_skipsWhenOff() throws Exception {
    String original = Utilities.readResource("modules/breast_cancer_trajectory_br.json");
    Config.set(PathwayTimingLoader.PROPERTY_KEY, "off");
    assertEquals(original,
        PathwayTimingLoader.maybeApplyPriors("modules/breast_cancer_trajectory_br.json", original));
  }

  @Test
  public void maybeApplyPriors_transformsWhenDefault() throws Exception {
    String original = Utilities.readResource("modules/breast_cancer_trajectory_br.json");
    Config.set(PathwayTimingLoader.PROPERTY_KEY, "default");
    String transformed = PathwayTimingLoader.maybeApplyPriors(
        "modules/breast_cancer_trajectory_br.json", original);
    assertFalse(original.equals(transformed));
    assertTrue(transformed.contains("TRIANGULAR"));
  }

  @Test
  public void assertNoPhi_rejectsCpfLikeReferenceNote() {
    PathwayTimingLoader.PathwayTimingPack pack =
        PathwayTimingLoader.loadFromClasspath(PathwayTimingLoader.DEFAULT_RESOURCE);
    pack.referenceNotes = java.util.List.of(
        "Paciente Joao Silva CPF 123.456.789-00 — nao deve existir");
    try {
      PathwayTimingLoader.assertNoPhi(pack);
      fail("Expected IllegalStateException for PHI-like note");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("PHI") || expected.getMessage().contains("NFR5"));
    }
  }

  @Test
  public void validatePack_rejectsUnknownTransitionKey() {
    PathwayTimingLoader.PathwayTimingPack pack =
        PathwayTimingLoader.loadFromClasspath(PathwayTimingLoader.DEFAULT_RESOURCE);
    PathwayTimingLoader.PathwayTimingPrior prior = new PathwayTimingLoader.PathwayTimingPrior();
    prior.min = 1.0;
    prior.median = 2.0;
    prior.max = 3.0;
    pack.transitions.put("foo->bar", prior);
    try {
      PathwayTimingLoader.validatePack(pack, "test");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("phase_id"));
    }
  }

  private static void restore(String key, String previous) {
    if (previous != null) {
      Config.set(key, previous);
    } else {
      Config.remove(key);
    }
  }
}
