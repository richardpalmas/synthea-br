package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.plausibility.rules.Plaus003MedicationDiagnosisCompatibility;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.geography.Location;

/**
 * Unit tests for {@code PLAUS-003} using manual HealthRecord fixtures.
 */
public class Plaus003Test {

  private static final String BREAST_CANCER_CODE = "254837009";
  private static final String DOXORUBICIN_CODE = "1790099";
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
  public void testViolationWhenChemotherapyWithoutDiagnosis() {
    Person person = buildPerson("p-020");
    long medicationTime = 8 * DAY;

    person.record.encounterStart(medicationTime, EncounterType.OUTPATIENT);
    Medication medication = person.record.medicationStart(medicationTime, DOXORUBICIN_CODE, false);
    medication.codes.add(new Code("RxNorm", DOXORUBICIN_CODE,
        "10 ML Doxorubicin Hydrochloride 2 MG/ML Injection"));

    PlausibilityRule rule =
        new Plaus003MedicationDiagnosisCompatibility(new PlausibilityCatalogLoader());
    List<Violation> violations = rule.evaluate(person);

    assertEquals(1, violations.size());
    Violation violation = violations.get(0);
    assertEquals("PLAUS-003", violation.getRuleId());
    assertEquals("média", violation.getSeverity());
    assertTrue(violation.getDescription().contains("quimioterapia"));
  }

  @Test
  public void testNoViolationWhenDiagnosisPresent() {
    Person person = buildPerson("p-021");
    long diagnosisTime = 5 * DAY;
    long medicationTime = 8 * DAY;

    Encounter diagnosisEncounter =
        person.record.encounterStart(diagnosisTime, EncounterType.OUTPATIENT);
    person.record.conditionStart(diagnosisTime, BREAST_CANCER_CODE);
    diagnosisEncounter.conditions.get(0).codes.add(new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)"));

    person.record.encounterStart(medicationTime, EncounterType.OUTPATIENT);
    Medication medication = person.record.medicationStart(medicationTime, DOXORUBICIN_CODE, false);
    medication.codes.add(new Code("RxNorm", DOXORUBICIN_CODE,
        "10 ML Doxorubicin Hydrochloride 2 MG/ML Injection"));

    PlausibilityRule rule =
        new Plaus003MedicationDiagnosisCompatibility(new PlausibilityCatalogLoader());
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
