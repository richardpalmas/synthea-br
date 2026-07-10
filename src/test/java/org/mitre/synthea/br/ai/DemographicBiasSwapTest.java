package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Unit tests for demographic bias swap and report writing (Story 8.2).
 */
public class DemographicBiasSwapTest {

  /**
   * Temporary output directory for bias report writes.
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private String previousBase;

  /**
   * Captures exporter base directory before each test.
   */
  @Before
  public void setUp() {
    previousBase = Config.get("exporter.baseDirectory");
  }

  /**
   * Restores exporter base directory after each test.
   */
  @After
  public void tearDown() {
    if (previousBase == null) {
      Config.remove("exporter.baseDirectory");
    } else {
      Config.set("exporter.baseDirectory", previousBase);
    }
  }

  @Test
  public void testGenderSwapPlanned() {
    Person person = new Person(1L);
    person.attributes.put(Person.GENDER, "F");
    person.attributes.put(Person.RACE_IBGE, "branca");
    person.attributes.put(Person.STATE, "Paraná");

    List<Map<String, String>> plans = DemographicBiasSwapper.planSwaps(person);
    assertFalse(plans.isEmpty());
    boolean hasGender = false;
    for (Map<String, String> plan : plans) {
      if ("gender".equals(plan.get("attribute"))) {
        hasGender = true;
        assertEquals("F", plan.get("from"));
        assertEquals("M", plan.get("to"));
      }
    }
    assertTrue(hasGender);
  }

  @Test
  public void testSwapDoesNotMutatePerson() {
    Person person = new Person(2L);
    person.attributes.put(Person.GENDER, "M");
    person.attributes.put(Person.RACE_IBGE, "preta");
    person.attributes.put(Person.STATE, "São Paulo");

    Map<String, String> attrs = DemographicBiasSwapper.readAttrs(person);
    Map<String, String> swapped = DemographicBiasSwapper.applySwap(attrs, "gender", "F");
    assertEquals("F", swapped.get("gender"));
    assertEquals("M", person.attributes.get(Person.GENDER));
  }

  @Test
  public void testCompareNarrativesFlagsLengthDelta() {
    Map<String, Object> metrics = BiasReportWriter.compareNarratives(
        "curto",
        "texto bem mais longo que o baseline original para forçar delta",
        0.20);
    assertTrue((Boolean) metrics.get("flagged"));
  }

  @Test
  public void testBiasReportWrittenWithoutPhi() throws Exception {
    Config.set("exporter.baseDirectory", folder.getRoot().getAbsolutePath() + "/");
    Map<String, Object> report = BiasReportWriter.newReport(0.20);
    Map<String, Object> patient = new LinkedHashMap<>();
    patient.put("patientId", "synth-001");
    List<Map<String, Object>> swaps = new ArrayList<>();
    Map<String, Object> swap = new LinkedHashMap<>();
    swap.put("attribute", "gender");
    swap.put("from", "F");
    swap.put("to", "M");
    swap.put("flagged", false);
    swaps.add(swap);
    patient.put("swaps", swaps);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> patients =
        (List<Map<String, Object>>) report.get("patients");
    patients.add(patient);

    Path written = BiasReportWriter.write(report);
    assertTrue(Files.exists(written));
    String json = Files.readString(written);
    assertTrue(json.contains("synth-001"));
    assertFalse(json.contains("api_key"));
    assertFalse(json.contains("sk-"));
  }

  @Test
  public void testSanitizePreservesNewlinesForBulletPersona() {
    List<Map<String, Object>> ops = new ArrayList<>();
    Map<String, Object> op = new LinkedHashMap<>();
    op.put("op", "set_person_attribute");
    op.put("key", "STATE");
    op.put("value", "PR");
    ops.add(op);
    PatientEnrichmentResult result = new PatientEnrichmentResult(
        "p-bullets", ops, new ArrayList<>(), "debate", true);
    MockLlmClient mock = new MockLlmClient(
        "- Correção de UF\n- Limitação temporal");
    String summary = AiNarrativeSummarizer.summarizePatient(
        mock, result, NarrativeWritingPersona.BULLET_POINTS);
    assertTrue(summary.contains("\n"));
    assertTrue(summary.contains("- Correção"));
  }

  @Test
  public void testBiasTestDisabledByDefault() {
    assertFalse(AiEnrichmentConfig.isBiasTestEnabled());
  }

  @Test
  public void testSummarizePatientUsesPersonaWithoutMutatingFacts() {
    List<Map<String, Object>> ops = new ArrayList<>();
    Map<String, Object> op = new LinkedHashMap<>();
    op.put("op", "set_person_attribute");
    op.put("key", "STATE");
    op.put("value", "PR");
    ops.add(op);
    PatientEnrichmentResult result = new PatientEnrichmentResult(
        "p1", ops, new ArrayList<>(), "debate", true);

    MockLlmClient mock = new MockLlmClient(
        "Resumo conciso das correções aplicadas ao paciente.");
    String summary = AiNarrativeSummarizer.summarizePatient(
        mock, result, NarrativeWritingPersona.CONCISE,
        DemographicBiasSwapper.buildDemographicBlock(
            Map.of("gender", "F", "race_ibge", "branca", "state", "Paraná")));
    assertTrue(summary.toLowerCase().contains("resumo")
        || summary.toLowerCase().contains("corre"));
    assertEquals(1, mock.getCallCount());
  }
}
