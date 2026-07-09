package org.mitre.synthea.export;

/**
 * Test-only helper for configuring {@link FhirR4} static export flags from outside the
 * {@code org.mitre.synthea.export} package.
 */
public final class FhirR4TestSupport {

  private FhirR4TestSupport() {
  }

  /**
   * Configure FHIR R4 exporter for base (non-US-Core) validation, matching
   * {@link FHIRR4ExporterTest#setupTestFhirExport()}.
   */
  public static void configureBaseR4Export() {
    FhirR4.reloadIncludeExclude();
    FhirR4.TRANSACTION_BUNDLE = true;
    FhirR4.USE_US_CORE_IG = false;
    FhirR4.US_CORE_VERSION = "4";
  }
}
