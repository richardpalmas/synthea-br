package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Provider;

/**
 * Integration test validating plausibility report generation on a real breast cancer cohort.
 */
public class PlausibilityReportIntegrationTest {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Configure isolated export settings for integration tests.
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
    Config.set("exporter.json.export", "false");
    Config.set("exporter.metadata.export", "false");
    Config.set("exporter.custom.export", "true");
    Config.set("br.plausibility.report.enabled", "true");
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.target_condition.gate_mode", "retry");
    PlausibilityReportAccumulator.getInstance().reset();
  }

  /**
   * Reset plausibility configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.target_condition.gate_mode");
    Config.remove("br.plausibility.report.enabled");
    PlausibilityReportAccumulator.getInstance().reset();
  }

  @Test
  public void testBreastCancerCohortProducesPlausibilityReport() throws Exception {
    File exportDir = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", exportDir.getAbsolutePath());

    long fixedReferenceTime = 1_600_000_000_000L;
    GeneratorOptions options = new GeneratorOptions();
    options.referenceTime = fixedReferenceTime;
    options.endTime = fixedReferenceTime;
    options.population = 50;
    options.seed = 20260708L;
    options.clinicianSeed = 20260709L;
    options.gender = "F";
    options.minAge = 40;
    options.maxAge = 75;
    options.ageSpecified = true;
    options.threadPoolSize = 2;

    ExporterRuntimeOptions exportOpts = new ExporterRuntimeOptions();
    Generator generator = new Generator(options, exportOpts);
    generator.options.overflow = false;
    generator.run();
    Exporter.runPostCompletionExports(generator, exportOpts);

    File reportFile = new File(exportDir, "plausibility_report.json");
    assertTrue(reportFile.exists());

    String json = Files.readString(reportFile.toPath(), StandardCharsets.UTF_8);
    Map<String, Object> report = new Gson().fromJson(json,
        new TypeToken<Map<String, Object>>() { }.getType());

    assertNotNull(report.get("seed"));
    assertNotNull(report.get("totalPatients"));
    assertNotNull(report.get("violationsByPatient"));
    assertNotNull(report.get("aggregates"));

    @SuppressWarnings("unchecked")
    Map<String, Object> aggregates = (Map<String, Object>) report.get("aggregates");
    assertNotNull(aggregates.get("alta"));
    assertNotNull(aggregates.get("media"));
    assertNotNull(aggregates.get("baixa"));
  }
}
