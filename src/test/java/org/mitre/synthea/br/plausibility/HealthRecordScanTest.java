package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.geography.Location;

/**
 * Unit tests for {@link HealthRecordScan} code-matching behavior.
 */
public class HealthRecordScanTest {

  private Provider provider;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    provider = TestHelper.buildMockProvider();
    PayerManager.loadPayers(new Location("Massachusetts", null));
  }

  @Test
  public void testMatchesCodeUsesSystemAndCodeWhenCodesPresent() {
    Person person = buildPerson();
    long eventTime = 10_000L;
    person.record.encounterStart(eventTime, EncounterType.OUTPATIENT);
    Procedure procedure = person.record.procedure(eventTime, "392023007");
    procedure.codes.add(new Code("RxNorm", "392023007", "Wrong system on purpose"));

    ClinicalCode expected = new ClinicalCode("SNOMED-CT", "392023007", "Mastectomy");

    assertFalse(HealthRecordScan.matchesCode(procedure, expected));
  }

  @Test
  public void testMatchesCodeFallsBackToTypeWhenCodesMissing() {
    Person person = buildPerson();
    long eventTime = 10_000L;
    person.record.encounterStart(eventTime, EncounterType.OUTPATIENT);
    Procedure procedure = person.record.procedure(eventTime, "392023007");

    ClinicalCode expected = new ClinicalCode("SNOMED-CT", "392023007", "Mastectomy");

    assertTrue(HealthRecordScan.matchesCode(procedure, expected));
  }

  private Person buildPerson() {
    Person person = new Person(0L);
    person.attributes.put(Person.BIRTHDATE, 0L);
    person.coverage.setPlanToNoInsurance(0L);
    for (EncounterType type : EncounterType.values()) {
      person.setProvider(type, provider);
    }
    return person;
  }
}
