package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link AiEnrichmentConfig}.
 */
public class AiEnrichmentConfigTest {

  /**
   * Loads test properties and resets AI enrichment config before each test.
   *
   * @throws Exception on configuration loading errors
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    Config.set("br.profile", "br");
    Config.set("br.ai.enrichment.enabled", "false");
    Config.set("br.ai.provider", "openai");
    Config.remove("br.ai.model");
    Config.remove("br.ai.api_key");
  }

  /**
   * Clears AI enrichment overrides after each test.
   */
  @After
  public void tearDown() {
    Config.set("br.ai.enrichment.enabled", "false");
    Config.set("br.ai.provider", "openai");
    Config.remove("br.ai.model");
    Config.remove("br.ai.api_key");
    Config.set("br.profile", "");
  }

  @Test
  public void testDisabledByDefault() {
    assertFalse(AiEnrichmentConfig.isEnabled());
  }

  @Test
  public void testEnabledRequiresBrProfile() {
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.profile", "");
    assertFalse(AiEnrichmentConfig.isEnabled());
  }

  @Test
  public void testEnabledWhenConfigured() {
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.profile", "br");
    assertTrue(AiEnrichmentConfig.isEnabled());
  }

  @Test
  public void testDefaultProviderAndModel() {
    assertEquals("openai", AiEnrichmentConfig.getProvider());
    assertEquals("gpt-4o-mini", AiEnrichmentConfig.getModel());
  }

  @Test
  public void testDefaultRobustnessProperties() {
    assertEquals(1, AiEnrichmentConfig.getJsonParseRetries());
    assertEquals(1, AiEnrichmentConfig.getTruncationContinuationMax());
  }

  @Test
  public void testNegativeRobustnessRejected() {
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.ai.api_key", "sk-test");
    Config.set("br.ai.json_parse_retries", "-1");
    try {
      AiEnrichmentConfig.validateWhenEnabled();
      org.junit.Assert.fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("json_parse_retries"));
    } finally {
      Config.remove("br.ai.json_parse_retries");
      Config.remove("br.ai.api_key");
    }
  }

  @Test
  public void testGeminiDefaultModel() {
    Config.set("br.ai.provider", "gemini");
    Config.remove("br.ai.model");
    assertEquals("gemini-2.5-flash", AiEnrichmentConfig.getModel());
  }

  @Test
  public void testInvalidModelRejected() {
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.profile", "br");
    AiEnrichmentConfig.setTransientApiKey("test-key");
    Config.set("br.ai.model", "gpt-99-invalid");
    try {
      AiEnrichmentConfig.validateWhenEnabled();
      assertTrue("expected exception", false);
    } catch (IllegalStateException ex) {
      assertTrue(ex.getMessage().contains("br.ai.model inválido"));
    }
  }

  @Test
  public void testMedgemmaProviderAccepted() {
    Config.set("br.ai.enrichment.enabled", "true");
    Config.set("br.profile", "br");
    Config.set("br.ai.provider", "medgemma");
    Config.set("br.ai.model", "google/medgemma-4b-it");
    AiEnrichmentConfig.setTransientApiKey("hf_test");
    AiEnrichmentConfig.validateWhenEnabled();
  }

  @Test
  public void testValidateRequiresApiKey() {
    Config.set("br.ai.enrichment.enabled", "true");
    try {
      AiEnrichmentConfig.validateWhenEnabled();
      assertTrue("expected exception", false);
    } catch (IllegalStateException ex) {
      assertTrue(ex.getMessage().contains("api_key"));
    }
  }

  @Test
  public void testTransientApiKey() {
    AiEnrichmentConfig.setTransientApiKey("test-key");
    assertEquals("test-key", AiEnrichmentConfig.resolveApiKey());
    AiEnrichmentConfig.clearApiKey();
    assertTrue(AiEnrichmentConfig.resolveApiKey() == null
        || AiEnrichmentConfig.resolveApiKey().isEmpty());
  }
}
