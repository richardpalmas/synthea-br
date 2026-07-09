package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.plausibility.rules.Plaus001TreatmentWithoutDiagnosis;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.geography.Location;

/**
 * Unit tests for {@code PLAUS-001} using manual HealthRecord fixtures.
 */
public class Plaus001Test {

  private static final String BREAST_CANCER_CODE = "254837009";
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
  public void testViolationWhenTreatmentWithoutPriorDiagnosis() {
    Person person = buildPerson("p-001");
    long treatmentTime = 10 * DAY;

    person.record.encounterStart(treatmentTime, EncounterType.INPATIENT);
    HealthRecord.Procedure procedure = person.record.procedure(treatmentTime, MASTECTOMY_CODE);
    procedure.codes.add(new Code("SNOMED-CT", MASTECTOMY_CODE,
        "Excision of lesion of breast (procedure)"));

    PlausibilityRule rule = new Plaus001TreatmentWithoutDiagnosis(new PlausibilityCatalogLoader());
    List<Violation> violations = rule.evaluate(person);

    assertEquals(1, violations.size());
    Violation violation = violations.get(0);
    assertEquals("PLAUS-001", violation.getRuleId());
    assertEquals("alta", violation.getSeverity());
    assertEquals("p-001", violation.getPatientId());
    assertTrue(violation.getDescription().contains("Tratamento sem diagnóstico"));
    assertEquals(Long.valueOf(treatmentTime), violation.getEventTimestamps().get("procedureStart"));
  }

  @Test
  public void testNoViolationWhenDiagnosisPrecedesTreatment() {
    Person person = buildPerson("p-002");
    long diagnosisTime = 5 * DAY;
    long treatmentTime = 10 * DAY;

    Encounter diagnosisEncounter =
        person.record.encounterStart(diagnosisTime, EncounterType.OUTPATIENT);
    person.record.conditionStart(diagnosisTime, BREAST_CANCER_CODE);
    diagnosisEncounter.conditions.get(0).codes.add(new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)"));

    person.record.encounterStart(treatmentTime, EncounterType.INPATIENT);
    HealthRecord.Procedure procedure = person.record.procedure(treatmentTime, MASTECTOMY_CODE);
    procedure.codes.add(new Code("SNOMED-CT", MASTECTOMY_CODE,
        "Excision of lesion of breast (procedure)"));

    PlausibilityRule rule = new Plaus001TreatmentWithoutDiagnosis(new PlausibilityCatalogLoader());
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
