package org.mitre.synthea.br.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API response describing the current or last generation job.
 */
public class GenerationStatus {

  /** idle | running | ai_enrichment | completed | failed */
  public String state = "idle";
  public String phase = "idle";
  public String jobId;
  public int requestedPopulation;
  public int generatedCount;
  public int aiEnrichedCount;
  public int aiEnrichmentTotal;
  public boolean aiEnrichmentEnabled;
  public String outputDirectory;
  public boolean manifestPresent;
  public boolean htmlExportEnabled;
  public String htmlIndexPath;
  /** Present when HTML was requested but no index was produced. */
  public String htmlMissingReason;
  public String errorMessage;
  public List<String> logTail = new ArrayList<>();
  public boolean plausibilityReportPresent;
  public String plausibilityReportPath;
  /** PT-BR summary of trajectory settings for the last job. */
  public String trajectorySummary;
  /** Per-format export failure counts from the last completed job. */
  public Map<String, Integer> exportFailureCounts = new LinkedHashMap<>();
  /** Human-readable warning when one or more export formats failed partially. */
  public String exportPartialWarning;
}
