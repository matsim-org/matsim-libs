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
package playground.thibautd.socnetsim.replanning;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.sharedvehicles.replanning.VehicularPlanBasedIdentifier;

/**
 * @author thibautd
 */
public class VehicularPlanLinkIdentifierTest {
	@Test
	public void testNotLinkedWhenNoVehicleDefined() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , null );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , null );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();

		Assert.assertFalse(
				"plans without vehicle allocated are considered linked",
				testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	public void testDifferentVehiclesAreNotLinked() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , Id.create( 1 , Vehicle.class ) );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , Id.create( 2 , Vehicle.class ) );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();
		Assert.assertFalse(
				"plans with different vehicle allocated are considered linked",
				testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	public void testSameVehiclesAreLinked() {
		final Plan plan1 = createVehicularPlan( Id.create( 1 , Person.class ) , Id.create( "car" , Vehicle.class ) );
		final Plan plan2 = createVehicularPlan( Id.create( 2 , Person.class ) , Id.create( "car" , Vehicle.class ) );

		final PlanLinkIdentifier testee = new VehicularPlanBasedIdentifier();
		Assert.assertTrue(
				"plans with same vehicle allocated are not considered linked",
				testee.areLinked( plan1 , plan2 ) );
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
		final NetworkRoute route1 = new LinkNetworkRouteImpl( null , null );
		leg1.setRoute( route1 );
		route1.setVehicleId( vehicleId );

		return plan1;
	}
}

