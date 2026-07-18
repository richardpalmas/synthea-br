package org.mitre.synthea.br.plausibility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mitre.synthea.br.plausibility.PlausibilityReportAccumulator.ReportSnapshot;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.export.PostCompletionExporter;
import org.mitre.synthea.helpers.Config;

/**
 * Writes {@code plausibility_report.json} after generation completes.
 */
public final class PlausibilityReportWriter implements PostCompletionExporter {

  private static final String REPORT_FILENAME = "plausibility_report.json";

  /**
   * Output filename for the plausibility report (Epic 4).
   *
   * @return report file name in the output directory
   */
  public static String getReportFilename() {
    return REPORT_FILENAME;
  }

  @Override
  public void export(Generator generator, ExporterRuntimeOptions options) {
    if (!Config.getAsBoolean("br.plausibility.report.enabled", true)) {
      // Clear stale state from a prior enabled run in the same JVM.
      PlausibilityReportAccumulator.getInstance().reset();
      return;
    }

    try {
      writeReport(generator);
    } catch (Exception e) {
      System.err.println("PlausibilityReportWriter: failed to write plausibility_report.json: "
          + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Build and write the plausibility report to the exporter base directory.
   * Always resets the accumulator after the write attempt (success or failure)
   * so a failed I/O cannot contaminate the next generation.
   *
   * @param generator completed generator instance
   * @throws IOException when the report cannot be written
   */
  public static void writeReport(Generator generator) throws IOException {
    if (!Config.getAsBoolean("br.plausibility.report.enabled", true)) {
      PlausibilityReportAccumulator.getInstance().reset();
      return;
    }

    PlausibilityReportAccumulator accumulator = PlausibilityReportAccumulator.getInstance();
    try {
      Map<String, Object> report = buildReportPayload(
          generator.options.seed,
          generator.totalGeneratedPopulation.get(),
          accumulator);

      String baseDirectory = Config.get("exporter.baseDirectory");
      File outputBase = new File(baseDirectory);
      outputBase.mkdirs();

      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      String json = gson.toJson(report);

      Path reportPath = outputBase.toPath().resolve(REPORT_FILENAME);
      Files.write(reportPath, json.getBytes(StandardCharsets.UTF_8));
    } finally {
      accumulator.reset();
    }
  }

  /**
   * Build the report JSON structure from accumulator data.
   *
   * @param seed generation seed
   * @param totalPatients total patients generated
   * @param accumulator violation accumulator
   * @return report payload map
   */
  public static Map<String, Object> buildReportPayload(long seed, int totalPatients,
      PlausibilityReportAccumulator accumulator) {
    ReportSnapshot snapshot = accumulator.snapshot();
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("seed", seed);
    report.put("totalPatients", totalPatients);
    report.put("violationsByPatient", buildViolationsByPatient(snapshot));
    report.put("aggregates", buildAggregates(snapshot, totalPatients));
    return report;
  }

  private static List<Map<String, Object>> buildViolationsByPatient(ReportSnapshot snapshot) {
    List<Map<String, Object>> patientEntries = new ArrayList<>();
    for (Map.Entry<String, List<Violation>> entry : snapshot.violationsByPatient.entrySet()) {
      Map<String, Object> patientEntry = new LinkedHashMap<>();
      patientEntry.put("patientId", entry.getKey());
      patientEntry.put("violations", serializeViolations(entry.getValue()));
      patientEntries.add(patientEntry);
    }
    return patientEntries;
  }

  private static List<Map<String, Object>> serializeViolations(List<Violation> violations) {
    List<Map<String, Object>> serialized = new ArrayList<>();
    for (Violation violation : violations) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("ruleId", violation.getRuleId());
      item.put("severity", violation.getSeverity());
      item.put("description", violation.getDescription());
      item.put("eventTimestamps", violation.getEventTimestamps());
      serialized.add(item);
    }
    return serialized;
  }

  private static Map<String, Object> buildAggregates(ReportSnapshot snapshot, int totalPatients) {
    Map<String, Integer> counts = snapshot.severityCounts;
    Map<String, Object> aggregates = new LinkedHashMap<>();
    aggregates.put("alta", aggregateEntry(counts.getOrDefault("alta", 0), totalPatients));
    aggregates.put("media", aggregateEntry(counts.getOrDefault("média", 0), totalPatients));
    aggregates.put("baixa", aggregateEntry(counts.getOrDefault("baixa", 0), totalPatients));
    return aggregates;
  }

  private static Map<String, Object> aggregateEntry(int count, int totalPatients) {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("count", count);
    double percentage = totalPatients == 0 ? 0.0 : (100.0 * count) / totalPatients;
    entry.put("percentage", Math.round(percentage * 100.0) / 100.0);
    return entry;
  }
}
