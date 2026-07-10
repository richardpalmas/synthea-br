package org.mitre.synthea.br.web;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link GenerationRequest} validation.
 */
public class GenerationRequestValidationTest {

  @Test
  public void testValidMinimalRequest() {
    GenerationRequest request = new GenerationRequest();
    request.population = 10;
    request.seed = 42L;
    assertTrue(request.validate().isEmpty());
  }

  @Test
  public void testPopulationMustBePositive() {
    GenerationRequest request = new GenerationRequest();
    request.population = 0;
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("População")));
  }

  @Test
  public void testMinAgeGreaterThanMaxAgeRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.minAge = 70;
    request.maxAge = 40;
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("Idade mínima")));
  }

  @Test
  public void testUnknownTargetConditionRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "diabetes_tipo_x";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("Condição alvo")));
  }

  @Test
  public void testInvalidGateModeRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "breast_cancer";
    request.gateMode = "invalid";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("gate")));
  }

  @Test
  public void testAiEnrichmentRequiresBrProfileAndApiKey() {
    GenerationRequest request = new GenerationRequest();
    request.population = 2;
    request.aiEnrichment = true;
    request.aiApiKey = "";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("API key")));

    request.aiApiKey = "sk-test";
    request.brProfile = false;
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("perfil brasileiro")));
  }

  @Test
  public void testInvalidAiProviderRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 2;
    request.aiEnrichment = true;
    request.aiApiKey = "sk-test";
    request.aiProvider = "anthropic";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("Provedor de IA")));
  }

  @Test
  public void testInvalidAiModelRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 2;
    request.aiEnrichment = true;
    request.aiApiKey = "sk-test";
    request.aiProvider = "openai";
    request.aiModel = "gpt-99-unknown";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("Modelo de IA inválido")));
  }

  @Test
  public void testMedgemmaProviderAndModelAccepted() {
    GenerationRequest request = new GenerationRequest();
    request.population = 2;
    request.brProfile = true;
    request.aiEnrichment = true;
    request.aiApiKey = "hf_test";
    request.aiProvider = "medgemma";
    request.aiModel = "google/medgemma-4b-it";
    assertTrue(request.validate().isEmpty());
  }

  @Test
  public void testPathwayFocusWithoutTargetConditionRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.pathwayFocus = true;
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("condição clínica alvo")));
  }

  @Test
  public void testEpisodicRequiresTargetCondition() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.trajectoryMode = "episodic";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("condição clínica alvo")));

    request.targetCondition = "breast_cancer";
    assertTrue(request.validate().isEmpty());
  }

  @Test
  public void testEpisodicRejectsNonBreastCancerCondition() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "appendicitis";
    request.trajectoryMode = "episodic";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("apenas breast_cancer")));
  }

  @Test
  public void testSimulationWindowRequiresAgeRange() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "breast_cancer";
    request.simulationWindow = "pre_onset_years:10";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("idade mínima e máxima")));

    request.minAge = 45;
    request.maxAge = 75;
    assertTrue(request.validate().isEmpty());
  }

  @Test
  public void testSimulationWindowPreOnsetIncompatibleWithMinAge() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "breast_cancer";
    request.minAge = 10;
    request.maxAge = 75;
    request.simulationWindow = "pre_onset_years:10";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("incompatível")));
  }

  @Test
  public void testSimulationWindowPreOnsetOutsideBreastCancerRangeRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.targetCondition = "breast_cancer";
    request.minAge = 45;
    request.maxAge = 75;
    request.simulationWindow = "pre_onset_years:16";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("fora do intervalo piloto")));
  }

  @Test
  public void testPathwayMinimalWithoutTargetConditionRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.moduleProfile = "pathway_minimal";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("condição clínica alvo")));
  }

  @Test
  public void testInvalidHtmlPathwayModeRejected() {
    GenerationRequest request = new GenerationRequest();
    request.population = 5;
    request.htmlPathwayMode = "invalid";
    assertTrue(request.validate().stream().anyMatch(m -> m.contains("Modo HTML inválido")));
  }
}
