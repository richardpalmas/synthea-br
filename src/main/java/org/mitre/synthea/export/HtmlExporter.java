package org.mitre.synthea.export;

import static org.mitre.synthea.export.ExportHelper.dateFromTimestamp;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.br.ai.AiEnrichmentService;
import org.mitre.synthea.br.ai.CohortEnrichmentLog;
import org.mitre.synthea.br.condition.SupportedConditions;
import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.br.demographics.BrRaceMapper;
import org.mitre.synthea.br.pathway.PathwayHtmlModeConfig;
import org.mitre.synthea.br.pathway.PathwayHtmlModelBuilder;
import org.mitre.synthea.br.pathway.PathwayExportFilter;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.br.terminology.BrTerminologyResolver;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.concepts.HealthRecord.Report;
import org.mitre.synthea.world.concepts.healthinsurance.PlanRecord;

/**
 * Exports a cohort-level HTML narrative index ({@code output/html/index.html}) with one accordion
 * per patient. Data is read from {@link Person}/{@link HealthRecord} at export time (AD-2).
 *
 * <p>Cohort-scale note: a single {@code index.html} is practical for pilot cohorts (n≈10–500).
 * For larger populations, prefer disabling HTML export or splitting output (v1.1:
 * {@code patients/{id}.html}).
 */
public class HtmlExporter {

  private static final Configuration TEMPLATES = templateConfiguration();

  private static final DateTimeFormatter BR_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

  private static final Map<String, String> TARGET_CONDITION_PT = Map.of(
      "breast_cancer", "Câncer de mama");

  private final List<PatientNarrative> patients = Collections.synchronizedList(new ArrayList<>());

  private static class SingletonHolder {
    private static final HtmlExporter instance = new HtmlExporter();
  }

  /**
   * Returns the shared HtmlExporter instance.
   *
   * @return singleton instance
   */
  public static HtmlExporter getInstance() {
    return SingletonHolder.instance;
  }

  /**
   * Clears accumulated patient narratives for a new cohort run.
   */
  public synchronized void reset() {
    patients.clear();
  }

  /**
   * Test hook for verifying cohort accumulation without writing index.html.
   *
   * @return number of patients accumulated since the last reset
   */
  synchronized int getAccumulatedPatientCountForTest() {
    return patients.size();
  }

  /**
   * Accumulates one patient's narrative model for the cohort index.
   *
   * @param person   patient to export
   * @param stopTime export cutoff time
   */
  public void appendPatient(Person person, long stopTime) {
    patients.add(buildPatientNarrative(person, stopTime, aiEnrichmentByPatientId()));
  }

  /**
   * Renders and writes {@code index.html} under {@code output/html/}.
   *
   * @throws IOException when the output file cannot be written
   */
  public void writeIndex() throws IOException {
    if (patients.isEmpty()) {
      reset();
      return;
    }

    try {
      Map<String, Object> model = new HashMap<>();
      model.put("generatedDate", formatExportDate(System.currentTimeMillis(), BrProfile.isActive()));
      model.put("patientCount", patients.size());
      model.put("patients", new ArrayList<>(patients));

      CohortEnrichmentLog aiLog = AiEnrichmentService.getLastLog();
      if (aiLog != null && aiLog.getCohortNarrativeSummary() != null) {
        model.put("aiEnrichmentEnabled", true);
        model.put("aiCohortSummary", aiLog.getCohortNarrativeSummary());
        model.put("aiModel", aiLog.getModel());
      } else {
        model.put("aiEnrichmentEnabled", false);
      }

      String html = renderTemplate("index.ftl", model);
      Path outFile = Exporter.getOutputFolder("html", null).toPath().resolve("index.html");
      Files.createDirectories(outFile.getParent());
      Files.writeString(outFile, html, StandardCharsets.UTF_8);
    } finally {
      reset();
    }
  }

