package org.mitre.synthea.br.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;

public class GateModeIntegrationTest {

  private static final long FIXED_REFERENCE_TIME = 1_600_000_000_000L;

  /**
   * Reset configuration and provider state before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Provider.clear();
    PayerManager.clear();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("generate.max_attempts_to_keep_patient", "1000");
  }

  /**
   * Reset {@code br.target_condition} configuration so it cannot leak into other test
   * classes sharing the same JVM/Config singleton.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("generate.max_attempts_to_keep_patient");
  }

  @Test
  public void testRetryModeProducesFullyConformingCohort() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");

    Generator generator = buildGenerator(25, 4242L);
    for (int i = 0; i < 25; i++) {
      Person person = generator.generatePerson(i, 4242L + i);
      assertTrue(GateEvaluator.hasBreastCancer(person));
    }
  }

  @Test
  public void testRetryModeIsReproducible() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");

    int conformingRun1 = countConformingPatients(buildGenerator(20, 777L), 20, 777L);
    Provider.clear();
    PayerManager.clear();
    int conformingRun2 = countConformingPatients(buildGenerator(20, 777L), 20, 777L);

    assertEquals(20, conformingRun1);
    assertEquals(conformingRun1, conformingRun2);
  }

  @Test
  public void testExcludeModeProducesFullyConformingCohort() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "exclude");

    Generator generator = buildGenerator(30, 8888L);
    int conforming = 0;
    for (int i = 0; i < 30; i++) {
      Person person = generator.generatePerson(i, 8888L + i);
      if (GateEvaluator.hasBreastCancer(person)) {
        conforming++;
      }
    }

    assertEquals("All slots must produce conforming patients in exclude mode", 30, conforming);
    assertTrue("Expected some excluded attempts in exclude mode",
        generator.getTargetConditionExcludedCount() > 0);
    assertEquals(30, generator.getTargetConditionExportedCount());
  }

  @Test
  public void testExcludeModeIsReproducible() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "exclude");

    Generator generatorRun1 = buildGenerator(15, 9999L);
    runExcludePopulation(generatorRun1, 15, 9999L);
    int exportedRun1 = generatorRun1.getTargetConditionExportedCount();
    int excludedRun1 = generatorRun1.getTargetConditionExcludedCount();

    Provider.clear();
    PayerManager.clear();

    Generator generatorRun2 = buildGenerator(15, 9999L);
    runExcludePopulation(generatorRun2, 15, 9999L);
    int exportedRun2 = generatorRun2.getTargetConditionExportedCount();
    int excludedRun2 = generatorRun2.getTargetConditionExcludedCount();

    assertEquals(15, exportedRun1);
    assertEquals(exportedRun1, exportedRun2);
    assertEquals(excludedRun1, excludedRun2);
  }

  @Test
  public void testExcludeModeFailsWhenMaxAttemptsExceeded() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "exclude");
    Config.set("generate.max_attempts_to_keep_patient", "1");

    Generator generator = buildGenerator(1, 1111L);

    try {
      generator.generatePerson(0, 1111L);
      fail("Expected RuntimeException when max attempts exceeded in exclude mode");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("Failed to produce a matching patient after"));
    }
  }

  @Test
  public void testInvalidGateModeThrowsClearError() {
    Config.set("br.target_condition.gate_mode", "invalido");

    try {
      GateMode.fromConfig();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("invalido"));
      assertTrue(e.getMessage().contains("retry"));
      assertTrue(e.getMessage().contains("exclude"));
    }
  }

  private Generator buildGenerator(int population, long seed) throws Exception {
    GeneratorOptions options = new GeneratorOptions();
    options.population = population;
    options.seed = seed;
    options.clinicianSeed = seed + 1L;
    options.gender = "F";
    options.minAge = 40;
    options.maxAge = 75;
    options.ageSpecified = true;
    options.referenceTime = FIXED_REFERENCE_TIME;
    options.endTime = FIXED_REFERENCE_TIME;

    Generator generator = new Generator(options);
    generator.options.overflow = false;
    return generator;
  }

  private int countConformingPatients(Generator generator, int population, long seedBase)
      throws Exception {
    int conforming = 0;
    for (int i = 0; i < population; i++) {
      Person person = generator.generatePerson(i, seedBase + i);
      if (GateEvaluator.hasBreastCancer(person)) {
        conforming++;
      }
    }
    return conforming;
  }

  private void runExcludePopulation(Generator generator, int population, long seedBase)
      throws Exception {
    for (int i = 0; i < population; i++) {
      generator.generatePerson(i, seedBase + i);
    }
  }
}
