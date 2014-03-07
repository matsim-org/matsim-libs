/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.scenario;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.households.Households;
import org.matsim.knowledges.Knowledges;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * @author thibautd
 */
public class ScenarioImplTest {
	@Test
	public void testCreateOnlyOneVehiclesContainer() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createVehicleContainer();
		final Vehicles vehicles = sc.getVehicles();
		final boolean recreated = sc.createVehicleContainer();

		Assert.assertSame(
				"vehicles re-created!",
				vehicles,
				sc.getVehicles() );

		Assert.assertTrue( "vehicles said not created" , created );
		Assert.assertFalse( "vehicles said recreated" , recreated );
	}

	@Test
	public void testCreateOnlyOneHouseholds() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createHouseholdsContainer();
		final Households households = sc.getHouseholds();
		final boolean recreated = sc.createHouseholdsContainer();

		Assert.assertSame(
				"households re-created!",
				households,
				sc.getHouseholds() );

		Assert.assertTrue( "households said not created" , created );
		Assert.assertFalse( "households said recreated" , recreated );
	}

	@Test
	public void testCreateOnlyOneKnowledges() {
		// for some reason, the default is true
		final Config config = ConfigUtils.createConfig();
		config.scenario().setUseKnowledge( false );
		final ScenarioImpl sc = new ScenarioImpl( config );

		final boolean created = sc.createKnowledges();
		final Knowledges knowledge = sc.getKnowledges();
		final boolean recreated = sc.createKnowledges();

		Assert.assertSame(
				"knowledges re-created!",
				knowledge,
				sc.getKnowledges() );

		Assert.assertTrue( "knowledge said not created" , created );
		Assert.assertFalse( "knowledge said recreated" , recreated );
	}


	@Test
	public void testCreateOnlyOneSchedule() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createTransitSchedule();
		final TransitSchedule schedule = sc.getTransitSchedule();
		final boolean recreated = sc.createTransitSchedule();

		Assert.assertSame(
				"schedule re-created!",
				schedule,
				sc.getTransitSchedule() );

		Assert.assertTrue( "schedule said not created" , created );
		Assert.assertFalse( "schedule said recreated" , recreated );
	}

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