  private static String renderTemplate(String templateName, Map<String, Object> model) {
    StringWriter writer = new StringWriter();
    try {
      Template template = TEMPLATES.getTemplate(templateName);
      template.process(model, writer);
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
    return writer.toString();
  }

  private static PatientNarrative buildPatientNarrative(Person person, long stopTime,
      Map<String, Map<String, Object>> aiByPatientId) {
    String pathwayMode = resolvePathwayMode();
    Person viewPerson = personForHtmlView(person, pathwayMode);
    boolean brProfile = BrProfile.isActive();

    AggregatedClinicalData aggregated = aggregateClinicalData(viewPerson, stopTime);

    PatientNarrative narrative = new PatientNarrative();
    narrative.displayName = stringAttr(viewPerson, Person.NAME);
    narrative.patientId = stringAttr(viewPerson, Person.ID);
    narrative.ageYears = viewPerson.age(stopTime).getYears();
    narrative.sexLabel = formatSexLabel(stringAttr(viewPerson, Person.GENDER));
    narrative.primaryCondition = resolvePrimaryCondition(aggregated, stopTime, brProfile);
    narrative.primaryConditionHighlight = isTargetConditionConfigured();
    narrative.demographics = buildDemographics(viewPerson, stopTime, brProfile);
    narrative.conditions = buildConditionRows(aggregated, stopTime, brProfile);
    narrative.medications = buildMedicationRows(aggregated, stopTime, brProfile);
    narrative.exams = buildExamRows(aggregated, stopTime, brProfile);
    narrative.procedures = buildProcedureRows(aggregated, stopTime, brProfile);
    narrative.encounters = buildEncounterRows(viewPerson, stopTime, brProfile);
    narrative.coverage = buildCoverageRows(viewPerson, stopTime, brProfile);
    narrative.timeline = buildTimeline(aggregated, viewPerson, stopTime, brProfile);

    if (PathwayHtmlModeConfig.usesPathwayTimeline(pathwayMode)
        && TargetConditionConfig.resolveConfigured() != null) {
      PathwayHtmlModelBuilder.applyPathwayTimeline(narrative, viewPerson, stopTime, pathwayMode,
          pathwayEventFactory(brProfile));
      formatTimelineDates(narrative.timeline, brProfile);
      formatTimelineDates(narrative.outOfPathwayEvents, brProfile);
      if (narrative.pathwayPhases != null) {
        for (PathwayPhaseSection section : narrative.pathwayPhases) {
          formatTimelineDates(section.events, brProfile);
        }
      }
      narrative.hideNonPathwayClinicalSections =
          PathwayHtmlModeConfig.hidesOutOfPathwayClinicalData(pathwayMode);
      narrative.showOutOfPathwaySection =
          PathwayHtmlModeConfig.MODE_PESQUISADOR.equals(pathwayMode);
    } else {
      narrative.pathwayMode = pathwayMode;
      narrative.pathwayTimelineEnabled = false;
      narrative.hideNonPathwayClinicalSections = false;
      narrative.showOutOfPathwaySection = false;
    }

    applyLastEvent(narrative);
    applyAiEnrichment(narrative, aiByPatientId);
    return narrative;
  }

  private static String resolvePathwayMode() {
    return PathwayHtmlModeConfig.resolveMode();
  }

  private static Person personForHtmlView(Person person, String pathwayMode) {
    if (!PathwayHtmlModeConfig.hidesOutOfPathwayClinicalData(pathwayMode)) {
      return person;
    }
    if (TargetConditionConfig.resolveConfigured() == null) {
      return person;
    }
    return PathwayExportFilter.filterForExport(person,
        org.mitre.synthea.br.pathway.PathwayCatalog.loadForConfiguredCondition());
  }

  private static boolean isTargetConditionConfigured() {
    return TargetConditionConfig.resolveConfigured() != null;
  }

  private static PathwayHtmlModelBuilder.TimelineEventFactory pathwayEventFactory(
      boolean brProfile) {
    return new PathwayHtmlModelBuilder.TimelineEventFactory() {
      @Override
      public TimelineEvent fromEntry(Entry entry, String type, boolean targetHighlight) {
        TimelineEvent event =
            new TimelineEvent(entry.start, type, formatEntryLabel(entry, brProfile));
        event.targetConditionHighlight = targetHighlight;
        event.codeKey = codeKeyOf(entry);
        if (entry instanceof Observation) {
          Observation observation = (Observation) entry;
          event.valueLabel = formatObservationValue(observation, brProfile);
          event.unit = observation.unit;
          if (observation.value instanceof Code) {
            Code valueCode = (Code) observation.value;
            String system = valueCode.system != null ? valueCode.system : "";
            String code = valueCode.code != null ? valueCode.code : "";
            event.valueKey = system + "|" + code;
          }
        }
        return event;
      }

      @Override
      public TimelineEvent fromEncounter(Encounter encounter) {
        TimelineEvent event = new TimelineEvent(encounter.start, "Encontro",
            encounterLabel(encounter, brProfile));
        event.codeKey = codeKeyOf(encounter);
        return event;
      }
    };
  }

  private static String codeKeyOf(Entry entry) {
    if (entry == null || entry.codes == null || entry.codes.isEmpty()) {
      return null;
    }
    Code code = entry.codes.get(0);
    String system = code.system != null ? code.system : "";
    String value = code.code != null ? code.code : "";
    return system + "|" + value;
  }

  private static Map<String, Map<String, Object>> aiEnrichmentByPatientId() {
    CohortEnrichmentLog log = AiEnrichmentService.getLastLog();
    if (log == null) {
      return Map.of();
    }
    Map<String, Map<String, Object>> byId = new HashMap<>();
    for (Map<String, Object> row : log.getPatients()) {
      Object patientId = row.get("patientId");
      if (patientId != null) {
        byId.put(patientId.toString(), row);
      }
    }
    return byId;
  }

  private static void applyAiEnrichment(PatientNarrative narrative,
      Map<String, Map<String, Object>> aiByPatientId) {
    if (narrative.patientId == null || "—".equals(narrative.patientId)) {
      return;
    }
    Map<String, Object> row = aiByPatientId.get(narrative.patientId);
    if (row == null) {
      return;
    }
    Object summary = row.get("narrativeSummary");
    if (summary == null) {
      return;
    }
    narrative.aiEnriched = true;
    narrative.aiSummary = summary.toString();
    narrative.aiAppliedCount = intValue(row.get("appliedCount"));
    narrative.aiFlagCount = intValue(row.get("flagCount"));
    Object writingPersona = row.get("writingPersona");
    if (writingPersona != null) {
      narrative.aiWritingPersona = writingPersona.toString();
    }
  }

  private static int intValue(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value == null) {
      return 0;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  private static final class AggregatedClinicalData {
    private final List<Entry> conditions = new ArrayList<>();
    private final List<Medication> medications = new ArrayList<>();
    private final List<Procedure> procedures = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
  }

  private static AggregatedClinicalData aggregateClinicalData(Person person, long stopTime) {
    AggregatedClinicalData aggregated = new AggregatedClinicalData();
    for (Encounter encounter : person.record.encounters) {
      if (encounter.start <= stopTime) {
        aggregated.conditions.addAll(encounter.conditions);
        aggregated.medications.addAll(encounter.medications);
        aggregated.procedures.addAll(encounter.procedures);
        aggregated.reports.addAll(encounter.reports);
        aggregated.observations.addAll(encounter.observations);
      } else {
        break;
      }
    }
    return aggregated;
  }

  /**
   * Primary condition: {@code br.target_condition} display when configured; otherwise the active
   * chronic condition with the most recent start before {@code stopTime}.
   */
  private static String resolvePrimaryCondition(AggregatedClinicalData aggregated,
      long stopTime, boolean brProfile) {
    String targetKey = Config.get("br.target_condition");
    if (targetKey != null && !targetKey.isEmpty()) {
      SupportedConditions.ConditionDefinition def = SupportedConditions.get(targetKey);
      if (def != null) {
        String pt = TARGET_CONDITION_PT.get(def.conditionKey);
        if (pt != null) {
          return pt;
        }
        return humanizeKey(def.conditionKey);
      }
    }

    Entry best = null;
    for (Entry condition : aggregated.conditions) {
      if (condition.start > stopTime) {
        continue;
      }
      if (condition.stop > 0 && condition.stop <= stopTime) {
        continue;
      }
      if (best == null || condition.start > best.start) {
        best = condition;
      }
    }
    if (best != null) {
      return formatEntryLabel(best, brProfile);
    }
    return "Sem condição principal registrada";
  }

  private static void formatTimelineDates(List<TimelineEvent> events, boolean brProfile) {
    if (events == null) {
      return;
    }
    for (TimelineEvent event : events) {
      event.date = formatExportDate(event.timestamp, brProfile);
    }
  }

  private static void applyLastEvent(PatientNarrative narrative) {
    if (narrative.timeline.isEmpty()) {
      narrative.lastEventDate = "—";
      narrative.lastEventLabel = "Sem eventos";
      return;
    }
    TimelineEvent last = narrative.timeline.get(narrative.timeline.size() - 1);
    narrative.lastEventDate = last.date;
    narrative.lastEventLabel = last.label;
  }

  private static List<TimelineEvent> buildTimeline(AggregatedClinicalData aggregated, Person person,
      long stopTime, boolean brProfile) {
    List<TimelineEvent> events = new ArrayList<>();

    for (Encounter encounter : person.record.encounters) {
      if (encounter.start <= stopTime) {
        events.add(new TimelineEvent(encounter.start, "Encontro",
            encounterLabel(encounter, brProfile)));
      }
    }
    for (Entry condition : aggregated.conditions) {
      if (condition.start <= stopTime) {
        events.add(new TimelineEvent(condition.start, "Condição",
            formatEntryLabel(condition, brProfile)));
      }
    }
    for (Medication medication : aggregated.medications) {
      if (medication.start <= stopTime) {
        events.add(new TimelineEvent(medication.start, "Medicamento",
            formatEntryLabel(medication, brProfile)));
      }
    }
    for (Procedure procedure : aggregated.procedures) {
      if (procedure.start <= stopTime) {
        events.add(new TimelineEvent(procedure.start, "Procedimento",
            formatEntryLabel(procedure, brProfile)));
      }
    }
    for (Report report : aggregated.reports) {
      if (report.start <= stopTime) {
        events.add(new TimelineEvent(report.start, "Exame", formatEntryLabel(report, brProfile)));
      }
    }
    for (Observation observation : aggregated.observations) {
      if (observation.start <= stopTime && observation.value != null) {
        events.add(new TimelineEvent(observation.start, "Observação",
            formatEntryLabel(observation, brProfile)));
      }
    }

    events.sort(Comparator.comparingLong(e -> e.timestamp));
    for (TimelineEvent event : events) {
      event.date = formatExportDate(event.timestamp, brProfile);
    }
    return events;
  }

  private static DemographicsSection buildDemographics(Person person, long stopTime,
      boolean brProfile) {
    DemographicsSection section = new DemographicsSection();
    section.name = stringAttr(person, Person.NAME);
    section.birthDate = person.attributes.containsKey(Person.BIRTHDATE)
        ? formatExportDate((Long) person.attributes.get(Person.BIRTHDATE), brProfile) : "—";
    Period age = person.age(stopTime);
    section.age = age.getYears() + " anos";
    section.sex = formatSexLabel(stringAttr(person, Person.GENDER));
    section.race = brProfile
        ? BrRaceMapper.toBrazilianDisplayRace(stringAttr(person, Person.RACE))
        : stringAttr(person, Person.RACE);
    String ethnicity = stringAttr(person, Person.ETHNICITY);
    section.ethnicity = brProfile
        ? BrRaceMapper.getIbgeDisplayName(ethnicity) : ethnicity;
    section.city = stringAttr(person, Person.CITY);
    section.state = stringAttr(person, Person.STATE);
    section.country = brProfile ? "Brasil" : stringAttr(person, Person.BIRTH_COUNTRY);
    return section;
  }

  private static List<RowItem> buildConditionRows(AggregatedClinicalData aggregated, long stopTime,
      boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (Entry condition : aggregated.conditions) {
      if (condition.start <= stopTime) {
        rows.add(rowFromEntry(condition, condition.start, condition.stop, stopTime, brProfile));
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static List<RowItem> buildMedicationRows(
      AggregatedClinicalData aggregated, long stopTime, boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (Medication medication : aggregated.medications) {
      if (medication.start <= stopTime) {
        rows.add(rowFromEntry(medication, medication.start, medication.stop, stopTime, brProfile));
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static List<RowItem> buildExamRows(AggregatedClinicalData aggregated, long stopTime,
      boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (Report report : aggregated.reports) {
      if (report.start <= stopTime) {
        rows.add(rowFromEntry(report, report.start, report.stop, stopTime, brProfile));
      }
    }
    for (Observation observation : aggregated.observations) {
      if (observation.start <= stopTime && observation.value != null) {
        String value = ExportHelper.getObservationValue(observation);
        RowItem row = rowFromEntry(observation, observation.start, observation.stop, stopTime,
            brProfile);
        if (value != null && !value.isEmpty()) {
          row.detail = value;
        }
        rows.add(row);
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static List<RowItem> buildProcedureRows(
      AggregatedClinicalData aggregated, long stopTime, boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (Procedure procedure : aggregated.procedures) {
      if (procedure.start <= stopTime) {
        rows.add(rowFromEntry(procedure, procedure.start, procedure.stop, stopTime, brProfile));
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static List<RowItem> buildEncounterRows(Person person, long stopTime,
      boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (Encounter encounter : person.record.encounters) {
      if (encounter.start <= stopTime) {
        RowItem row = new RowItem();
        row.sortKey = encounter.start;
        row.startDate = formatExportDate(encounter.start, brProfile);
        row.endDate = encounter.stop > 0 && encounter.stop <= stopTime
            ? formatExportDate(encounter.stop, brProfile) : "—";
        row.label = encounterLabel(encounter, brProfile);
        rows.add(row);
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static List<RowItem> buildCoverageRows(Person person, long stopTime,
      boolean brProfile) {
    List<RowItem> rows = new ArrayList<>();
    for (PlanRecord planRecord : person.coverage.getPlanHistory()) {
      if (planRecord.getStartTime() <= stopTime) {
        RowItem row = new RowItem();
        row.sortKey = planRecord.getStartTime();
        row.startDate = formatExportDate(planRecord.getStartTime(), brProfile);
        long stop = planRecord.getStopTime();
        row.endDate = stop > 0 && stop <= stopTime ? formatExportDate(stop, brProfile) : "—";
        row.label = planRecord.getPlan() != null && planRecord.getPlan().getPayer() != null
            ? BrTerminologyResolver.resolvePayerLabel(planRecord.getPlan().getPayer().getName())
            : "Sem plano";
        rows.add(row);
      }
    }
    rows.sort(Comparator.comparing(r -> r.sortKey));
    return rows;
  }

  private static RowItem rowFromEntry(Entry entry, long start, long stop, long stopTime,
      boolean brProfile) {
    RowItem row = new RowItem();
    row.sortKey = start;
    row.startDate = formatExportDate(start, brProfile);
    row.endDate = stop > 0 && stop <= stopTime ? formatExportDate(stop, brProfile) : "—";
    row.label = formatEntryLabel(entry, brProfile);
    return row;
  }

  private static String formatEntryLabel(Entry entry, boolean brProfile) {
    if (entry.codes == null || entry.codes.isEmpty()) {
      return "Registro clínico";
    }
    Code code = entry.codes.get(0);
    String base = brProfile
        ? BrTerminologyResolver.resolveDisplay(code)
        : (code.display != null ? code.display : code.code);
    if (entry instanceof Observation) {
      String valueLabel = formatObservationValue((Observation) entry, brProfile);
      if (valueLabel != null && !valueLabel.isEmpty()) {
        return base + " — " + valueLabel;
      }
    }
    return base;
  }

  private static String formatObservationValue(Observation observation, boolean brProfile) {
    if (observation.value == null) {
      return null;
    }
    if (observation.value instanceof Code) {
      Code valueCode = (Code) observation.value;
      if (brProfile) {
        return BrTerminologyResolver.resolveDisplay(valueCode);
      }
      return valueCode.display != null ? valueCode.display : valueCode.code;
    }
    String text;
    if (observation.value instanceof Number) {
      text = String.format(Locale.ROOT, "%.1f", ((Number) observation.value).doubleValue())
          .replaceAll("\\.0$", "");
      if (observation.unit != null && !observation.unit.isBlank()) {
        text += " " + observation.unit;
      }
    } else {
      text = observation.value.toString();
    }
    if (brProfile) {
      String mapped = BrTerminologyResolver.resolveDisplayText(text);
      if (mapped != null && !mapped.isEmpty()) {
        return mapped;
      }
    }
    return text;
  }

  private static String encounterLabel(Encounter encounter, boolean brProfile) {
    // Prefer encounter code/type label; reason is often the target condition and
    // can make distinct follow-up encounters look like duplicated diagnosis rows.
    if (encounter.codes != null && !encounter.codes.isEmpty()) {
      Code code = encounter.codes.get(0);
      return brProfile ? BrTerminologyResolver.resolveDisplay(code) : code.display;
    }
    if (encounter.reason != null) {
      if (brProfile) {
        return "Encontro: " + BrTerminologyResolver.resolveDisplay(encounter.reason);
      }
      if (encounter.reason.display != null) {
        return "Encontro: " + encounter.reason.display;
      }
    }
    return "Encontro clínico";
  }

  private static String formatExportDate(long timestamp, boolean brProfile) {
    if (brProfile) {
      return BR_DATE_FORMAT.format(
          Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
    }
    return dateFromTimestamp(timestamp);
  }

  private static String formatSexLabel(String gender) {
    if ("F".equalsIgnoreCase(gender)) {
      return "Feminino";
    }
    if ("M".equalsIgnoreCase(gender)) {
      return "Masculino";
    }
    return gender != null && !gender.isEmpty() ? gender : "—";
  }

  private static String stringAttr(Person person, String key) {
    Object value = person.attributes.get(key);
    return value != null ? value.toString() : "—";
  }

  private static String humanizeKey(String key) {
    return key.replace('_', ' ');
  }

  private static Configuration templateConfiguration() {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
    configuration.setDefaultEncoding("UTF-8");
    configuration.setLogTemplateExceptions(false);
    try {
      configuration.setSetting("object_wrapper",
          "DefaultObjectWrapper(2.3.26, forceLegacyNonListCollections=false, "
              + "iterableSupport=true, exposeFields=true)");
    } catch (TemplateException e) {
      throw new RuntimeException(e);
    }
    configuration.setAPIBuiltinEnabled(true);
    configuration.setClassLoaderForTemplateLoading(ClassLoader.getSystemClassLoader(),
        "templates/html");
    configuration.setLocale(Locale.forLanguageTag("pt-BR"));
    return configuration;
  }

  /** FreeMarker model for one patient accordion. */
  public static final class PatientNarrative {
    public String displayName;
    public String patientId;
    public int ageYears;
    public String sexLabel;
    public String primaryCondition;
    public boolean primaryConditionHighlight;
    public String lastEventDate;
    public String lastEventLabel;
    public String pathwayMode;
    public boolean pathwayTimelineEnabled;
    public List<PathwayPhaseSection> pathwayPhases;
    public List<TimelineEvent> outOfPathwayEvents;
    public DemographicsSection demographics;
    public List<RowItem> conditions;
    public List<RowItem> medications;
    public List<RowItem> exams;
    public List<RowItem> procedures;
    public List<RowItem> encounters;
    public List<RowItem> coverage;
    public List<TimelineEvent> timeline;
    public boolean aiEnriched;
    public String aiSummary;
    public int aiAppliedCount;
    public int aiFlagCount;
    public String aiWritingPersona;
    /** When true, hide empty clinical accordion sections (orientador mode). */
    public boolean hideNonPathwayClinicalSections;
    /** When true, render collapsible out-of-pathway timeline (pesquisador). */
    public boolean showOutOfPathwaySection;
  }

  /** One clinical pathway phase section for grouped HTML timeline. */
  public static final class PathwayPhaseSection {
    public String phaseId;
    public int order;
    public String title;
    public String description;
    public List<TimelineEvent> events;
  }

  /** Demographics nested section. */
  public static final class DemographicsSection {
    public String name;
    public String birthDate;
    public String age;
    public String sex;
    public String race;
    public String ethnicity;
    public String city;
    public String state;
    public String country;
  }

  /** Generic dated row for section lists. */
  public static final class RowItem {
    public long sortKey;
    public String startDate;
    public String endDate;
    public String label;
    public String detail;
  }

  /** Timeline event entry. */
  public static final class TimelineEvent {
    public long timestamp;
    public String date;
    public String type;
    public String label;
    public String phaseId;
    public boolean targetConditionHighlight;
    /** Stable key {@code system|code} for HTML dedup/collapse (Story C.1). */
    public String codeKey;
    /** Structured observation value used by orientador panel aggregation. */
    public String valueKey;
    public String valueLabel;
    public String unit;

    public TimelineEvent(long timestamp, String type, String label) {
      this.timestamp = timestamp;
      this.type = type;
      this.label = label;
    }
  }
}
