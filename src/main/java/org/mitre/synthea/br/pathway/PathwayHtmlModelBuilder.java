package org.mitre.synthea.br.pathway;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mitre.synthea.export.HtmlExporter.PatientNarrative;
import org.mitre.synthea.export.HtmlExporter.PathwayPhaseSection;
import org.mitre.synthea.export.HtmlExporter.TimelineEvent;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;

/**
 * Builds pathway-grouped timeline models for HTML export (Story 9.4, AD-2 read-only).
 * Story C.1 adds HTML-only dedup and cycle collapse for orientador readability.
 */
public final class PathwayHtmlModelBuilder {

  private static final String CHEMO_PROCEDURE_CODE = "367336001";
  private static final String PROBLEM_ENCOUNTER_CODE = "185347001";
  private static final String MAMMOGRAPHY_CODE = "71651007";
  private static final String SCREENING_ENCOUNTER_CODE = "410410006";
  private static final String T_CODE = "21905-5";
  private static final String N_CODE = "21906-3";
  private static final String M_CODE = "21907-1";
  private static final String ER_CODE = "85337-4";
  private static final String PR_CODE = "85339-0";
  private static final String HER2_CODE = "85319-2";
  private static final String HORMONE_PANEL_CODE = "10480-2";
  private static final String ERBB2_FISH_CODE = "85318-4";
  private static final String RESPONSE_CODE = "88040-1";
  private static final String TREATMENT_STATUS_CODE = "59557-9";
  private static final long TREATMENT_SERIES_GAP_MS = TimeUnit.DAYS.toMillis(60);
  private static final Set<String> RADIOTHERAPY_CODES = Set.of(
      "33195004", "385798007", "1287742003", "447759004", "113120007", "384692006");
  private static final DateTimeFormatter BR_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
  private static final Pattern TNM_VALUE_PATTERN = Pattern.compile("\\bc([TNM][0-4])\\b",
      Pattern.CASE_INSENSITIVE);

  /**
   * Creates timeline events with formatted labels from {@link HtmlExporter}.
   */
  public interface TimelineEventFactory {
    TimelineEvent fromEntry(Entry entry, String type, boolean targetHighlight);

    TimelineEvent fromEncounter(Encounter encounter);
  }

  private PathwayHtmlModelBuilder() {
  }

  /**
   * Applies pathway timeline grouping to a patient narrative when mode requires it.
   *
   * @param narrative narrative to enrich (mutates model fields only)
   * @param person    source person (read-only)
   * @param stopTime  export cutoff
   * @param mode      resolved {@link PathwayHtmlModeConfig} mode
   * @param factory   factory for timeline events
   */
  public static void applyPathwayTimeline(PatientNarrative narrative, Person person,
      long stopTime, String mode, TimelineEventFactory factory) {
    narrative.pathwayMode = mode;
    if (!PathwayHtmlModeConfig.usesPathwayTimeline(mode)) {
      narrative.pathwayTimelineEnabled = false;
      return;
    }

    PathwayEventClassifier classifier = PathwayEventClassifier.forConfiguredCatalog();
    PathwayCatalog catalog = classifier.getCatalog();

    List<TimelineEvent> pathwayEvents = new ArrayList<>();
    List<TimelineEvent> outOfPathwayEvents = new ArrayList<>();

    collectClinicalEvents(person, stopTime, classifier, factory, pathwayEvents, outOfPathwayEvents);
    collectEncounterEvents(person, stopTime, classifier, factory, pathwayEvents);

    pathwayEvents.sort(Comparator.comparingLong(e -> e.timestamp));
    outOfPathwayEvents.sort(Comparator.comparingLong(e -> e.timestamp));

    pathwayEvents = prepareTimelineForMode(pathwayEvents, mode);

    narrative.pathwayTimelineEnabled = true;
    narrative.pathwayPhases = groupByPhase(catalog, pathwayEvents);
    narrative.outOfPathwayEvents = outOfPathwayEvents;
    narrative.timeline = pathwayEvents;

    if (PathwayHtmlModeConfig.MODE_ORIENTADOR.equals(mode)) {
      narrative.outOfPathwayEvents = List.of();
    }
  }

