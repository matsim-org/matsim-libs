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

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.households.Households;
import org.matsim.knowledges.Knowledges;
import org.matsim.lanes.data.v11.LaneDefinitions;
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
	public void testCreateOnlyOneLanes() {
		final ScenarioImpl sc = new ScenarioImpl( ConfigUtils.createConfig() );

		final boolean created = sc.createLaneDefinitionsContainer();
		final LaneDefinitions lanes = sc.getLaneDefinitions11();
		final boolean recreated = sc.createKnowledges();

		Assert.assertSame(
				"lanes re-created!",
				lanes,
				sc.getLaneDefinitions11() );

		Assert.assertTrue( "lanes said not created" , created );
		Assert.assertFalse( "lanes said recreated" , recreated );
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
}

