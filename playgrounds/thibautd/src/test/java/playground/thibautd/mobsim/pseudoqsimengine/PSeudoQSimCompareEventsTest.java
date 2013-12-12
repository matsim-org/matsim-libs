/* *********************************************************************** *
 * project: org.matsim.*
 * PSeudoQSimCompareEventsTest.java
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
package playground.thibautd.mobsim.pseudoqsimengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.thibautd.mobsim.CompareEventsUtils;
import playground.thibautd.scripts.CreateGridNetworkWithDimensions;

/**
 * @author thibautd
 */
public class PSeudoQSimCompareEventsTest {
	@Test
	public void testEventsSimilarToQsim() {
		final Scenario scenario = createTestScenario();

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());

		CompareEventsUtils.testEventsSimilarToQsim(
				createTestScenario(),
				new PlanRouter(
					new TripRouterFactoryBuilderWithDefaults().build(
						scenario ).instantiateAndConfigureTripRouter(
							new RoutingContextImpl(
								new TravelTimeAndDistanceBasedTravelDisutility(
									travelTime.getLinkTravelTimes(),
									scenario.getConfig().planCalcScore() ),
								travelTime.getLinkTravelTimes() ) ) ),
				new QSimFactory(),
				new QSimWithPseudoEngineFactory(
					travelTime.getLinkTravelTimes() ),
				travelTime );
	}

	private Scenario createTestScenario() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		CreateGridNetworkWithDimensions.createNetwork(
				sc.getNetwork(),
				2,
				10 );

		final Random random = new Random( 1234 );
		final List<Id> linkIds = new ArrayList<Id>( sc.getNetwork().getLinks().keySet() );
		for ( int i = 0; i < 20; i++ ) {
			final Person person = sc.getPopulation().getFactory().createPerson( new IdImpl( i ) );
			sc.getPopulation().addPerson( person );

			final Plan plan = sc.getPopulation().getFactory().createPlan();
			person.addPlan( plan );

			final Activity firstActivity = 
					sc.getPopulation().getFactory().createActivityFromLinkId(
						"h",
						linkIds.get(
							random.nextInt(
								linkIds.size() ) ) );

			// everybody leaves at the same time, to have some congestion
			firstActivity.setEndTime( 10 );
			plan.addActivity( firstActivity );

			plan.addLeg( sc.getPopulation().getFactory().createLeg( TransportMode.car ) );

			plan.addActivity(
					sc.getPopulation().getFactory().createActivityFromLinkId(
						"h",
						linkIds.get(
							random.nextInt(
								linkIds.size() ) ) ) );

		}
		return sc;
	}

}

