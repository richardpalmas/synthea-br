package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;

/**
 * Unit tests for {@link Gatekeeper}.
 */
public class GatekeeperTest {

  @Test
  public void testGatekeeperDoesNotDumpFullRecordOnBroadQuery() {
    Person person = buildPerson();
    Gatekeeper gatekeeper = new Gatekeeper(person);

    String answer = gatekeeper.respond("AskQuestion", "me diga tudo sobre o paciente");
    assertFalse(answer.contains("LOINC"));
    assertTrue(answer.contains("específico") || answer.contains("não encontrada"));
  }

  @Test
  public void testGatekeeperAnswersAgeQuestion() {
    Person person = buildPerson();
    Gatekeeper gatekeeper = new Gatekeeper(person);

    String answer = gatekeeper.respond("AskQuestion", "qual a idade do paciente?");
    assertTrue(answer.contains("anos"));
  }

  @Test
  public void testGatekeeperReturnsObservationsOnSliceRequest() {
    Person person = buildPerson();
    Gatekeeper gatekeeper = new Gatekeeper(person);
    String answer = gatekeeper.respond("RequestRecordSlice", "listar exames laboratoriais");
    assertTrue(answer.contains("Nenhum exame") || answer.contains("Observações"));
  }

  private static Person buildPerson() {
    Person person = new Person(42L);
    person.attributes.put(Person.ID, "test-patient-1");
    person.attributes.put(Person.GENDER, "F");
    long birth = Utilities.convertTime("years", -50);
    person.attributes.put(Person.BIRTHDATE, birth);
    person.record = new HealthRecord(person);
    return person;
  }
}
