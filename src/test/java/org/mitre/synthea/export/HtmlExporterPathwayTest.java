package org.mitre.synthea.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.Rule;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.pathway.PathwayHtmlModeConfig;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

/**
 * Tests for pathway-grouped HTML export (Story 9.4).
 */
public class HtmlExporterPathwayTest {

  private static final String BREAST_CANCER_CODE = "254837009";
  private static final String DENTAL_CODE = "225358003";
  private static final long TIME = 1_600_000_000_000L;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String previousHtmlExport;
  private String previousBaseDirectory;
  private String previousPathwayMode;
  private String previousPathwayFocus;
  private String previousTargetCondition;
  private String previousBrProfile;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    previousHtmlExport = Config.get("exporter.html.export");
    previousBaseDirectory = Config.get("exporter.baseDirectory");
    previousPathwayMode = Config.get("exporter.html.pathway_mode");
    previousPathwayFocus = Config.get("br.pathway.focus");
    previousTargetCondition = Config.get("br.target_condition");
    previousBrProfile = Config.get("br.profile");
    HtmlExporter.getInstance().reset();
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.profile", "br");
    org.mitre.synthea.world.agents.PayerManager.loadPayers(
        new org.mitre.synthea.world.geography.Location("Massachusetts", null));
  }

  @After
  public void tearDown() {
    HtmlExporter.getInstance().reset();
    org.mitre.synthea.world.agents.PayerManager.clear();
    restoreConfig("exporter.html.export", previousHtmlExport);
    restoreConfig("exporter.baseDirectory", previousBaseDirectory);
    restoreConfig("exporter.html.pathway_mode", previousPathwayMode);
    restoreConfig("br.pathway.focus", previousPathwayFocus);
    restoreConfig("br.target_condition", previousTargetCondition);
    restoreConfig("br.profile", previousBrProfile);
  }

  @Test
  public void orientadorMode_groupsByPhaseAndHidesNoise() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("exporter.html.pathway_mode", PathwayHtmlModeConfig.MODE_ORIENTADOR);

    Person person = buildPathwayPerson();
    exportSinglePatient(person, tempOutputFolder);

    String html = readIndexHtml(tempOutputFolder);
    assertTrue(html.contains("class=\"pathway-phase\""));
    assertTrue(html.contains("Rastreio"));
    assertTrue(html.contains("Diagnóstico"));
    assertTrue(html.indexOf("Rastreio") < html.indexOf("Diagnóstico"));
    assertTrue(html.contains("target-badge"));
    assertTrue(html.contains("primary-highlight"));
    assertFalse(html.contains("Dental caries"));
    assertFalse(html.contains("Caries dental"));
  }

  @Test
  public void pesquisadorMode_includesCollapsibleOutOfPathwaySection() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("exporter.html.pathway_mode", PathwayHtmlModeConfig.MODE_PESQUISADOR);

    Person person = buildPathwayPerson();
    exportSinglePatient(person, tempOutputFolder);

    String html = readIndexHtml(tempOutputFolder);
    assertTrue(html.contains("Fora da trajetória"));
    assertTrue(html.contains("out-of-pathway-section"));
    assertTrue(html.contains("Dental caries") || html.contains("Caries dental"));
  }

  @Test
  public void fullMode_usesFlatTimelineWithoutPhaseSections() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("exporter.html.pathway_mode", PathwayHtmlModeConfig.MODE_FULL);

    Person person = buildPathwayPerson();
    exportSinglePatient(person, tempOutputFolder);

    String html = readIndexHtml(tempOutputFolder);
    assertFalse(html.contains("class=\"pathway-phase\""));
    assertTrue(html.contains("Dental caries") || html.contains("Caries dental"));
  }

  @Test
  public void defaultMode_orientadorWhenFocusTrueAndModeAbsent() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("br.pathway.focus", "true");
    Config.remove("exporter.html.pathway_mode");

    Person person = buildPathwayPerson();
    exportSinglePatient(person, tempOutputFolder);

    String html = readIndexHtml(tempOutputFolder);
    assertTrue(html.contains("class=\"pathway-phase\""));
    assertFalse(html.contains("Dental caries") || html.contains("Caries dental"));
    // Orientador hides empty clinical accordion sections (trajectory + demografia only).
    assertFalse(html.contains(">Condições</"));
  }

  @Test
  public void pesquisadorWithFocus_stillShowsOutOfPathwaySection() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("br.pathway.focus", "true");
    Config.set("exporter.html.pathway_mode", PathwayHtmlModeConfig.MODE_PESQUISADOR);

    Person person = buildPathwayPerson();
    exportSinglePatient(person, tempOutputFolder);

    String html = readIndexHtml(tempOutputFolder);
    assertTrue(html.contains("Fora da trajetória"));
    assertTrue(html.contains("Dental caries") || html.contains("Caries dental"));
  }

  @Test
  public void encounterLabel_prefersEncounterCodeOverReason() throws Exception {
    Person person = buildPathwayPerson();
    Encounter encounter = person.record.encounterStart(TIME + 1000L, EncounterType.OUTPATIENT);
    encounter.reason = new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)");
    encounter.codes.clear();
    encounter.codes.add(new Code("SNOMED-CT", "439740005",
        "Postoperative follow-up visit (procedure)"));

    Method method = HtmlExporter.class.getDeclaredMethod("encounterLabel",
        Encounter.class, boolean.class);
    method.setAccessible(true);
    String label = (String) method.invoke(null, encounter, false);

    assertEquals("Postoperative follow-up visit (procedure)", label);
  }

  private static Person buildPathwayPerson() throws Exception {
    Provider provider = TestHelper.buildMockProvider();
    Person person = new Person(42L);
    long birthdate = TIME - 40L * 365L * 86_400_000L;
    person.attributes.put(Person.BIRTHDATE, birthdate);
    person.attributes.put(Person.NAME, "Ana Pathway");
    person.attributes.put(Person.GENDER, "F");
    person.attributes.put(Person.ID, "pathway-html-test");
    person.coverage.setPlanToNoInsurance(birthdate);
    person.coverage.getLastPlanRecord().updateStopTime(TIME + 86_400_000L);
    for (EncounterType type : EncounterType.values()) {
      person.setProvider(type, provider);
    }

    long screeningTime = birthdate + 86_400_000L;
    Encounter screeningEncounter = person.record.encounterStart(screeningTime, EncounterType.WELLNESS);
    person.record.conditionStart(screeningTime, "268547008");
    screeningEncounter.conditions.get(0).codes.add(new Code("SNOMED-CT", "268547008",
        "Screening for malignant neoplasm of breast (procedure)"));

    long diagnosisTime = screeningTime + 86_400_000L;
    Encounter diagnosisEncounter = person.record.encounterStart(diagnosisTime, EncounterType.OUTPATIENT);
    person.record.conditionStart(diagnosisTime, BREAST_CANCER_CODE);
    diagnosisEncounter.conditions.get(0).codes.add(new Code("SNOMED-CT", BREAST_CANCER_CODE,
        "Malignant neoplasm of breast (disorder)"));

    person.record.conditionStart(diagnosisTime, DENTAL_CODE);
    diagnosisEncounter.conditions.get(1).codes.add(new Code("SNOMED-CT", DENTAL_CODE,
        "Dental caries (disorder)"));

    return person;
  }

  private static void exportSinglePatient(Person person, File tempOutputFolder) throws Exception {
    long stopTime = TIME;
    Exporter.prepareHtmlCohortExport();
    Exporter.export(person, stopTime);
    HtmlExporter.getInstance().writeIndex();
  }

  private static String readIndexHtml(File tempOutputFolder) throws Exception {
    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertTrue(htmlIndex.exists());
    return Files.readString(htmlIndex.toPath(), StandardCharsets.UTF_8);
  }

  private static void restoreConfig(String key, String previousValue) {
    if (previousValue != null) {
      Config.set(key, previousValue);
    } else {
      Config.remove(key);
    }
  }
}
