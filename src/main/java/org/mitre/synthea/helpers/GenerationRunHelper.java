package org.mitre.synthea.helpers;

import java.io.IOException;

import org.mitre.synthea.br.output.BrOutputDirectoryResolver;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter;

/**
 * Shared bootstrap helpers for CLI ({@code App}) and web generation ({@code GenerationService}).
 */
public final class GenerationRunHelper {

  private GenerationRunHelper() {
  }

  /**
   * Prepares the output directory for a generation run (timestamped subfolder when enabled).
   *
   * @return effective {@code exporter.baseDirectory} for this run
   * @throws IOException when the run directory cannot be created
   */
  public static String prepareOutputDirectory() throws IOException {
    return BrOutputDirectoryResolver.prepareRunDirectory();
  }

  /**
   * Reset generator options from the current {@link Config} after property overrides.
   *
   * @param options generator options to update
   * @param exportOptions exporter runtime options to update
   */
  public static void resetOptionsFromConfig(Generator.GeneratorOptions options,
      Exporter.ExporterRuntimeOptions exportOptions) {
    options.population = Config.getAsInteger("generate.default_population", 1);
    options.threadPoolSize = Config.getAsInteger("generate.thread_pool_size", -1);

    exportOptions.yearsOfHistory = Config.getAsInteger("exporter.years_of_history", 10);
    exportOptions.terminologyService = !Config.get("generate.terminology_service_url", "")
        .isEmpty();
  }

  /**
   * Validate configuration before starting generation.
   *
   * @param options generator options
   * @param overrideFutureDateError when true, skip end-time vs history check
   * @return {@code true} when configuration is valid
   */
  public static boolean validateConfig(Generator.GeneratorOptions options,
      boolean overrideFutureDateError) {
    boolean valid = true;
    if (Config.getAsBoolean("exporter.fhir.transaction_bundle")
        && !Config.getAsBoolean("exporter.practitioner.fhir.export")
        && !Config.getAsBoolean("exporter.hospital.fhir.export")) {
      System.out.println("Warning: Synthea is configured to export FHIR transaction bundles "
          + "for generated patients but not to export the practitioners and organizations "
          + "that the patient bundle entries will reference. "
          + "See https://github.com/synthetichealth/synthea/wiki/FHIR-Transaction-Bundles "
          + "for more information.");
    }
    if (!overrideFutureDateError) {
      int yearsOfHistory = Config.getAsInteger("exporter.years_of_history");
      long millisToEndTime = options.endTime - System.currentTimeMillis();
      if (millisToEndTime > Utilities.convertTime("years", yearsOfHistory)) {
        System.out.println("Error: the specified end time is further in the future than the "
            + "number of years of export history. The first exported events will be in the "
            + "future. Consider adjusting the values of the '-e endDate' command line switch "
            + "and the 'exporter.years_of_history' configuration file entry.\n"
            + "You may override this error by using '-E endDate' instead of '-e EndDate'.");
        valid = false;
      }
    }
    return valid;
  }
}
