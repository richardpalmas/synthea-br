package org.mitre.synthea.br.research;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.mitre.synthea.br.ai.AiEnrichmentConfig;
import org.mitre.synthea.br.ai.AiEnrichmentService;
import org.mitre.synthea.br.ai.CohortEnrichmentLog;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.ExportHelper;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.export.PostCompletionExporter;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.helpers.Utilities;

/**
 * Writes an academic traceability manifest ({@code manifest.json}) after each generation run.
 *
 * <p>The manifest captures seed, configuration hash, build commit SHA and a deterministic checksum
 * of exported output files. Absence of this manifest invalidates a run for official academic use
 * (see {@code docs/CONTRIBUTING-ACADEMICO.md}), but write failures are logged without aborting
 * the generation pipeline.
 *
 * <p>The {@code metadata/} subdirectory is excluded from {@code output_checksum} because files
 * written by {@link org.mitre.synthea.export.MetadataExporter} contain wall-clock fields
 * ({@code runStartTime}, {@code runTimeInSeconds}) that are not reproducible across runs.
 */
public class ResearchManifestWriter implements PostCompletionExporter {

  private static final String MANIFEST_FILENAME = "manifest.json";
  private static final String METADATA_FOLDER = "metadata";

  /**
   * Config keys excluded from {@link #computeConfigHash()} because their values are specific to
   * the machine/checkout running the generation (e.g. absolute filesystem paths), not to the
   * logical configuration. Including them would make {@code config_hash} differ between
   * collaborators running an otherwise identical configuration on different machines.
   */
  private static final Set<String> CONFIG_HASH_EXCLUDED_KEYS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
          "exporter.baseDirectory",
          "br.ai.api_key",
          "br.ai.api_key_file")));

  @Override
  public void export(Generator generator, ExporterRuntimeOptions options) {
    if (!Config.getAsBoolean("br.manifest.enabled", true)) {
      return;
    }

    try {
      writeManifest(generator);
    } catch (Exception e) {
      System.err.println("ResearchManifestWriter: failed to write manifest.json: "
          + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Write {@code manifest.json} to the configured exporter base directory.
   *
   * @param generator the completed generator instance
   * @throws IOException if manifest cannot be written
   */
  public static void writeManifest(Generator generator) throws IOException {
    if (!Config.getAsBoolean("br.manifest.enabled", true)) {
      return;
    }

    String baseDirectory = Config.get("exporter.baseDirectory");
    File outputBase = new File(baseDirectory);
    outputBase.mkdirs();

    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("seed", generator.options.seed);
    manifest.put("config_hash", computeConfigHash());
    manifest.put("commit_sha", Utilities.SYNTHEA_COMMIT_SHA);
    manifest.put("output_checksum", computeOutputChecksum(outputBase));
    manifest.put("generated_at_iso8601",
        ExportHelper.iso8601Timestamp(System.currentTimeMillis()));

    if (AiEnrichmentConfig.isEnabled()) {
      Map<String, Object> aiSection = new LinkedHashMap<>();
      aiSection.put("enabled", true);
      aiSection.put("provider", AiEnrichmentConfig.getProvider());
      aiSection.put("model", AiEnrichmentConfig.getModel());
      aiSection.put("deterministic", false);
      aiSection.put("log_present", AiEnrichmentService.isLogPresent());
      CohortEnrichmentLog enrichmentLog = AiEnrichmentService.getLastLog();
      if (enrichmentLog != null) {
        aiSection.put("patients_enriched", enrichmentLog.getPatients().size());
      }
      manifest.put("ai_enrichment", aiSection);
    }

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(manifest);

    Path manifestPath = outputBase.toPath().resolve(MANIFEST_FILENAME);
    Files.write(manifestPath, json.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * SHA-256 hex digest of all resolved configuration properties (keys sorted alphabetically),
   * excluding machine-specific keys (see {@link #CONFIG_HASH_EXCLUDED_KEYS}).
   *
   * @return lowercase hex-encoded SHA-256
   */
  public static String computeConfigHash() {
    Set<String> propertyNames = new TreeSet<>(Config.allPropertyNames());
    StringBuilder serialized = new StringBuilder();

    for (String key : propertyNames) {
      if (CONFIG_HASH_EXCLUDED_KEYS.contains(key)) {
        continue;
      }
      String value = Config.get(key);
      if (value == null) {
        value = "";
      }
      // Escape embedded newlines/backslashes so distinct values can't serialize to the same
      // line-delimited representation and collide in the resulting hash.
      String escaped = value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
      serialized.append(key).append('=').append(escaped).append('\n');
    }

    return sha256Hex(serialized.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * SHA-256 hex digest over sorted {@code relativePath:contentHash} lines for exported files.
   *
   * <p>Excludes {@code manifest.json} (self-reference) and the entire {@code metadata/} tree
   * because metadata JSON includes non-deterministic wall-clock timestamps.
   *
   * @param outputBase exporter base directory
   * @return lowercase hex-encoded SHA-256
   * @throws IOException if output files cannot be read
   */
  public static String computeOutputChecksum(File outputBase) throws IOException {
    Path basePath = outputBase.toPath().toAbsolutePath().normalize();
    List<String> entries = new ArrayList<>();

    if (Files.exists(basePath)) {
      try (Stream<Path> paths = Files.walk(basePath)) {
        paths.filter(Files::isRegularFile).forEach(path -> {
          String relative = basePath.relativize(path).toString().replace('\\', '/');
          if (MANIFEST_FILENAME.equals(relative)) {
            return;
          }
          if (relative.equals(METADATA_FOLDER)
              || relative.startsWith(METADATA_FOLDER + "/")) {
            return;
          }
          try {
            byte[] content = Files.readAllBytes(path);
            String fileHash = sha256Hex(content);
            entries.add(relative + ':' + fileHash);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      } catch (RuntimeException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException) e.getCause();
        }
        throw e;
      }
    }

    Collections.sort(entries);
    StringBuilder concatenated = new StringBuilder();
    for (String entry : entries) {
      concatenated.append(entry).append('\n');
    }

    return sha256Hex(concatenated.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256Hex(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data);
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
