package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

/**
 * Regression tests for encounter phase classification precedence.
 */
public class PathwayEventClassifierTest {

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    Config.set("br.target_condition", "breast_cancer");
    PayerManager.clear();
    PayerManager.loadNoInsurance();
  }

  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    PayerManager.clear();
  }

  @Test
  public void classifyEncounter_prefersEncounterCodeOverReason() {
    PathwayEventClassifier classifier = PathwayEventClassifier.forConfiguredCatalog();
    Encounter encounter = encounterWithReasonAndCode("254837009", "439740005");

    assertEquals("follow_up", classifier.classifyEncounter(encounter));
  }

  @Test
  public void classifyEncounter_ignoresTargetConditionReasonWithoutEncounterCode() {
    PathwayEventClassifier classifier = PathwayEventClassifier.forConfiguredCatalog();
    Encounter encounter = encounterWithReasonOnly("254837009");

    assertEquals(null, classifier.classifyEncounter(encounter));
  }

  private static Encounter encounterWithReasonOnly(String reasonCode) {
    Person person = new Person(102L);
    person.attributes.put(Person.BIRTHDATE, 0L);
    person.coverage.setPlanToNoInsurance(0L);
    Encounter encounter = person.record.encounterStart(1L, EncounterType.OUTPATIENT);
    encounter.reason = new Code("SNOMED-CT", reasonCode, "Reason");
    encounter.codes.clear();
    return encounter;
  }

  private static Encounter encounterWithReasonAndCode(String reasonCode, String encounterCode) {
    Person person = new Person(101L);
    person.attributes.put(Person.BIRTHDATE, 0L);
    person.coverage.setPlanToNoInsurance(0L);
    Encounter encounter = person.record.encounterStart(1L, EncounterType.OUTPATIENT);
    encounter.reason = new Code("SNOMED-CT", reasonCode, "Reason");
    encounter.codes.clear();
    encounter.codes.add(new Code("SNOMED-CT", encounterCode, "Encounter Code"));
    return encounter;
  }
}

