package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Generator.GeneratorOptions;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;

/**
 * Integration test for AI enrichment service with mock LLM.
 */
public class AiEnrichmentIntegrationTest {

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Configures an isolated output directory and a mock LLM client before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    TestHelper.exportOff();
    Config.set("br.profile", "br");
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.ai.api_key", "mock-key");
    Config.set("exporter.baseDirectory", tempFolder.newFolder().getAbsolutePath());
    LlmClientFactoryHolder.setClient(new MockLlmClient(
        "{\"action\":\"ProposeCorrection\",\"operations\":["
            + "{\"op\":\"set_person_attribute\",\"key\":\"ENRICHED\",\"value\":true}"
            + "]}",
        "{\"action\":\"FinalizePatient\"}"));
  }

  /**
   * Clears AI enrichment overrides and test doubles after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.ai.enrichment.enabled", "false");
    Config.remove("br.ai.api_key");
    LlmClientFactoryHolder.reset();
    AiEnrichmentProgress.getInstance().resetForTest();
  }

  @Test
  public void testEnrichCohortWritesLog() throws Exception {
    GeneratorOptions options = new GeneratorOptions();
    options.population = 0;
    ExporterRuntimeOptions exportOptions = new ExporterRuntimeOptions();

    Generator generator = new Generator(options, exportOptions);
    Person person = new Person(7L);
    person.attributes.put(Person.ID, "integ-1");
    person.record = new HealthRecord(person);
    generator.recordPerson(person, 0);

    AiEnrichmentService.enrichCohort(generator);

    File logFile = new File(Config.get("exporter.baseDirectory"), "br/ai/enrichment_log.json");
    assertTrue(logFile.exists());
    String content = Files.readString(logFile.toPath());
    assertTrue(content.contains("MAI-DxO"));
    assertTrue(content.contains("integ-1"));
    assertTrue(content.contains("narrativeSummary"));
    assertTrue(content.contains("cohortNarrativeSummary"));
    assertTrue(content.contains("debateLog"));
    assertEquals(Boolean.TRUE, person.attributes.get("ENRICHED"));
  }
}
