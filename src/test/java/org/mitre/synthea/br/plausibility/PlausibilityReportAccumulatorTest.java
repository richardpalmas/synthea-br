package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link PlausibilityReportAccumulator}.
 */
public class PlausibilityReportAccumulatorTest {

  @Before
  public void setUp() {
    PlausibilityReportAccumulator.getInstance().reset();
  }

  @After
  public void tearDown() {
    PlausibilityReportAccumulator.getInstance().reset();
  }

  @Test
  public void testSortedSnapshotOrdersByPatientId() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("patient-z",
        List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("patient-a",
        List.of(sampleViolation("PLAUS-002", "média")));
    accumulator.recordPatientViolations("patient-m",
        List.of(sampleViolation("PLAUS-003", "média")));

    List<String> order = accumulator.getViolationsByPatientSorted().keySet()
        .stream().toList();
    assertEquals(Arrays.asList("patient-a", "patient-m", "patient-z"), order);
  }

  @Test
  public void testCountPatientsBySeverity() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("p1", Arrays.asList(
        sampleViolation("PLAUS-001", "alta"),
        sampleViolation("PLAUS-002", "média")));
    accumulator.recordPatientViolations("p2", List.of(sampleViolation("PLAUS-002", "média")));

    Map<String, Integer> counts = accumulator.countPatientsBySeverity();
    assertEquals(Integer.valueOf(1), counts.get("alta"));
    assertEquals(Integer.valueOf(2), counts.get("média"));
    assertEquals(Integer.valueOf(0), counts.get("baixa"));
  }

  @Test
  public void testResetClearsState() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("p1", List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.reset();
    assertTrue(accumulator.getViolationsByPatientSorted().isEmpty());
  }

  @Test
  public void testRecordReplacesPreviousViolationsForSamePatient() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("p1", List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("p1", List.of(sampleViolation("PLAUS-002", "média")));

    List<Violation> violations = accumulator.getViolationsByPatientSorted().get("p1");
    assertEquals(1, violations.size());
    assertEquals("PLAUS-002", violations.get(0).getRuleId());
  }

  @Test
  public void testSnapshotIsConsistentAcrossViolationsAndAggregates() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("p1", List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("p2", List.of(sampleViolation("PLAUS-002", "média")));

    PlausibilityReportAccumulator.ReportSnapshot snapshot = accumulator.snapshot();
    assertEquals(2, snapshot.violationsByPatient.size());
    assertEquals(Integer.valueOf(1), snapshot.severityCounts.get("alta"));
    assertEquals(Integer.valueOf(1), snapshot.severityCounts.get("média"));
  }

  private static Violation sampleViolation(String ruleId, String severity) {
    return new Violation(ruleId, severity, "patient-x", "desc", new LinkedHashMap<>());
  }
}
