package org.mitre.synthea.br.research;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
    long fixedReferenceTime = 1_600_000_000_000L;
    ExporterRuntimeOptions exportOpts = new ExporterRuntimeOptions();
    GeneratorOptions generatorOpts = new GeneratorOptions();
    generatorOpts.referenceTime = fixedReferenceTime;
    generatorOpts.endTime = fixedReferenceTime;
    generatorOpts.population = 1;
    generatorOpts.singlePersonSeed = seed;
    generatorOpts.seed = seed;
    generatorOpts.clinicianSeed = seed + 1L;
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
