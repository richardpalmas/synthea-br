package org.mitre.synthea.br.condition;

import java.nio.file.Path;

import org.mitre.synthea.engine.Generator;
import org.mitre.synthea.engine.Module;
import org.mitre.synthea.helpers.Config;

/**
 * Resolves and validates {@code br.target_condition} configuration.
 */
public final class TargetConditionConfig {

  private TargetConditionConfig() {
  }

  /**
   * Resolved target condition with validated disease and keep modules.
   */
  public static final class ResolvedTargetCondition {
    public final SupportedConditions.ConditionDefinition definition;
    public final Path keepModulePath;

    ResolvedTargetCondition(SupportedConditions.ConditionDefinition definition,
        Path keepModulePath) {
      this.definition = definition;
      this.keepModulePath = keepModulePath;
    }
  }

  /**
   * Resolve and validate a target condition key.
   *
   * @param conditionKey value of {@code br.target_condition}
   * @return resolved condition with keep-module path
   */
  public static ResolvedTargetCondition resolve(String conditionKey) {
    SupportedConditions.ConditionDefinition definition = SupportedConditions.get(conditionKey);
    if (definition == null) {
      throw new UnknownTargetConditionException(conditionKey);
    }

    if (Module.getModuleByPath(definition.diseaseModulePath) == null) {
      throw new IllegalStateException(String.format(
          "Módulo GMF de doença '%s' não encontrado para a condição '%s'.",
          definition.diseaseModulePath, conditionKey));
    }

    if (!KeepModulePaths.existsRelative(definition.keepModuleRelativePath)) {
      throw new GateModuleNotAvailableException(conditionKey);
    }

    try {
      Path keepModulePath = KeepModulePaths.resolveRelative(definition.keepModuleRelativePath);
      return new ResolvedTargetCondition(definition, keepModulePath);
    } catch (java.io.FileNotFoundException e) {
      throw new GateModuleNotAvailableException(conditionKey);
    }
  }

  /**
   * Resolve the configured target condition from {@code br.target_condition}, if set.
   *
   * @return resolved condition or {@code null} when the property is unset
   */
  public static ResolvedTargetCondition resolveConfigured() {
    String conditionKey = Config.get("br.target_condition");
    if (conditionKey == null || conditionKey.isBlank()) {
      return null;
    }
    return resolve(conditionKey.trim());
  }

  /**
   * Apply retry-mode gate configuration to generator options.
   *
   * @param options generator options to update
   * @param resolved validated target condition
   * @throws IllegalStateException if {@code options.keepPatientsModulePath} was already set
   *     (e.g. via the {@code -k} CLI flag), since {@code br.target_condition} retry mode would
   *     otherwise silently discard the user-supplied keep module
   */
  public static void applyToOptions(Generator.GeneratorOptions options,
      ResolvedTargetCondition resolved) {
    if (options.keepPatientsModulePath != null) {
      throw new IllegalStateException(String.format(
          "Conflito de configuração: keepPatientsModulePath já definido ('%s') quando "
              + "br.target_condition='%s' (modo retry) também tentou definir um keep module. "
              + "Use apenas um dos dois mecanismos.",
          options.keepPatientsModulePath, resolved.definition.conditionKey));
    }
    options.keepPatientsModulePath = resolved.keepModulePath;
  }
}
