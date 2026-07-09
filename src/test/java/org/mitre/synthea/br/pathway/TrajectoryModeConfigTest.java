package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link TrajectoryModeConfig}.
 */
public class TrajectoryModeConfigTest {

  /**
   * Reset trajectory configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.generation.trajectory_mode");
    Config.remove("br.target_condition");
  }

  @Test
  public void buildPathPredicate_lifespan_allowsBreastCancerModule() {
    Config.set("br.generation.trajectory_mode", "lifespan");
    assertTrue(TrajectoryModeConfig.buildPathPredicate().test("breast_cancer"));
  }

  @Test
  public void buildPathPredicate_episodic_allowsBreastCancerAndTrajectoryModules() {
    Config.set("br.generation.trajectory_mode", "episodic");
    Config.set("br.target_condition", "breast_cancer");
    assertTrue(TrajectoryModeConfig.buildPathPredicate().test("breast_cancer"));
    assertTrue(TrajectoryModeConfig.buildPathPredicate().test("breast_cancer_trajectory_br"));
  }

  @Test
  public void buildPathPredicate_episodicWithoutTargetCondition_throwsClearError() {
    Config.set("br.generation.trajectory_mode", "episodic");
    try {
      TrajectoryModeConfig.buildPathPredicate();
      fail("Expected IllegalStateException when br.target_condition is unset");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("br.target_condition"));
    }
  }
}
