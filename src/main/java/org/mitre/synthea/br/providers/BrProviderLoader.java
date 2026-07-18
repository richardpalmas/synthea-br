package org.mitre.synthea.br.providers;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.mitre.synthea.helpers.DefaultRandomNumberGenerator;
import org.mitre.synthea.world.agents.Provider;
import org.mitre.synthea.world.agents.Provider.ProviderType;
import org.mitre.synthea.world.concepts.HealthRecord.EncounterType;
import org.mitre.synthea.world.geography.Location;

/**
 * Loads Brazilian provider datasets into the upstream {@link Provider} registry (Story 3.4).
 *
 * <p>Uses {@link Provider#loadAllProvidersFromFile} because {@code location.state} on
 * {@link org.mitre.synthea.engine.Generator} remains the CLI/config US state while
 * {@code Person.STATE} carries IBGE UF names (Story 3.2).
 *
 * <p>Hospital genérico also serves {@link EncounterType#URGENTCARE} and
 * {@link EncounterType#EMERGENCY} (pronto-socorro MVP — Story 3.4 review decision a).
 */
public final class BrProviderLoader {

  private BrProviderLoader() {
  }

  /**
   * Load BR primary-care and hospital providers for the simulation.
   *
   * @param location generator location context (attached to each provider)
   * @param random clinician random source
   */
  public static void load(Location location, DefaultRandomNumberGenerator random) {
    if (Provider.isProvidersLoadedForState(BrProviderConfig.LOADED_STATE_MARKER)) {
      return;
    }
    try {
      Set<EncounterType> hospitalServices = new HashSet<>();
      hospitalServices.add(EncounterType.AMBULATORY);
      hospitalServices.add(EncounterType.OUTPATIENT);
      hospitalServices.add(EncounterType.INPATIENT);
      hospitalServices.add(EncounterType.EMERGENCY);
      hospitalServices.add(EncounterType.URGENTCARE);

      Provider.loadAllProvidersFromFile(location, BrProviderConfig.HOSPITAL_FILE,
          ProviderType.HOSPITAL, hospitalServices, random, false);

      Set<EncounterType> primaryServices = new HashSet<>();
      primaryServices.add(EncounterType.WELLNESS);
      primaryServices.add(EncounterType.AMBULATORY);

      Provider.loadAllProvidersFromFile(location, BrProviderConfig.PRIMARY_CARE_FILE,
          ProviderType.PRIMARY, primaryServices, random, false);

      if (Provider.getProviderList().isEmpty()) {
        throw new IllegalStateException("BR provider data pack loaded zero providers");
      }

      Provider.markProvidersLoadedForState(BrProviderConfig.LOADED_STATE_MARKER);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load BR provider data pack", e);
    }
  }
}
