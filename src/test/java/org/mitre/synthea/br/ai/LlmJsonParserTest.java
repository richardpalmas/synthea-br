package org.mitre.synthea.br.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link LlmJsonParser}.
 */
public class LlmJsonParserTest {

  @Test
  public void testParseValidJson() {
    LlmJsonParser.ParseResult result = LlmJsonParser.parseLocal(
        "{\"action\":\"FinalizePatient\"}");
    assertTrue(result.isSuccess());
    assertEquals("FinalizePatient", result.getObject().get("action").getAsString());
  }

  @Test
  public void testParseMarkdownFence() {
    String raw = "```json\n{\"action\":\"AskQuestion\",\"query\":\"idade\"}\n```";
    LlmJsonParser.ParseResult result = LlmJsonParser.parseLocal(raw);
    assertTrue(result.isSuccess());
    assertEquals("AskQuestion", result.getObject().get("action").getAsString());
  }

  @Test
  public void testParseInvalidWithoutFallback() {
    LlmJsonParser.ParseResult result = LlmJsonParser.parseLocal("not json at all");
    assertFalse(result.isSuccess());
    assertNotNull(result.getErrorMessage());
  }

  @Test
  public void testFallbackCleansInvalidJson() {
    MockLlmClient mock = new MockLlmClient("{\"action\":\"FinalizePatient\"}");
    RobustnessStats stats = new RobustnessStats();
    LlmJsonParser.ParseResult result = LlmJsonParser.parseWithFallback(
        mock, "HERE IS BAD {{{", 1, stats);
    assertTrue(result.isSuccess());
    assertEquals(1, stats.getJsonParseRetries());
    assertEquals(1, mock.getCallCount());
  }

  @Test
  public void testFallbackExhausted() {
    MockLlmClient mock = new MockLlmClient("still not json", "also broken");
    RobustnessStats stats = new RobustnessStats();
    LlmJsonParser.ParseResult result = LlmJsonParser.parseWithFallback(
        mock, "garbage", 2, stats);
    assertFalse(result.isSuccess());
    assertEquals(2, stats.getJsonParseRetries());
    assertEquals(2, mock.getCallCount());
  }

  @Test
  public void testZeroRetriesSkipsLlm() {
    MockLlmClient mock = new MockLlmClient("{\"action\":\"FinalizePatient\"}");
    LlmJsonParser.ParseResult result = LlmJsonParser.parseWithFallback(
        mock, "garbage", 0, null);
    assertFalse(result.isSuccess());
    assertEquals(0, mock.getCallCount());
  }
}