  /**
   * HTML-only collapse: exact dedup, chemo cycle summary, follow-up pair collapse,
   * and hide redundant generic encounters. Does not mutate HealthRecord.
   *
   * @param events sorted pathway events
   * @return collapsed list for timeline display
   */
  static List<TimelineEvent> collapseForOrientadorView(List<TimelineEvent> events) {
    if (events == null || events.isEmpty()) {
      return events;
    }
    List<TimelineEvent> deduped = deduplicateExact(events);
    List<TimelineEvent> withoutRedundantEncounters = hideRedundantEncounters(deduped);
    List<TimelineEvent> withFollowUp = collapseFollowUpPairs(withoutRedundantEncounters);
    List<TimelineEvent> withPanels = collapseDiagnosticPanels(withFollowUp);
    List<TimelineEvent> withTreatmentMilestones = reduceTreatmentSeries(withPanels);
    List<TimelineEvent> withFollowUpMilestones = reduceFollowUpSeries(withTreatmentMilestones);
    withFollowUpMilestones.sort(Comparator.comparingLong(e -> e.timestamp));
    return withFollowUpMilestones;
  }

  static List<TimelineEvent> prepareTimelineForMode(List<TimelineEvent> events, String mode) {
    if (PathwayHtmlModeConfig.MODE_ORIENTADOR.equals(mode)) {
      return collapseForOrientadorView(events);
    }
    return deduplicateExact(events);
  }

  private static List<TimelineEvent> deduplicateExact(List<TimelineEvent> events) {
    List<TimelineEvent> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (TimelineEvent event : events) {
      String key = exactDedupKey(event);
      if (!seen.add(key)) {
        continue;
      }
      result.add(event);
    }
    return result;
  }

  private static String exactDedupKey(TimelineEvent event) {
    String code = event.codeKey != null ? event.codeKey : event.label;
    String value = valueIdentity(event);
    return event.type + "|" + code + "|" + value + "|" + event.timestamp;
  }

  private static List<TimelineEvent> hideRedundantEncounters(List<TimelineEvent> events) {
    Set<Long> daysWithClinical = new HashSet<>();
    for (TimelineEvent event : events) {
      if ("Procedimento".equals(event.type) || "Medicamento".equals(event.type)
          || "Condição".equals(event.type) || "Observação".equals(event.type)) {
        daysWithClinical.add(dayKey(event.timestamp));
      }
    }
    List<TimelineEvent> result = new ArrayList<>();
    for (TimelineEvent event : events) {
      if ("Encontro".equals(event.type)
          && codeEndsWith(event.codeKey, PROBLEM_ENCOUNTER_CODE)
          && daysWithClinical.contains(dayKey(event.timestamp))) {
        continue;
      }
      result.add(event);
    }
    return result;
  }

  private static List<TimelineEvent> collapseFollowUpPairs(List<TimelineEvent> events) {
    Map<Long, TimelineEvent> mammographyByDay = new LinkedHashMap<>();
    Set<Long> screeningDays = new HashSet<>();
    for (TimelineEvent event : events) {
      long day = dayKey(event.timestamp);
      if ("Procedimento".equals(event.type) && codeEndsWith(event.codeKey, MAMMOGRAPHY_CODE)) {
        mammographyByDay.putIfAbsent(day, event);
      }
      if ("Encontro".equals(event.type) && codeEndsWith(event.codeKey, SCREENING_ENCOUNTER_CODE)) {
        screeningDays.add(day);
      }
    }
    if (mammographyByDay.isEmpty() || screeningDays.isEmpty()) {
      return events;
    }
    List<TimelineEvent> result = new ArrayList<>();
    Set<Long> collapsedDays = new HashSet<>();
    for (TimelineEvent event : events) {
      long day = dayKey(event.timestamp);
      boolean isMammo = "Procedimento".equals(event.type)
          && codeEndsWith(event.codeKey, MAMMOGRAPHY_CODE);
      boolean isScreening = "Encontro".equals(event.type)
          && codeEndsWith(event.codeKey, SCREENING_ENCOUNTER_CODE);
      if ((isMammo || isScreening) && mammographyByDay.containsKey(day)
          && screeningDays.contains(day)) {
        if (isMammo && collapsedDays.add(day)) {
          TimelineEvent summary = new TimelineEvent(event.timestamp, "Procedimento",
              "Mamografia de seguimento");
          summary.phaseId = event.phaseId;
          summary.codeKey = event.codeKey;
          result.add(summary);
        }
        continue;
      }
      result.add(event);
    }
    return result;
  }

