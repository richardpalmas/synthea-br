package org.mitre.synthea.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ExportFailureTracker}.
 */
public class ExportFailureTrackerTest {

  /**
   * Reset tracker before each test.
   */
  @Before
  public void setUp() {
    ExportFailureTracker.reset();
  }

  @Test
  public void snapshotReturnsEmptyWhenNoFailures() {
    assertTrue(ExportFailureTracker.snapshot().isEmpty());
  }

  @Test
  public void recordFailureIncrementsPerFormat() {
    ExportFailureTracker.recordFailure("csv");
    ExportFailureTracker.recordFailure("csv");
    ExportFailureTracker.recordFailure("html");

    Map<String, Integer> snapshot = ExportFailureTracker.snapshot();
    assertEquals(2, snapshot.get("csv").intValue());
    assertEquals(1, snapshot.get("html").intValue());
  }

  @Test
  public void resetClearsFailures() {
    ExportFailureTracker.recordFailure("csv");
    ExportFailureTracker.reset();
    assertEquals(0, ExportFailureTracker.getFailureCount("csv"));
    assertTrue(ExportFailureTracker.snapshot().isEmpty());
  }
}
