package org.mitre.synthea.br.pathway.generation;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.mitre.synthea.br.condition.TargetConditionConfig;
import org.mitre.synthea.helpers.Config;

/**
 * Resolves {@code br.generation.module_profile} and applies module path filters (Story 9.5, AD-3).
 */
public final class ModuleProfileConfig {

  private static final String PROFILE_FULL = "full";
  private static final String PROFILE_PATHWAY_MINIMAL = "pathway_minimal";
  private static final String RESOURCE_PATTERN = "/br/generation/module_profiles/%s.json";

  private static final Gson GSON = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();

  private String profileVersion;
  private String profileId;
  private List<String> allowedModulePaths;
  private List<String> allowedCoreModulePaths;
  private List<String> deniedModulePaths;
  private List<String> deniedCoreModulePaths;
  private Map<String, List<String>> alwaysIncludeForTargetCondition;

  private ModuleProfileConfig() {
  }

  /**
   * Active profile key from configuration ({@code full} when unset).
   *
   * @return profile key
   */
  public static String getActiveProfileKey() {
    String value = Config.get("br.generation.module_profile", PROFILE_FULL);
    if (value == null || value.isBlank()) {
      return PROFILE_FULL;
    }
    return value.trim();
  }

  /**
   * Version string of the active curated profile data pack, or {@code null} when {@code full}.
   *
   * @return {@code profile_version} from JSON, or {@code null}
   */
  public static String getActiveProfileVersion() {
    String profileKey = getActiveProfileKey();
    if (PROFILE_FULL.equalsIgnoreCase(profileKey)) {
      return null;
    }
    if (!PROFILE_PATHWAY_MINIMAL.equalsIgnoreCase(profileKey)) {
      return null;
    }
    return loadProfile(PROFILE_PATHWAY_MINIMAL).profileVersion;
  }

  /**
   * Whether the active profile restricts module loading.
   *
   * @return {@code true} when a curated profile (not {@code full}) is active
   */
  public static boolean isCuratedProfileActive() {
    return !PROFILE_FULL.equalsIgnoreCase(getActiveProfileKey());
  }

  /**
   * Combined module path predicate for {@link org.mitre.synthea.engine.Module#getModules}.
   *
   * @return predicate; {@code true} for all paths when profile is {@code full}
   */
  public static Predicate<String> buildPathPredicate() {
    String profileKey = getActiveProfileKey();
    if (PROFILE_FULL.equalsIgnoreCase(profileKey)) {
      return path -> true;
    }
    if (!PROFILE_PATHWAY_MINIMAL.equalsIgnoreCase(profileKey)) {
      throw new IllegalArgumentException(String.format(
          "br.generation.module_profile='%s' invalido. Valores suportados: full, pathway_minimal.",
          profileKey));
    }
    ModuleProfileConfig profile = loadProfile(PROFILE_PATHWAY_MINIMAL);
    List<String> forcedIncludes = resolveForcedIncludes(profile);
    return path -> profile.allowsPath(path, forcedIncludes);
  }

  private static List<String> resolveForcedIncludes(ModuleProfileConfig profile) {
    TargetConditionConfig.ResolvedTargetCondition resolved =
        TargetConditionConfig.resolveConfigured();
    if (resolved == null
        || profile.alwaysIncludeForTargetCondition == null) {
      return Collections.emptyList();
    }
    List<String> includes = profile.alwaysIncludeForTargetCondition.get(
        resolved.definition.conditionKey);
    return includes == null ? Collections.emptyList() : includes;
  }

  private static ModuleProfileConfig loadProfile(String profileKey) {
    String resourcePath = String.format(RESOURCE_PATTERN, profileKey);
    try (InputStream stream = ModuleProfileConfig.class.getResourceAsStream(resourcePath)) {
      if (stream == null) {
        throw new IllegalStateException(
            "Perfil de modulo nao encontrado: " + resourcePath);
      }
      try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
        return GSON.fromJson(reader, ModuleProfileConfig.class);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Falha ao ler perfil de modulo: " + resourcePath, e);
    }
  }

  private boolean allowsPath(String path, List<String> forcedIncludes) {
    if (path == null) {
      return false;
    }
    if (matchesAny(path, deniedModulePaths) || matchesAny(path, deniedCoreModulePaths)) {
      return false;
    }
    if (matchesAny(path, forcedIncludes)) {
      return true;
    }
    if (matchesAny(path, allowedModulePaths) || matchesAny(path, allowedCoreModulePaths)) {
      return true;
    }
    return false;
  }

  private static boolean matchesAny(String path, List<String> patterns) {
    if (patterns == null || patterns.isEmpty()) {
      return false;
    }
    for (String pattern : patterns) {
      if (path.equals(pattern) || path.startsWith(pattern + "/")) {
        return true;
      }
    }
    return false;
  }
}
