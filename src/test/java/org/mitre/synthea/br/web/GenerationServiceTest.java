package org.mitre.synthea.br.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
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
}
