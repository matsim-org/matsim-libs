/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultPlanLinkIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.replanning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.sharedvehicles.replanning.VehicularPlanBasedIdentifier;

/**
 * @author thibautd
 */
public class VehicularPlanLinkIdentifierTest {
	@Test
	void testNotLinkedWhenNoVehicleDefined() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , null );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , null );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();

		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans without vehicle allocated are considered linked" );
	}

	@Test
	void testDifferentVehiclesAreNotLinked() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , Id.create( 1 , Vehicle.class ) );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , Id.create( 2 , Vehicle.class ) );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();
		Assertions.assertFalse(
				testee.areLinked( plan1 , plan2 ),
				"plans with different vehicle allocated are considered linked" );
	}

	@Test
	void testSameVehiclesAreLinked() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , Id.create( "car" , Vehicle.class ) );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , Id.create( "car" , Vehicle.class ) );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();
		Assertions.assertTrue(
				testee.areLinked( plan1 , plan2 ),
				"plans with same vehicle allocated are not considered linked" );
	}

	// TODO test joint trips

	private static Plan createVehicularPlan(
			final Id<Person> personId,
			final Id<Vehicle> vehicleId) {
		final PopulationFactory fact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();

		final Plan plan1 = fact.createPlan();
		plan1.setPerson( fact.createPerson( personId ) );
		final Leg leg1 = fact.createLeg( TransportMode.car );
		plan1.addLeg( leg1 );
		final NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(null, null);
		leg1.setRoute( route1 );
		route1.setVehicleId( vehicleId );

		return plan1;
	}
}

