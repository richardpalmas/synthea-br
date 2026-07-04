package org.mitre.synthea.br.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;

/**
 * Applies validated correction operations to a person's {@link HealthRecord}.
 */
public final class CorrectionApplicator {

  private CorrectionApplicator() {
  }

  /**
   * Applies all operations from the proposal and returns an audit trail.
   *
   * @param person patient to mutate
   * @param proposal structured corrections
   * @return list of applied/skipped operation summaries
   */
  public static List<Map<String, Object>> apply(Person person, CorrectionProposal proposal) {
    List<Map<String, Object>> audit = new ArrayList<>();
    HealthRecord record = person.record;

    for (Map<String, Object> op : proposal.getOperations()) {
      Map<String, Object> entry = new LinkedHashMap<>(op);
      try {
        String opType = stringField(op, "op");
        if (opType == null || !CorrectionProposal.SUPPORTED_OPS.contains(opType)) {
          entry.put("status", "skipped");
          entry.put("reason", "unsupported_op");
          audit.add(entry);
          continue;
        }
        switch (opType) {
          case "add_observation":
            applyAddObservation(record, op);
            entry.put("status", "applied");
            break;
          case "fix_encounter_date":
            applyFixEncounterDate(record, op);
            entry.put("status", "applied");
            break;
          case "set_person_attribute":
            applySetAttribute(person, op);
            entry.put("status", "applied");
            break;
          case "add_procedure":
            applyAddProcedure(record, op);
            entry.put("status", "applied");
            break;
          case "flag_unfixable":
            entry.put("status", "flagged");
            break;
          default:
            entry.put("status", "skipped");
            entry.put("reason", "unknown_op");
        }
      } catch (Exception ex) {
        entry.put("status", "error");
        entry.put("reason", ex.getMessage());
      }
      audit.add(entry);
    }
    return audit;
  }

  private static void applyAddObservation(HealthRecord record, Map<String, Object> op) {
    String code = requiredString(op, "code");
    Object value = op.get("value");
    if (value == null) {
      throw new IllegalArgumentException("add_observation requires value");
    }
    long time = parseTime(op.get("time"));
    String display = stringField(op, "display");
    Encounter encounter = record.currentEncounter(time);
    if (display != null) {
      encounter.addObservation(time, code, value, display);
    } else {
      encounter.addObservation(time, code, value);
    }
    String units = stringField(op, "units");
    if (units != null) {
      Observation obs = encounter.findObservation(code);
      if (obs != null) {
        obs.unit = units;
      }
    }
  }

  private static void applyFixEncounterDate(HealthRecord record, Map<String, Object> op) {
    int index = intField(op, "encounterIndex");
    if (index < 0 || index >= record.encounters.size()) {
      throw new IllegalArgumentException("encounterIndex out of range: " + index);
    }
    long newTime = parseTime(op.get("newTime"));
    Encounter encounter = record.encounters.get(index);
    encounter.start = newTime;
    if (encounter.stop > 0 && encounter.stop < newTime) {
      encounter.stop = newTime;
    }
  }

  private static void applySetAttribute(Person person, Map<String, Object> op) {
    String key = requiredString(op, "key");
    Object value = op.get("value");
    if (value == null) {
      throw new IllegalArgumentException("set_person_attribute requires value");
    }
    person.attributes.put(key, value);
  }

  private static void applyAddProcedure(HealthRecord record, Map<String, Object> op) {
    String code = requiredString(op, "code");
    long time = parseTime(op.get("time"));
    record.procedure(time, code);
  }

  private static long parseTime(Object raw) {
    if (raw == null) {
      throw new IllegalArgumentException("time is required");
    }
    if (raw instanceof Number) {
      return ((Number) raw).longValue();
    }
    String text = raw.toString().trim();
    if (text.matches("\\d+")) {
      return Long.parseLong(text);
    }
    LocalDate date = LocalDate.parse(text);
    return Utilities.localDateToTimestamp(date);
  }

  private static String requiredString(Map<String, Object> op, String key) {
    String value = stringField(op, key);
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(key + " is required");
    }
    return value;
  }

  private static String stringField(Map<String, Object> op, String key) {
    Object value = op.get(key);
    return value == null ? null : value.toString();
  }

  private static int intField(Map<String, Object> op, String key) {
    Object value = op.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    if (value == null) {
      throw new IllegalArgumentException(key + " is required");
    }
    return Integer.parseInt(value.toString());
  }
}
