package org.mitre.synthea.br.pathway.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.condition.GateEvaluator;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;

/**
 * Integration tests for {@link SimulationWindowConfig} (Story 9.6).
 */
public class SimulationWindowIntegrationTest {

  private static final long FIXED_REFERENCE_TIME = 1_600_000_000_000L;
  private static final long FIXED_SEED = 92006L;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Provider.clear();
    PayerManager.clear();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");
    Config.set("generate.max_attempts_to_keep_patient", "3000");
  }

  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("br.generation.simulation_window");
    Config.remove("br.generation.module_profile");
    Config.remove("generate.max_attempts_to_keep_patient");
    Config.remove("exporter.baseDirectory");
  }

  @Test
  public void simulationWindow_preservesTargetConditionGate() throws Exception {
    Config.set("br.generation.simulation_window", "pre_onset_years:5");
    Generator generator = buildGenerator(4, FIXED_SEED);
    for (int i = 0; i < 4; i++) {
      Person person = generator.generatePerson(i, FIXED_SEED + i);
      assertTrue(GateEvaluator.hasBreastCancer(person));
    }
  }

  @Test
  public void simulationWindow_isReproducibleWithSameSeed() throws Exception {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.set("br.generation.simulation_window", "pre_onset_years:5");
    int run1 = averageDistinctConditions(FIXED_SEED + 100L);
    Provider.clear();
    PayerManager.clear();
    int run2 = averageDistinctConditions(FIXED_SEED + 100L);
    assertEquals(run1, run2);
  }

  @Test
  public void simulationWindow_startsAfterBirthdate() throws Exception {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.set("br.generation.simulation_window", "pre_onset_years:5");
    Generator generator = buildGenerator(1, FIXED_SEED + 50L);
    Person person = generator.generatePerson(0, FIXED_SEED + 50L);
    long birthdate = (long) person.attributes.get(Person.BIRTHDATE);
    assertTrue("Simulation should start after birth when window is active",
        person.record.encounters.isEmpty()
            || person.record.encounters.get(0).start > birthdate + 4L * 365L * 86_400_000L);
  }

  @Test
  public void simulationWindow_manifestRecordsWindowAndDuration() throws Exception {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    File exportDir = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", exportDir.getAbsolutePath());
    Config.set("exporter.custom.export", "true");
    Config.set("br.manifest.enabled", "true");
    Config.set("br.generation.simulation_window", "pre_onset_years:5");
    Config.set("br.generation.module_profile", "pathway_minimal");

    GeneratorOptions generatorOpts = new GeneratorOptions();
    generatorOpts.referenceTime = FIXED_REFERENCE_TIME;
    generatorOpts.endTime = FIXED_REFERENCE_TIME;
    generatorOpts.population = 1;
    generatorOpts.singlePersonSeed = FIXED_SEED;
    generatorOpts.seed = FIXED_SEED;
    generatorOpts.clinicianSeed = FIXED_SEED + 1L;
    generatorOpts.gender = "F";
    generatorOpts.minAge = 40;
    generatorOpts.maxAge = 75;
    generatorOpts.ageSpecified = true;

    Generator generator = new Generator(generatorOpts, new ExporterRuntimeOptions());
    generator.options.overflow = false;
    generator.run();
    Exporter.runPostCompletionExports(generator, new ExporterRuntimeOptions());

    File manifestFile = new File(exportDir, "manifest.json");
    assertTrue(manifestFile.exists());
    String json = Files.readString(manifestFile.toPath(), StandardCharsets.UTF_8);
    Map<String, Object> manifest = new Gson().fromJson(json,
        new TypeToken<Map<String, Object>>() { }.getType());

    assertEquals("pre_onset_years:5", manifest.get("simulation_window"));
    assertEquals("pathway_minimal", manifest.get("module_profile"));
    assertNotNull(manifest.get("generation_duration_ms"));
  }

  private int averageDistinctConditions(long seed) throws Exception {
    Generator generator = buildGenerator(4, seed);
    int total = 0;
    for (int i = 0; i < 4; i++) {
      Person person = generator.generatePerson(i, seed + i);
      total += person.record.present.size();
    }
    return total / 4;
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
