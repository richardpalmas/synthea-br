package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mitre.synthea.export.HtmlExporter.TimelineEvent;

/**
 * Unit tests for Story C.1 HTML timeline collapse helpers.
 */
public class PathwayHtmlModelBuilderTest {

  private static final long DAY = TimeUnit.DAYS.toMillis(1);

  @Test
  public void deduplicatesExactSameCodeTimestampAndType() {
    List<TimelineEvent> events = new ArrayList<>();
    events.add(event(1000L, "Observação", "Estádio clínico", "LOINC|21908-9", "staging"));
    events.add(event(1000L, "Observação", "Estádio clínico", "LOINC|21908-9", "staging"));
    events.add(event(1000L + DAY, "Observação", "Estádio clínico", "LOINC|21908-9", "staging"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
  }

  @Test
  public void researcherPreservesRepeatedClinicalOccurrences() {
    List<TimelineEvent> events = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      events.add(event((10 + i) * DAY, "Procedimento", "Radioterapia",
          "SNOMED-CT|33195004", "treatment"));
    }

    List<TimelineEvent> prepared = PathwayHtmlModelBuilder.prepareTimelineForMode(
        events, PathwayHtmlModeConfig.MODE_PESQUISADOR);
    assertEquals(5, prepared.size());
  }

  @Test
  public void consolidatesMolecularPanel() {
    List<TimelineEvent> events = new ArrayList<>();
    events.add(observation(DAY, "Receptor de estrógeno", "LOINC|85337-4",
        "SNOMED-CT|10828004", "Positivo", "diagnosis"));
    events.add(observation(DAY, "Receptor de progesterona", "LOINC|85339-0",
        "SNOMED-CT|10828004", "Positivo", "diagnosis"));
    events.add(observation(DAY, "HER2", "LOINC|85319-2",
        "SNOMED-CT|260385009", "Negativo", "diagnosis"));
    events.add(observation(DAY, "Painel combinado", "LOINC|10480-2",
        "SNOMED-CT|10828004", "Positivo", "diagnosis"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(1, collapsed.size());
    assertEquals("Perfil molecular — RE Positivo · RP Positivo · HER2 Negativo",
        collapsed.get(0).label);
  }

  @Test
  public void consolidatesTnmAndPreservesAjccStage() {
    List<TimelineEvent> events = new ArrayList<>();
    events.add(observation(DAY, "T", "LOINC|21905-5", "SNOMED-CT|1228929004",
        "cT2", "staging"));
    events.add(observation(DAY, "N", "LOINC|21906-3", "SNOMED-CT|1229973008",
        "cN1", "staging"));
    events.add(observation(DAY, "M", "LOINC|21907-1", "SNOMED-CT|1229901006",
        "cM0", "staging"));
    events.add(observation(2 * DAY, "Estádio IIB", "LOINC|21908-9",
        "SNOMED-CT|1222769001", "Estádio clínico AJCC IIB", "staging"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
    assertTrue(collapsed.stream().anyMatch(e ->
        "TNM clínico — cT2 · cN1 · cM0".equals(e.label)));
    assertTrue(collapsed.stream().anyMatch(e -> "LOINC|21908-9".equals(e.codeKey)));
  }

  @Test
  public void reducesRadiotherapyCourseToStartAndCompletion() {
    List<TimelineEvent> events = new ArrayList<>();
    for (int i = 0; i < 25; i++) {
      events.add(event((10 + i) * DAY, "Procedimento", "Radioterapia de feixe externo",
          "SNOMED-CT|33195004", "treatment"));
    }

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
    assertTrue(collapsed.get(0).label.startsWith("Início da radioterapia"));
    assertTrue(collapsed.get(1).label.contains("25 sessões"));
  }

  @Test
  public void preservesRadiotherapyModalityChangeAsMilestone() {
    List<TimelineEvent> events = new ArrayList<>();
    events.add(event(10 * DAY, "Procedimento", "Feixe", "SNOMED-CT|33195004", "treatment"));
    events.add(event(11 * DAY, "Procedimento", "Feixe", "SNOMED-CT|33195004", "treatment"));
    events.add(event(12 * DAY, "Procedimento", "Hipo", "SNOMED-CT|385798007", "treatment"));
    events.add(event(13 * DAY, "Procedimento", "Hipo", "SNOMED-CT|385798007", "treatment"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(3, collapsed.size());
    assertTrue(collapsed.stream().anyMatch(e -> e.label.startsWith("Mudança de radioterapia")));
  }

  @Test
  public void collapsesChemoCyclesIntoStartAndCompletion() {
    List<TimelineEvent> events = new ArrayList<>();
    events.addAll(chemoDay(10 * DAY, "Cyclophosphamide"));
    events.addAll(chemoDay(30 * DAY, "Cyclophosphamide"));
    events.addAll(chemoDay(50 * DAY, "Cyclophosphamide"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
    assertTrue(collapsed.get(0).label.startsWith("Início da quimioterapia"));
    assertTrue(collapsed.get(1).label.contains("3 ciclos"));
    assertFalse(collapsed.stream().anyMatch(e -> "Encontro".equals(e.type)));
    assertFalse(collapsed.stream().anyMatch(e -> "Medicamento".equals(e.type)));
  }

  @Test
  public void preservesChemoDrugChangeAsMilestone() {
    List<TimelineEvent> events = new ArrayList<>();
    events.addAll(chemoDay(10 * DAY, "Cyclophosphamide", "RxNorm|1734919"));
    events.addAll(chemoDay(30 * DAY, "Paclitaxel", "RxNorm|583214"));
    events.addAll(chemoDay(50 * DAY, "Paclitaxel", "RxNorm|583214"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(3, collapsed.size());
    assertTrue(collapsed.stream().anyMatch(e ->
        e.label.startsWith("Mudança de esquema quimioterápico")));
  }

  @Test
  public void collapsesFollowUpMammographyAndScreeningEncounter() {
    long day = 100 * DAY;
    List<TimelineEvent> events = new ArrayList<>();
    events.add(event(day, "Procedimento", "Mamografia", "SNOMED-CT|71651007", "follow_up"));
    events.add(event(day + 1000, "Encontro", "Vigilância", "SNOMED-CT|410410006", "follow_up"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(1, collapsed.size());
    assertEquals("Mamografia de seguimento", collapsed.get(0).label);
  }

  @Test
  public void hidesGenericProblemEncounterWhenClinicalEventsSameDay() {
    long day = 5 * DAY;
    List<TimelineEvent> events = new ArrayList<>();
    events.add(event(day, "Encontro", "Consulta", "SNOMED-CT|185347001", "treatment"));
    events.add(event(day + 60_000, "Procedimento", "Mastectomia",
        "SNOMED-CT|172138000", "treatment"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(1, collapsed.size());
    assertEquals("Procedimento", collapsed.get(0).type);
  }

  @Test
  public void reducesRepeatedResponseToFirstAndLast() {
    List<TimelineEvent> events = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      events.add(observation((100 + i * 365L) * DAY, "Resposta", "LOINC|88040-1",
          "SNOMED-CT|385633008", "Melhora", "treatment"));
    }

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
    assertTrue(collapsed.get(1).label.contains("4 registros"));
  }

  @Test
  public void preservesResponseValueChange() {
    List<TimelineEvent> events = new ArrayList<>();
    events.add(observation(100 * DAY, "Resposta", "LOINC|88040-1",
        "SNOMED-CT|385633008", "Melhora", "treatment"));
    events.add(observation(200 * DAY, "Resposta", "LOINC|88040-1",
        "SNOMED-CT|230993007", "Piora", "treatment"));
    events.add(observation(300 * DAY, "Resposta", "LOINC|88040-1",
        "SNOMED-CT|230993007", "Piora", "treatment"));

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(3, collapsed.size());
    assertTrue(collapsed.stream().anyMatch(e -> e.label.contains("Mudança")));
  }

  @Test
  public void reducesAnnualMammogramsToFirstAndLatest() {
    List<TimelineEvent> events = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      events.add(event((100 + i * 365L) * DAY, "Procedimento", "Mamografia de seguimento",
          "SNOMED-CT|71651007", "follow_up"));
    }

    List<TimelineEvent> collapsed = PathwayHtmlModelBuilder.collapseForOrientadorView(events);
    assertEquals(2, collapsed.size());
    assertEquals("Primeira mamografia de seguimento", collapsed.get(0).label);
    assertTrue(collapsed.get(1).label.contains("6 exames"));
  }

  private static List<TimelineEvent> chemoDay(long dayStart, String drugLabel) {
    return chemoDay(dayStart, drugLabel, "RxNorm|224905");
  }

  private static List<TimelineEvent> chemoDay(long dayStart, String drugLabel, String drugCode) {
    List<TimelineEvent> day = new ArrayList<>();
    day.add(event(dayStart, "Encontro", "Consulta", "SNOMED-CT|185347001", "treatment"));
    day.add(event(dayStart + 60_000, "Medicamento", drugLabel, drugCode, "treatment"));
    day.add(event(dayStart + 120_000, "Procedimento", "Chemotherapy",
        "SNOMED-CT|367336001", "treatment"));
    return day;
  }

  private static TimelineEvent observation(long timestamp, String label, String codeKey,
      String valueKey, String valueLabel, String phaseId) {
    TimelineEvent event = event(timestamp, "Observação", label, codeKey, phaseId);
    event.valueKey = valueKey;
    event.valueLabel = valueLabel;
    return event;
  }

  private static TimelineEvent event(long timestamp, String type, String label, String codeKey,
      String phaseId) {
    TimelineEvent event = new TimelineEvent(timestamp, type, label);
    event.codeKey = codeKey;
    event.phaseId = phaseId;
    return event;
  }
}
