package org.mitre.synthea.br.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.world.agents.Person;

/**
 * MAI-DxO-inspired orchestrator: personas debate and propose structured corrections.
 */
public final class MaiDxoOrchestrator {

  private static final String[] PERSONA_CHAIN = {
      "hypothesis", "test_chooser", "stewardship", "checklist", "challenger"
  };

  private static final Gson GSON = new Gson();

  private final LlmClient llmClient;
  private final int maxIterations;

  /**
   * Creates an orchestrator.
   *
   * @param llmClient LLM client (or mock for tests)
   * @param maxIterations debate iteration cap per patient
   */
  public MaiDxoOrchestrator(LlmClient llmClient, int maxIterations) {
    this.llmClient = llmClient;
    this.maxIterations = maxIterations;
  }

  /**
   * Runs enrichment debate for one patient.
   *
   * @param person patient to enrich
   * @return enrichment result with audit trail
   */
  public PatientEnrichmentResult enrichPatient(Person person) {
    Gatekeeper gatekeeper = new Gatekeeper(person);
    StringBuilder debateLog = new StringBuilder();
    debateLog.append("=== Caso ===\n").append(gatekeeper.buildCaseSummary()).append('\n');
    debateLog.append("Encounters index: ")
        .append(GSON.toJson(gatekeeper.encounterIndex())).append('\n');

    List<Map<String, Object>> appliedOps = new ArrayList<>();
    List<Map<String, Object>> flags = new ArrayList<>();
    boolean finalized = false;

    for (int iteration = 0; iteration < maxIterations && !finalized; iteration++) {
      debateLog.append("\n--- Iteração ").append(iteration + 1).append(" ---\n");
      CorrectionProposal iterationProposal = new CorrectionProposal(new ArrayList<>());

      for (String persona : PERSONA_CHAIN) {
        String response;
        try {
          response = llmClient.complete(
              PersonaPrompts.get(persona),
              buildUserPrompt(persona, debateLog.toString()));
        } catch (LlmException e) {
          debateLog.append(persona).append(" ERRO: ").append(e.getMessage()).append('\n');
          continue;
        }
        debateLog.append('[').append(persona).append("] ").append(response).append('\n');

        PersonaDecision decision = parseDecision(response);
        if (decision.action == null) {
          continue;
        }

        switch (decision.action.toUpperCase(Locale.ROOT)) {
          case "ASKQUESTION":
          case "REQUESTRECORDSLICE":
            String gateAnswer = gatekeeper.respond(decision.action, decision.query);
            debateLog.append("Gatekeeper: ").append(gateAnswer).append('\n');
            break;
          case "PROPOSECORRECTION":
            if (decision.operations != null && !decision.operations.isEmpty()) {
              iterationProposal = new CorrectionProposal(decision.operations);
            }
            break;
          case "FINALIZEPATIENT":
            finalized = true;
            break;
          default:
            break;
        }
        if (finalized) {
          break;
        }
      }

      if (iterationProposal.hasOperations()) {
        List<Map<String, Object>> audit = CorrectionApplicator.apply(person, iterationProposal);
        for (Map<String, Object> row : audit) {
          if ("flagged".equals(row.get("status"))) {
            flags.add(row);
          } else if ("applied".equals(row.get("status"))) {
            appliedOps.add(row);
          }
        }
        debateLog.append("Correções aplicadas: ").append(GSON.toJson(audit)).append('\n');
      }
    }

    return new PatientEnrichmentResult(
        person.attributes.getOrDefault(Person.ID, "unknown").toString(),
        appliedOps,
        flags,
        debateLog.toString(),
        finalized);
  }

  private static String buildUserPrompt(String persona, String context) {
    return "Contexto do debate:\n" + context + "\n\nPersona ativa: " + persona
        + "\nAnalise o caso e responda em JSON com: "
        + "{\"action\":\"...\",\"query\":\"...\",\"rationale\":\"...\","
        + "\"operations\":[{\"op\":\"...\", ...}]}";
  }

  private static PersonaDecision parseDecision(String raw) {
    PersonaDecision decision = new PersonaDecision();
    if (raw == null || raw.trim().isEmpty()) {
      return decision;
    }
    String json = extractJson(raw);
    if (json == null) {
      return decision;
    }
    try {
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      if (obj.has("action")) {
        decision.action = obj.get("action").getAsString();
      }
      if (obj.has("query")) {
        decision.query = obj.get("query").getAsString();
      }
      if (obj.has("operations") && obj.get("operations").isJsonArray()) {
        decision.operations = jsonArrayToMaps(obj.getAsJsonArray("operations"));
      }
    } catch (Exception expected) {
      // Non-JSON model output — skip this persona turn
    }
    return decision;
  }

  private static String extractJson(String raw) {
    int start = raw.indexOf('{');
    int end = raw.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return raw.substring(start, end + 1);
    }
    return null;
  }

  private static List<Map<String, Object>> jsonArrayToMaps(JsonArray array) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (JsonElement element : array) {
      if (!element.isJsonObject()) {
        continue;
      }
      JsonObject obj = element.getAsJsonObject();
      Map<String, Object> map = new LinkedHashMap<>();
      for (String key : obj.keySet()) {
        JsonElement val = obj.get(key);
        if (val.isJsonPrimitive()) {
          if (val.getAsJsonPrimitive().isNumber()) {
            map.put(key, val.getAsNumber());
          } else if (val.getAsJsonPrimitive().isBoolean()) {
            map.put(key, val.getAsBoolean());
          } else {
            map.put(key, val.getAsString());
          }
        } else {
          map.put(key, val.toString());
        }
      }
      result.add(map);
    }
    return result;
  }

  private static final class PersonaDecision {
    private String action;
    private String query;
    private List<Map<String, Object>> operations;
  }
}
