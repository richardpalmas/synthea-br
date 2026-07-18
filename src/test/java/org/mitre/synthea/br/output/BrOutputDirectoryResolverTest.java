package org.mitre.synthea.br.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link BrOutputDirectoryResolver}.
 */
public class BrOutputDirectoryResolverTest {

  private static final Pattern RUN_FOLDER_PATTERN =
      Pattern.compile("\\d{4}-\\d{2}-\\d{2}_\\d{6}(-\\d{3})?");

  private File tempRoot;
  private String previousBaseDirectory;
  private String previousTimestampedRuns;

  /**
   * Saves config and creates temp root directory.
   *
   * @throws Exception on setup errors
   */
  @Before
  public void setUp() throws Exception {
    tempRoot = Files.createTempDirectory("br-output-resolver-").toFile();
    previousBaseDirectory = Config.get("exporter.baseDirectory");
    previousTimestampedRuns = Config.get("br.output.timestamped_runs");
    Config.set("exporter.baseDirectory", tempRoot.getAbsolutePath() + File.separator);
  }

  /**
   * Restores config after each test.
   */
  @After
  public void tearDown() {
    if (previousBaseDirectory != null) {
      Config.set("exporter.baseDirectory", previousBaseDirectory);
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
  public void testPrepareRunDirectoryCreatesTimestampSubfolder() throws Exception {
    Config.remove("br.output.timestamped_runs");

    String resolved = BrOutputDirectoryResolver.prepareRunDirectory();

    Path resolvedPath = Paths.get(resolved).normalize();
    assertTrue(resolvedPath.startsWith(tempRoot.toPath().toAbsolutePath().normalize()));
    Path runFolder = resolvedPath.getFileName();
    assertTrue(RUN_FOLDER_PATTERN.matcher(runFolder.toString()).matches());
    assertTrue(Files.isDirectory(resolvedPath));
    assertEquals(normalizePath(resolved), normalizePath(Config.get("exporter.baseDirectory")));
  }

  private static String normalizePath(String path) {
    return Paths.get(path).toAbsolutePath().normalize().toString();
  }

  @Test
  public void testPrepareRunDirectoryDisabledKeepsBaseDirectory() throws Exception {
    Config.set("br.output.timestamped_runs", "false");

    String resolved = BrOutputDirectoryResolver.prepareRunDirectory();

    String expected = normalizePath(tempRoot.getAbsolutePath());

    assertEquals(expected, normalizePath(resolved));
    assertEquals(expected, normalizePath(Config.get("exporter.baseDirectory")));
    assertFalse(Files.list(tempRoot.toPath()).findAny().isPresent());
  }
}
