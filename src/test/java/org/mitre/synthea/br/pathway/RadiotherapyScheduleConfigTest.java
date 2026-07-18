package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.mitre.synthea.helpers.Utilities;

/**
 * Contract tests for the breast-cancer radiotherapy course calendar.
 */
public class RadiotherapyScheduleConfigTest {

  @Test
  public void modalitiesDeclareClinicalFractionTargets() throws Exception {
    JsonObject states = states();

    assertEquals(25, value(states, "Set External Radiation Target"));
    assertEquals(15, value(states, "Set Hypofractionated Radiation Target"));
    assertEquals(1, value(states, "Set IORT Radiation Target"));
    assertEquals(1, value(states, "Initialize Radiation Fraction Count"));
    assertEquals(1, value(states, "Initialize Radiation Weekday Count"));
  }

  @Test
  public void cadenceUsesOneDayIntervalsAndThreeDayWeekendGap() throws Exception {
    JsonObject states = states();

    assertDelay(states, "Delay One Radiation Day", 1);
    assertDelay(states, "Delay Radiation Weekend", 3);
    assertFalse(states.has("Delay Until Next Cycle"));
    assertFalse(states.has("Increment Radiation Treatment Count"));
  }

  @Test
  public void eachModalityHasItsOwnFractionProcedure() throws Exception {
    JsonObject states = states();

    assertProcedureCode(states, "External Beam Radiation Fraction", "33195004");
    assertProcedureCode(states, "Hypofractionated Radiation Fraction", "385798007");
    assertProcedureCode(states, "IORT Radiation Fraction", "1287742003");
  }

  private static JsonObject states() throws Exception {
    String json = Utilities.readResource(
        "modules/breast_cancer/surgery_therapy_breast.json");
    return JsonParser.parseString(json).getAsJsonObject().getAsJsonObject("states");
  }

  private static int value(JsonObject states, String stateName) {
    return states.getAsJsonObject(stateName).get("value").getAsInt();
  }

  private static void assertDelay(JsonObject states, String stateName, int days) {
    JsonObject state = states.getAsJsonObject(stateName);
    JsonObject range = state.getAsJsonObject("range");
    assertEquals(days, range.get("low").getAsInt());
    assertEquals(days, range.get("high").getAsInt());
    assertEquals("days", range.get("unit").getAsString());
  }

  private static void assertProcedureCode(JsonObject states, String stateName, String code) {
    JsonObject state = states.getAsJsonObject(stateName);
    assertEquals("Procedure", state.get("type").getAsString());
    boolean found = false;
    for (JsonElement element : state.getAsJsonArray("codes")) {
      if (code.equals(element.getAsJsonObject().get("code").getAsString())) {
        found = true;
      }
    }
    assertTrue(found);
  }
}
