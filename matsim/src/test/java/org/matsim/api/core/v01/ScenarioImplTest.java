package org.matsim.api.core.v01;

import junit.framework.Assert;

import org.junit.Test;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author mrieser
 */
public class ScenarioImplTest {

	/**
	 * Tests that {@link ScenarioImpl#createId(String)} returns the same
	 * Id object for equil Strings.
	 */
	@Test
	public void testCreateId_sameObjectForSameId() {
		ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String str1 = "9832";
		String str2 = new String(str1);
		Assert.assertNotSame(str1, str2);
		Assert.assertEquals(str1, str2);
		Id id1 = s.createId(str1);
		Id id2 = s.createId(str2);
		Id id3 = s.createId(str1);
		Assert.assertSame(id1, id2);
		Assert.assertSame(id1, id3);
		Assert.assertSame(id2, id3);
	}

	@Test
	public void testAddAndGetScenarioElement() {
		final ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final Object element1 = new Object();
		final String name1 = "peter_parker";
		final Object element2 = new Object();
		final String name2 = "popeye";

		s.addScenarioElement( name1 , element1 );
		s.addScenarioElement( name2 , element2 );
		Assert.assertSame(
				"unexpected scenario element",
				element1,
				s.getScenarioElement( name1 ) );
		// just check that it is got, not removed
		Assert.assertSame(
				"unexpected scenario element",
				element1,
				s.getScenarioElement( name1 ) );

		Assert.assertSame(
				"unexpected scenario element",
				element2,
				s.getScenarioElement( name2 ) );

	}

	@Test
	public void testCannotAddAnElementToAnExistingName() {
		final ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final String name = "bruce_wayne";

		s.addScenarioElement( name , new Object() );
		try {
			s.addScenarioElement( name , new Object() );
		}
		catch (IllegalStateException e) {
			return;
		}
		catch (Exception e) {
			Assert.fail( "wrong exception thrown when trying to add an element for an existing name "+e.getClass().getName() );
		}
		Assert.fail( "no exception thrown when trying to add an element for an existing name" );
	}

	@Test
	public void testRemoveElement() {
		final ScenarioImpl s = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		final Object element = new Object();
		final String name = "clark_kent";

		s.addScenarioElement( name , element );
		Assert.assertSame(
				"unexpected removed element",
				element,
				s.removeScenarioElement( name ) );
		Assert.assertNull(
				"element was not removed",
				s.getScenarioElement( name ) );

	}
}
