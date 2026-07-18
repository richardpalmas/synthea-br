package org.mitre.synthea.br.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
  private final int jsonParseRetries;
  private final int truncationContinuationMax;

  /**
   * Creates an orchestrator using configured robustness defaults.
   *
   * @param llmClient LLM client (or mock for tests)
   * @param maxIterations debate iteration cap per patient
   */
  public MaiDxoOrchestrator(LlmClient llmClient, int maxIterations) {
    this(llmClient, maxIterations, AiEnrichmentConfig.getJsonParseRetries(),
        AiEnrichmentConfig.getTruncationContinuationMax());
  }

  /**
   * Creates an orchestrator with explicit robustness limits (tests).
   *
   * @param llmClient LLM client (or mock for tests)
   * @param maxIterations debate iteration cap per patient
   * @param jsonParseRetries LLM clean retries after local parse failure
   * @param truncationContinuationMax max continuation calls per persona turn
   */
  public MaiDxoOrchestrator(LlmClient llmClient, int maxIterations, int jsonParseRetries,
      int truncationContinuationMax) {
    this.llmClient = llmClient;
    this.maxIterations = maxIterations;
    this.jsonParseRetries = Math.max(0, jsonParseRetries);
    this.truncationContinuationMax = Math.max(0, truncationContinuationMax);
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
    RobustnessStats stats = new RobustnessStats();
    boolean finalized = false;

    for (int iteration = 0; iteration < maxIterations && !finalized; iteration++) {
      debateLog.append("\n--- Iteração ").append(iteration + 1).append(" ---\n");
      List<Map<String, Object>> mergedOps = new ArrayList<>();

      for (String persona : PERSONA_CHAIN) {
        String response;
        try {
          response = LlmResponseGuard.completeWithContinuation(
              llmClient,
              PersonaPrompts.get(persona),
              buildUserPrompt(persona, debateLog.toString()),
              truncationContinuationMax,
              stats);
        } catch (LlmException e) {
          debateLog.append(persona).append(" ERRO: ").append(e.getMessage()).append('\n');
          continue;
        }
        debateLog.append('[').append(persona).append("] ").append(response).append('\n');

        PersonaDecision decision = parseDecision(response, stats);
        if (decision == null) {
          debateLog.append(persona).append(" ERRO: json_parse_failed\n");
          stats.incrementPersonaTurnsSkipped();
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
              mergedOps.addAll(decision.operations);
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

      CorrectionProposal iterationProposal = new CorrectionProposal(mergedOps);
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
        finalized,
        null,
        stats);
  }

  private static String buildUserPrompt(String persona, String context) {
    return "Contexto do debate:\n" + context + "\n\nPersona ativa: " + persona
        + "\nAnalise o caso e responda em JSON com: "
        + "{\"action\":\"...\",\"query\":\"...\",\"rationale\":\"...\","
        + "\"operations\":[{\"op\":\"...\", ...}]}\n"
        + "Para flag_unfixable, inclua reason obrigatório descrevendo a limitação.";
  }

  /**
   * Parses a persona reply with local extraction and optional LLM cleanup.
   *
   * @param raw model text
   * @param stats robustness counters
   * @return decision or null when parse failed after retries
   */
  PersonaDecision parseDecision(String raw, RobustnessStats stats) {
    LlmJsonParser.ParseResult parsed = LlmJsonParser.parseWithFallback(
        llmClient, raw, jsonParseRetries, stats);
    if (!parsed.isSuccess()) {
      return null;
    }
    JsonObject obj = parsed.getObject();
    PersonaDecision decision = new PersonaDecision();
    try {
      String action = readJsonString(obj, "action");
      if (action == null || action.trim().isEmpty()) {
        return null;
      }
      decision.action = action;
      decision.query = readJsonString(obj, "query");
      if (obj.has("operations") && obj.get("operations").isJsonArray()) {
        decision.operations = jsonArrayToMaps(obj.getAsJsonArray("operations"));
      }
      return decision;
    } catch (RuntimeException expected) {
      return null;
    }
  }

  /**
   * Reads a JSON string field, or null when missing / not a string primitive.
   *
   * @param obj JSON object
   * @param field field name
   * @return string value or null
   */
  private static String readJsonString(JsonObject obj, String field) {
    if (!obj.has(field) || obj.get(field).isJsonNull()) {
      return null;
    }
    JsonElement element = obj.get(field);
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      return null;
    }
    return element.getAsString();
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

  static final class PersonaDecision {
    private String action;
    private String query;
    private List<Map<String, Object>> operations;
  }
}
