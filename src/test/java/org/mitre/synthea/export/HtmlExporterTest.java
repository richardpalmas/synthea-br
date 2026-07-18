package org.mitre.synthea.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.br.coding.BrCodeMapper;
import org.mitre.synthea.br.terminology.BrTerminologyResolver;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.PayerManager;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;

public class HtmlExporterTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private String previousHtmlExport;
  private String previousBaseDirectory;
  private String previousBrProfile;
  private String previousTargetCondition;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    previousHtmlExport = Config.get("exporter.html.export");
    previousBaseDirectory = Config.get("exporter.baseDirectory");
    previousBrProfile = Config.get("br.profile");
    previousTargetCondition = Config.get("br.target_condition");
    HtmlExporter.getInstance().reset();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
  }

  @After
  public void tearDown() {
    HtmlExporter.getInstance().reset();
    restoreConfig("exporter.html.export", previousHtmlExport);
    restoreConfig("exporter.baseDirectory", previousBaseDirectory);
    restoreConfig("br.profile", previousBrProfile);
    restoreConfig("br.target_condition", previousTargetCondition);
    BrCodeMapper.resetCacheForTest();
    BrTerminologyResolver.resetCacheForTest();
  }

  private static void restoreConfig(String key, String previousValue) {
    if (previousValue != null) {
      Config.set(key, previousValue);
    } else {
      Config.remove(key);
    }
  }

  @Test
  public void htmlExportDisabledDoesNotWriteFile() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    TestHelper.exportOff();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "false");

    Generator generator = new Generator(1);
    generator.options.overflow = false;
    Person person = generator.generatePerson(0);
    Exporter.export(person, System.currentTimeMillis());
    Exporter.runPostCompletionExports(generator);

    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertFalse(htmlIndex.exists());
  }

  @Test
  public void htmlExportEnabledWritesValidIndexViaPostCompletion() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    TestHelper.exportOff();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");

    int patientCount = 3;
    Generator generator = new Generator(patientCount);
    generator.options.overflow = false;
    long stopTime = System.currentTimeMillis();
    for (int i = 0; i < patientCount; i++) {
      Person person = generator.generatePerson(i);
      Exporter.export(person, stopTime);
    }
    Exporter.runPostCompletionExports(generator);

    String html = readIndexHtml(tempOutputFolder);
    assertCommonStructure(html, patientCount);
    assertTrue(html.contains("class=\"triage-header\""));
    assertTrue(html.contains(" anos · "));
    assertTrue(html.contains("último:"));
    assertTrue(html.contains("Feminino") || html.contains("Masculino"));
  }

  @Test
  public void htmlExportBrProfileResolvesCid10PilotCondition() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    TestHelper.exportOff();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");
    Config.set("br.profile", "br");
    PayerManager.clear();
    PayerManager.loadNoInsurance();
    BrCodeMapper.resetCacheForTest();
    BrTerminologyResolver.resetCacheForTest();

    Person person = buildPersonWithBreastCancerCondition();
    long stopTime = System.currentTimeMillis();
    Exporter.prepareHtmlCohortExport();
    Exporter.export(person, stopTime);
    HtmlExporter.getInstance().writeIndex();

    String html = readIndexHtml(tempOutputFolder);
    assertTrue(html.contains("Neoplasia maligna da mama"));
    assertTrue(html.contains("Condições"));
  }

  @Ignore("Manual scalability check — single index.html for n≈500; run locally when validating AC7")
  @Test
  public void htmlExportScalabilityCohort500() throws Exception {
    File tempOutputFolder = tempFolder.newFolder();
    TestHelper.exportOff();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");

    int patientCount = 500;
    Generator generator = new Generator(patientCount);
    generator.options.overflow = false;
    long stopTime = System.currentTimeMillis();
    for (int i = 0; i < patientCount; i++) {
      Person person = generator.generatePerson(i);
      Exporter.export(person, stopTime);
    }
    Exporter.runPostCompletionExports(generator);

    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertTrue(htmlIndex.exists());
    assertTrue(htmlIndex.length() > 0);
  }

  private static String readIndexHtml(File tempOutputFolder) throws Exception {
    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertTrue(htmlIndex.exists());
    return Files.readString(htmlIndex.toPath(), StandardCharsets.UTF_8);
  }

  private static void assertCommonStructure(String html, int patientCount) {
    assertTrue(html.contains("<!DOCTYPE html>"));
    assertTrue(html.contains("<details"));
    assertTrue(html.contains("class=\"section-accordion\""));
    int sectionAccordionCount = html.split("class=\"section-accordion\"").length - 1;
    assertTrue("Expected 8 section accordions per patient",
        sectionAccordionCount >= 8 * patientCount);
    assertTrue(html.contains("Linha do tempo"));
    assertTrue(html.contains("Demografia"));
    assertTrue(html.contains("Condições"));
    assertTrue(html.contains("Medicamentos"));
    assertTrue(html.contains("Exames"));
    assertTrue(html.contains("Procedimentos"));
    assertTrue(html.contains("Encontros"));
    assertTrue(html.contains("Cobertura"));
    assertTrue(html.contains("Visualizador Narrativo da Cohort"));
  }

  private static Person buildPersonWithBreastCancerCondition() {
    Person person = new Person(0L);
    person.attributes.put(Person.NAME, "Maria Silva");
    person.attributes.put(Person.GENDER, "F");
    person.attributes.put(Person.BIRTHDATE, TestHelper.timestamp(1975, 3, 15, 0, 0, 0));
    person.attributes.put(Person.ID, "br-cid10-test");
    person.coverage.setPlanToNoInsurance(0L);

    long encounterTime = 0L;
    person.record.encounterStart(encounterTime, EncounterType.WELLNESS);
    person.record.conditionStart(encounterTime, "254837009")
        .codes.add(new Code("SNOMED-CT", "254837009",
            "Malignant neoplasm of breast (disorder)"));
    return person;
  }
}
