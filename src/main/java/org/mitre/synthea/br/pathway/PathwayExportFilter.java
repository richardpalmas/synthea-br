package org.mitre.synthea.br.pathway;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Code;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Report;

/**
 * Read-only export filter that retains only clinical events present in the active
 * {@link PathwayCatalog} allowlist (Story 9.3, AD-2). Operates on a cloned {@link Person} so the
 * simulated {@link HealthRecord} used during generation is not mutated.
 */
public final class PathwayExportFilter {

  private static final String SNOMED_URI = "http://snomed.info/sct";

  private PathwayExportFilter() {
  }

  /**
   * Whether pathway-focused export is enabled via {@link PathwayFocusConfig}.
   *
   * @return {@code true} when focus export should run
   */
  public static boolean isEnabled() {
    return PathwayFocusConfig.isEnabled();
  }

  /**
   * Returns a deep-cloned person whose health record contains only pathway-relevant entries.
   *
   * @param original person produced by simulation (left unchanged)
   * @return filtered clone for export
   */
  public static Person filterForExport(Person original) {
    PathwayFocusConfig.validateFocusPrerequisites();
    return filterForExport(original, PathwayCatalog.loadForConfiguredCondition());
  }

  /**
   * Returns a deep-cloned person filtered by the given catalog allowlist (read-only AD-2).
   *
   * @param original person produced by simulation (left unchanged)
   * @param catalog  pathway catalog defining the allowlist
   * @return filtered clone for export or HTML view
   */
  public static Person filterForExport(Person original, PathwayCatalog catalog) {
    Person filtered = clonePerson(original);
    if (filtered.hasMultipleRecords) {
      for (String key : filtered.records.keySet()) {
        filterRecord(filtered.records.get(key), catalog.unifiedAllowlistCodes());
      }
    } else {
      filterRecord(filtered.record, catalog.unifiedAllowlistCodes());
    }
    return filtered;
  }

  private static Person clonePerson(Person original) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
        out.writeObject(original);
      }
      try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
        return (Person) in.readObject();
      }
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Falha ao clonar paciente para export focado em trajetoria", e);
    }
  }

  private static void filterRecord(HealthRecord record, Set<String> allowlist) {
    for (Encounter encounter : record.encounters) {
      retainAllowed(encounter.conditions, allowlist);
      retainAllowed(encounter.allergies, allowlist);
      retainAllowed(encounter.procedures, allowlist);
      retainAllowed(encounter.medications, allowlist);
      retainAllowed(encounter.immunizations, allowlist);
      retainAllowed(encounter.careplans, allowlist);
      retainAllowed(encounter.imagingStudies, allowlist);
      retainAllowed(encounter.devices, allowlist);
      retainAllowed(encounter.supplies, allowlist);
      filterObservations(encounter.observations, allowlist);
      filterReports(encounter.reports, allowlist);
    }
    removeEmptyEncounters(record);
  }

  private static void filterObservations(List<Observation> observations, Set<String> allowlist) {
    Iterator<Observation> iterator = observations.iterator();
    while (iterator.hasNext()) {
      Observation observation = iterator.next();
      filterObservations(observation.observations, allowlist);
      if (!entryMatchesAllowlist(observation, allowlist)
          && observation.observations.isEmpty()) {
        iterator.remove();
      }
    }
  }

  private static void filterReports(List<Report> reports, Set<String> allowlist) {
    Iterator<Report> iterator = reports.iterator();
    while (iterator.hasNext()) {
      Report report = iterator.next();
      filterObservations(report.observations, allowlist);
      if (!entryMatchesAllowlist(report, allowlist)
          && report.observations.isEmpty()) {
        iterator.remove();
      }
    }
  }

  private static <E extends Entry> void retainAllowed(List<E> entries, Set<String> allowlist) {
    entries.removeIf(entry -> !entryMatchesAllowlist(entry, allowlist));
  }

  private static void removeEmptyEncounters(HealthRecord record) {
    record.encounters.removeIf(encounter -> !encounterHasRetainedClinicalContent(encounter));
  }

  private static boolean encounterHasRetainedClinicalContent(Encounter encounter) {
    return !encounter.conditions.isEmpty()
        || !encounter.allergies.isEmpty()
        || !encounter.observations.isEmpty()
        || !encounter.reports.isEmpty()
        || !encounter.procedures.isEmpty()
        || !encounter.medications.isEmpty()
        || !encounter.immunizations.isEmpty()
        || !encounter.careplans.isEmpty()
        || !encounter.imagingStudies.isEmpty()
        || !encounter.devices.isEmpty()
        || !encounter.supplies.isEmpty();
  }

  private static boolean entryMatchesAllowlist(Entry entry, Set<String> allowlist) {
    for (Code code : entry.codes) {
      if (allowlist.contains(toAllowlistKey(code.system, code.code))) {
        return true;
      }
    }
    if (entry.type != null && !entry.type.isBlank()) {
      if (allowlist.contains("SNOMED-CT|" + entry.type)) {
        return true;
      }
    }
    return false;
  }

  private static String toAllowlistKey(String system, String code) {
    return normalizeSystem(system) + "|" + code;
  }

  private static String normalizeSystem(String system) {
    if (system == null) {
      return "";
    }
    if (SNOMED_URI.equals(system)) {
      return "SNOMED-CT";
    }
    return system;
  }
}