  private static List<TimelineEvent> collapseDiagnosticPanels(List<TimelineEvent> events) {
    List<TimelineEvent> result = new ArrayList<>(events);
    List<TimelineEvent> molecular = events.stream()
        .filter(e -> codeIs(e, ER_CODE) || codeIs(e, PR_CODE) || codeIs(e, HER2_CODE))
        .toList();
    for (List<TimelineEvent> panel : splitPanelsByPhaseAndGap(molecular)) {
      result.removeAll(panel);
      TimelineEvent anchor = panel.stream()
          .max(Comparator.comparingLong(e -> e.timestamp)).orElse(panel.get(0));
      result.removeIf(e -> samePanelWindow(e, anchor)
          && (codeIs(e, HORMONE_PANEL_CODE) || codeIs(e, ERBB2_FISH_CODE)));
      List<String> parts = new ArrayList<>();
      addPanelValue(parts, panel, ER_CODE, "RE");
      addPanelValue(parts, panel, PR_CODE, "RP");
      addPanelValue(parts, panel, HER2_CODE, "HER2");
      TimelineEvent summary = summaryFrom(anchor,
          "Perfil molecular — " + String.join(" · ", parts), "SUMMARY|molecular");
      result.add(summary);
    }

    List<TimelineEvent> tnm = events.stream()
        .filter(e -> codeIs(e, T_CODE) || codeIs(e, N_CODE) || codeIs(e, M_CODE))
        .toList();
    for (List<TimelineEvent> panel : splitPanelsByPhaseAndGap(tnm)) {
      result.removeAll(panel);
      TimelineEvent anchor = panel.stream()
          .max(Comparator.comparingLong(e -> e.timestamp)).orElse(panel.get(0));
      List<String> parts = new ArrayList<>();
      addTnmValue(parts, panel, T_CODE, "cT");
      addTnmValue(parts, panel, N_CODE, "cN");
      addTnmValue(parts, panel, M_CODE, "cM");
      result.add(summaryFrom(anchor, "TNM clínico — " + String.join(" · ", parts),
          "SUMMARY|tnm"));
    }
    return result;
  }

  private static void addPanelValue(List<String> parts, List<TimelineEvent> events,
      String code, String prefix) {
    TimelineEvent event = events.stream().filter(e -> codeIs(e, code))
        .max(Comparator.comparingLong(e -> e.timestamp)).orElse(null);
    if (event != null) {
      parts.add(prefix + " " + cleanValue(event.valueLabel));
    }
  }

  private static void addTnmValue(List<String> parts, List<TimelineEvent> events,
      String code, String fallback) {
    TimelineEvent event = events.stream().filter(e -> codeIs(e, code))
        .max(Comparator.comparingLong(e -> e.timestamp)).orElse(null);
    if (event == null) {
      return;
    }
    String compact = compactTnm(event.valueLabel);
    parts.add(compact != null ? compact : fallback + " não informado");
  }

  private static List<List<TimelineEvent>> splitPanelsByPhaseAndGap(
      List<TimelineEvent> events) {
    Map<String, List<TimelineEvent>> byPhase = new LinkedHashMap<>();
    for (TimelineEvent event : events) {
      byPhase.computeIfAbsent(event.phaseId != null ? event.phaseId : "",
          key -> new ArrayList<>()).add(event);
    }
    List<List<TimelineEvent>> result = new ArrayList<>();
    for (List<TimelineEvent> phaseEvents : byPhase.values()) {
      phaseEvents.sort(Comparator.comparingLong(e -> e.timestamp));
      result.addAll(splitByGap(phaseEvents, TimeUnit.DAYS.toMillis(90)));
    }
    return result;
  }

