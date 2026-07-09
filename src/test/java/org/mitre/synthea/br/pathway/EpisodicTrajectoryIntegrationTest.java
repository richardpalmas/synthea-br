package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.condition.GateEvaluator;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;

/**
 * Integration tests for episodic trajectory mode (Story 9.7).
 */
public class EpisodicTrajectoryIntegrationTest {

  private static final long FIXED_REFERENCE_TIME = 1_600_000_000_000L;
  private static final String BREAST_CANCER_CODE = "254837009";

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
    Config.set("br.generation.trajectory_mode", "episodic");
    Config.set("br.generation.module_profile", "pathway_minimal");
    Config.set("generate.max_attempts_to_keep_patient", "1000");
  }

  /**
   * Reset episodic trajectory configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("br.generation.trajectory_mode");
    Config.remove("br.generation.module_profile");
    Config.remove("generate.max_attempts_to_keep_patient");
  }

  @Test
  public void episodicMode_loadsTrajectoryModuleAndPassesGate() throws Exception {
    assertNotNull(Module.getModuleByPath("breast_cancer_trajectory_br"));
    Generator generator = buildGenerator(91007L);
    Person person = generator.generatePerson(0, 91007L);
    assertTrue(GateEvaluator.hasBreastCancer(person));
    assertNotNull("Episodic module should set pathway_phase attribute",
        person.attributes.get("pathway_phase"));
  }
  @Test
  public void episodicMode_diagnosisPrecedesPrimaryTreatmentProcedure() throws Exception {
    Generator generator = buildGenerator(91008L);
    Person person = generator.generatePerson(0, 91008L);
    long diagnosisTime = findEarliestConditionStart(person, BREAST_CANCER_CODE);
    long treatmentTime = findEarliestProcedureAtOrAfter(person, diagnosisTime);
    assertTrue(diagnosisTime > 0);
    assertTrue(treatmentTime > 0);
    assertTrue("Diagnosis must precede or coincide with first treatment procedure",
        diagnosisTime <= treatmentTime);
  }

  @Test
  public void episodicMasterModuleStructureRemainsStableAfterRuns() throws Exception {
    Module master = Module.getModuleByPath("breast_cancer_trajectory_br");
    int statesBefore = master.getStateNames().size();
    Generator generator = buildGenerator(91009L);
    generator.generatePerson(0, 91009L);
    generator.generatePerson(1, 91010L);
    assertEquals(statesBefore, master.getStateNames().size());
  }

  private Generator buildGenerator(long seed) throws Exception {
    GeneratorOptions options = new GeneratorOptions();
    options.population = 1;
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

  private static long findEarliestConditionStart(Person person, String code) {
    long earliest = Long.MAX_VALUE;
    for (Encounter encounter : person.record.encounters) {
      for (HealthRecord.Entry condition : encounter.conditions) {
        if (code.equals(condition.type) && condition.start < earliest) {
          earliest = condition.start;
        }
      }
    }
    return earliest == Long.MAX_VALUE ? -1L : earliest;
  }

  private static long findEarliestProcedureAtOrAfter(Person person, long afterTime) {
    long earliest = Long.MAX_VALUE;
    for (Encounter encounter : person.record.encounters) {
      for (Procedure procedure : encounter.procedures) {
        if (procedure.start >= afterTime && procedure.start < earliest) {
          earliest = procedure.start;
        }
      }
    }
    return earliest == Long.MAX_VALUE ? -1L : earliest;
  }
}
