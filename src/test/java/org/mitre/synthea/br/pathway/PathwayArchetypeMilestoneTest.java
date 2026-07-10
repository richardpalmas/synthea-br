package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.condition.GateEvaluator;
import org.mitre.synthea.br.pathway.PathwayExportFilter;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;

/**
 * Integration tests for Epic 10 pathway archetype milestone coverage.
 */
public class PathwayArchetypeMilestoneTest {

  private static final long REMISSION_SEED = 100100L;
  private static final long PROGRESSION_SEED = 100200L;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Provider.clear();
    PayerManager.clear();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");
    Config.set("br.pathway.focus", "true");
    Config.set("br.generation.module_profile", "pathway_minimal");
    Config.set("br.generation.simulation_window", "pre_onset_years:10");
    Config.set("generate.max_attempts_to_keep_patient", "5000");
    Config.set("generate.only_alive_patients", "false");
  }

  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("br.pathway.focus");
    Config.remove("br.pathway.archetype");
    Config.remove("br.generation.module_profile");
    Config.remove("br.generation.simulation_window");
    Config.remove("generate.max_attempts_to_keep_patient");
    Config.remove("generate.only_alive_patients");
  }

  @Test
  public void remissionArchetype_containsRequiredMilestones() throws Exception {
    Config.set("br.pathway.archetype", "remission");
    Person person = generateFemalePatient(REMISSION_SEED);
    assertTrue(GateEvaluator.hasBreastCancer(person));
    assertTrue(person.alive(Long.MAX_VALUE));

    Set<String> codes = collectFilteredCodes(person);
    assertMilestonePresent(codes, "SNOMED-CT", "185345009");
    assertMilestonePresent(codes, "SNOMED-CT", "122548005");
    assertMilestonePresent(codes, "SNOMED-CT", "169069000");
    assertMilestonePresent(codes, "SNOMED-CT", "418714002");
    assertMilestonePresent(codes, "SNOMED-CT", "77477000");
    assertMilestonePresent(codes, "SNOMED-CT", "392021009");
    assertMilestonePresent(codes, "SNOMED-CT", "367336001");
    assertMilestonePresent(codes, "SNOMED-CT", "1287742003");
    assertMilestonePresent(codes, "RxNorm", "199224");
    assertMilestonePresent(codes, "SNOMED-CT", "71651007");
  }

  @Test
  public void progressionArchetype_containsRequiredMilestonesAndDeath() throws Exception {
    Config.set("br.pathway.archetype", "progression");
    Person person = generateFemalePatient(PROGRESSION_SEED);
    assertTrue(GateEvaluator.hasBreastCancer(person));
    assertTrue(Boolean.TRUE.equals(person.attributes.get("breast_cancer_triple_negative")));
    assertFalse(person.alive(Long.MAX_VALUE));

    Set<String> codes = collectFilteredCodes(person);
    assertMilestonePresent(codes, "SNOMED-CT", "172138000");
    assertMilestonePresent(codes, "SNOMED-CT", "418891003");
    assertMilestonePresent(codes, "SNOMED-CT", "78899008");
    assertMilestonePresent(codes, "SNOMED-CT", "94222008");
    assertMilestonePresent(codes, "SNOMED-CT", "305336008");
  }

  private static Person generateFemalePatient(long seed) throws Exception {
    GeneratorOptions options = new GeneratorOptions();
    options.seed = seed;
    options.population = 1;
    options.gender = "F";
    options.ageSpecified = true;
    options.minAge = 50;
    options.maxAge = 65;
    options.overflow = false;
    ExporterRuntimeOptions exportOptions = new ExporterRuntimeOptions();
    Generator generator = new Generator(options, exportOptions);
    return generator.generatePerson(0, seed);
  }

  private static Set<String> collectFilteredCodes(Person person) {
    Person filtered = PathwayExportFilter.filterForExport(person);
    Set<String> codes = new HashSet<>();
    for (Encounter encounter : filtered.record.encounters) {
      addEntryCodes(encounter.conditions, codes);
      addEntryCodes(encounter.procedures, codes);
      addEntryCodes(encounter.medications, codes);
      addEntryCodes(encounter.reports, codes);
      addEntryCodes(encounter.observations, codes);
    }
  }

  private static void addEntryCodes(java.util.List<? extends Entry> entries, Set<String> codes) {
    if (entries == null) {
      return;
    }
    for (Entry entry : entries) {
      if (entry.codes == null) {
        continue;
      }
      for (Code code : entry.codes) {
        codes.add(code.system + "|" + code.code);
      }
    }
  }

  private static void assertMilestonePresent(Set<String> codes, String system, String code) {
    assertTrue("Marco ausente: " + system + "|" + code, codes.contains(system + "|" + code));
  }
}
