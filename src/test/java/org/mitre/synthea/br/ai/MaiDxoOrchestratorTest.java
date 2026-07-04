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

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 2);
    PatientEnrichmentResult result = orchestrator.enrichPatient(person);

    assertEquals("orch-test", result.getPatientId());
    assertTrue(mock.getCallCount() >= 1);
    assertEquals("PR", person.attributes.get("STATE"));
  }

  @Test
  public void testOrchestratorRespectsMaxIterations() {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"AskQuestion\",\"query\":\"idade\"}");

    Person person = new Person(1L);
    person.attributes.put(Person.ID, "iter-test");
    person.record = new HealthRecord(person);

    MaiDxoOrchestrator orchestrator = new MaiDxoOrchestrator(mock, 1);
    orchestrator.enrichPatient(person);

    assertTrue(mock.getCallCount() <= 5);
  }
}
