package org.mitre.synthea.br.research;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Provider;

public class ResearchManifestWriterTest {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Configure isolated export settings for manifest tests.
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("exporter.json.export", "true");
    Config.set("exporter.metadata.export", "false");
    Config.set("exporter.custom.export", "true");
    Config.set("br.manifest.enabled", "true");
    Config.remove("br.target_condition");
    Config.remove("br.pathway.focus");
    Config.set("br.profile", "");
  }

  @Test
  public void testReproducibleManifestHashes() throws Exception {
    File exportDir = tempFolder.newFolder();
    Map<String, Object> manifest1 = runGenerationAndReadManifest(exportDir, 42L);
    Map<String, Object> manifest2 = runGenerationAndReadManifest(exportDir, 42L);

    assertEquals(manifest1.get("config_hash"), manifest2.get("config_hash"));
    assertEquals(manifest1.get("output_checksum"), manifest2.get("output_checksum"));
    assertEquals(manifest1.get("seed"), manifest2.get("seed"));
    assertEquals(manifest1.get("commit_sha"), manifest2.get("commit_sha"));
  }

  @Test
  public void testDifferentSeedsProduceDifferentChecksums() throws Exception {
    Map<String, Object> manifest1 = runGenerationAndReadManifest(tempFolder.newFolder(), 42L);
    Map<String, Object> manifest2 = runGenerationAndReadManifest(tempFolder.newFolder(), 99L);

    assertNotEquals(manifest1.get("output_checksum"), manifest2.get("output_checksum"));
  }

  @Test
  public void testGeneratedAtIso8601IsParseableUtc() throws Exception {
    Map<String, Object> manifest = runGenerationAndReadManifest(tempFolder.newFolder(), 7L);
    String timestamp = (String) manifest.get("generated_at_iso8601");

    Instant.parse(timestamp);
    assertTrue(timestamp.endsWith("Z"));
  }

  @Test
  public void testManifestDisabledSkipsWrite() throws Exception {
    File exportDir = tempFolder.newFolder();
    resetTestConfig(exportDir);
    Config.set("br.manifest.enabled", "false");

    runSmallGeneration(exportDir, 1L);

    File manifestFile = new File(exportDir, "manifest.json");
    assertTrue(!manifestFile.exists());
  }

  @Test
  public void testResolveProfileAndTargetConditionFields() {
    Config.set("br.profile", "");
    Config.remove("br.target_condition");
    assertEquals(null, ResearchManifestWriter.resolveProfileField());
    assertEquals(null, ResearchManifestWriter.resolveTargetConditionField());

    Config.set("br.profile", "br");
    assertEquals("br", ResearchManifestWriter.resolveProfileField());

    Config.set("br.target_condition", "breast_cancer");
    assertEquals("breast_cancer", ResearchManifestWriter.resolveTargetConditionField());
  }

  @Test
  public void testProvenanceFieldsNullWhenBrFeaturesInactive() throws Exception {
    File exportDir = tempFolder.newFolder();
    Map<String, Object> manifest = runGenerationAndReadManifest(exportDir, 42L);
    String json = new String(Files.readAllBytes(new File(exportDir, "manifest.json").toPath()),
        StandardCharsets.UTF_8);

    assertEquals("Synthea-br", manifest.get("forkName"));
    assertEquals(Utilities.SYNTHEA_VERSION, manifest.get("version"));
    assertTrue(manifest.containsKey("profile"));
    assertTrue(manifest.containsKey("targetCondition"));
    assertEquals(null, manifest.get("profile"));
    assertEquals(null, manifest.get("targetCondition"));
    assertTrue(json.contains("\"profile\": null"));
    assertTrue(json.contains("\"targetCondition\": null"));
    assertNotNull(manifest.get("commit_sha"));
    assertNotEquals("", manifest.get("commit_sha"));
  }

  @Test
  public void testProvenanceFieldsPopulatedWhenBrFeaturesActive() throws Exception {
    File exportDir = tempFolder.newFolder();
    resetTestConfig(exportDir);
    Config.set("br.profile", "br");
    Config.set("br.target_condition", "breast_cancer");
    cleanExportDir(exportDir);
    runSmallBrCohortGeneration(exportDir, 42L);

    File manifestFile = new File(exportDir, "manifest.json");
    assertTrue("manifest.json should exist", manifestFile.exists());
    String json = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
    Map<String, Object> manifest = new Gson().fromJson(json,
        new TypeToken<Map<String, Object>>() { }.getType());

    assertEquals("Synthea-br", manifest.get("forkName"));
    assertEquals(Utilities.SYNTHEA_VERSION, manifest.get("version"));
    assertTrue(BrProfile.isActive());
    assertEquals("br", manifest.get("profile"));
    assertEquals("breast_cancer", manifest.get("targetCondition"));
    assertTrue(manifest.containsKey("seed"));
    assertTrue(manifest.containsKey("config_hash"));
    assertNotNull(manifest.get("commit_sha"));
    assertNotEquals("", manifest.get("commit_sha"));
    assertTrue(manifest.containsKey("output_checksum"));
    assertTrue(manifest.containsKey("generated_at_iso8601"));
  }

  @Test
  public void testPathwayFieldsWhenFocusEnabled() throws Exception {
    File exportDir = tempFolder.newFolder();
    resetTestConfig(exportDir);
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.pathway.focus", "true");
    cleanExportDir(exportDir);
    runSmallBrCohortGeneration(exportDir, 42L);

    String json = new String(Files.readAllBytes(new File(exportDir, "manifest.json").toPath()),
        StandardCharsets.UTF_8);
    Map<String, Object> manifest = new Gson().fromJson(json,
        new TypeToken<Map<String, Object>>() { }.getType());

    assertEquals(true, manifest.get("pathway_focus"));
    assertEquals("breast_cancer", manifest.get("pathway_condition"));
    assertEquals("1.0.0", manifest.get("pathway_catalog_version"));
  }

  @After
  public void tearDownBrConfig() {
    Config.remove("br.target_condition");
    Config.remove("br.pathway.focus");
    Config.set("br.profile", "");
  }

  private Map<String, Object> runGenerationAndReadManifest(File exportDir, long seed)
      throws Exception {
    resetTestConfig(exportDir);
    cleanExportDir(exportDir);
    runSmallGeneration(exportDir, seed);

    File manifestFile = new File(exportDir, "manifest.json");
    assertTrue("manifest.json should exist", manifestFile.exists());

    String json = new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8);
    return new Gson().fromJson(json, new TypeToken<Map<String, Object>>() { }.getType());
  }

  private void runSmallGeneration(File exportDir, long seed) throws Exception {
    runSmallGeneration(exportDir, seed, false);
  }

  private void runSmallBrCohortGeneration(File exportDir, long seed) throws Exception {
    runSmallGeneration(exportDir, seed, true);
  }

  private void runSmallGeneration(File exportDir, long seed, boolean brCohort)
      throws Exception {
    long fixedReferenceTime = 1_600_000_000_000L;
    ExporterRuntimeOptions exportOpts = new ExporterRuntimeOptions();
    GeneratorOptions generatorOpts = new GeneratorOptions();
    generatorOpts.referenceTime = fixedReferenceTime;
    generatorOpts.endTime = fixedReferenceTime;
    generatorOpts.population = 1;
    generatorOpts.singlePersonSeed = seed;
    generatorOpts.seed = seed;
    generatorOpts.clinicianSeed = seed + 1L;
    if (brCohort) {
      generatorOpts.gender = "F";
      generatorOpts.minAge = 40;
      generatorOpts.maxAge = 75;
      generatorOpts.ageSpecified = true;
    }
    Generator generator = new Generator(generatorOpts, exportOpts);
    generator.options.overflow = false;
    generator.run();
    Exporter.runPostCompletionExports(generator, exportOpts);
  }

  private void resetTestConfig(File exportDir) throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Provider.clear();
    PayerManager.clear();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("exporter.baseDirectory", exportDir.toString());
    Config.set("exporter.fhir.export", "true");
    Config.set("exporter.fhir.transaction_bundle", "true");
    Config.set("exporter.pretty_print", "false");
    Config.set("exporter.json.export", "false");
    Config.set("exporter.metadata.export", "false");
    Config.set("exporter.custom.export", "true");
    Config.set("br.manifest.enabled", "true");
    Config.remove("br.target_condition");
    Config.remove("br.pathway.focus");
    Config.set("br.profile", "");
  }

  private void cleanExportDir(File exportDir) throws IOException {
    Path root = exportDir.toPath();
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(root)) {
      paths.sorted(Comparator.reverseOrder()).forEach(path -> {
        try {
          if (!path.equals(root)) {
            Files.deleteIfExists(path);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }
}
