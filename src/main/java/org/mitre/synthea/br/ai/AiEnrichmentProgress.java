package org.mitre.synthea.br.ai;

/**
 * Thread-safe progress tracker for web UI polling during AI enrichment.
 */
public final class AiEnrichmentProgress {

  private static final AiEnrichmentProgress INSTANCE = new AiEnrichmentProgress();

  private volatile boolean active;
  private volatile int total;
  private volatile int enriched;
  private volatile String phase = "idle";

  private AiEnrichmentProgress() {
  }

  /**
   * Returns the process-wide singleton instance.
   *
   * @return singleton instance
   */
  public static AiEnrichmentProgress getInstance() {
    return INSTANCE;
  }

  /**
   * Starts tracking enrichment for a cohort.
   *
   * @param patientCount patients to enrich
   */
  public synchronized void start(int patientCount) {
    active = true;
    total = patientCount;
    enriched = 0;
    phase = "AI_ENRICHMENT";
  }

  /**
   * Marks one patient as enriched.
   */
  public synchronized void increment() {
    enriched++;
  }

  /**
   * Clears tracking state.
   */
  public synchronized void reset() {
    active = false;
    total = 0;
    enriched = 0;
    phase = "idle";
  }

  /**
   * Whether enrichment is currently in progress.
   *
   * @return whether enrichment is in progress
   */
  public boolean isActive() {
    return active;
  }

  /**
   * Returns the current phase label.
   *
   * @return current phase label
   */
  public String getPhase() {
    return phase;
  }

  /**
   * Returns the number of patients enriched so far.
   *
   * @return enriched patient count
   */
  public int getEnriched() {
    return enriched;
  }

  /**
   * Returns the total number of patients to enrich in this run.
   *
   * @return total patients to enrich
   */
  public int getTotal() {
    return total;
  }

  /**
   * Resets progress state for unit tests.
   */
  void resetForTest() {
    reset();
  }
}
