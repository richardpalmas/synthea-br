package org.mitre.synthea.br.pathway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mitre.synthea.TestHelper;
import org.mitre.synthea.helpers.Config;
import org.mitre.synthea.world.agents.Person;

/**
 * Unit tests for {@link PathwayArchetypeConfig}.
 */
public class PathwayArchetypeConfigTest {

  @Before
  public void setUp() throws Exception {
    TestHelper.loadTestProperties();
  }

  @After
  public void tearDown() {
    Config.remove("br.pathway.archetype");
    Config.remove("br.target_condition");
  }

  @Test
  public void getActiveMode_defaultsToAuto() {
    assertEquals(PathwayArchetypeConfig.MODE_AUTO, PathwayArchetypeConfig.getActiveMode());
    assertFalse(PathwayArchetypeConfig.isForced());
  }

  @Test
  public void applyToPerson_writesAttributeWhenForced() {
    Config.set("br.target_condition", "breast_cancer");
    Config.set("br.pathway.archetype", "remission");
    Person person = new Person(42L);
    PathwayArchetypeConfig.applyToPerson(person);
    assertEquals("remission", person.attributes.get(PathwayArchetypeConfig.PERSON_ATTRIBUTE));
  }

  @Test
  public void applyToPerson_skipsAttributeWhenAuto() {
    Person person = new Person(42L);
    PathwayArchetypeConfig.applyToPerson(person);
    assertFalse(person.attributes.containsKey(PathwayArchetypeConfig.PERSON_ATTRIBUTE));
  }
}
