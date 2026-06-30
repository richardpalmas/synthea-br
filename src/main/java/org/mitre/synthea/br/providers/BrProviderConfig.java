package org.mitre.synthea.br.providers;

/**
 * Paths and design notes for Brazilian provider datasets (Story 3.4).
 *
 * <p><strong>Task 1 decision (native vs dedicated loader):</strong> Story 3.2 sets
 * {@code Person.STATE} to the IBGE UF <em>nome completo</em> (e.g. {@code São Paulo}), while
 * {@link org.mitre.synthea.engine.Generator} still constructs {@code Location} from CLI/config
 * (typically a US state). {@code Provider.loadProviders} filters CSV rows by {@code location.state},
 * so reusing only config path overrides (AC #2) would load zero BR rows. This story uses
 * {@link BrProviderLoader} (AC #4 fallback) — same {@link org.mitre.synthea.world.agents.Provider}
 * domain objects, no US state filter.
 */
public final class BrProviderConfig {

  /** Primary care (UBS) — ambulatory / wellness encounters. */
  public static final String PRIMARY_CARE_FILE = "br/providers/ubs.csv";

  /** Generic hospital — inpatient / outpatient / emergency / urgent care. */
  public static final String HOSPITAL_FILE = "br/providers/hospital_generico.csv";

  /** Sentinel recorded in {@code Provider} state cache after BR load. */
  public static final String LOADED_STATE_MARKER = "BR_PROFILE";

  private BrProviderConfig() {
  }
}
