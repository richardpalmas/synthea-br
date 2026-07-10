package org.mitre.synthea.br.plausibility;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Unit tests for {@link PlausibilityCatalogLoader}.
 */
public class PlausibilityCatalogLoaderTest {

  @Test
  public void testParseRulesRejectsDuplicateRuleIds() throws Exception {
    JsonArray rules = new JsonArray();
    rules.add(rule("PLAUS-001", "alta"));
    rules.add(rule("PLAUS-001", "média"));

    Method parseRules = PlausibilityCatalogLoader.class.getDeclaredMethod("parseRules",
        JsonArray.class);
    parseRules.setAccessible(true);

    try {
      parseRules.invoke(null, rules);
      fail("Expected duplicate rule id to throw");
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      assertTrue(cause instanceof IllegalStateException);
      assertTrue(cause.getMessage().contains("Duplicate rule id"));
    }
  }

  private static JsonObject rule(String id, String severity) {
    JsonObject rule = new JsonObject();
    rule.addProperty("id", id);
    rule.addProperty("severity", severity);
    rule.addProperty("title", id + " title");
    rule.addProperty("description", id + " description");
    return rule;
  }
}
