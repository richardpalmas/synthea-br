package org.mitre.synthea.br.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.ExportFailureTracker;
import org.mitre.synthea.helpers.Config;

/**
 * Integration test for {@link GenerationService}.
 */
public class GenerationServiceTest {

  private File tempOutputDir;
  private String previousOutputDir;
  private String previousBrProfile;
  private String previousTargetCondition;
  private String previousTimestampedRuns;
  private String previousPathwayFocus;
  private String previousModuleProfile;
  private String previousTrajectoryMode;
  private String previousSimulationWindow;

  /**
   * Prepare isolated output directory and test properties.
   *
   * @throws Exception on setup errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    tempOutputDir = Files.createTempDirectory("synthea-web-test-").toFile();
    previousOutputDir = Config.get("exporter.baseDirectory");
    previousBrProfile = Config.get("br.profile");
    previousTargetCondition = Config.get("br.target_condition");
    previousTimestampedRuns = Config.get("br.output.timestamped_runs");
    previousPathwayFocus = Config.get("br.pathway.focus");
    previousModuleProfile = Config.get("br.generation.module_profile");
    previousTrajectoryMode = Config.get("br.generation.trajectory_mode");
    previousSimulationWindow = Config.get("br.generation.simulation_window");
    Config.set("exporter.baseDirectory", tempOutputDir.getAbsolutePath() + File.separator);
    Config.set("br.output.timestamped_runs", "false");
    Config.set("exporter.fhir.export", "false");
    Config.set("exporter.csv.export", "false");
    Config.set("exporter.html.export", "false");
    Config.set("br.manifest.enabled", "true");
    Config.remove("br.profile");
    Config.remove("br.target_condition");
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
  }

  /**
   * Restore global configuration.
   */
  @After
  public void tearDown() {
    if (previousOutputDir != null) {
      Config.set("exporter.baseDirectory", previousOutputDir);
    }
    if (previousBrProfile != null) {
      Config.set("br.profile", previousBrProfile);
    } else {
      Config.remove("br.profile");
    }
    if (previousTargetCondition != null) {
      Config.set("br.target_condition", previousTargetCondition);
    } else {
      Config.remove("br.target_condition");
    }
    if (previousTimestampedRuns != null) {
      Config.set("br.output.timestamped_runs", previousTimestampedRuns);
    } else {
      Config.remove("br.output.timestamped_runs");
    }
    restoreConfigKey("br.pathway.focus", previousPathwayFocus);
    restoreConfigKey("br.generation.module_profile", previousModuleProfile);
    restoreConfigKey("br.generation.trajectory_mode", previousTrajectoryMode);
    restoreConfigKey("br.generation.simulation_window", previousSimulationWindow);
  }

  private static void restoreConfigKey(String key, String previous) {
    if (previous != null) {
      Config.set(key, previous);
    } else {
      Config.remove(key);
    }
  }

  @Test
  public void testWebGenerationMatchesRequestedPopulationWithExcludeGate() throws Exception {
    GenerationRequest request = new GenerationRequest();
    request.seed = 42L;
    request.population = 10;
    request.gender = "F";
    request.minAge = 40;
    request.maxAge = 90;
    request.brProfile = true;
    request.targetCondition = "breast_cancer";
    request.gateMode = "exclude";
    request.exportFhir = false;
    request.exportCsv = false;
    request.exportHtml = true;

    Config.set("exporter.html.export", "true");

    GenerationService service = new GenerationService();
    Generator generator = service.run(request, null);

    assertEquals(
        "Web UI must export exactly one patient per slot (deceased may be in the cohort)",
        10, generator.totalGeneratedPopulation.get());
    File htmlIndex = new File(tempOutputDir, "html/index.html");
    assertTrue("HTML narrative index should exist", htmlIndex.exists());
    String html = Files.readString(htmlIndex.toPath());
    assertTrue("HTML index should report 10 patients", html.contains("10 pacientes"));
  }

  @Test
  public void testGenerateOnePatientWithFixedSeed() throws Exception {
    GenerationRequest request = new GenerationRequest();
    request.seed = 42L;
    request.population = 1;
    request.exportFhir = false;
    request.exportCsv = false;
    request.exportHtml = false;

    Config.set("generate.default_population", "1");

    GenerationService service = new GenerationService();
    Generator generator = service.run(request, null);

    assertTrue("At least one patient should be generated",
        generator.totalGeneratedPopulation.get() >= 1);
    File manifest = new File(tempOutputDir, "manifest.json");
    assertTrue("manifest.json should exist", manifest.exists());
  }

  @Test
  public void testFocusedTrajectoryManifestFields() throws Exception {
    GenerationRequest request = new GenerationRequest();
    request.seed = 42L;
    request.population = 1;
    request.gender = "F";
    request.minAge = 45;
    request.maxAge = 75;
    request.brProfile = true;
    request.targetCondition = "breast_cancer";
    request.gateMode = "exclude";
    request.exportFhir = false;
    request.exportCsv = false;
    request.exportHtml = false;
    request.pathwayFocus = true;
    request.moduleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    request.trajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC;
    request.simulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;

    GenerationService service = new GenerationService();
    service.run(request, null);

    File manifest = new File(tempOutputDir, "manifest.json");
    assertTrue("manifest.json should exist", manifest.exists());
    String json = Files.readString(manifest.toPath());
    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
    assertTrue("pathway_focus should be true", root.get("pathway_focus").getAsBoolean());
    assertEquals("pathway_minimal", root.get("module_profile").getAsString());
    assertEquals("pre_onset_years:10", root.get("simulation_window").getAsString());

    File plausibility = new File(tempOutputDir, "plausibility_report.json");
    assertTrue("plausibility_report.json should exist", plausibility.exists());
  }

  @Test
  public void testFocusedTrajectoryCsvAndHtmlProducesIndex() throws Exception {
    GenerationRequest request = new GenerationRequest();
    request.seed = 42L;
    request.population = 1;
    request.gender = "F";
    request.minAge = 45;
    request.maxAge = 75;
    request.brProfile = true;
    request.targetCondition = "breast_cancer";
    request.gateMode = "retry";
    request.exportFhir = false;
    request.exportCsv = true;
    request.exportHtml = true;
    request.pathwayFocus = true;
    request.moduleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    request.trajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC;
    request.simulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;

    Config.set("exporter.csv.export", "true");
    Config.set("exporter.html.export", "true");

    GenerationService service = new GenerationService();
    Generator generator = service.run(request, null);

    assertEquals(1, generator.totalGeneratedPopulation.get());
    File htmlIndex = new File(tempOutputDir, "html/index.html");
    assertTrue("HTML index should exist with CSV+HTML focused trajectory", htmlIndex.exists());
    assertEquals(0, ExportFailureTracker.getFailureCount("csv"));
    assertEquals(0, ExportFailureTracker.getFailureCount("html"));
    File patientsCsv = new File(tempOutputDir, "csv/patients.csv");
    assertTrue("CSV patients file should exist", patientsCsv.exists());
  }
}
