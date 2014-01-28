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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public class DefaultPlanLinkIdentifierTest {
	@Test
	public void testNotLinkedWhenNoVehicleDefined() {
		final Plan plan1 = createVehicularPlan( new IdImpl( 1 ) , null );
		final Plan plan2 = createVehicularPlan( new IdImpl( 2 ) , null );

		final PlanLinkIdentifier testee = PlanLinkIdentifierUtils.createDefaultPlanLinkIdentifier();

		Assert.assertFalse(
				"plans without vehicle allocated are considered linked",
				testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	public void testDifferentVehiclesAreNotLinked() {
		final Plan plan1 = createVehicularPlan( new IdImpl( 1 ) , new IdImpl( 1 ) );
		final Plan plan2 = createVehicularPlan( new IdImpl( 2 ) , new IdImpl( 2 ) );

		final PlanLinkIdentifier testee = PlanLinkIdentifierUtils.createDefaultPlanLinkIdentifier();
		Assert.assertFalse(
				"plans with different vehicle allocated are considered linked",
				testee.areLinked( plan1 , plan2 ) );
	}

	@Test
	public void testSameVehiclesAreLinked() {
		final Plan plan1 = createVehicularPlan( new IdImpl( 1 ) , new IdImpl( "car" ) );
		final Plan plan2 = createVehicularPlan( new IdImpl( 2 ) , new IdImpl( "car" ) );

		final PlanLinkIdentifier testee = PlanLinkIdentifierUtils.createDefaultPlanLinkIdentifier();
		Assert.assertTrue(
				"plans with same vehicle allocated are not considered linked",
				testee.areLinked( plan1 , plan2 ) );
	}

	// TODO test joint trips

	private static Plan createVehicularPlan(
			final Id personId,
			final Id vehicleId) {
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

