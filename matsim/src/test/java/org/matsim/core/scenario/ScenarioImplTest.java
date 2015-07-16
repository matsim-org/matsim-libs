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
import org.matsim.core.config.ConfigUtils;
import org.matsim.households.Households;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;

/**
 * @author thibautd
 */
public class ScenarioImplTest {
	@Test
	public void testCreateOnlyOneVehiclesContainer() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createTransitVehicleContainer();
		final Vehicles vehicles = sc.getTransitVehicles();
		final boolean recreated = sc.createTransitVehicleContainer();

		Assert.assertSame(
				"vehicles re-created!",
				vehicles,
				sc.getTransitVehicles() );

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
	public void testCreateOnlyOneSchedule() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createTransitScheduleContainer();
		final TransitSchedule schedule = sc.getTransitSchedule();
		final boolean recreated = sc.createTransitScheduleContainer();

		Assert.assertSame(
				"schedule re-created!",
				schedule,
				sc.getTransitSchedule() );

		Assert.assertTrue( "schedule said not created" , created );
		Assert.assertFalse( "schedule said recreated" , recreated );
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

