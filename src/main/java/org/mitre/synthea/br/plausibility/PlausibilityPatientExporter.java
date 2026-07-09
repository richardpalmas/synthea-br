package org.mitre.synthea.br.plausibility;

import java.util.List;

import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.export.PatientExporter;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Evaluates plausibility rules per patient during export and accumulates violations.
 */
public final class PlausibilityPatientExporter implements PatientExporter {

  private final PlausibilityCatalog catalog;

  /**
   * Create exporter with the default catalog.
   */
  public PlausibilityPatientExporter() {
    this(PlausibilityCatalog.getInstance());
  }

  /**
   * Create exporter with a specific catalog (for testing).
   *
   * @param catalog plausibility catalog to evaluate
   */
  public PlausibilityPatientExporter(PlausibilityCatalog catalog) {
    this.catalog = catalog;
  }

  @Override
  public void export(Person person, long stopTime, ExporterRuntimeOptions options) {
    if (!Config.getAsBoolean("br.plausibility.report.enabled", true)) {
      return;
    }

    List<Violation> violations = catalog.evaluateAll(person);
    String patientId = HealthRecordScan.patientId(person);
    PlausibilityReportAccumulator.getInstance().recordPatientViolations(patientId, violations);
  }
}
