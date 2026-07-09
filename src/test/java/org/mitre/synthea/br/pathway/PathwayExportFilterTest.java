package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.geography.Location;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

/**
 * Unit tests for {@link PathwayExportFilter} using manual {@link Person} fixtures.
 */
public class PathwayExportFilterTest {

  private static final String BREAST_CANCER_CODE = "254837009";
  private static final String DENTAL_CODE = "225358003";
  private static final long TIME = 1_600_000_000_000L;

  private Provider provider;

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    provider = TestHelper.buildMockProvider();
    PayerManager.loadPayers(new Location("Massachusetts", null));
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.pathway.focus", "true");
  }

  /**
   * Reset pathway configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.pathway.focus");
  }

  @Test
  public void filterForExport_retainsAllowlistedConditionAndRemovesNoise() {
    Person original = buildPerson();
    long encounterTime = (long) original.attributes.get(Person.BIRTHDATE) + 86_400_000L;
    Encounter encounter = original.record.encounterStart(encounterTime, EncounterType.OUTPATIENT);

    original.record.conditionStart(encounterTime, BREAST_CANCER_CODE);
    encounter.conditions.get(0).codes.add(new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)"));

    original.record.conditionStart(encounterTime, DENTAL_CODE);
    encounter.conditions.get(1).codes.add(new Code("SNOMED-CT", DENTAL_CODE,
        "Dental caries (disorder)"));

    int originalConditionCount = countConditions(original);
    Person filtered = PathwayExportFilter.filterForExport(original);

    assertEquals(2, originalConditionCount);
    assertEquals(2, countConditions(original));
    assertEquals(1, countConditions(filtered));
    assertEquals(BREAST_CANCER_CODE, filtered.record.encounters.get(0).conditions.get(0).codes.get(0).code);
  }

  @Test
  public void filterForExport_sameSeedAndConfig_producesEquivalentStructure() {
    Person firstOriginal = buildPersonWithBreastCancerAndDentalNoise();
    Person secondOriginal = buildPersonWithBreastCancerAndDentalNoise();

    Person firstFiltered = PathwayExportFilter.filterForExport(firstOriginal);
    Person secondFiltered = PathwayExportFilter.filterForExport(secondOriginal);

    assertEquals(countConditions(firstFiltered), countConditions(secondFiltered));
    assertEquals(countProcedures(firstFiltered), countProcedures(secondFiltered));
  }

  @Test
  public void filterForExport_focusWithoutTargetCondition_throwsClearError() {
    Config.remove("br.target_condition");
    Person person = buildPersonWithBreastCancerAndDentalNoise();
    try {
      PathwayExportFilter.filterForExport(person);
      fail("Expected IllegalStateException when br.target_condition is unset");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("br.target_condition"));
    }
  }

  @Test
  public void isEnabled_respectsConfigDefault() {
    Config.remove("br.pathway.focus");
    assertFalse(PathwayExportFilter.isEnabled());
    Config.set("br.pathway.focus", "true");
    assertTrue(PathwayExportFilter.isEnabled());
  }

  private Person buildPersonWithBreastCancerAndDentalNoise() {
    Person person = buildPerson();
    long encounterTime = (long) person.attributes.get(Person.BIRTHDATE) + 86_400_000L;
    Encounter encounter = person.record.encounterStart(encounterTime, EncounterType.OUTPATIENT);
    person.record.conditionStart(encounterTime, BREAST_CANCER_CODE);
    encounter.conditions.get(0).codes.add(new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)"));
    person.record.conditionStart(encounterTime, DENTAL_CODE);
    encounter.conditions.get(1).codes.add(new Code("SNOMED-CT", DENTAL_CODE,
        "Dental caries (disorder)"));
    return person;
  }

  private Person buildPerson() {
    Person person = new Person(42L);
    long birthdate = TIME - 40L * 365L * 86_400_000L;
    person.attributes.put(Person.BIRTHDATE, birthdate);
    person.attributes.put(Person.INCOME, 100_000);
    person.coverage.setPlanToNoInsurance(birthdate);
    for (EncounterType type : EncounterType.values()) {
      person.setProvider(type, provider);
    }
    return person;
  }

  private static int countConditions(Person person) {
    int total = 0;
    for (Encounter encounter : person.record.encounters) {
      total += encounter.conditions.size();
    }
    return total;
  }

  private static int countProcedures(Person person) {
    int total = 0;
    for (Encounter encounter : person.record.encounters) {
      total += encounter.procedures.size();
    }
    return total;
  }
}
