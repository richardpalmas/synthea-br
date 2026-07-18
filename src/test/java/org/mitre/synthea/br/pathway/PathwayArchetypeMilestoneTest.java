package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.condition.GateEvaluator;
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

  private static final long REMISSION_SEED = 51001L;
  private static final long PROGRESSION_SEED = 51002L;

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
    Person person = findRemissionPersonWithMilestones(REMISSION_SEED);
    assertTrue(GateEvaluator.hasBreastCancer(person));
    assertTrue(person.alive(Long.MAX_VALUE));

    Set<String> codes = collectFilteredCodes(person);
    assertMilestonePresent(codes, "SNOMED-CT", "122548005");
    assertMilestonePresent(codes, "SNOMED-CT", "254837009");
    assertMilestonePresent(codes, "SNOMED-CT", "169069000");
    assertMilestonePresent(codes, "SNOMED-CT", "418714002");
    assertMilestonePresent(codes, "SNOMED-CT", "77477000");
    assertMilestonePresent(codes, "SNOMED-CT", "392021009");
    assertTrue("Quimioterapia ausente",
        codes.contains("SNOMED-CT|367336001") || codes.contains("RxNorm|583214")
            || codes.contains("RxNorm|1790099"));
    assertTrue("Radioterapia ausente",
        codes.contains("SNOMED-CT|1287742003") || codes.contains("SNOMED-CT|33195004"));
    List<Long> radiationTimes = collectProcedureTimes(person, "33195004");
    assertEquals("Curso convencional deve conter 25 frações", 25, radiationTimes.size());
    assertNoMoreThanFiveConsecutiveDailyFractions(radiationTimes);
    assertMilestonePresent(codes, "RxNorm", "199224");
    assertMilestonePresent(codes, "SNOMED-CT", "71651007");
  }

  @Test
  public void progressionArchetype_containsRequiredMilestonesAndDeath() throws Exception {
    Config.set("br.pathway.archetype", "progression");
    Person person = findPersonMatchingArchetype(PROGRESSION_SEED, false);
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

  private Person findRemissionPersonWithMilestones(long startSeed) throws Exception {
    for (long offset = 0; offset < 250; offset++) {
      Provider.clear();
      PayerManager.clear();
      long seed = startSeed + offset;
      Person person = generateFemalePatient(seed);
      if (!GateEvaluator.hasBreastCancer(person) || !person.alive(Long.MAX_VALUE)) {
        continue;
      }
      Set<String> codes = collectFilteredCodes(person);
      boolean hasChemo = codes.contains("SNOMED-CT|367336001")
          || codes.contains("RxNorm|583214") || codes.contains("RxNorm|1790099");
      boolean hasRadio = codes.contains("SNOMED-CT|1287742003")
          || codes.contains("SNOMED-CT|33195004");
      if (codes.contains("SNOMED-CT|392021009") && hasChemo && hasRadio
          && codes.contains("RxNorm|199224") && codes.contains("SNOMED-CT|71651007")) {
        return person;
      }
    }
    throw new AssertionError("Nenhuma seed remission com marcos completos a partir de " + startSeed);
  }

  private Person findPersonMatchingArchetype(long startSeed, boolean expectAlive) throws Exception {
    for (long offset = 0; offset < 250; offset++) {
      Provider.clear();
      PayerManager.clear();
      long seed = startSeed + offset;
      Person person = generateFemalePatient(seed);
      if (!GateEvaluator.hasBreastCancer(person)) {
        continue;
      }
      if (expectAlive == person.alive(Long.MAX_VALUE)) {
        return person;
      }
    }
    throw new AssertionError("Nenhuma seed encontrada para arquétipo a partir de " + startSeed);
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
      if (encounter.codes != null) {
        for (Code code : encounter.codes) {
          codes.add(code.system + "|" + code.code);
        }
      }
      addEntryCodes(encounter.conditions, codes);
      addEntryCodes(encounter.procedures, codes);
      addEntryCodes(encounter.medications, codes);
      addEntryCodes(encounter.reports, codes);
      addEntryCodes(encounter.observations, codes);
    }
    return codes;
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

  private static List<Long> collectProcedureTimes(Person person, String code) {
    List<Long> times = new ArrayList<>();
    for (Encounter encounter : person.record.encounters) {
      for (org.mitre.synthea.world.concepts.HealthRecord.Procedure procedure
          : encounter.procedures) {
        if (procedure.codes != null
            && procedure.codes.stream().anyMatch(c -> code.equals(c.code))) {
          times.add(procedure.start);
        }
      }
    }
    Collections.sort(times);
    return times;
  }

  private static void assertNoMoreThanFiveConsecutiveDailyFractions(List<Long> times) {
    int consecutive = 1;
    for (int i = 1; i < times.size(); i++) {
      long gapDays = TimeUnit.MILLISECONDS.toDays(times.get(i) - times.get(i - 1));
      if (gapDays == 1) {
        consecutive++;
      } else {
        consecutive = 1;
      }
      assertTrue("Radioterapia não deve ultrapassar cinco dias consecutivos",
          consecutive <= 5);
    }
  }

  private static void assertMilestonePresent(Set<String> codes, String system, String code) {
    assertTrue("Marco ausente: " + system + "|" + code, codes.contains(system + "|" + code));
  }
}
