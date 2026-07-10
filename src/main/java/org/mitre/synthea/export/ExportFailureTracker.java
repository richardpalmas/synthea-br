package org.mitre.synthea.export;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe per-format export failure counts for a single generation run.
 */
public final class ExportFailureTracker {

  private static final ConcurrentHashMap<String, AtomicInteger> FAILURES = new ConcurrentHashMap<>();

  private ExportFailureTracker() {
  }

  /**
   * Clears failure counters before a new generation run.
   */
  public static void reset() {
    FAILURES.clear();
  }

  /**
   * Records one failed export attempt for the given format key.
   *
   * @param format export format identifier (e.g. {@code csv}, {@code html})
   */
  public static void recordFailure(String format) {
    if (format == null || format.isBlank()) {
      return;
    }
    FAILURES.computeIfAbsent(format, ignored -> new AtomicInteger()).incrementAndGet();
  }

  /**
   * Returns a snapshot of failure counts keyed by format.
   *
   * @return immutable copy of current failure counts
   */
  public static Map<String, Integer> snapshot() {
    if (FAILURES.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Integer> copy = new LinkedHashMap<>();
    FAILURES.forEach((format, count) -> copy.put(format, count.get()));
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Returns the failure count for a format, or zero when none recorded.
   *
   * @param format export format identifier
   * @return failure count
   */
  public static int getFailureCount(String format) {
    AtomicInteger count = FAILURES.get(format);
    return count == null ? 0 : count.get();
  }
}
