package org.mitre.synthea.br.ai;

/**
 * Counters for LLM parse/truncation robustness during MAI-DxO enrichment.
 */
public final class RobustnessStats {

  private int jsonParseRetries;
  private int truncationContinuations;
  private int personaTurnsSkipped;

  /**
   * Creates zeroed counters.
   */
  public RobustnessStats() {
  }

  /**
   * Creates counters with explicit values.
   *
   * @param jsonParseRetries LLM clean retries that ran
   * @param truncationContinuations continuation calls that ran
   * @param personaTurnsSkipped turns skipped after parse exhaustion
   */
  public RobustnessStats(int jsonParseRetries, int truncationContinuations,
      int personaTurnsSkipped) {
    this.jsonParseRetries = jsonParseRetries;
    this.truncationContinuations = truncationContinuations;
    this.personaTurnsSkipped = personaTurnsSkipped;
  }

  /**
   * Adds another stats instance into this one.
   *
   * @param other other counters
   */
  public void add(RobustnessStats other) {
    if (other == null) {
      return;
    }
    this.jsonParseRetries += other.jsonParseRetries;
    this.truncationContinuations += other.truncationContinuations;
    this.personaTurnsSkipped += other.personaTurnsSkipped;
  }

  /**
   * Increments JSON parse retry count by one.
   */
  public void incrementJsonParseRetries() {
    jsonParseRetries++;
  }

  /**
   * Increments truncation continuation count by one.
   */
  public void incrementTruncationContinuations() {
    truncationContinuations++;
  }

  /**
   * Increments skipped persona turn count by one.
   */
  public void incrementPersonaTurnsSkipped() {
    personaTurnsSkipped++;
  }

  /**
   * Returns JSON parse retry count.
   *
   * @return retries
   */
  public int getJsonParseRetries() {
    return jsonParseRetries;
  }

  /**
   * Returns truncation continuation count.
   *
   * @return continuations
   */
  public int getTruncationContinuations() {
    return truncationContinuations;
  }

  /**
   * Returns skipped persona turn count.
   *
   * @return skipped turns
   */
  public int getPersonaTurnsSkipped() {
    return personaTurnsSkipped;
  }
}
