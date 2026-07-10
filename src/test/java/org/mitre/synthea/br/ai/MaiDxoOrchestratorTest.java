package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;

/**
 * Unit tests for {@link MaiDxoOrchestrator}.
 */
public class MaiDxoOrchestratorTest {

  @Test
  public void testOrchestratorAppliesCorrectionAndFinalizes() {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"AskQuestion\",\"query\":\"idade do paciente\"}",
        "{\"action\":\"RequestRecordSlice\",\"query\":\"exames laboratoriais\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"municipio\"}",
        "{\"action\":\"ProposeCorrection\",\"operations\":["
            + "{\"op\":\"set_person_attribute\",\"key\":\"STATE\",\"value\":\"PR\"}"
            + "]}",
        "{\"action\":\"FinalizePatient\"}");

    Person person = new Person(99L);
    person.attributes.put(Person.ID, "orch-test");
    person.attributes.put(Person.GENDER, "F");
    person.attributes.put(Person.BIRTHDATE, Utilities.convertTime("years", -55));
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 2, 0, 0);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertEquals("orch-test", result.getPatientId());
    assertTrue(mock.getCallCount() >= 1);
    assertEquals("PR", person.attributes.get("STATE"));
  }

  @Test
  public void testOrchestratorMergesProposalsFromMultiplePersonas() {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"ProposeCorrection\",\"operations\":["
            + "{\"op\":\"set_person_attribute\",\"key\":\"STATE\",\"value\":\"PR\"}"
            + "]}",
        "{\"action\":\"AskQuestion\",\"query\":\"idade\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"municipio\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"exames\"}",
        "{\"action\":\"ProposeCorrection\",\"operations\":["
            + "{\"op\":\"flag_unfixable\",\"reason\":\"sequência temporal irreconciliável\"}"
            + "]}",
        "{\"action\":\"FinalizePatient\"}");

    Person person = new Person(42L);
    person.attributes.put(Person.ID, "merge-test");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 0, 0);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertEquals("PR", person.attributes.get("STATE"));
    assertEquals(1, result.getAppliedOperations().size());
    assertEquals(1, result.getFlags().size());
    assertEquals("sequência temporal irreconciliável", result.getFlags().get(0).get("reason"));
  }

  @Test
  public void testOrchestratorRespectsMaxIterations() {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"AskQuestion\",\"query\":\"idade\"}");

    Person person = new Person(1L);
    person.attributes.put(Person.ID, "iter-test");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 0, 0);
    orchestrator.enrichPatient(person);

    assertTrue(mock.getCallCount() <= 5);
  }

  @Test
  public void testMalformedJsonSkipsTurnWithExplicitLog() {
    // First persona: garbage → fallback fails → skip with json_parse_failed
    // Remaining personas: valid AskQuestion (no Finalize needed)
    MockLlmClient mock = new MockLlmClient(
        "NOT_JSON",
        "still-bad",
        "{\"action\":\"AskQuestion\",\"query\":\"idade\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"sexo\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"cidade\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"exame\"}");

    Person person = new Person(7L);
    person.attributes.put(Person.ID, "parse-fail");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 1, 0);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertTrue(result.getDebateLog().contains("ERRO: json_parse_failed"));
    assertEquals(1, result.getRobustnessStats().getPersonaTurnsSkipped());
    assertEquals(1, result.getRobustnessStats().getJsonParseRetries());
  }

  @Test
  public void testTruncationContinuationBeforeParse() {
    // Incomplete JSON + truncation marker; continuation closes the object.
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"FinalizePatient\" For brevity I have stopped",
        "}");

    Person person = new Person(8L);
    person.attributes.put(Person.ID, "trunc-test");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 0, 1);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertTrue(result.isFinalized());
    assertEquals(1, result.getRobustnessStats().getTruncationContinuations());
    assertTrue(mock.getCallCount() >= 2);
  }

  @Test
  public void testFallbackRecoversMalformedThenFinalizes() {
    MockLlmClient mock = new MockLlmClient(
        "broken output",
        "{\"action\":\"FinalizePatient\"}");

    Person person = new Person(9L);
    person.attributes.put(Person.ID, "fallback-ok");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 1, 0);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertTrue(result.isFinalized());
    assertEquals(1, result.getRobustnessStats().getJsonParseRetries());
    assertEquals(0, result.getRobustnessStats().getPersonaTurnsSkipped());
  }

  @Test
  public void testNullActionSkipsTurnWithoutAbortingPatient() {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":null}",
        "{\"action\":\"AskQuestion\",\"query\":\"idade\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"sexo\"}",
        "{\"action\":\"AskQuestion\",\"query\":\"cidade\"}",
        "{\"action\":\"FinalizePatient\"}");

    Person person = new Person(11L);
    person.attributes.put(Person.ID, "null-action");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 0, 0);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertTrue(result.getDebateLog().contains("ERRO: json_parse_failed"));
    assertEquals(1, result.getRobustnessStats().getPersonaTurnsSkipped());
    assertTrue(result.isFinalized());
  }

  @Test
  public void testValidJsonWithMarkerPhraseDoesNotWasteContinuation() {
    String valid = "{\"action\":\"FinalizePatient\","
        + "\"query\":\"deseja que eu continue?\"}";
    MockLlmClient mock = new MockLlmClient(valid);

    Person person = new Person(12L);
    person.attributes.put(Person.ID, "false-positive");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1, 0, 1);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertTrue(result.isFinalized());
    assertEquals(0, result.getRobustnessStats().getTruncationContinuations());
    assertEquals(1, mock.getCallCount());
  }

  @Test
  public void testCohortLogAggregatesRobustnessCounters() {
    CohortEnrichmentLog log = new CohortEnrichmentLog();
    log.setMetadata("openai", "gpt-4o-mini", false);
    log.addRobustnessStats(new RobustnessStats(2, 1, 3));
    assertEquals(2, log.getMetadata().get("json_parse_retries"));
    assertEquals(1, log.getMetadata().get("truncation_continuations"));
    assertEquals(3, log.getMetadata().get("persona_turns_skipped"));
  }
}
