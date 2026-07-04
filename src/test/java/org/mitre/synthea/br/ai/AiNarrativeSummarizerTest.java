package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link AiNarrativeSummarizer} fallback summaries.
 */
public class AiNarrativeSummarizerTest {

  @Test
  public void testFallbackPatientSummaryWhenNoChanges() {
    PatientEnrichmentResult result = new PatientEnrichmentResult(
        "p1", new ArrayList<>(), new ArrayList<>(), "", true);
    String summary = AiNarrativeSummarizer.summarizePatient(new MockLlmClient(), result);
    assertTrue(summary.contains("Nenhuma inconsistência"));
  }

  @Test
  public void testFallbackPatientSummaryWhenLlmReturnsJson() {
    Map<String, Object> op = new LinkedHashMap<>();
    op.put("op", "set_person_attribute");
    op.put("status", "applied");
    PatientEnrichmentResult result = new PatientEnrichmentResult(
        "p2", List.of(op), new ArrayList<>(), "", true);
    String summary = AiNarrativeSummarizer.summarizePatient(
        new MockLlmClient("{\"action\":\"FinalizePatient\"}"), result);
    assertTrue(summary.contains("1 correção"));
  }

  @Test
  public void testFallbackCohortSummary() {
    CohortEnrichmentLog log = new CohortEnrichmentLog();
    log.setMetadata("openai", "gpt-4o-mini", false);
    Map<String, Object> op1 = new LinkedHashMap<>();
    op1.put("op", "set_person_attribute");
    Map<String, Object> op2 = new LinkedHashMap<>();
    op2.put("op", "add_observation");
    Map<String, Object> flag = new LinkedHashMap<>();
    flag.put("op", "flag_unfixable");
    log.addPatient(new PatientEnrichmentResult(
        "p1", List.of(op1, op2), List.of(flag), "", true, "Resumo paciente."));

    String summary = AiNarrativeSummarizer.summarizeCohort(
        new MockLlmClient("{\"invalid\":true}"), log);
    assertTrue(summary.contains("1 paciente"));
    assertTrue(summary.contains("2 correção"));
  }
}
