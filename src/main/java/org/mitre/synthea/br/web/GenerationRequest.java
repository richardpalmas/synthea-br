package org.mitre.synthea.br.web;

import java.util.ArrayList;
import java.util.List;

import org.mitre.synthea.br.ai.AiEnrichmentConfig;
import org.mitre.synthea.br.ai.AiModelCatalog;
import org.mitre.synthea.br.condition.GateMode;
import org.mitre.synthea.br.condition.SupportedConditions;

/**
 * Web UI request payload for a cohort generation run.
 */
public class GenerationRequest {

  private static final int MAX_POPULATION = 100_000;

  public long seed = 42L;
  public int population = 10;
  /**
   * {@code M}, {@code F}, or empty/null for any.
   */
  public String gender;
  public Integer minAge;
  public Integer maxAge;
  public boolean brProfile;
  /** Empty or null when no target condition. */
  public String targetCondition;
  public String gateMode = "retry";
  public boolean exportFhir = true;
  public boolean exportCsv = false;
  public boolean exportHtml = false;
  public boolean aiEnrichment = false;
  public String aiProvider = "openai";
  public String aiModel;
  public String aiApiKey;

  /**
   * Validate request fields.
   *
   * @return list of error messages in Portuguese; empty when valid
   */
  public List<String> validate() {
    List<String> errors = new ArrayList<>();
    if (population < 1) {
      errors.add("População deve ser um inteiro maior ou igual a 1.");
    } else if (population > MAX_POPULATION) {
      errors.add("População não pode exceder " + MAX_POPULATION
          + " (limite de proteção do MVP local).");
    }
    if (seed < 0) {
      errors.add("Seed deve ser um número não negativo.");
    }
    String normalizedGender = normalizedGender();
    if (normalizedGender != null && !normalizedGender.isEmpty()
        && !"M".equals(normalizedGender) && !"F".equals(normalizedGender)) {
      errors.add("Gênero deve ser M, F ou qualquer (vazio).");
    }
    if ((minAge == null) != (maxAge == null)) {
      errors.add("Informe idade mínima e máxima juntas, ou nenhuma das duas.");
    }
    if (minAge != null && maxAge != null && minAge > maxAge) {
      errors.add("Idade mínima não pode ser maior que a idade máxima.");
    }
    if (minAge != null && (minAge < 0 || minAge > 140)) {
      errors.add("Idade mínima deve estar entre 0 e 140.");
    }
    if (maxAge != null && (maxAge < 0 || maxAge > 140)) {
      errors.add("Idade máxima deve estar entre 0 e 140.");
    }
    if (targetCondition != null && !targetCondition.trim().isEmpty()) {
      if (SupportedConditions.get(targetCondition.trim()) == null) {
        errors.add("Condição alvo não suportada: " + targetCondition
            + ". Valores aceitos: " + SupportedConditions.supportedKeys());
      }
    }
    if (gateMode != null && !gateMode.isEmpty()) {
      try {
        GateMode.fromConfigValue(gateMode);
      } catch (IllegalArgumentException ex) {
        errors.add("Modo de gate inválido. Use retry ou exclude.");
      }
    }
    if (aiEnrichment) {
      if (!brProfile) {
        errors.add("Enriquecimento por IA requer perfil brasileiro ativo.");
      }
      if (aiApiKey == null || aiApiKey.trim().isEmpty()) {
        errors.add("Informe a API key do provedor de IA (BYOK).");
      }
      String provider = aiProvider == null ? "openai" : aiProvider.trim().toLowerCase();
      if (!AiModelCatalog.isSupportedProvider(provider)) {
        errors.add("Provedor de IA deve ser um de: "
            + String.join(", ", AiModelCatalog.getProviderIds()) + ".");
      }
      if (aiModel != null && !aiModel.trim().isEmpty()
          && !AiModelCatalog.isSupportedModel(provider, aiModel.trim())) {
        errors.add("Modelo de IA inválido para " + provider + ": " + aiModel.trim());
      }
      if (population > AiEnrichmentConfig.getMaxPatients()) {
        errors.add("Com IA ativa, população não pode exceder "
            + AiEnrichmentConfig.getMaxPatients() + " (br.ai.max_patients).");
      }
    }
    return errors;
  }

  /**
   * Returns {@code M}/{@code F} when set (trimmed and upper-cased), or {@code null} for any
   * gender.
   *
   * @return normalized gender code or null
   */
  public String normalizedGender() {
    if (gender == null) {
      return null;
    }
    String trimmed = gender.trim().toUpperCase();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
