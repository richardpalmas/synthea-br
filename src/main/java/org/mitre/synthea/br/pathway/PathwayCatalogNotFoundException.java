package org.mitre.synthea.br.pathway;

/**
 * Thrown when {@link PathwayCatalog#loadForCondition(String)} is called with a condition key
 * that has no corresponding {@code br/pathways/{condition}_phases.json} data pack.
 */
public class PathwayCatalogNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception with a clear, actionable message naming the missing condition.
   *
   * @param conditionKey the unresolved {@code br.target_condition} value
   */
  public PathwayCatalogNotFoundException(String conditionKey) {
    super(String.format(
        "Nenhum catalogo de trajetoria encontrado para a condicao '%s'. "
            + "Esperado o recurso 'br/pathways/%s_phases.json' em src/main/resources.",
        conditionKey, conditionKey));
  }
}
