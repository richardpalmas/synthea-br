package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link PathwayHtmlModeConfig} (Story 9.4).
 */
public class PathwayHtmlModeConfigTest {

  private String previousMode;
  private String previousFocus;

  /**
   * Loads test properties and captures pathway HTML config.
   *
   * @throws Exception on config load failure
   */
  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    previousMode = Config.get("exporter.html.pathway_mode");
    previousFocus = Config.get("br.pathway.focus");
  }

  /**
   * Restores pathway HTML config after each test.
   */
  @After
  public void tearDown() {
    restore("exporter.html.pathway_mode", previousMode);
    restore("br.pathway.focus", previousFocus);
  }

  @Test
  public void defaultIsOrientadorWhenFocusEnabled() {
    Config.remove("exporter.html.pathway_mode");
    Config.set("br.pathway.focus", "true");
    assertEquals(PathwayHtmlModeConfig.MODE_ORIENTADOR, PathwayHtmlModeConfig.resolveMode());
  }

  @Test
  public void defaultIsFullWhenFocusDisabled() {
    Config.remove("exporter.html.pathway_mode");
    Config.set("br.pathway.focus", "false");
    assertEquals(PathwayHtmlModeConfig.MODE_FULL, PathwayHtmlModeConfig.resolveMode());
  }

  @Test
  public void invalidModeThrowsClearError() {
    Config.set("exporter.html.pathway_mode", "invalid-mode");
    try {
      PathwayHtmlModeConfig.resolveMode();
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("exporter.html.pathway_mode"));
      assertTrue(expected.getMessage().contains("orientador"));
    }
  }

  @Test
  public void hidesOutOfPathwayOnlyForOrientador() {
    assertTrue(PathwayHtmlModeConfig.hidesOutOfPathwayClinicalData(
        PathwayHtmlModeConfig.MODE_ORIENTADOR));
    assertFalse(PathwayHtmlModeConfig.hidesOutOfPathwayClinicalData(
        PathwayHtmlModeConfig.MODE_PESQUISADOR));
    assertFalse(PathwayHtmlModeConfig.hidesOutOfPathwayClinicalData(
        PathwayHtmlModeConfig.MODE_FULL));
  }

  private static void restore(String key, String previous) {
    if (previous != null) {
      Config.set(key, previous);
    } else {
      Config.remove(key);
    }
  }
}
