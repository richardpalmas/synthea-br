package org.mitre.synthea.export;

/**
 * Test-only helper for configuring {@link FhirR4} static export flags from outside the
 * {@code org.mitre.synthea.export} package.
 */
public final class FhirR4TestSupport {

  private FhirR4TestSupport() {
  }

  /**
   * Snapshot of {@link FhirR4} static export flags for restore after tests.
   */
  public static final class FhirR4ExportState {
    public final boolean transactionBundle;
    public final boolean useUsCoreIg;
    public final String usCoreVersion;

    /**
     * Capture current static flags.
     *
     * @param transactionBundle transaction bundle flag
     * @param useUsCoreIg US Core IG flag
     * @param usCoreVersion US Core version string
     */
    public FhirR4ExportState(boolean transactionBundle, boolean useUsCoreIg,
        String usCoreVersion) {
      this.transactionBundle = transactionBundle;
      this.useUsCoreIg = useUsCoreIg;
      this.usCoreVersion = usCoreVersion;
    }
  }

  /**
   * Capture current {@link FhirR4} static export flags.
   *
   * @return snapshot for later restore
   */
  public static FhirR4ExportState captureState() {
    return new FhirR4ExportState(
        FhirR4.TRANSACTION_BUNDLE,
        FhirR4.USE_US_CORE_IG,
        FhirR4.US_CORE_VERSION);
  }

  /**
   * Restore {@link FhirR4} static export flags from a snapshot.
   *
   * @param state previously captured state
   */
  public static void restoreState(FhirR4ExportState state) {
    if (state == null) {
      return;
    }
    FhirR4.TRANSACTION_BUNDLE = state.transactionBundle;
    FhirR4.USE_US_CORE_IG = state.useUsCoreIg;
    FhirR4.US_CORE_VERSION = state.usCoreVersion;
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
