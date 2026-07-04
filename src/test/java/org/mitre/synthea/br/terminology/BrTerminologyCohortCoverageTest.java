package org.mitre.synthea.br.terminology;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.helpers.Config;

/**
 * Integration-style coverage check for cohort breast cancer terminology packs.
 */
public class BrTerminologyCohortCoverageTest {

  private String previousProfile;

  @Before
  public void setUp() {
    previousProfile = Config.get("br.profile");
    BrTerminologyResolver.resetCacheForTest();
    Config.set("br.profile", "br");
  }

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
  public void testCohortScreenshotTermsResolveToPortuguese() {
    assertPortuguese("LOINC", "58410-2", "CBC panel - Blood by Automated count",
        "Hemograma completo");
    assertPortuguese("SNOMED-CT", "82423001", "Chronic pain (finding)", "Dor crônica");
    assertPortuguese("SNOMED-CT", "473461003", "Educated to high school level (finding)",
        "Escolaridade até ensino médio");
    assertPortuguese("SNOMED-CT", "18718003", "Gingival disease (disorder)", "Doença gengival");
    assertPortuguese("LOINC", "70274-6",
        "Generalized anxiety disorder 7 item (GAD-7) total score [Reported.PHQ]",
        "Escore total GAD-7");
    assertPortuguese("RxNorm", "856987",
        "Acetaminophen 300 MG / HYDROcodone Bitartrate 5 MG Oral Tablet", "Paracetamol");
    assertPortuguese("LOINC", "69453-9", "Cause of Death [US Standard Certificate of Death]",
        "Causa da morte");
  }

  @Test
  public void testPilotOutputCoverageWhenAvailable() throws Exception {
    Path pilotDir = Path.of("output", "pilot-terminology");
    if (!Files.isDirectory(pilotDir.resolve("csv"))) {
      return;
    }
    Path report = BrUnmappedCodeReporter.writeReport(pilotDir);
    assertTrue(Files.exists(report));
    double ptRate = estimatePortugueseRate(pilotDir);
    assertTrue("Expected >= 70% PT descriptions in pilot CSV, was: " + ptRate,
        ptRate >= 70.0);
  }

  private static double estimatePortugueseRate(Path pilotDir) throws Exception {
    String[] files = {"conditions.csv", "medications.csv", "procedures.csv",
        "observations.csv", "encounters.csv"};
    int total = 0;
    int likelyPt = 0;
    for (String name : files) {
      Path csv = pilotDir.resolve("csv").resolve(name);
      if (!Files.isRegularFile(csv)) {
        continue;
      }
      for (String line : Files.readAllLines(csv)) {
        if (line.isEmpty() || line.startsWith("START,")) {
          continue;
        }
        String[] parts = line.split(",", -1);
        if (parts.length < 6) {
          continue;
        }
        String description = parts[parts.length - 1];
        if (description.isEmpty()) {
          continue;
        }
        total++;
        if (!looksEnglishClinical(description)) {
          likelyPt++;
        }
      }
    }
    return total == 0 ? 100.0 : (100.0 * likelyPt / total);
  }

  private static boolean looksEnglishClinical(String description) {
    return description.contains("(finding)")
        || description.contains("(disorder)")
        || description.contains("(procedure)")
        || description.contains("[Reported")
        || description.contains("[Presence]")
        || description.contains(" in Blood")
        || description.contains(" in Serum");
  }

  private static void assertPortuguese(String system, String code, String displayEn,
      String expectedFragment) {
    org.mitre.synthea.world.concepts.HealthRecord.Code clinical =
        new org.mitre.synthea.world.concepts.HealthRecord.Code(system, code, displayEn);
    String resolved = BrTerminologyResolver.resolveDisplay(clinical);
    assertTrue("Expected PT for " + code + " but got: " + resolved,
        resolved.contains(expectedFragment));
  }
}
