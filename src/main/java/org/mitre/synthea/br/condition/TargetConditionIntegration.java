package org.mitre.synthea.br.condition;

import java.util.concurrent.atomic.AtomicInteger;

import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.world.agents.Person;

/**
 * Bootstraps {@code br.target_condition} into {@link Generator} execution.
 */
public final class TargetConditionIntegration {

  private final GateMode gateMode;
  private final TargetConditionConfig.ResolvedTargetCondition resolved;
  private final Module excludeGateModule;
  private final AtomicInteger excludedCount = new AtomicInteger();
  private final AtomicInteger exportedCount = new AtomicInteger();

  private TargetConditionIntegration(GateMode gateMode,
      TargetConditionConfig.ResolvedTargetCondition resolved, Module excludeGateModule) {
    this.gateMode = gateMode;
    this.resolved = resolved;
    this.excludeGateModule = excludeGateModule;
  }

  /**
   * Initialize targeted-condition integration when configured.
   *
   * @param options generator options to update for retry mode
   * @return integration instance or {@code null} when {@code br.target_condition} is unset
   */
  public static TargetConditionIntegration tryInitialize(Generator.GeneratorOptions options) {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null) {
      return null;
    }

    GateMode gateMode = GateMode.fromConfig();
    Module excludeGateModule = null;
    if (gateMode == GateMode.RETRY) {
      TargetConditionConfig.applyToOptions(options, resolved);
    } else {
      try {
        excludeGateModule = Module.loadFile(resolved.keepModulePath, false, null, true);
      } catch (Exception e) {
        throw new IllegalStateException(
            "Unable to load keep module for exclude mode: " + resolved.keepModulePath, e);
      }
    }

    return new TargetConditionIntegration(gateMode, resolved, excludeGateModule);
  }

  /**
   * Returns true when export filtering is active instead of native retry.
   *
   * @return true when export filtering is active instead of native retry
   */
  public boolean isExcludeMode() {
    return gateMode == GateMode.EXCLUDE;
  }

  /**
   * Evaluate whether a generated person should be exported in exclude mode.
   *
   * @param person simulated person
   * @param finishTime simulation finish timestamp
   * @return true when the patient satisfies the gate and may be exported
   */
  public boolean patientMatchesGate(Person person, long finishTime) {
    return GateEvaluator.matchesCondition(person, excludeGateModule, finishTime);
  }

  /**
   * Record one excluded patient in exclude mode.
   */
  public void recordExcluded() {
    excludedCount.incrementAndGet();
  }

  /**
   * Record one conforming export in exclude mode.
   */
  public void recordExported() {
    exportedCount.incrementAndGet();
  }

  /**
   * Log cohort gate summary after generation completes.
   *
   * @param requestedPopulation population size requested by the user
   */
  public void logSummary(int requestedPopulation) {
    if (gateMode != GateMode.EXCLUDE) {
      return;
    }

    int excluded = excludedCount.get();
    int exported = exportedCount.get();
    double conformingPercent = requestedPopulation == 0
        ? 0.0
        : (exported * 100.0) / requestedPopulation;

    System.out.printf(
        "Synthea-br gate (exclude): requested=%d exported=%d excluded=%d conforming=%.1f%%%n",
        requestedPopulation,
        exported,
        excluded,
        conformingPercent);
  }

  /**
   * Returns the number of patients excluded in exclude mode.
   *
   * @return number of patients excluded in exclude mode
   */
  public int getExcludedCount() {
    return excludedCount.get();
  }

  /**
   * Returns the number of conforming patients exported in exclude mode.
   *
   * @return number of conforming patients exported in exclude mode
   */
  public int getExportedCount() {
    return exportedCount.get();
  }

  /**
   * Returns the configured condition key.
   *
   * @return configured condition key
   */
  public String getConditionKey() {
    return resolved.definition.conditionKey;
  }
}
