package org.mitre.synthea.br.geography;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.mitre.synthea.helpers.RandomNumberGenerator;
import org.mitre.synthea.helpers.SimpleCSV;
import org.mitre.synthea.helpers.Utilities;
import org.mitre.synthea.world.agents.Person;

/**
 * Brazilian geography resolver loaded from {@code br/geography/} data packs.
 *
 * <p>Parallel to upstream {@link org.mitre.synthea.world.geography.Location} — activated only
 * when {@link org.mitre.synthea.br.profile.BrProfile#isActive()}. Story 3.2 integration points:
 * {@code Generator.pickDemographics} (city/state) and {@code LifecycleModule.birth}
 * (CEP/coordinates/birthplace).
 */
public final class BrGeographyResolver {

  /** Coordinate perturbation magnitude (degrees), same order as {@code Location.assignPoint}. */
  public static final double COORDINATE_PERTURBATION = 0.05;

  /**
   * Bounding-box validation tolerance in degrees (~100 m at equator). Documented for AC #2 tests.
   */
  public static final double BOUNDING_BOX_TOLERANCE = 0.001;

  private static final String UFS_FILE = "br/geography/ufs.csv";
  private static final String MUNICIPIOS_FILE = "br/geography/municipios_piloto.csv";

  private static volatile BrGeographyResolver cached;

  private final Map<String, UfData> ufsBySigla;
  private final List<MunicipioData> municipios;
  private final long totalPopulation;

  private BrGeographyResolver(Map<String, UfData> ufsBySigla, List<MunicipioData> municipios) {
    this.ufsBySigla = ufsBySigla;
    this.municipios = municipios;
    long sum = 0;
    for (MunicipioData municipio : municipios) {
      sum += municipio.populacao;
    }
    this.totalPopulation = sum;
  }

  /**
   * Load (or return cached) BR geography data.
   *
   * @return resolver instance
   * @throws IOException if CSV data cannot be read or parsed
   */
  public static BrGeographyResolver load() throws IOException {
    BrGeographyResolver local = cached;
    if (local != null) {
      return local;
    }
    synchronized (BrGeographyResolver.class) {
      if (cached == null) {
        cached = parseFromResources();
      }
      return cached;
    }
  }

  /**
   * Clear cached data (test use only).
   */
  public static void resetCacheForTest() {
    cached = null;
  }

  /**
   * Weighted random municipality selection (by population).
   *
   * @param random source of randomness
   * @return selected municipality with resolved UF metadata
   */
  public MunicipioSelection selectMunicipio(RandomNumberGenerator random) {
    long target = (long) (random.rand() * totalPopulation);
    for (MunicipioData municipio : municipios) {
      target -= municipio.populacao;
      if (target < 0) {
        UfData uf = ufsBySigla.get(municipio.ufSigla);
        return new MunicipioSelection(municipio, uf);
      }
    }
    MunicipioData last = municipios.get(municipios.size() - 1);
    return new MunicipioSelection(last, ufsBySigla.get(last.ufSigla));
  }

  /**
   * Generate a Brazilian postal code ({@code NNNNN-NNN}) within the municipality CEP range.
   *
   * @param municipio selected municipality
   * @param random source of randomness
   * @return formatted CEP
   */
  public static String generateCep(MunicipioData municipio, RandomNumberGenerator random) {
    int cepValue = municipio.cepMin
        + random.randInt(municipio.cepMax - municipio.cepMin + 1);
    String digits = String.format(Locale.US, "%08d", cepValue);
    return digits.substring(0, 5) + "-" + digits.substring(5);
  }

  /**
   * Assign geographic coordinates within the UF bounding box (AC #2).
   *
   * @param municipio municipality center reference
   * @param uf UF bounding box
   * @param random source of randomness
   * @return coordinate within UF limits
   */
  public static Point2D.Double assignCoordinate(MunicipioData municipio, UfData uf,
      RandomNumberGenerator random) {
    double lat = municipio.lat + random.rand(-COORDINATE_PERTURBATION, COORDINATE_PERTURBATION);
    double lon = municipio.lon + random.rand(-COORDINATE_PERTURBATION, COORDINATE_PERTURBATION);
    lat = clamp(lat, uf.latMin, uf.latMax);
    lon = clamp(lon, uf.lonMin, uf.lonMax);
    return new Point2D.Double(lon, lat);
  }

