package org.mitre.synthea.br.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;

/**
 * Tests timestamped output directory reporting in {@link GenerationJobManager}.
 */
public class GenerationJobManagerOutputTest {

  private static final Pattern RUN_FOLDER_PATTERN =
      Pattern.compile(".*[/\\\\]\\d{4}-\\d{2}-\\d{2}_\\d{6}([/\\\\]|(-\\d{3}[/\\\\])).*");

  private File tempOutputDir;
  private String previousOutputDir;
  private String previousTimestampedRuns;

  /**
   * Prepare isolated output root and enable timestamped runs.
   *
   * @throws Exception on setup errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    tempOutputDir = Files.createTempDirectory("synthea-web-job-output-").toFile();
    previousOutputDir = Config.get("exporter.baseDirectory");
    previousTimestampedRuns = Config.get("br.output.timestamped_runs");
    Config.set("exporter.baseDirectory", tempOutputDir.getAbsolutePath() + File.separator);
    Config.remove("br.output.timestamped_runs");
    Config.set("exporter.fhir.export", "false");
    Config.set("exporter.csv.export", "false");
    Config.set("exporter.html.export", "false");
    Config.set("br.manifest.enabled", "true");
    Config.set("generate.default_population", "1");
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
  }

  /**
   * Restore global configuration.
   */
  @After
  public void tearDown() {
    if (previousOutputDir != null) {
      Config.set("exporter.baseDirectory", previousOutputDir);
    } else {
      Config.remove("exporter.baseDirectory");
    }
    if (previousTimestampedRuns != null) {
      Config.set("br.output.timestamped_runs", previousTimestampedRuns);
    } else {
      Config.remove("br.output.timestamped_runs");
    }
  }

  @Test
  public void testJobReportsTimestampedOutputDirectory() throws Exception {
    GenerationRequest request = new GenerationRequest();
    request.seed = 42L;
    request.population = 1;
    request.exportFhir = false;
    request.exportCsv = false;
    request.exportHtml = false;

    GenerationJobManager manager = GenerationJobManager.getInstance();
    manager.startJob(request);

    long deadline = System.currentTimeMillis() + 120_000L;
    GenerationStatus status;
    do {
      Thread.sleep(200L);
      status = manager.getStatus();
    } while ("running".equals(status.state) && System.currentTimeMillis() < deadline);

    assertTrue("Job should complete", "completed".equals(status.state));
    assertNotNull(status.outputDirectory);
    assertTrue("Output directory should include timestamp subfolder",
        RUN_FOLDER_PATTERN.matcher(status.outputDirectory.replace('\\', '/')).matches());
    assertTrue(new File(status.outputDirectory, "manifest.json").exists());
  }
}
