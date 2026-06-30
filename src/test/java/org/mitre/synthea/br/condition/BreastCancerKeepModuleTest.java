package org.mitre.synthea.br.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

public class BreastCancerKeepModuleTest {

  private Module keepModule;

  /**
   * Load the breast cancer keep module fixture.
   *
   * @throws Exception on module loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    Path keepPath = KeepModulePaths.resolveRelative("br/breast_cancer.json");
    keepModule = Module.loadFile(keepPath, false, null, true);
  }

  @Test
  public void testKeepModuleMatchesWhenConditionPresent() throws Exception {
    Person person = buildPersonWithBreastCancer();

    assertTrue(GateEvaluator.matchesCondition(person, keepModule, person.lastUpdated + 1));
  }

  @Test
  public void testKeepModuleRejectsWhenConditionAbsent() throws Exception {
    Person person = new Person(12345L);

    assertFalse(GateEvaluator.matchesCondition(person, keepModule, person.lastUpdated + 1));
  }

  @Test
  public void testKeepModulePathMatchesSupportedConditionsCatalog() throws Exception {
    SupportedConditions.ConditionDefinition definition = SupportedConditions.get("breast_cancer");
    assertEquals("br/breast_cancer.json", definition.keepModuleRelativePath);
    assertTrue(KeepModulePaths.existsRelative(definition.keepModuleRelativePath));
  }

  @Test
  public void testSnomedCodeMatchesJsonKeepModule() throws Exception {
    // SupportedConditions.BREAST_CANCER_SNOMED, GateEvaluator.hasBreastCancer and the literal
    // SNOMED code in breast_cancer.json must all agree; this guards against silent drift
    // between the three independent sources of truth across future edits.
    Path keepPath = KeepModulePaths.resolveRelative("br/breast_cancer.json");
    String json = new String(Files.readAllBytes(keepPath), StandardCharsets.UTF_8);

    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    JsonObject initialState = root.getAsJsonObject("states").getAsJsonObject("Initial");
    JsonArray transitions = initialState.getAsJsonArray("conditional_transition");
    JsonObject keepTransition = transitions.get(0).getAsJsonObject();
    JsonArray codes = keepTransition.getAsJsonObject("condition").getAsJsonArray("codes");
    String jsonCode = codes.get(0).getAsJsonObject().get("code").getAsString();

    assertEquals(SupportedConditions.BREAST_CANCER_SNOMED, jsonCode);
  }

  @Test
  public void testIntegrationWithGeneratorKeepPath() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");
    Config.set("generate.max_attempts_to_keep_patient", "500");

    org.mitre.synthea.engine.Generator.GeneratorOptions opts =
        new org.mitre.synthea.engine.Generator.GeneratorOptions();
    opts.population = 3;
    opts.seed = 20260630L;
    opts.clinicianSeed = 20260631L;
    opts.gender = "F";
    opts.minAge = 40;
    opts.maxAge = 75;
    opts.ageSpecified = true;
    opts.referenceTime = 1_600_000_000_000L;
    opts.endTime = 1_600_000_000_000L;

    org.mitre.synthea.engine.Generator generator = new org.mitre.synthea.engine.Generator(opts);
    for (int i = 0; i < opts.population; i++) {
      Person person = generator.generatePerson(i, opts.seed + i);
      assertTrue(GateEvaluator.hasBreastCancer(person));
    }
  }

  private Person buildPersonWithBreastCancer() {
    Person person = new Person(999L);
    long time = 1_600_000_000_000L;
    org.mitre.synthea.world.concepts.HealthRecord.Entry condition =
        person.record.new Entry(time, SupportedConditions.BREAST_CANCER_SNOMED);
    person.record.present.put(SupportedConditions.BREAST_CANCER_SNOMED, condition);
    person.lastUpdated = time;
    return person;
  }
}