  private static boolean samePanelWindow(TimelineEvent event, TimelineEvent anchor) {
    return safeEquals(event.phaseId, anchor.phaseId)
        && Math.abs(event.timestamp - anchor.timestamp) <= TimeUnit.DAYS.toMillis(90);
  }

  private static String compactTnm(String value) {
    if (value == null) {
      return null;
    }
    Matcher matcher = TNM_VALUE_PATTERN.matcher(value);
    return matcher.find() ? "c" + matcher.group(1).toUpperCase() : null;
  }

  private static String cleanValue(String value) {
    if (value == null || value.isBlank()) {
      return "não informado";
    }
    return value
        .replace(" (qualifier value)", "")
        .replace(" (valor qualificador)", "");
  }

  private static List<TimelineEvent> reduceTreatmentSeries(List<TimelineEvent> events) {
    return reduceChemoSeries(reduceRadiotherapySeries(events));
  }

  private static List<TimelineEvent> reduceRadiotherapySeries(List<TimelineEvent> events) {
    List<TimelineEvent> radiation = events.stream()
        .filter(PathwayHtmlModelBuilder::isRadiotherapy)
        .sorted(Comparator.comparingLong(e -> e.timestamp))
        .toList();
    if (radiation.isEmpty()) {
      return events;
    }
    List<TimelineEvent> result = new ArrayList<>(events);
    result.removeIf(PathwayHtmlModelBuilder::isRadiotherapy);
    for (List<TimelineEvent> series : splitByGap(radiation, TREATMENT_SERIES_GAP_MS)) {
      TimelineEvent first = series.get(0);
      if (series.size() == 1) {
        result.add(summaryFrom(first,
            "Radioterapia — sessão única (" + radiotherapyModality(first) + ")",
            "SUMMARY|radiotherapy"));
        continue;
      }
      result.add(summaryFrom(first,
          "Início da radioterapia — " + radiotherapyModality(first) + " (1ª sessão)",
          "SUMMARY|radiotherapy-start"));
      String previousCode = first.codeKey;
      for (int i = 1; i < series.size() - 1; i++) {
        TimelineEvent current = series.get(i);
        if (!safeEquals(previousCode, current.codeKey)) {
          result.add(summaryFrom(current,
              "Mudança de radioterapia — " + radiotherapyModality(current),
              "SUMMARY|radiotherapy-change"));
          previousCode = current.codeKey;
        }
      }
      TimelineEvent last = series.get(series.size() - 1);
      result.add(summaryFrom(last,
          "Conclusão da radioterapia — " + series.size() + " sessões · "
              + dateRange(first.timestamp, last.timestamp),
          "SUMMARY|radiotherapy-end"));
    }
    return result;
  }

  private static List<TimelineEvent> reduceChemoSeries(List<TimelineEvent> events) {
    Map<Long, List<TimelineEvent>> byDay = new LinkedHashMap<>();
    for (TimelineEvent event : events) {
      byDay.computeIfAbsent(dayKey(event.timestamp), key -> new ArrayList<>()).add(event);
    }
    List<ChemoCycle> cycles = new ArrayList<>();
    Set<TimelineEvent> consumed = new HashSet<>();
    for (List<TimelineEvent> dayEvents : byDay.values()) {
      TimelineEvent procedure = findByCode(dayEvents, "Procedimento", CHEMO_PROCEDURE_CODE);
      TimelineEvent medication = findFirstOfType(dayEvents, "Medicamento");
      if (procedure != null && medication != null) {
        cycles.add(new ChemoCycle(procedure, medication));
        consumed.add(procedure);
        consumed.add(medication);
      }
    }
    if (cycles.isEmpty()) {
      return events;
    }
    List<TimelineEvent> result = new ArrayList<>(events);
    result.removeAll(consumed);
    for (List<ChemoCycle> series : splitChemoByGap(cycles)) {
      ChemoCycle first = series.get(0);
      if (series.size() == 1) {
        result.add(summaryFrom(first.procedure,
            "Quimioterapia — ciclo único (" + first.medication.label + ")",
            "SUMMARY|chemotherapy"));
        continue;
      }
      result.add(summaryFrom(first.procedure,
          "Início da quimioterapia — " + first.medication.label,
          "SUMMARY|chemotherapy-start"));
      String previousDrug = first.medication.codeKey;
      for (int i = 1; i < series.size() - 1; i++) {
        ChemoCycle current = series.get(i);
        if (!safeEquals(previousDrug, current.medication.codeKey)) {
          result.add(summaryFrom(current.procedure,
              "Mudança de esquema quimioterápico — " + current.medication.label,
              "SUMMARY|chemotherapy-change"));
          previousDrug = current.medication.codeKey;
        }
      }
      ChemoCycle last = series.get(series.size() - 1);
      result.add(summaryFrom(last.procedure,
          "Conclusão da quimioterapia — " + series.size() + " ciclos · "
              + dateRange(first.procedure.timestamp, last.procedure.timestamp),
          "SUMMARY|chemotherapy-end"));
    }
    return result;
  }

