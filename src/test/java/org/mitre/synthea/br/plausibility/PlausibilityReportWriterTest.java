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
 * Unit tests for {@link PlausibilityReportWriter} aggregation and ordering.
 */
public class PlausibilityReportWriterTest {

  @Before
  public void setUp() {
    PlausibilityReportAccumulator.getInstance().reset();
  }

  @After
  public void tearDown() {
    PlausibilityReportAccumulator.getInstance().reset();
  }

  @Test
  public void testBuildReportPayloadDeterministicOrder() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("patient-c",
        List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("patient-a",
        List.of(sampleViolation("PLAUS-002", "média")));
    accumulator.recordPatientViolations("patient-b",
        List.of(sampleViolation("PLAUS-003", "média")));

    Map<String, Object> report =
        PlausibilityReportWriter.buildReportPayload(42L, 100, accumulator);

    assertEquals(42L, report.get("seed"));
    assertEquals(100, report.get("totalPatients"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> byPatient =
        (List<Map<String, Object>>) report.get("violationsByPatient");
    assertEquals("patient-a", byPatient.get(0).get("patientId"));
    assertEquals("patient-b", byPatient.get(1).get("patientId"));
    assertEquals("patient-c", byPatient.get(2).get("patientId"));
  }

  @Test
  public void testAggregatesPercentages() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("p1", List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("p2", List.of(sampleViolation("PLAUS-002", "média")));
    accumulator.recordPatientViolations("p3", List.of(sampleViolation("PLAUS-002", "média")));

    Map<String, Object> report = PlausibilityReportWriter.buildReportPayload(7L, 200, accumulator);

    @SuppressWarnings("unchecked")
    Map<String, Object> aggregates = (Map<String, Object>) report.get("aggregates");

    @SuppressWarnings("unchecked")
    Map<String, Object> alta = (Map<String, Object>) aggregates.get("alta");
    assertEquals(1, alta.get("count"));
    assertEquals(0.5, alta.get("percentage"));

    @SuppressWarnings("unchecked")
    Map<String, Object> media = (Map<String, Object>) aggregates.get("media");
    assertEquals(2, media.get("count"));
    assertEquals(1.0, media.get("percentage"));
  }

  @Test
  public void testOutOfOrderInsertionProducesSortedOutput() {
    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    accumulator.recordPatientViolations("z-last", List.of(sampleViolation("PLAUS-001", "alta")));
    accumulator.recordPatientViolations("a-first", List.of(sampleViolation("PLAUS-002", "média")));
    accumulator.recordPatientViolations("m-middle", List.of(sampleViolation("PLAUS-003", "baixa")));

    Map<String, Object> report = PlausibilityReportWriter.buildReportPayload(1L, 3, accumulator);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> byPatient =
        (List<Map<String, Object>>) report.get("violationsByPatient");

    assertEquals(3, byPatient.size());
    assertEquals("a-first", byPatient.get(0).get("patientId"));
    assertEquals("m-middle", byPatient.get(1).get("patientId"));
    assertEquals("z-last", byPatient.get(2).get("patientId"));
    assertTrue(byPatient.get(2).get("violations") instanceof List);
  }

  private static Violation sampleViolation(String ruleId, String severity) {
    return new Violation(ruleId, severity, "patient-x", "desc", new LinkedHashMap<>());
  }
}