  /**
   * Whether a coordinate lies within the UF bounding box (with documented tolerance).
   *
   * @param coordinate point to validate
   * @param uf UF bounding box
   * @return true if inside box ± {@link #BOUNDING_BOX_TOLERANCE}
   */
  public static boolean isWithinUfBoundingBox(Point2D.Double coordinate, UfData uf) {
    return coordinate.y >= uf.latMin - BOUNDING_BOX_TOLERANCE
        && coordinate.y <= uf.latMax + BOUNDING_BOX_TOLERANCE
        && coordinate.x >= uf.lonMin - BOUNDING_BOX_TOLERANCE
        && coordinate.x <= uf.lonMax + BOUNDING_BOX_TOLERANCE;
  }

  /**
   * Complete ZIP, coordinates, and birthplace for a person already assigned city/state (BR profile).
   *
   * @param person person with {@link Person#CITY} and {@link Person#STATE} set
   */
  public void completePersonGeography(Person person) {
    completePersonGeography(person, true);
  }

  /**
   * Complete residence geography (ZIP, coordinates) for a BR-profile person.
   *
   * @param person person with {@link Person#CITY} and {@link Person#STATE} set
   * @param setBirthplace when {@code true}, also sets {@link Person#BIRTH_CITY},
   *     {@link Person#BIRTH_STATE}, {@link Person#BIRTH_COUNTRY}, and {@link Person#BIRTHPLACE};
   *     use {@code false} on fixed-record relocation to match upstream {@code assignPoint} behavior
   */
  public void completePersonGeography(Person person, boolean setBirthplace) {
    String cityName = (String) person.attributes.get(Person.CITY);
    String stateName = (String) person.attributes.get(Person.STATE);
    MunicipioData municipio = findMunicipio(cityName, stateName);
    if (municipio == null) {
      System.err.println("WARNING: BR geography could not resolve municipality for city='"
          + cityName + "', state='" + stateName
          + "'; selecting weighted random municipality.");
      MunicipioSelection selection = selectMunicipio(person);
      municipio = selection.municipio;
      UfData uf = selection.uf;
      person.attributes.put(Person.CITY, municipio.nome);
      person.attributes.put(Person.STATE, uf.nome);
      person.attributes.put(Person.COUNTY, uf.sigla);
    }
    UfData ufData = ufsBySigla.get(municipio.ufSigla);
    String cep = generateCep(municipio, person);
    person.attributes.put(Person.ZIP, cep);
    Point2D.Double coordinate = assignCoordinate(municipio, ufData, person);
    person.attributes.put(Person.COORDINATE, coordinate);
    person.attributes.put(Person.COUNTY, ufData.sigla);

    if (setBirthplace) {
      String country = org.mitre.synthea.br.profile.BrProfile.getEffectiveCountryCode();
      person.attributes.put(Person.BIRTH_CITY, municipio.nome);
      person.attributes.put(Person.BIRTH_STATE, ufData.nome);
      person.attributes.put(Person.BIRTH_COUNTRY, country);
      person.attributes.put(Person.BIRTHPLACE,
          municipio.nome + ", " + ufData.nome + ", " + country);
    }
  }

  /**
   * All UFs in the data pack (for tests).
   *
   * @return unmodifiable map sigla → UF data
   */
  public Map<String, UfData> getUfsBySigla() {
    return Collections.unmodifiableMap(ufsBySigla);
  }

  /**
   * All pilot municipalities (for tests).
   *
   * @return unmodifiable list
   */
  public List<MunicipioData> getMunicipios() {
    return Collections.unmodifiableList(municipios);
  }

