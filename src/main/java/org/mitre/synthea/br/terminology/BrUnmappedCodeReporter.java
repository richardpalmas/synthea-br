package org.mitre.synthea.br.terminology;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.mitre.synthea.br.profile.BrProfile;
import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.export.Exporter.ExporterRuntimeOptions;
import org.mitre.synthea.export.PostCompletionExporter;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.concepts.HealthRecord.Code;

/**
 * Scans exported CSV output and lists clinical codes without a PT-BR mapping in
 * {@code br/terminology/} data packs. Supports incremental expansion of translations.
 */
public final class BrUnmappedCodeReporter implements PostCompletionExporter {

  private static final String REPORT_FILENAME = "br/terminology/_unmapped_report.csv";
  private static final String[] CSV_FILES = {
      "csv/conditions.csv",
      "csv/medications.csv",
      "csv/procedures.csv",
      "csv/observations.csv",
      "csv/encounters.csv",
      "csv/allergies.csv",
      "csv/immunizations.csv"
  };

  @Override
  public void export(Generator generator, ExporterRuntimeOptions options) {
    if (!BrProfile.isActive()) {
      return;
    }
    if (!Config.getAsBoolean("br.terminology.report.enabled", true)) {
      return;
    }
    try {
      String baseDirectory = Config.get("exporter.baseDirectory", "./output/");
      Path baseDir = Paths.get(baseDirectory).toAbsolutePath().normalize();
      writeReport(baseDir);
    } catch (Exception e) {
      System.err.println("BrUnmappedCodeReporter: failed to write unmapped report: "
          + e.getMessage());
    }
  }

  /**
   * Scans CSV exports under {@code outputDir} and writes the unmapped terminology report.
   *
   * @param outputDir exporter base directory (typically {@code output/})
   * @return path to the written report file
   * @throws IOException when report cannot be written
   */
  public static Path writeReport(Path outputDir) throws IOException {
    Set<String> seen = new HashSet<>();
    List<UnmappedEntry> unmapped = new ArrayList<>();
    for (String relative : CSV_FILES) {
      Path csvFile = outputDir.resolve(relative);
      if (Files.isRegularFile(csvFile)) {
        collectFromCsv(csvFile, seen, unmapped);
      }
    }
    unmapped.sort(Comparator.comparing(e -> e.system + "|" + e.code));
    Path reportPath = outputDir.resolve(REPORT_FILENAME);
    Files.createDirectories(reportPath.getParent());
    StringBuilder sb = new StringBuilder();
    sb.append("system,code,display_en,mapped").append(System.lineSeparator());
    for (UnmappedEntry entry : unmapped) {
      sb.append(csvEscape(entry.system)).append(',');
      sb.append(csvEscape(entry.code)).append(',');
      sb.append(csvEscape(entry.displayEn)).append(',');
      sb.append(entry.mapped ? "yes" : "no").append(System.lineSeparator());
    }
    Files.writeString(reportPath, sb.toString(), StandardCharsets.UTF_8);
    return reportPath;
  }

  private static void collectFromCsv(Path csvFile, Set<String> seen, List<UnmappedEntry> unmapped)
      throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        return;
      }
      Map<String, Integer> columns = parseHeader(headerLine);
      Integer codeIdx = columns.get("CODE");
      if (codeIdx == null) {
        return;
      }
      Integer systemIdx = columns.get("SYSTEM");
      Integer descIdx = columns.get("DESCRIPTION");
      Integer reasonCodeIdx = columns.get("REASONCODE");
      Integer reasonDescIdx = columns.get("REASONDESCRIPTION");
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = splitCsvLine(line);
        addIfUnmapped(fields, codeIdx, systemIdx, descIdx, seen, unmapped);
        if (reasonCodeIdx != null && reasonDescIdx != null
            && reasonCodeIdx < fields.length && reasonDescIdx < fields.length) {
          addIfUnmapped(fields, reasonCodeIdx, null, reasonDescIdx, seen, unmapped);
        }
      }
    }
  }

  private static void addIfUnmapped(String[] fields, int codeIdx, Integer systemIdx,
      Integer descIdx, Set<String> seen, List<UnmappedEntry> unmapped) {
    if (codeIdx >= fields.length) {
      return;
    }
    String code = fields[codeIdx].trim();
    if (code.isEmpty()) {
      return;
    }
    String system = systemIdx != null && systemIdx < fields.length
        ? fields[systemIdx].trim() : inferSystemFromCode(code);
    String display = descIdx != null && descIdx < fields.length
        ? fields[descIdx].trim() : "";
    String key = system + "|" + code + "|" + display;
    if (!seen.add(key)) {
      return;
    }
    Code clinical = new Code(system, code, display);
    boolean mapped = BrTerminologyResolver.hasMapping(system, code);
    if (!mapped) {
      unmapped.add(new UnmappedEntry(system, code, display, false));
    }
  }

  private static String inferSystemFromCode(String code) {
    if (code.matches("\\d+")) {
      return "RxNorm";
    }
    if (code.contains("-")) {
      return "LOINC";
    }
    return "SNOMED-CT";
  }

  private static Map<String, Integer> parseHeader(String headerLine) {
    Map<String, Integer> columns = new HashMap<>();
    String[] parts = splitCsvLine(headerLine);
    for (int i = 0; i < parts.length; i++) {
      columns.put(parts[i].trim().toUpperCase(Locale.ROOT), i);
    }
    return columns;
  }

  private static String[] splitCsvLine(String line) {
    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (c == ',' && !inQuotes) {
        fields.add(current.toString());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    fields.add(current.toString());
    return fields.toArray(new String[0]);
  }

  private static String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private static final class UnmappedEntry {
    private final String system;
    private final String code;
    private final String displayEn;
    private final boolean mapped;

    private UnmappedEntry(String system, String code, String displayEn, boolean mapped) {
      this.system = system;
      this.code = code;
      this.displayEn = displayEn;
      this.mapped = mapped;
    }
  }
}
