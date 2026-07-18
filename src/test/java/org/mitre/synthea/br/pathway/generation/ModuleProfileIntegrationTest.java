package org.mitre.synthea.br.pathway.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.condition.GateEvaluator;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;

/**
 * Integration tests for {@link ModuleProfileConfig} (Story 9.5).
 */
public class ModuleProfileIntegrationTest {

  private static final long FIXED_REFERENCE_TIME = 1_600_000_000_000L;
  private static final long FIXED_SEED = 91005L;

  /**
   * Load test configuration before each test.
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
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");
    Config.set("generate.max_attempts_to_keep_patient", "1000");
  }

  /**
   * Reset generation profile configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("br.generation.module_profile");
    Config.remove("generate.max_attempts_to_keep_patient");
  }

  @Test
  public void pathwayMinimal_producesFewerDistinctConditionsThanFull() throws Exception {
    int fullConditions = averageDistinctConditions("full", FIXED_SEED);
    int minimalConditions = averageDistinctConditions("pathway_minimal", FIXED_SEED);
    assertTrue("pathway_minimal should reduce distinct conditions vs full profile",
        minimalConditions < fullConditions);
  }

  @Test
  public void pathwayMinimal_preservesTargetConditionGate() throws Exception {
    Config.set("br.generation.module_profile", "pathway_minimal");
    Generator generator = buildGenerator(8, FIXED_SEED + 1L);
    for (int i = 0; i < 8; i++) {
      Person person = generator.generatePerson(i, FIXED_SEED + 1L + i);
      assertTrue(GateEvaluator.hasBreastCancer(person));
    }
  }

  @Test
  public void pathwayMinimal_isReproducibleWithSameSeed() throws Exception {
    Config.set("br.generation.module_profile", "pathway_minimal");
    int run1 = averageDistinctConditions("pathway_minimal", 424242L);
    Provider.clear();
    PayerManager.clear();
    int run2 = averageDistinctConditions("pathway_minimal", 424242L);
    assertEquals(run1, run2);
  }

  private int averageDistinctConditions(String profile, long seed) throws Exception {
    Config.set("br.generation.module_profile", profile);
    Generator generator = buildGenerator(5, seed);
    int total = 0;
    for (int i = 0; i < 5; i++) {
      Person person = generator.generatePerson(i, seed + i);
      total += person.record.present.size();
    }
    return total / 5;
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
}
