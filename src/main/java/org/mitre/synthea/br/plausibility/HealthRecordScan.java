package org.mitre.synthea.br.plausibility;

import java.util.ArrayList;
import java.util.List;

import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;

/**
 * Read-only helpers for scanning {@link HealthRecord} entries across encounters.
 */
public final class HealthRecordScan {

  private HealthRecordScan() {
  }

  /**
   * Resolve patient identifier from person attributes.
   *
   * @param person patient
   * @return patient ID string
   */
  public static String patientId(Person person) {
    Object id = person.attributes.get(Person.ID);
    if (id != null) {
      return String.valueOf(id);
    }
    Object seedId = person.attributes.get(Person.IDENTIFIER_SEED_ID);
    return seedId == null ? "unknown" : String.valueOf(seedId);
  }

  /**
   * Return all conditions across encounters.
   *
   * @param person patient
   * @return condition entries
   */
  public static List<Entry> allConditions(Person person) {
    List<Entry> conditions = new ArrayList<>();
    for (Encounter encounter : person.record.encounters) {
      conditions.addAll(encounter.conditions);
    }
    return conditions;
  }

  /**
   * Return all procedures across encounters.
   *
   * @param person patient
   * @return procedure entries
   */
  public static List<Procedure> allProcedures(Person person) {
    List<Procedure> procedures = new ArrayList<>();
    for (Encounter encounter : person.record.encounters) {
      procedures.addAll(encounter.procedures);
    }
    return procedures;
  }

  /**
   * Return all medications across encounters.
   *
   * @param person patient
   * @return medication entries
   */
  public static List<Medication> allMedications(Person person) {
    List<Medication> medications = new ArrayList<>();
    for (Encounter encounter : person.record.encounters) {
      medications.addAll(encounter.medications);
    }
    return medications;
  }

  /**
   * Check whether an entry matches a clinical code (type field or codes list).
   *
   * @param entry health record entry
   * @param clinicalCode expected code
   * @return true when matched
   */
  public static boolean matchesCode(Entry entry, ClinicalCode clinicalCode) {
    if (clinicalCode.getCode().equals(entry.type)) {
      return true;
    }
    return entry.containsCode(clinicalCode.getCode(), clinicalCode.getSystem());
  }

  /**
   * Check whether an entry matches any code in the provided list.
   *
   * @param entry health record entry
   * @param codes candidate codes
   * @return true when any code matches
   */
  public static boolean matchesAnyCode(Entry entry, List<ClinicalCode> codes) {
    for (ClinicalCode code : codes) {
      if (matchesCode(entry, code)) {
        return true;
      }
    }
    return false;
  }
}
