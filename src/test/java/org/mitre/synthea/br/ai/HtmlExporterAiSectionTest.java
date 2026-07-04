package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter;
import org.mitre.synthea.export.HtmlExporter;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Verifies AI enrichment narrative sections in exported HTML.
 */
public class HtmlExporterAiSectionTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @After
  public void cleanup() {
    HtmlExporter.getInstance().reset();
    AiEnrichmentService.setLastLogForTest(null);
  }

  @Test
  public void htmlExportIncludesAiEnrichmentSectionWhenLogPresent() throws Exception {
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");

    CohortEnrichmentLog log = new CohortEnrichmentLog();
    log.setMetadata("openai", "gpt-4o-mini", false);
    log.addPatient(new PatientEnrichmentResult(
        "patient-abc",
        java.util.Collections.emptyList(),
        java.util.Collections.emptyList(),
        "",
        true,
        "Foi ajustado o perfil regional do paciente."));
    log.setCohortNarrativeSummary(
        "A cohort teve inconsistências de perfil corrigidas pelo painel MAI-DxO.");
    AiEnrichmentService.setLastLogForTest(log);

    Generator generator = new Generator(1);
    generator.options.overflow = false;
    Person person = generator.generatePerson(0);
    person.attributes.put(Person.ID, "patient-abc");
    Exporter.export(person, System.currentTimeMillis());
    HtmlExporter.getInstance().writeIndex();

    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    String html = Files.readString(htmlIndex.toPath(), StandardCharsets.UTF_8);
    assertTrue(html.contains("Dados enriquecidos por IA - Modelo Usado: gpt-4o-mini"));
    assertTrue(html.contains("A cohort teve inconsistências de perfil corrigidas"));
    assertTrue(html.contains("Foi ajustado o perfil regional do paciente."));
  }
}
