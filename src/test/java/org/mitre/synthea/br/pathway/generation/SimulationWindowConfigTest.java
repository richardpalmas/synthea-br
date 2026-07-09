package org.mitre.synthea.br.pathway.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link SimulationWindowConfig} (Story 9.6).
 */
public class SimulationWindowConfigTest {

  private String previousWindow;
  private String previousTargetCondition;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    previousWindow = Config.get("br.generation.simulation_window");
    previousTargetCondition = Config.get("br.target_condition");
  }

  @After
  public void tearDown() {
    restoreConfig("br.generation.simulation_window", previousWindow);
    restoreConfig("br.target_condition", previousTargetCondition);
  }

  @Test
  public void isActive_falseForFullLifespanDefault() {
    Config.remove("br.generation.simulation_window");
    assertFalse(SimulationWindowConfig.isActive());
    assertEquals(SimulationWindowConfig.FULL_LIFESPAN, SimulationWindowConfig.getEffectiveValue());
  }

  @Test
  public void simulationStartTime_offsetsFromBirthdate() {
    Config.set("br.generation.simulation_window", "pre_onset_years:10");
    long birthdate = 1_000_000_000_000L;
    long start = SimulationWindowConfig.simulationStartTime(birthdate, 55);
    long expectedOffset = 45L * 365L * 86_400_000L;
    assertEquals(birthdate + expectedOffset, start);
  }

  @Test
  public void validateForGeneration_rejectsOutOfRangeForBreastCancer() {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.generation.simulation_window", "pre_onset_years:3");
    GeneratorOptions options = ageOptions(40, 75);
    try {
      SimulationWindowConfig.validateForGeneration(options);
      fail("Expected IllegalArgumentException for N below pilot minimum");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("5"));
    }
  }

  @Test
  public void validateForGeneration_rejectsWhenNExceedsMinAge() {
    Config.set("br.generation.simulation_window", "pre_onset_years:40");
    GeneratorOptions options = ageOptions(40, 75);
    try {
      SimulationWindowConfig.validateForGeneration(options);
      fail("Expected IllegalArgumentException when N >= minAge");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("idade alvo"));
    }
  }

  @Test
  public void validateForGeneration_rejectsInvalidFormat() {
    Config.set("br.generation.simulation_window", "last_10_years");
    GeneratorOptions options = ageOptions(40, 75);
    try {
      SimulationWindowConfig.validateForGeneration(options);
      fail("Expected IllegalArgumentException for invalid format");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("pre_onset_years"));
    }
  }

  private static GeneratorOptions ageOptions(int minAge, int maxAge) {
    GeneratorOptions options = new GeneratorOptions();
    options.minAge = minAge;
    options.maxAge = maxAge;
    options.ageSpecified = true;
    return options;
  }

  private static void restoreConfig(String key, String previousValue) {
    if (previousValue != null) {
      Config.set(key, previousValue);
    } else {
      Config.remove(key);
    }
  }
}
