package org.mitre.synthea.br.pathway.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;

import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link ModuleProfileConfig} path predicates.
 */
public class ModuleProfileConfigTest {

  /**
   * Reset module profile configuration after each test.
   */
  @After
  public void tearDown() {
    Config.remove("br.generation.module_profile");
    Config.remove("br.target_condition");
  }

  @Test
  public void buildPathPredicate_fullProfile_allowsAllPaths() {
    Config.set("br.generation.module_profile", "full");
    assertTrue(ModuleProfileConfig.buildPathPredicate().test("dental"));
    assertTrue(ModuleProfileConfig.buildPathPredicate().test("core/Cardiovascular Disease"));
  }

  @Test
  public void buildPathPredicate_pathwayMinimal_excludesDentalAndVeteran() {
    Config.set("br.generation.module_profile", "pathway_minimal");
    assertFalse(ModuleProfileConfig.buildPathPredicate().test("dental"));
    assertFalse(ModuleProfileConfig.buildPathPredicate().test("veteran"));
    assertFalse(ModuleProfileConfig.buildPathPredicate().test("core/Cardiovascular Disease"));
  }

  @Test
  public void buildPathPredicate_pathwayMinimal_allowsWellnessAndBreastCancer() {
    Config.set("br.generation.module_profile", "pathway_minimal");
    Config.set("br.target_condition", "breast_cancer");
    assertTrue(ModuleProfileConfig.buildPathPredicate().test("wellness_encounters"));
    assertTrue(ModuleProfileConfig.buildPathPredicate().test("breast_cancer"));
    assertTrue(ModuleProfileConfig.buildPathPredicate().test("core/Lifecycle Module"));
    assertFalse("episodic marker must not be in pathway_minimal allowlist",
        ModuleProfileConfig.buildPathPredicate().test("breast_cancer_trajectory_br"));
  }

  @Test
  public void getActiveProfileVersion_pathwayMinimal_exposesDataPackVersion() {
    Config.set("br.generation.module_profile", "pathway_minimal");
    assertEquals("1.0.0", ModuleProfileConfig.getActiveProfileVersion());
  }

  @Test
  public void getActiveProfileVersion_full_returnsNull() {
    Config.set("br.generation.module_profile", "full");
    assertEquals(null, ModuleProfileConfig.getActiveProfileVersion());
  }

  @Test
  public void buildPathPredicate_invalidProfile_throwsClearError() {
    Config.set("br.generation.module_profile", "invalido");
    try {
      ModuleProfileConfig.buildPathPredicate();
      fail("Expected IllegalArgumentException for invalid profile");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("invalido"));
      assertTrue(expected.getMessage().contains("pathway_minimal"));
    }
  }
}
