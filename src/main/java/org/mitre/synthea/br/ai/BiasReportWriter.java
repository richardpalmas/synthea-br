package org.mitre.synthea.br.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.helpers.Config;

/**
 * Writes aggregated demographic bias reports without PHI (Story 8.2).
 */
public final class BiasReportWriter {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final String[] CLINICAL_KEYWORDS = {
      "mama", "biópsia", "biopsia", "câncer", "cancer", "quimioterapia",
      "radioterapia", "mamografia", "cirurgia", "estadiamento", "seguimento"
  };

  private BiasReportWriter() {
  }

  /**
   * Compares baseline vs swapped narrative lengths and keyword presence.
   *
   * @param baseline baseline narrative
   * @param swapped swapped narrative
   * @param threshold relative length delta threshold (e.g. 0.20)
   * @return metrics map including {@code flagged}
   */
  public static Map<String, Object> compareNarratives(String baseline, String swapped,
      double threshold) {
    String base = baseline == null ? "" : baseline;
    String swap = swapped == null ? "" : swapped;
    int baseLen = base.length();
    int swapLen = swap.length();
    double deltaPct = 0.0;
    if (baseLen > 0) {
      deltaPct = Math.abs(swapLen - baseLen) / (double) baseLen;
    } else if (swapLen > 0) {
      deltaPct = 1.0;
    }
    List<String> baseKw = findKeywords(base);
    List<String> swapKw = findKeywords(swap);
    boolean flagged = deltaPct > threshold || !baseKw.equals(swapKw);

    Map<String, Object> metrics = new LinkedHashMap<>();
    metrics.put("baselineLength", baseLen);
    metrics.put("swappedLength", swapLen);
    metrics.put("lengthDeltaPct", deltaPct);
    metrics.put("baselineKeywords", baseKw);
    metrics.put("swappedKeywords", swapKw);
    metrics.put("flagged", flagged);
    return metrics;
  }

  /**
   * Writes the bias report under {@code output/br/ai/bias_report.json}.
   *
   * @param report root report map
   * @return path written
   * @throws IOException on write failure
   */
  public static Path write(Map<String, Object> report) throws IOException {
    String base = Config.get("exporter.baseDirectory", "./output/");
    Path dir = Path.of(base, "br", "ai");
    Files.createDirectories(dir);
    Path file = dir.resolve("bias_report.json");
    Files.write(file, GSON.toJson(report).getBytes(StandardCharsets.UTF_8));
    return file;
  }

  /**
   * Creates an empty report shell.
   *
   * @param threshold length threshold used
   * @return report root
   */
  public static Map<String, Object> newReport(double threshold) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("enabled", true);
    root.put("threshold_length_pct", threshold);
    root.put("patients", new ArrayList<Map<String, Object>>());
    return root;
  }

  private static List<String> findKeywords(String text) {
    List<String> found = new ArrayList<>();
    String lower = text.toLowerCase(Locale.ROOT);
    for (String keyword : CLINICAL_KEYWORDS) {
      if (lower.contains(keyword)) {
        found.add(keyword);
      }
    }
    return found;
  }
}
