package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link LlmResponseGuard}.
 */
public class LlmResponseGuardTest {

  @Test
  public void testDetectsEnglishTruncationMarker() {
    assertTrue(LlmResponseGuard.looksTruncated(
        "{\"action\":\"ProposeCorrection\"} For brevity I have stopped"));
  }

  @Test
  public void testDetectsPortugueseTruncationMarker() {
    assertTrue(LlmResponseGuard.looksTruncated(
        "Resposta parcial... deseja que eu continue?"));
  }

  @Test
  public void testCompleteJsonNotTruncated() {
    assertFalse(LlmResponseGuard.looksTruncated("{\"action\":\"FinalizePatient\"}"));
  }

  @Test
  public void testValidJsonWithMarkerPhraseDoesNotContinue() throws Exception {
    // Clinical text containing a marker phrase but already valid JSON — no extra call.
    String valid = "{\"action\":\"AskQuestion\","
        + "\"query\":\"O paciente deseja que eu continue o acompanhamento?\"}";
    MockLlmClient mock = new MockLlmClient(valid);
    RobustnessStats stats = new RobustnessStats();
    String combined = LlmResponseGuard.completeWithContinuation(
        mock, "system", "user", 1, stats);
    assertEquals(valid, combined);
    assertEquals(0, stats.getTruncationContinuations());
    assertEquals(1, mock.getCallCount());
  }

  @Test
  public void testContinuationConcatenatesWhenParseFails() throws Exception {
    MockLlmClient mock = new MockLlmClient(
        "{\"action\":\"ProposeCorrection\" For brevity I have stopped",
        ",\"operations\":[]}");
    RobustnessStats stats = new RobustnessStats();
    String combined = LlmResponseGuard.completeWithContinuation(
        mock, "system", "user", 1, stats);
    assertTrue(combined.contains("ProposeCorrection"));
    assertTrue(combined.contains("operations"));
    assertFalse(LlmResponseGuard.looksTruncated(combined));
    assertEquals(1, stats.getTruncationContinuations());
    assertEquals(2, mock.getCallCount());
  }

  @Test
  public void testZeroContinuationsDoesNotRetry() throws Exception {
    MockLlmClient mock = new MockLlmClient(
        "Would you like me to continue with more?");
    RobustnessStats stats = new RobustnessStats();
    String combined = LlmResponseGuard.completeWithContinuation(
        mock, "system", "user", 0, stats);
    assertTrue(combined.contains("Would you like me to continue"));
    assertEquals(0, stats.getTruncationContinuations());
    assertEquals(1, mock.getCallCount());
  }
}