  private static List<TimelineEvent> reduceFollowUpSeries(List<TimelineEvent> events) {
    List<TimelineEvent> result = reduceValueSeries(events, RESPONSE_CODE, "Resposta ao tratamento");
    result = reduceValueSeries(result, TREATMENT_STATUS_CODE, "Status do tratamento");
    return reduceMammographySeries(result);
  }

  private static List<TimelineEvent> reduceValueSeries(List<TimelineEvent> events,
      String code, String title) {
    Map<String, List<TimelineEvent>> byPhase = new LinkedHashMap<>();
    for (TimelineEvent event : events) {
      if (codeIs(event, code)) {
        byPhase.computeIfAbsent(event.phaseId != null ? event.phaseId : "", key -> new ArrayList<>())
            .add(event);
      }
    }
    if (byPhase.isEmpty()) {
      return events;
    }
    List<TimelineEvent> result = new ArrayList<>(events);
    for (List<TimelineEvent> series : byPhase.values()) {
      series.sort(Comparator.comparingLong(e -> e.timestamp));
      if (series.size() < 2) {
        continue;
      }
      result.removeAll(series);
      TimelineEvent first = series.get(0);
      result.add(summaryFrom(first, title + " — " + cleanValue(first.valueLabel),
          "SUMMARY|" + code + "-first"));
      String previousValue = valueIdentity(first);
      for (int i = 1; i < series.size() - 1; i++) {
        TimelineEvent current = series.get(i);
        if (!safeEquals(previousValue, valueIdentity(current))) {
          result.add(summaryFrom(current,
              "Mudança em " + title.toLowerCase() + " — " + cleanValue(current.valueLabel),
              "SUMMARY|" + code + "-change"));
          previousValue = valueIdentity(current);
        }
      }
      TimelineEvent last = series.get(series.size() - 1);
      if (!safeEquals(previousValue, valueIdentity(last))) {
        result.add(summaryFrom(last,
            "Mudança em " + title.toLowerCase() + " — " + cleanValue(last.valueLabel),
            "SUMMARY|" + code + "-change"));
      } else {
        result.add(summaryFrom(last,
            "Última avaliação de " + title.toLowerCase() + " — "
                + cleanValue(last.valueLabel) + " (" + series.size() + " registros)",
            "SUMMARY|" + code + "-last"));
      }
    }
    return result;
  }

  private static List<TimelineEvent> reduceMammographySeries(List<TimelineEvent> events) {
    List<TimelineEvent> mammograms = events.stream()
        .filter(e -> "follow_up".equals(e.phaseId) && codeIs(e, MAMMOGRAPHY_CODE))
        .sorted(Comparator.comparingLong(e -> e.timestamp))
        .toList();
    if (mammograms.size() < 2) {
      return events;
    }
    List<TimelineEvent> result = new ArrayList<>(events);
    result.removeAll(mammograms);
    TimelineEvent first = mammograms.get(0);
    TimelineEvent last = mammograms.get(mammograms.size() - 1);
    result.add(summaryFrom(first, "Primeira mamografia de seguimento",
        "SUMMARY|mammography-first"));
    result.add(summaryFrom(last,
        "Última mamografia registrada (" + mammograms.size() + " exames desde "
            + BR_DATE.format(Instant.ofEpochMilli(first.timestamp)) + ")",
        "SUMMARY|mammography-last"));
    return result;
  }

