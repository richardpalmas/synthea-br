package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.geography.Location;

/**
 * Tests for {@link PlausibilityCatalog#evaluateAll(Person)} aggregation.
 */
public class PlausibilityCatalogTest {

  private static final String MASTECTOMY_CODE = "392023007";
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
  public void testEvaluateAllAggregatesMultipleViolations() {
    Person person = buildPerson("p-100");
    long treatmentTime = 10 * DAY;

    person.record.encounterStart(treatmentTime, EncounterType.INPATIENT);
    HealthRecord.Procedure procedure = person.record.procedure(treatmentTime, MASTECTOMY_CODE);
    procedure.codes.add(new Code("SNOMED-CT", MASTECTOMY_CODE,
        "Excision of lesion of breast (procedure)"));

    Medication medication = person.record.medicationStart(treatmentTime, DOXORUBICIN_CODE, false);
    medication.codes.add(new Code("RxNorm", DOXORUBICIN_CODE,
        "10 ML Doxorubicin Hydrochloride 2 MG/ML Injection"));

    PlausibilityCatalog catalog = new PlausibilityCatalog();
    List<Violation> violations = catalog.evaluateAll(person);

    assertTrue(violations.size() >= 2);
    assertTrue(violations.stream().anyMatch(v -> "PLAUS-001".equals(v.getRuleId())));
    assertTrue(violations.stream().anyMatch(v -> "PLAUS-003".equals(v.getRuleId())));
  }

  @Test
  public void testCatalogLoadsThreePilotRules() {
    PlausibilityCatalog catalog = new PlausibilityCatalog();
    assertEquals(3, catalog.getRules().size());
    assertEquals("1.0.0", catalog.getVersion());
    assertEquals("alta", catalog.getRuleMetadata("PLAUS-001").getSeverity());
    assertEquals("média", catalog.getRuleMetadata("PLAUS-002").getSeverity());
    assertEquals("média", catalog.getRuleMetadata("PLAUS-003").getSeverity());
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
