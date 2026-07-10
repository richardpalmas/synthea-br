package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.mitre.synthea.helpers.Config;

/**
 * Unit tests for {@link PathwayCatalog} — parsing, canonical order, critical codes.
 */
public class PathwayCatalogTest {

  @After
  public void tearDown() {
    Config.remove("br.target_condition");
  }

  @Test
  public void loadForCondition_breastCancer_parsesWithoutError() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    assertNotNull(catalog);
    assertEquals("breast_cancer", catalog.getCondition());
  }

  @Test
  public void loadForCondition_breastCancer_exposesMinimumPhases() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    List<PathwayPhase> phases = catalog.getPhasesInOrder();
    List<String> phaseIds = phases.stream().map(PathwayPhase::getPhaseId).toList();
    assertTrue(phaseIds.containsAll(
        List.of("screening", "diagnosis", "staging", "treatment", "follow_up",
            "progression", "palliative")));
  }

  @Test
  public void getPhasesInOrder_returnsCanonicalOrder() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    List<String> phaseIds = catalog.getPhasesInOrder().stream()
        .map(PathwayPhase::getPhaseId).toList();
    assertEquals(
        List.of("screening", "diagnosis", "staging", "treatment", "follow_up",
            "progression", "palliative"),
        phaseIds);
  }

  @Test
  public void phase_hasRequiredStructure() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    for (PathwayPhase phase : catalog.getPhasesInOrder()) {
      assertFalse("phase_id must not be blank", phase.getPhaseId().isBlank());
      assertFalse("title_pt_br must not be blank", phase.getTitlePtBr().isBlank());
      assertFalse("description_pt_br must not be blank", phase.getDescriptionPtBr().isBlank());
      assertFalse("code_allowlist must not be empty for " + phase.getPhaseId(),
          phase.getCodeAllowlist().isEmpty());
      assertTrue("order must be positive", phase.getOrder() > 0);
    }
  }

  @Test
  public void codeAllowlist_containsCriticalTargetConditionCode() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    PathwayPhase diagnosis = catalog.getPhase("diagnosis");
    assertNotNull(diagnosis);
    boolean hasCriticalCode = diagnosis.getCodeAllowlist().stream()
        .anyMatch(entry -> "254837009".equals(entry.getCode())
            && "SNOMED-CT".equals(entry.getSystem()));
    assertTrue("diagnosis phase must include SNOMED 254837009", hasCriticalCode);
  }

  @Test
  public void getCatalogVersion_isExposedAndNotBlank() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    assertNotNull(catalog.getCatalogVersion());
    assertFalse(catalog.getCatalogVersion().isBlank());
  }

  @Test
  public void isAlwaysIncludeAttribute_recognizesDemographicsAndCohortMetadata() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    assertTrue(catalog.isAlwaysIncludeAttribute("age"));
    assertTrue(catalog.isAlwaysIncludeAttribute("gender"));
    assertTrue(catalog.isAlwaysIncludeAttribute("municipio_ibge"));
    assertFalse(catalog.isAlwaysIncludeAttribute("not_a_real_attribute"));
  }

  @Test
  public void getPhase_unknownPhaseId_returnsNull() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    assertEquals(null, catalog.getPhase("not_a_real_phase"));
  }

  @Test
  public void unifiedAllowlistCodes_includesEntriesFromAllPhases() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    Set<String> unified = catalog.unifiedAllowlistCodes();
    assertTrue(unified.contains("SNOMED-CT|254837009"));
    assertTrue(unified.contains("LOINC|21908-9"));
    assertTrue(unified.contains("RxNorm|198240"));
  }

  @Test
  public void loadForConfiguredCondition_resolvesFromTargetConditionProperty() {
    Config.set("br.target_condition", "breast_cancer");
    PathwayCatalog catalog = PathwayCatalog.loadForConfiguredCondition();
    assertEquals("breast_cancer", catalog.getCondition());
  }

  @Test
  public void loadForConfiguredCondition_unsetProperty_throwsClearError() {
    Config.remove("br.target_condition");
    try {
      PathwayCatalog.loadForConfiguredCondition();
      fail("Expected IllegalStateException when br.target_condition is unset");
    } catch (IllegalStateException expected) {
      assertTrue(expected.getMessage().contains("br.target_condition"));
    }
  }

  @Test
  public void loadForCondition_unknownCondition_throwsClearError() {
    try {
      PathwayCatalog.loadForCondition("condicao_inexistente_xyz");
      fail("Expected PathwayCatalogNotFoundException for unknown condition");
    } catch (PathwayCatalogNotFoundException expected) {
      assertTrue(expected.getMessage().contains("condicao_inexistente_xyz"));
    }
  }

  @Test
  public void loadForCondition_blank_throwsIllegalArgument() {
    try {
      PathwayCatalog.loadForCondition("  ");
      fail("Expected IllegalArgumentException for blank condition");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains("targetCondition"));
    }
  }

  @Test
  public void unifiedAllowlistCodes_isUnmodifiable() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    try {
      catalog.unifiedAllowlistCodes().add("SNOMED-CT|000");
      fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
  }

  @Test
  public void getCatalogVersion_equalsDataPackValue() {
    PathwayCatalog catalog = PathwayCatalog.loadForCondition("breast_cancer");
    assertEquals("2.0.0", catalog.getCatalogVersion());
  }
}