  private static TimelineEvent findByCode(List<TimelineEvent> events, String type, String code) {
    for (TimelineEvent event : events) {
      if (type.equals(event.type) && codeEndsWith(event.codeKey, code)) {
        return event;
      }
    }
    return null;
  }

  private static TimelineEvent findFirstOfType(List<TimelineEvent> events, String type) {
    for (TimelineEvent event : events) {
      if (type.equals(event.type)) {
        return event;
      }
    }
    return null;
  }

  private static List<List<TimelineEvent>> splitByGap(List<TimelineEvent> events, long maxGap) {
    List<List<TimelineEvent>> result = new ArrayList<>();
    List<TimelineEvent> current = new ArrayList<>();
    for (TimelineEvent event : events) {
      if (!current.isEmpty()
          && event.timestamp - current.get(current.size() - 1).timestamp > maxGap) {
        result.add(current);
        current = new ArrayList<>();
      }
      current.add(event);
    }
    if (!current.isEmpty()) {
      result.add(current);
    }
    return result;
  }

  private static List<List<ChemoCycle>> splitChemoByGap(List<ChemoCycle> cycles) {
    List<List<ChemoCycle>> result = new ArrayList<>();
    List<ChemoCycle> current = new ArrayList<>();
    for (ChemoCycle cycle : cycles) {
      if (!current.isEmpty()
          && cycle.procedure.timestamp
              - current.get(current.size() - 1).procedure.timestamp > TREATMENT_SERIES_GAP_MS) {
        result.add(current);
        current = new ArrayList<>();
      }
      current.add(cycle);
    }
    if (!current.isEmpty()) {
      result.add(current);
    }
    return result;
  }

  private static TimelineEvent summaryFrom(TimelineEvent source, String label, String codeKey) {
    TimelineEvent summary = new TimelineEvent(source.timestamp, source.type, label);
    summary.phaseId = source.phaseId;
    summary.codeKey = codeKey;
    summary.targetConditionHighlight = source.targetConditionHighlight;
    return summary;
  }

  private static boolean isRadiotherapy(TimelineEvent event) {
    for (String code : RADIOTHERAPY_CODES) {
      if (codeIs(event, code)) {
        return true;
      }
    }
    return false;
  }

  private static String radiotherapyModality(TimelineEvent event) {
    if (codeIs(event, "33195004")) {
      return "feixe externo";
    }
    if (codeIs(event, "385798007")) {
      return "hipofracionada";
    }
    if (codeIs(event, "1287742003")) {
      return "intraoperatória (IORT)";
    }
    return "braquiterapia";
  }

  private static String dateRange(long start, long end) {
    return BR_DATE.format(Instant.ofEpochMilli(start)) + " a "
        + BR_DATE.format(Instant.ofEpochMilli(end));
  }

  private static String valueIdentity(TimelineEvent event) {
    return event.valueKey != null ? event.valueKey : event.valueLabel;
  }

  private static boolean safeEquals(String left, String right) {
    return left == null ? right == null : left.equals(right);
  }

  private static boolean codeIs(TimelineEvent event, String code) {
    return codeEndsWith(event.codeKey, code);
  }

  private static boolean codeEndsWith(String codeKey, String code) {
    if (codeKey == null || code == null) {
      return false;
    }
    return codeKey.endsWith("|" + code) || codeKey.equals(code)
        || codeKey.endsWith(code);
  }

  private static long dayKey(long timestamp) {
    return TimeUnit.MILLISECONDS.toDays(timestamp);
  }

  private static final class ChemoCycle {
    private final TimelineEvent procedure;
    private final TimelineEvent medication;

