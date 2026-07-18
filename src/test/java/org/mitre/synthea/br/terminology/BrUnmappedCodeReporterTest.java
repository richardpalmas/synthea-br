package org.mitre.synthea.br.terminology;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.helpers.Config;

/**
 * Tests for {@link BrUnmappedCodeReporter}.
 */
public class BrUnmappedCodeReporterTest {

  private String previousProfile;

  /**
   * Saves config state before each test.
   */
  @Before
  public void setUp() {
    previousProfile = Config.get("br.profile");
    BrTerminologyResolver.resetCacheForTest();
  }

  /**
   * Restores config state after each test.
   */
  @After
  public void tearDown() {
    BrTerminologyResolver.resetCacheForTest();
    if (previousProfile != null) {
      Config.set("br.profile", previousProfile);
    } else {
      Config.remove("br.profile");
    }
  }

  @Test
  public void testWriteReportFromSampleCsv() throws Exception {
    Config.set("br.profile", "br");
    Path tempDir = Files.createTempDirectory("br-unmapped-test");
    Path csvDir = tempDir.resolve("csv");
    Files.createDirectories(csvDir);
    Files.writeString(csvDir.resolve("conditions.csv"),
        "START,STOP,PATIENT,ENCOUNTER,SYSTEM,CODE,DESCRIPTION\n"
            + "2020-01-01,2021-01-01,abc,enc1,SNOMED-CT,254837009,"
            + "Malignant neoplasm of breast (disorder)\n"
            + "2020-01-01,,abc,enc2,SNOMED-CT,999999999,Unknown disorder (disorder)\n");

    Path report = BrUnmappedCodeReporter.writeReport(tempDir);
    assertTrue(Files.exists(report));
    String content = Files.readString(report);
    assertTrue(content.contains("999999999"));
    assertTrue(content.contains("Unknown disorder"));
  }
}