  private MunicipioData findMunicipio(String cityName, String stateName) {
    if (cityName == null || stateName == null) {
      return null;
    }
    for (MunicipioData municipio : municipios) {
      UfData uf = ufsBySigla.get(municipio.ufSigla);
      if (municipio.nome.equalsIgnoreCase(cityName)
          && (uf.nome.equalsIgnoreCase(stateName) || uf.sigla.equalsIgnoreCase(stateName))) {
        return municipio;
      }
    }
    return null;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private static BrGeographyResolver parseFromResources() throws IOException {
    Map<String, UfData> ufs = parseUfs();
    List<MunicipioData> municipios = parseMunicipios(stripCommentLines(
        Utilities.readResource(MUNICIPIOS_FILE, true, false)));
    for (MunicipioData municipio : municipios) {
      if (!ufs.containsKey(municipio.ufSigla)) {
        throw new IOException("Unknown UF sigla in municipios CSV: " + municipio.ufSigla);
      }
    }
    return new BrGeographyResolver(ufs, municipios);
  }

  private static Map<String, UfData> parseUfs() throws IOException {
    String raw = Utilities.readResource(UFS_FILE, true, false);
    List<? extends Map<String, String>> rows = SimpleCSV.parse(stripCommentLines(raw));
    Map<String, UfData> ufs = new HashMap<>();
    for (Map<String, String> row : rows) {
      UfData uf = new UfData(
          row.get("sigla"),
          row.get("nome"),
          row.get("regiao"),
          Double.parseDouble(row.get("lat_min")),
          Double.parseDouble(row.get("lat_max")),
          Double.parseDouble(row.get("lon_min")),
          Double.parseDouble(row.get("lon_max")));
      ufs.put(uf.sigla, uf);
    }
    if (ufs.size() != 27) {
      throw new IOException("Expected 27 UFs in data pack, found " + ufs.size());
    }
    return ufs;
  }

  private static List<MunicipioData> parseMunicipios(String csv) throws IOException {
    List<? extends Map<String, String>> rows = SimpleCSV.parse(csv);
    List<MunicipioData> result = new ArrayList<>();
    for (Map<String, String> row : rows) {
      result.add(new MunicipioData(
          row.get("id"),
          row.get("nome"),
          row.get("uf_sigla"),
          Double.parseDouble(row.get("lat")),
          Double.parseDouble(row.get("lon")),
          Integer.parseInt(row.get("cep_min")),
          Integer.parseInt(row.get("cep_max")),
          Long.parseLong(row.get("populacao"))));
    }
    if (result.isEmpty()) {
      throw new IOException("No municipalities found in pilot data pack");
    }
    return result;
  }

  private static String stripCommentLines(String raw) {
    StringBuilder builder = new StringBuilder();
    for (String line : raw.split("\\R")) {
      if (!line.trim().startsWith("#")) {
        builder.append(line).append('\n');
      }
    }
    return builder.toString();
  }

  /**
   * UF metadata with bounding box.
   */
  public static final class UfData {
    public final String sigla;
    public final String nome;
    public final String regiao;
    public final double latMin;
    public final double latMax;
    public final double lonMin;
    public final double lonMax;

    UfData(String sigla, String nome, String regiao,
        double latMin, double latMax, double lonMin, double lonMax) {
      this.sigla = sigla;
      this.nome = nome;
      this.regiao = regiao;
      this.latMin = latMin;
      this.latMax = latMax;
      this.lonMin = lonMin;
      this.lonMax = lonMax;
    }
  }

  /**
   * Pilot municipality record.
   */
  public static final class MunicipioData {
    public final String id;
    public final String nome;
    public final String ufSigla;
    public final double lat;
    public final double lon;
    public final int cepMin;
    public final int cepMax;
    public final long populacao;

    MunicipioData(String id, String nome, String ufSigla,
        double lat, double lon, int cepMin, int cepMax, long populacao) {
      this.id = id;
      this.nome = nome;
      this.ufSigla = ufSigla;
      this.lat = lat;
      this.lon = lon;
      this.cepMin = cepMin;
      this.cepMax = cepMax;
      this.populacao = populacao;
    }
  }

  /**
   * Municipality selection with resolved UF.
   */
  public static final class MunicipioSelection {
    private final MunicipioData municipio;
    private final UfData uf;

    MunicipioSelection(MunicipioData municipio, UfData uf) {
      this.municipio = municipio;
      this.uf = uf;
    }

    public String getNome() {
      return municipio.nome;
    }

    public String getUfNome() {
      return uf.nome;
    }

    public String getUfSigla() {
      return uf.sigla;
    }

    public MunicipioData getMunicipio() {
      return municipio;
    }

    public UfData getUf() {
      return uf;
    }
  }
}