    private ChemoCycle(TimelineEvent procedure, TimelineEvent medication) {
      this.procedure = procedure;
      this.medication = medication;
    }
  }

  private static List<PathwayPhaseSection> groupByPhase(PathwayCatalog catalog,
      List<TimelineEvent> pathwayEvents) {
    Map<String, PathwayPhaseSection> sections = new HashMap<>();
    for (PathwayPhase phase : catalog.getPhasesInOrder()) {
      PathwayPhaseSection section = new PathwayPhaseSection();
      section.phaseId = phase.getPhaseId();
      section.title = phase.getTitlePtBr();
      section.description = phase.getDescriptionPtBr();
      section.order = phase.getOrder();
      section.events = new ArrayList<>();
      sections.put(phase.getPhaseId(), section);
    }

    for (TimelineEvent event : pathwayEvents) {
      if (event.phaseId == null) {
        continue;
      }
      PathwayPhaseSection section = sections.get(event.phaseId);
      if (section != null) {
        section.events.add(event);
      }
    }

    List<PathwayPhaseSection> ordered = new ArrayList<>();
    for (PathwayPhase phase : catalog.getPhasesInOrder()) {
      PathwayPhaseSection section = sections.get(phase.getPhaseId());
      if (section != null && !section.events.isEmpty()) {
        ordered.add(section);
      }
    }
    return ordered;
  }

  private static void collectClinicalEvents(Person person, long stopTime,
      PathwayEventClassifier classifier, TimelineEventFactory factory,
      List<TimelineEvent> pathwayEvents, List<TimelineEvent> outOfPathwayEvents) {
    for (Encounter encounter : person.record.encounters) {
      if (encounter.start > stopTime) {
        break;
      }
      addEntries(encounter.conditions, stopTime, "Condição", classifier, factory,
          pathwayEvents, outOfPathwayEvents);
      addEntries(encounter.medications, stopTime, "Medicamento", classifier, factory,
          pathwayEvents, outOfPathwayEvents);
      addEntries(encounter.procedures, stopTime, "Procedimento", classifier, factory,
          pathwayEvents, outOfPathwayEvents);
      addEntries(encounter.reports, stopTime, "Exame", classifier, factory,
          pathwayEvents, outOfPathwayEvents);
      for (Observation observation : encounter.observations) {
        if (observation.start <= stopTime && observation.value != null) {
          addEntry(observation, stopTime, "Observação", classifier, factory,
              pathwayEvents, outOfPathwayEvents);
        }
      }
    }
  }

  private static <E extends Entry> void addEntries(List<E> entries, long stopTime, String type,
      PathwayEventClassifier classifier, TimelineEventFactory factory,
      List<TimelineEvent> pathwayEvents, List<TimelineEvent> outOfPathwayEvents) {
    for (E entry : entries) {
      addEntry(entry, stopTime, type, classifier, factory, pathwayEvents, outOfPathwayEvents);
    }
  }

  private static void addEntry(Entry entry, long stopTime, String type,
      PathwayEventClassifier classifier, TimelineEventFactory factory,
      List<TimelineEvent> pathwayEvents, List<TimelineEvent> outOfPathwayEvents) {
    if (entry.start > stopTime) {
      return;
    }
    String phaseId = classifier.classifyEntry(entry);
    boolean highlight = classifier.isTargetConditionEntry(entry);
    TimelineEvent event = factory.fromEntry(entry, type, highlight);
    event.phaseId = phaseId;
    if (phaseId == null) {
      outOfPathwayEvents.add(event);
    } else {
      pathwayEvents.add(event);
    }
  }

  private static void collectEncounterEvents(Person person, long stopTime,
      PathwayEventClassifier classifier, TimelineEventFactory factory,
      List<TimelineEvent> pathwayEvents) {
    for (Encounter encounter : person.record.encounters) {
      if (encounter.start > stopTime) {
        break;
      }
      if (classifier.classifyEncounter(encounter) == null) {
        continue;
      }
      TimelineEvent event = factory.fromEncounter(encounter);
      event.phaseId = classifier.classifyEncounter(encounter);
      pathwayEvents.add(event);
    }
  }
}
