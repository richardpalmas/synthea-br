package org.mitre.synthea.br.ai;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.world.agents.Person;
import org.mitre.synthea.world.concepts.HealthRecord;
import org.mitre.synthea.world.concepts.HealthRecord.Encounter;
import org.mitre.synthea.world.concepts.HealthRecord.Entry;
import org.mitre.synthea.world.concepts.HealthRecord.Medication;
import org.mitre.synthea.world.concepts.HealthRecord.Observation;
import org.mitre.synthea.world.concepts.HealthRecord.Procedure;

/**
 * Gatekeeper: holds the full case and answers only explicitly requested slices.
 */
public final class Gatekeeper {

  private static final DateTimeFormatter ISO_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.ROOT).withZone(ZoneOffset.UTC);

  private final Person person;

  /**
   * Creates a gatekeeper for the given patient.
   *
   * @param person patient with populated record
   */
  public Gatekeeper(Person person) {
    this.person = person;
  }

  /**
   * Returns a minimal case summary (no full record dump).
   *
   * @return summary for initial persona context
   */
  public String buildCaseSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Paciente sintético. Queixa inicial: dados gerados pelo Synthea-br.\n");
    if (person.attributes.containsKey(Person.GENDER)) {
      sb.append("Sexo: ").append(person.attributes.get(Person.GENDER)).append("\n");
    }
    if (person.attributes.containsKey(Person.BIRTHDATE)) {
      long birth = (long) person.attributes.get(Person.BIRTHDATE);
      sb.append("Nascimento: ").append(ISO_DATE.format(Instant.ofEpochMilli(birth))).append("\n");
    }
    HealthRecord record = person.record;
    sb.append("Encounters: ").append(record.encounters.size()).append("\n");
    sb.append("Condições ativas: ").append(record.present.keySet().size()).append("\n");
    return sb.toString();
  }

  /**
   * Answers a persona query. Only returns data explicitly requested.
   *
   * @param action action type from persona
   * @param query query payload
   * @return gatekeeper response text
   */
  public String respond(String action, String query) {
    if (query == null || query.trim().isEmpty()) {
      return "Nenhuma informação disponível para consulta vazia.";
    }
    String normalized = query.trim().toLowerCase(Locale.ROOT);

    if ("askquestion".equalsIgnoreCase(action)) {
      return answerQuestion(normalized);
    }
    if ("requestrecordslice".equalsIgnoreCase(action)) {
      return answerRecordSlice(normalized);
    }
    return "Ação não reconhecida pelo Gatekeeper.";
  }

  private String answerQuestion(String query) {
    Map<String, Object> attrs = person.attributes;
    if (query.contains("municip") || query.contains("cidade")) {
      Object city = attrs.get("CITY");
      Object state = attrs.get("STATE");
      if (city == null && state == null) {
        return "Município/UF: não informado no prontuário.";
      }
      return "Localização: " + (city != null ? city : "?") + " / " + (state != null ? state : "?");
    }
    if (query.contains("febre") || query.contains("fever")) {
      return "Febre: não consta medição de temperatura elevada nos registros consultados.";
    }
    if (query.contains("sexo") || query.contains("gênero") || query.contains("gender")) {
      return "Sexo: " + attrs.getOrDefault(Person.GENDER, "não informado");
    }
    if (query.contains("idade") || query.contains("age")) {
      return "Idade atual (aprox.): "
          + person.ageInYears(person.record.lastEncounterTime()) + " anos";
    }
    return "Informação solicitada não encontrada ou consulta muito ampla. "
        + "Seja mais específico (ex.: 'idade', 'município', 'exames laboratoriais').";
  }

  private String answerRecordSlice(String query) {
    HealthRecord record = person.record;
    StringBuilder sb = new StringBuilder();

    if (query.contains("observation") || query.contains("exame") || query.contains("lab")) {
      List<String> lines = new ArrayList<>();
      for (Encounter enc : record.encounters) {
        for (Observation obs : enc.observations) {
          lines.add(formatEntry(obs) + " value=" + obs.value
              + (obs.unit != null ? " " + obs.unit : ""));
        }
      }
      if (lines.isEmpty()) {
        return "Nenhum exame/observação registrado.";
      }
      return "Observações:\n- " + String.join("\n- ", lines);
    }

    if (query.contains("procedure") || query.contains("procedimento")) {
      List<String> lines = new ArrayList<>();
      for (Encounter enc : record.encounters) {
        for (Procedure proc : enc.procedures) {
          lines.add(formatEntry(proc));
        }
      }
      if (lines.isEmpty()) {
        return "Nenhum procedimento registrado.";
      }
      return "Procedimentos:\n- " + String.join("\n- ", lines);
    }

    if (query.contains("medication") || query.contains("medicamento")) {
      List<String> lines = new ArrayList<>();
      for (Encounter enc : record.encounters) {
        for (Medication med : enc.medications) {
          lines.add(formatEntry(med));
        }
      }
      if (lines.isEmpty()) {
        return "Nenhum medicamento registrado.";
      }
      return "Medicamentos:\n- " + String.join("\n- ", lines);
    }

    if (query.contains("encounter") || query.contains("atendimento")) {
      List<String> lines = new ArrayList<>();
      for (int i = 0; i < record.encounters.size(); i++) {
        Encounter enc = record.encounters.get(i);
        lines.add("[" + i + "] " + enc.type + " " + formatEntry(enc));
      }
      if (lines.isEmpty()) {
        return "Nenhum atendimento registrado.";
      }
      return "Atendimentos:\n- " + String.join("\n- ", lines);
    }

    return "Tipo de registro não identificado. "
        + "Use: exames, procedimentos, medicamentos ou atendimentos.";
  }

  private static String formatEntry(Entry entry) {
    return entry.type + " @ " + ISO_DATE.format(Instant.ofEpochMilli(entry.start));
  }

  /**
   * Exports encounter index map for correction proposals.
   *
   * @return encounter metadata
   */
  public List<Map<String, Object>> encounterIndex() {
    List<Map<String, Object>> result = new ArrayList<>();
    int i = 0;
    for (Encounter enc : person.record.encounters) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("encounterIndex", i++);
      row.put("type", enc.type);
      row.put("start", ISO_DATE.format(Instant.ofEpochMilli(enc.start)));
      result.add(row);
    }
    return result;
  }
}
