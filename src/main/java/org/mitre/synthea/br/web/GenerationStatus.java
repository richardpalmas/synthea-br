package org.mitre.synthea.br.web;

import java.util.ArrayList;
import java.util.List;

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
  public String errorMessage;
  public List<String> logTail = new ArrayList<>();
}
