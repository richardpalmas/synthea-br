package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link AiModelCatalog}.
 */
public class AiModelCatalogTest {

  @Test
  public void testProviderIdsIncludeMedgemma() {
    assertTrue(AiModelCatalog.getProviderIds().contains("openai"));
    assertTrue(AiModelCatalog.getProviderIds().contains("gemini"));
    assertTrue(AiModelCatalog.getProviderIds().contains("medgemma"));
    assertEquals(3, AiModelCatalog.getProviderIds().size());
  }

  @Test
  public void testDefaultModels() {
    assertEquals("gpt-4o-mini", AiModelCatalog.defaultModel("openai"));
    assertEquals("gemini-2.5-flash", AiModelCatalog.defaultModel("gemini"));
    assertEquals("google/medgemma-4b-it", AiModelCatalog.defaultModel("medgemma"));
  }

  @Test
  public void testIsSupportedModel() {
    assertTrue(AiModelCatalog.isSupportedModel("openai", "gpt-5.5"));
    assertTrue(AiModelCatalog.isSupportedModel("gemini", "gemini-3.5-flash"));
    assertTrue(AiModelCatalog.isSupportedModel("medgemma", "google/medgemma-4b-it"));
    assertFalse(AiModelCatalog.isSupportedModel("openai", "gemini-2.5-flash"));
    assertFalse(AiModelCatalog.isSupportedModel("gemini", "gpt-4o-mini"));
    assertFalse(AiModelCatalog.isSupportedModel("unknown", "gpt-4o-mini"));
  }

  @Test
  public void testApiKeyHints() {
    assertTrue(AiModelCatalog.apiKeyHint("openai").contains("OpenAI"));
    assertTrue(AiModelCatalog.apiKeyHint("gemini").contains("Gemini"));
    assertTrue(AiModelCatalog.apiKeyHint("medgemma").contains("hf_"));
  }

  @Test
  public void testMedgemmaProviderHasHelpText() {
    AiModelCatalog.ProviderDefinition medgemma = AiModelCatalog.getProviders().stream()
        .filter(p -> "medgemma".equals(p.getId()))
        .findFirst()
        .orElseThrow();
    assertTrue(medgemma.getProviderHelpText().contains("Inference"));
    assertEquals(2, medgemma.getModels().size());
  }
}
