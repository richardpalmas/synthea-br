package org.mitre.synthea.export;

import static org.junit.Assert.assertFalse;
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
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

public class HtmlExporterTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @After
  public void cleanup() {
    HtmlExporter.getInstance().reset();
  }

  @Test
  public void htmlExportDisabledDoesNotWriteFile() throws Exception {
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "false");

    Generator generator = new Generator(1);
    generator.options.overflow = false;
    Person person = generator.generatePerson(0);
    Exporter.export(person, System.currentTimeMillis());

    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertFalse(htmlIndex.exists());
  }

  @Test
  public void htmlExportEnabledWritesValidIndex() throws Exception {
    TestHelper.loadTestProperties();
    Generator.DEFAULT_STATE = Config.get("test_state.default", "Massachusetts");
    File tempOutputFolder = tempFolder.newFolder();
    Config.set("exporter.baseDirectory", tempOutputFolder.toString());
    Config.set("exporter.html.export", "true");

    int patientCount = 3;
    Generator generator = new Generator(patientCount);
    generator.options.overflow = false;
    long stopTime = System.currentTimeMillis();
    for (int i = 0; i < patientCount; i++) {
      TestHelper.exportOff();
      Config.set("exporter.html.export", "true");
      Config.set("exporter.baseDirectory", tempOutputFolder.toString());
      Person person = generator.generatePerson(i);
      Exporter.export(person, stopTime);
    }
    HtmlExporter.getInstance().writeIndex();

    File htmlIndex = tempOutputFolder.toPath().resolve("html").resolve("index.html").toFile();
    assertTrue(htmlIndex.exists());
    String html = Files.readString(htmlIndex.toPath(), StandardCharsets.UTF_8);
    assertTrue(html.contains("<!DOCTYPE html>"));
    assertTrue(html.contains("<details"));
    assertTrue(html.contains("Linha do tempo"));
    assertTrue(html.contains("Demografia"));
    assertTrue(html.contains("Condições"));
    assertTrue(html.contains("Medicamentos"));
    assertTrue(html.contains("Encontros"));
    assertTrue(html.contains("Cobertura"));
    assertTrue(html.contains("Visualizador Narrativo da Cohort"));
  }
}
