package org.mitre.synthea.br.coding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.export.ExportHelper;
import org.mitre.synthea.export.FhirR4;
import org.mitre.synthea.export.ValidationResources;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Clinician;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.ClinicianSpecialty;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

/**
 * FHIR R4 export integration tests for CID-10 BR condition codings (Story 3.3 AC #3, #4).
 */
public class BrCid10ExportIntegrationTest {

  private static final String BREAST_CANCER_SNOMED = "254837009";
  private static final String SNOMED_URI = "http://snomed.info/sct";
  private static final String CID10_URI = ExportHelper.getSystemURI("CID-10");
  private static final String ICD10_CM_URI = ExportHelper.getSystemURI("ICD10-CM");

  private String previousUseUsCore;

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    BrCodeMapper.resetCacheForTest();
    Config.set("br.profile", "");
    PayerManager.clear();
    PayerManager.loadNoInsurance();
    previousUseUsCore = Config.get("exporter.fhir.use_us_core_ig");
    Config.set("exporter.fhir.use_us_core_ig", "false");
  }

  /**
   * Reset BR profile and export settings after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.profile", "");
    BrCodeMapper.resetCacheForTest();
    if (previousUseUsCore != null) {
      Config.set("exporter.fhir.use_us_core_ig", previousUseUsCore);
    }
  }

  @Test
  public void testBrProfileAddsSnomedAndCid10Codings() throws Exception {
    Config.set("br.profile", "br");
    assertTrue(BrProfile.isActive());

    Person person = buildPersonWithBreastCancerCondition();
    Bundle bundle = FhirR4.convertToFHIR(person, 1_600_000_000_000L);
    Condition condition = findBreastCancerCondition(bundle);

    CodeableConcept code = condition.getCode();
    assertTrue(hasCoding(code, SNOMED_URI, BREAST_CANCER_SNOMED));
    assertTrue(hasCoding(code, CID10_URI, "C50.9"));
    assertFalse(hasCodingWithSystem(code, ICD10_CM_URI));

    validateCondition(condition);
  }

  @Test
  public void testInactiveProfileExportsSnomedOnly() throws Exception {
    Config.set("br.profile", "");
    assertFalse(BrProfile.isActive());

    Person person = buildPersonWithBreastCancerCondition();
    Bundle bundle = FhirR4.convertToFHIR(person, 1_600_000_000_000L);
    Condition condition = findBreastCancerCondition(bundle);

    CodeableConcept code = condition.getCode();
    assertTrue(hasCoding(code, SNOMED_URI, BREAST_CANCER_SNOMED));
    assertFalse(hasCoding(code, CID10_URI, "C50.9"));
    assertEquals(1, code.getCoding().size());
  }

  private static Person buildPersonWithBreastCancerCondition() {
    Person person = new Person(0L);
    person.attributes.put(Person.RACE, "white");
    person.attributes.put(Person.ETHNICITY, "nonhispanic");
    person.attributes.put(Person.FIRST_LANGUAGE, "english");
    person.attributes.put(Person.BIRTHDATE, 0L);
    person.attributes.put(Person.GENDER, "F");
    person.coverage.setPlanToNoInsurance(0L);

    Provider stub = new Provider();
    stub.name = "Fake Provider";
    stub.npi = "0";
    Clinician doc = new Clinician(0, person, 0, stub);
    ArrayList<Clinician> docs = new ArrayList<>();
    docs.add(doc);
    stub.clinicianMap.put(ClinicianSpecialty.GENERAL_PRACTICE, docs);
    person.setProvider(EncounterType.AMBULATORY, stub);
    person.setProvider(EncounterType.WELLNESS, stub);
    person.setProvider(EncounterType.EMERGENCY, stub);
    person.setProvider(EncounterType.INPATIENT, stub);
    person.record.provider = stub;

    person.record.encounterStart(0L, EncounterType.WELLNESS);
    person.record.conditionStart(0L, BREAST_CANCER_SNOMED)
        .codes.add(new Code("SNOMED-CT", BREAST_CANCER_SNOMED,
            "Malignant neoplasm of breast (disorder)"));
    return person;
  }

  private static Condition findBreastCancerCondition(Bundle bundle) {
    for (BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource() instanceof Condition) {
        Condition condition = (Condition) entry.getResource();
        if (hasCoding(condition.getCode(), SNOMED_URI, BREAST_CANCER_SNOMED)) {
          return condition;
        }
      }
    }
    throw new AssertionError("Breast cancer Condition not found in FHIR bundle");
  }

  private static boolean hasCoding(CodeableConcept concept, String system, String code) {
    for (Coding coding : concept.getCoding()) {
      if (system.equals(coding.getSystem()) && code.equals(coding.getCode())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasCodingWithSystem(CodeableConcept concept, String system) {
    for (Coding coding : concept.getCoding()) {
      if (system.equals(coding.getSystem())) {
        return true;
      }
    }
    return false;
  }

  private static void validateCondition(Condition condition) {
    ValidationResources validator = ValidationResources.forR4(null);
    ValidationResult result = validator.validateR4(condition);
    List<String> errors = new ArrayList<>();
    for (SingleValidationMessage message : result.getMessages()) {
      if (message.getSeverity() == ResultSeverityEnum.ERROR
          || message.getSeverity() == ResultSeverityEnum.FATAL) {
        errors.add(message.getMessage());
      }
    }
    if (!errors.isEmpty()) {
      FhirContext ctx = FhirR4.getContext();
      IParser parser = ctx.newJsonParser().setPrettyPrint(true);
      System.out.println(parser.encodeResourceToString(condition));
    }
    assertTrue("HAPI validation failed: " + String.join("; ", errors), errors.isEmpty());
  }
}
