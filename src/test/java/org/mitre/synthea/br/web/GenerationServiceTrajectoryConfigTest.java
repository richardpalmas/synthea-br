package org.mitre.synthea.br.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link GenerationService#applyTrajectoryToConfig(GenerationRequest)}.
 */
public class GenerationServiceTrajectoryConfigTest {

  private String previousPathwayFocus;
  private String previousHtmlMode;
  private String previousModuleProfile;
  private String previousTrajectoryMode;
  private String previousSimulationWindow;

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
    previousPathwayFocus = Config.get("br.pathway.focus");
    previousHtmlMode = Config.get("exporter.html.pathway_mode");
    previousModuleProfile = Config.get("br.generation.module_profile");
    previousTrajectoryMode = Config.get("br.generation.trajectory_mode");
    previousSimulationWindow = Config.get("br.generation.simulation_window");
  }

  @After
  public void tearDown() {
    restore("br.pathway.focus", previousPathwayFocus);
    restore("exporter.html.pathway_mode", previousHtmlMode);
    restore("br.generation.module_profile", previousModuleProfile);
    restore("br.generation.trajectory_mode", previousTrajectoryMode);
    restore("br.generation.simulation_window", previousSimulationWindow);
  }

  private static void restore(String key, String previous) {
    if (previous != null) {
      Config.set(key, previous);
    } else {
      Config.remove(key);
    }
  }

  @Test
  public void testApplyFocusedTrajectorySetsConfigKeys() {
    GenerationRequest request = new GenerationRequest();
    request.pathwayFocus = true;
    request.htmlPathwayMode = TrajectoryWebConstants.HTML_MODE_ORIENTADOR;
    request.moduleProfile = TrajectoryWebConstants.MODULE_PROFILE_PATHWAY_MINIMAL;
    request.trajectoryMode = TrajectoryWebConstants.TRAJECTORY_MODE_EPISODIC;
    request.simulationWindow = TrajectoryWebConstants.SIMULATION_WINDOW_PRE_ONSET_10;

    GenerationService.applyTrajectoryToConfig(request);

    assertEquals("true", Config.get("br.pathway.focus"));
    assertEquals("orientador", Config.get("exporter.html.pathway_mode"));
    assertEquals("pathway_minimal", Config.get("br.generation.module_profile"));
    assertEquals("episodic", Config.get("br.generation.trajectory_mode"));
    assertEquals("pre_onset_years:10", Config.get("br.generation.simulation_window"));
  }

  @Test
  public void testApplyDefaultsRemovesTrajectoryKeys() {
    Config.set("br.pathway.focus", "true");
    Config.set("exporter.html.pathway_mode", "orientador");
    Config.set("br.generation.module_profile", "pathway_minimal");
    Config.set("br.generation.trajectory_mode", "episodic");
    Config.set("br.generation.simulation_window", "pre_onset_years:10");

    GenerationRequest request = new GenerationRequest();
    GenerationService.applyTrajectoryToConfig(request);

    assertNull(Config.get("br.pathway.focus"));
    assertNull(Config.get("exporter.html.pathway_mode"));
    assertNull(Config.get("br.generation.module_profile"));
    assertNull(Config.get("br.generation.trajectory_mode"));
    assertNull(Config.get("br.generation.simulation_window"));
  }
}
