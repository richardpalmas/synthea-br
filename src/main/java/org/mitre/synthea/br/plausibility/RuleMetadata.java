package org.mitre.synthea.br.plausibility;

import com.google.gson.JsonObject;

import java.util.Objects;

/**
 * Metadata for a single catalog rule loaded from {@code catalog_breast_cancer.json}.
 */
public final class RuleMetadata {

  private final String id;
  private final String severity;
  private final String title;
  private final String description;

  /**
   * Parse rule metadata from a catalog JSON object.
   *
   * @param json rule object from catalog
   * @return parsed metadata
   */
  public static RuleMetadata fromJson(JsonObject json) {
    return new RuleMetadata(
        json.get("id").getAsString(),
        json.get("severity").getAsString(),
        json.get("title").getAsString(),
        json.get("description").getAsString());
  }

  /**
   * Create rule metadata.
   *
   * @param id stable rule identifier
   * @param severity severity level
   * @param title short title
   * @param description full description
   */
  public RuleMetadata(String id, String severity, String title, String description) {
    this.id = Objects.requireNonNull(id, "id");
    this.severity = Objects.requireNonNull(severity, "severity");
    this.title = Objects.requireNonNull(title, "title");
    this.description = Objects.requireNonNull(description, "description");
  }

  public String getId() {
    return id;
  }

  public String getSeverity() {
    return severity;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }
}
