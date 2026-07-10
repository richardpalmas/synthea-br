package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Tests for {@link ExportFailureTracker} and export isolation in {@link Exporter}.
 */
public class ExportFailureIsolationTest {

  /**
   * Load test properties before each test.
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    ExportFailureTracker.reset();
  }

  @Test
  public void runExportStepRecordsFailureWithoutPropagating() {
    Exporter.runExportStep("csv", () -> {
      throw new RuntimeException("simulated csv failure");
    });

    assertEquals(1, ExportFailureTracker.getFailureCount("csv"));
  }

  @Test
  public void csvFailureDoesNotPreventHtmlAppend() throws Exception {
    ExportFailureTracker.reset();
    Config.set("exporter.html.export", "true");
    Exporter.prepareHtmlCohortExport();

    Person person = new Person(7L);
    person.attributes.put(Person.NAME, "Maria Export");
    person.attributes.put(Person.GENDER, "F");
    person.attributes.put(Person.BIRTHDATE, TestHelper.timestamp(1980, 1, 1, 0, 0, 0));
    person.attributes.put(Person.ID, "html-after-csv-failure");
    long stopTime = System.currentTimeMillis();

    Exporter.runExportStep("csv", () -> {
      throw new RuntimeException("simulated csv failure");
    });
    Exporter.runExportStep("html", () ->
        HtmlExporter.getInstance().appendPatient(person, stopTime));

    assertEquals(1, ExportFailureTracker.getFailureCount("csv"));
    assertEquals(0, ExportFailureTracker.getFailureCount("html"));
    assertTrue("HTML cohort should retain patient after CSV failure",
        HtmlExporter.getInstance().getAccumulatedPatientCountForTest() == 1);
  }
}
