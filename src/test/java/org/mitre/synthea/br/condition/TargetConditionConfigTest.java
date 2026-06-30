package org.mitre.synthea.br.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;

public class TargetConditionConfigTest {

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    Module.getModules(path -> false);
    Config.set("br.target_condition", "");
  }

  /**
   * Restore default condition catalog after each test.
   */
  @After
  public void tearDown() {
    SupportedConditions.resetToDefaultsForTest();
  }

  @Test
  public void testBreastCancerResolvesDiseaseModule() {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolve("breast_cancer");

    assertNotNull(resolved);
    assertEquals("breast_cancer", resolved.definition.conditionKey);
    assertNotNull(Module.getModuleByPath("breast_cancer"));
    assertNotNull(resolved.keepModulePath);
  }

  @Test
  public void testUnknownConditionThrowsClearMessage() {
    try {
      TargetConditionConfig.resolve("diabetes_tipo_x");
      fail("Expected UnknownTargetConditionException");
    } catch (UnknownTargetConditionException e) {
      assertTrue(e.getMessage().contains("diabetes_tipo_x"));
      assertTrue(e.getMessage().contains("breast_cancer"));
    }
  }

  @Test
  public void testMissingGateModuleThrowsStoryReference() {
    SupportedConditions.registerForTest(
        "test_missing_gate",
        "breast_cancer",
        "br/does_not_exist.json");

    try {
      TargetConditionConfig.resolve("test_missing_gate");
      fail("Expected GateModuleNotAvailableException");
    } catch (GateModuleNotAvailableException e) {
      assertTrue(e.getMessage().contains("test_missing_gate"));
      assertTrue(e.getMessage().contains("Story 2.2"));
    }
  }

  @Test
  public void testWithoutTargetConditionGeneratorOptionsUnchanged() throws Exception {
    Config.set("br.target_condition", "");

    Generator.GeneratorOptions options = new Generator.GeneratorOptions();
    options.population = 1;
    options.seed = 42L;
    options.referenceTime = 1_600_000_000_000L;
    options.endTime = 1_600_000_000_000L;

    assertNull(options.keepPatientsModulePath);

    Generator generator = new Generator(options);
    assertNull(generator.options.keepPatientsModulePath);
  }

  @Test
  public void testRetryModeSetsKeepModulePath() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");

    Generator.GeneratorOptions options = new Generator.GeneratorOptions();
    options.population = 1;
    options.seed = 42L;
    options.referenceTime = 1_600_000_000_000L;
    options.endTime = 1_600_000_000_000L;

    Generator generator = new Generator(options);
    assertNotNull(generator.options.keepPatientsModulePath);
    assertFalse(generator.options.keepPatientsModulePath.toString().isEmpty());
  }
}
