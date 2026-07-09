package org.mitre.synthea.br.export;

import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mitre.synthea.FailedExportHelper;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.coding.BrCodeMapper;
import org.mitre.synthea.br.demographics.BrDemographicsLoader;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.br.terminology.BrTerminologyResolver;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.engine.State;
import org.mitre.synthea.export.FhirR4;
import org.mitre.synthea.export.FhirR4TestSupport;
import org.mitre.synthea.export.ValidationResources;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;

/**
 * FHIR R4 integration/regression tests for BR cohort exports (Story 5.1 AC #2).
 */
public class BrFHIRR4ExporterTest {

  private static final int PILOT_POPULATION = 15;
  private static final long FIXED_SEED = 51001L;
  private static final long FIXED_REFERENCE_TIME = 1_600_000_000_000L;

  private static boolean physStateEnabled;

  /**
   * Enable physiology state and baseline payer configuration for exporter tests.
   */
  @BeforeClass
  public static void setupClass() {
    physStateEnabled = State.ENABLE_PHYSIOLOGY_STATE;
    State.ENABLE_PHYSIOLOGY_STATE = true;
    PayerManager.clear();
    PayerManager.loadNoInsurance();
  }

  /**
   * Restore physiology state after all tests in this class complete.
   */
  @org.junit.AfterClass
  public static void tearDownClass() {
    State.ENABLE_PHYSIOLOGY_STATE = physStateEnabled;
  }

  /**
   * Load test configuration before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    BrCodeMapper.resetCacheForTest();
    BrTerminologyResolver.resetCacheForTest();
    BrDemographicsLoader.resetCacheForTest();
    Provider.clear();
    PayerManager.clear();
    PayerManager.loadNoInsurance();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    Config.set("exporter.fhir.use_us_core_ig", "false");
    FhirR4TestSupport.configureBaseR4Export();
  }

  /**
   * Reset BR configuration so it cannot leak into other test classes.
   */
  @After
  public void tearDown() {
    Config.remove("br.target_condition");
    Config.remove("br.profile");
    Config.set("br.profile", "");
    BrCodeMapper.resetCacheForTest();
    BrTerminologyResolver.resetCacheForTest();
    BrDemographicsLoader.resetCacheForTest();
  }

  @Test
  public void testBrCohortFhirR4ExportPassesBaseValidation() throws Exception {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.profile", "br");
    assertTrue(BrProfile.isActive());

    ValidationResources validator = ValidationResources.forR4(null);
    FhirContext ctx = FhirR4.getContext();
    IParser parser = ctx.newJsonParser().setPrettyPrint(true);

    GeneratorOptions options = new GeneratorOptions();
    options.population = PILOT_POPULATION;
    options.seed = FIXED_SEED;
    options.clinicianSeed = FIXED_SEED + 1L;
    options.gender = "F";
    options.minAge = 40;
    options.maxAge = 75;
    options.ageSpecified = true;
    options.referenceTime = FIXED_REFERENCE_TIME;
    options.endTime = FIXED_REFERENCE_TIME;

    Generator generator = new Generator(options);
    generator.options.overflow = false;

    List<String> errors = new ArrayList<>();
    for (int i = 0; i < PILOT_POPULATION; i++) {
      Person person = generator.generatePerson(i, FIXED_SEED + i);
      errors.addAll(validateExportedBundle(person, validator, parser));
    }

    assertTrue("Validation of BR cohort FHIR R4 export failed: "
        + String.join("|", errors), errors.isEmpty());
  }

  private static List<String> validateExportedBundle(Person person, ValidationResources validator,
      IParser parser) throws Exception {
    List<String> validationErrors = new ArrayList<>();

    String fhirJson = FhirR4.convertToFHIRJson(person, FIXED_REFERENCE_TIME);

    if (fhirJson.contains("SNOMED-CT")) {
      validationErrors.add(
          "JSON contains unconverted references to 'SNOMED-CT' (should be URIs)");
    }

    Bundle bundle = parser.parseResource(Bundle.class, fhirJson);
    for (BundleEntryComponent entry : bundle.getEntry()) {
      ValidationResult eresult = validator.validateR4(entry.getResource());
      if (!eresult.isSuccessful()) {
        for (SingleValidationMessage emessage : eresult.getMessages()) {
          if (emessage.getSeverity() != ResultSeverityEnum.INFORMATION
              && emessage.getSeverity() != ResultSeverityEnum.WARNING) {
            System.out.println(parser.encodeResourceToString(entry.getResource()));
            System.out.println("ERROR: " + emessage.getMessage());
            validationErrors.add(emessage.getMessage());
          }
        }
      }
    }

    List<String> allFullUrls = bundle.getEntry().stream()
        .map(BundleEntryComponent::getFullUrl)
        .collect(Collectors.toList());
    Set<String> uniqueFullUrls = new HashSet<>(allFullUrls);

    if (allFullUrls.size() != uniqueFullUrls.size()) {
      Map<String, List<BundleEntryComponent>> entriesByUrl = bundle.getEntry().stream()
          .collect(Collectors.groupingBy(BundleEntryComponent::getFullUrl));

      entriesByUrl.values().forEach(entryList -> {
        if (entryList.size() > 1) {
          String fullUrl = entryList.get(0).getFullUrl();
          String resourceTypes = entryList.stream()
              .map(e -> e.getResource().getResourceType().toString())
              .collect(Collectors.joining(" , "));
          validationErrors.add("Found bundle entries with duplicate fullURL: " + fullUrl
              + " - Types: " + resourceTypes);
        }
      });
    }

    if (!validationErrors.isEmpty()) {
      FailedExportHelper.dumpInfo("BrFHIRR4", fhirJson, validationErrors, person);
    }
    return validationErrors;
  }
}
