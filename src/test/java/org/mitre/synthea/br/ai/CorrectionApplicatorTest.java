package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

/**
 * Unit tests for {@link CorrectionApplicator}.
 */
public class CorrectionApplicatorTest {

  @Test
  public void testSetPersonAttribute() {
    Person person = newPerson();
    Map<String, Object> op = new HashMap<>();
    op.put("op", "set_person_attribute");
    op.put("key", "CITY");
    op.put("value", "Curitiba");

    List<Map<String, Object>> audit = CorrectionApplicator.apply(
        person, new CorrectionProposal(Arrays.asList(op)));

    assertEquals("applied", audit.get(0).get("status"));
    assertEquals("Curitiba", person.attributes.get("CITY"));
  }

  @Test
  public void testUnsupportedOpSkipped() {
    Person person = newPerson();
    Map<String, Object> op = new HashMap<>();
    op.put("op", "unknown_op");

    List<Map<String, Object>> audit = CorrectionApplicator.apply(
        person, new CorrectionProposal(Arrays.asList(op)));

    assertEquals("skipped", audit.get(0).get("status"));
  }

  @Test
  public void testFlagUnfixableRequiresReason() {
    Person person = newPerson();
    Map<String, Object> op = new HashMap<>();
    op.put("op", "flag_unfixable");

    List<Map<String, Object>> audit = CorrectionApplicator.apply(
        person, new CorrectionProposal(Arrays.asList(op)));

    assertEquals("skipped", audit.get(0).get("status"));
    assertEquals("missing_reason", audit.get(0).get("reason"));
  }

  @Test
  public void testFlagUnfixable() {
    Person person = newPerson();
    Map<String, Object> op = new HashMap<>();
    op.put("op", "flag_unfixable");
    op.put("severity", "medium");
    op.put("reason", "sequência temporal irreconciliável");

    List<Map<String, Object>> audit = CorrectionApplicator.apply(
        person, new CorrectionProposal(Arrays.asList(op)));

    assertEquals("flagged", audit.get(0).get("status"));
  }

  private static Person newPerson() {
    Person person = new Person(1L);
    person.attributes.put(Person.ID, "corr-test");
    person.record = new HealthRecord(person);
    return person;
  }
}
