package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.plausibility.rules.Plaus002TreatmentBeforeDiagnosticExam;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.geography.Location;

/**
 * Unit tests for {@code PLAUS-002} using manual HealthRecord fixtures.
 */
public class Plaus002Test {

  private static final String MAMMOGRAM_CODE = "241055006";
  private static final String MASTECTOMY_CODE = "392023007";
  private static final long DAY = 86_400_000L;

  private Provider provider;

  /**
   * Load test properties and payer data.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    provider = TestHelper.buildMockProvider();
    PayerManager.loadPayers(new Location("Massachusetts", null));
  }

  @Test
  public void testViolationWhenTreatmentBeforeDiagnosticExam() {
    Person person = buildPerson("p-010");
    long treatmentTime = 5 * DAY;
    long diagnosticTime = 10 * DAY;

    person.record.encounterStart(treatmentTime, EncounterType.INPATIENT);
    HealthRecord.Procedure treatment = person.record.procedure(treatmentTime, MASTECTOMY_CODE);
    treatment.codes.add(new Code("SNOMED-CT", MASTECTOMY_CODE,
        "Excision of lesion of breast (procedure)"));

    person.record.encounterStart(diagnosticTime, EncounterType.OUTPATIENT);
    HealthRecord.Procedure diagnostic = person.record.procedure(diagnosticTime, MAMMOGRAM_CODE);
    diagnostic.codes.add(new Code("SNOMED-CT", MAMMOGRAM_CODE,
        "Mammogram - symptomatic (procedure)"));

    PlausibilityRule rule =
        new Plaus002TreatmentBeforeDiagnosticExam(new PlausibilityCatalogLoader());
    List<Violation> violations = rule.evaluate(person);

    assertEquals(1, violations.size());
    Violation violation = violations.get(0);
    assertEquals("PLAUS-002", violation.getRuleId());
    assertEquals("média", violation.getSeverity());
    assertEquals(Long.valueOf(treatmentTime), violation.getEventTimestamps().get("treatmentStart"));
    assertEquals(Long.valueOf(diagnosticTime),
        violation.getEventTimestamps().get("diagnosticStart"));
  }

  @Test
  public void testNoViolationWhenDiagnosticPrecedesTreatment() {
    Person person = buildPerson("p-011");
    long diagnosticTime = 5 * DAY;
    long treatmentTime = 10 * DAY;

    person.record.encounterStart(diagnosticTime, EncounterType.OUTPATIENT);
    HealthRecord.Procedure diagnostic = person.record.procedure(diagnosticTime, MAMMOGRAM_CODE);
    diagnostic.codes.add(new Code("SNOMED-CT", MAMMOGRAM_CODE,
        "Mammogram - symptomatic (procedure)"));

    person.record.encounterStart(treatmentTime, EncounterType.INPATIENT);
    HealthRecord.Procedure treatment = person.record.procedure(treatmentTime, MASTECTOMY_CODE);
    treatment.codes.add(new Code("SNOMED-CT", MASTECTOMY_CODE,
        "Excision of lesion of breast (procedure)"));

    PlausibilityRule rule =
        new Plaus002TreatmentBeforeDiagnosticExam(new PlausibilityCatalogLoader());
    List<Violation> violations = rule.evaluate(person);

    assertTrue(violations.isEmpty());
  }

  private Person buildPerson(String id) {
    Person person = new Person(0L);
    person.attributes.put(Person.ID, id);
    person.attributes.put(Person.BIRTHDATE, 0L);
    person.coverage.setPlanToNoInsurance(0L);
    for (EncounterType type : EncounterType.values()) {
      person.setProvider(type, provider);
    }
    return person;
  }
}
