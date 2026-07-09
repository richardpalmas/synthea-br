package org.mitre.synthea.br.pathway;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.export.HtmlExporter.PatientNarrative;
import org.mitre.synthea.export.HtmlExporter.PathwayPhaseSection;
import org.mitre.synthea.export.HtmlExporter.TimelineEvent;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;
import org.mitre.synthea.world.concepts.HealthRecord.Report;

/**
 * Builds pathway-grouped timeline models for HTML export (Story 9.4, AD-2 read-only).
 */
public final class PathwayHtmlModelBuilder {

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

    narrative.pathwayTimelineEnabled = true;
    narrative.pathwayPhases = groupByPhase(catalog, pathwayEvents);
    narrative.outOfPathwayEvents = outOfPathwayEvents;
    narrative.timeline = pathwayEvents;

    if (PathwayHtmlModeConfig.MODE_ORIENTADOR.equals(mode)) {
      narrative.outOfPathwayEvents = List.of();
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
