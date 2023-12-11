/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertionWithSocialNetworkTest.java
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;

/**
 * @author thibautd
 */
public class JointTripInsertionWithSocialNetworkTest {
	private static final Logger log =
		LogManager.getLogger(JointTripInsertionWithSocialNetworkTest.class);

	@Test
	void testJointTripsGeneratedOnlyAlongSocialTies() {
		final Random random = new Random( 123 );

		for ( int i=0; i < 10; i++ ) {
			final Scenario scenario = generateScenario();

			final SocialNetwork sn =
					(SocialNetwork) scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME );
			final JointTripInsertorAlgorithm algo =
				new JointTripInsertorAlgorithm(
						random,
						sn,
						new JointTripInsertorConfigGroup(),
//						new TripRouter() );
//						new TripRouter.Builder( scenario.getConfig() ).build() ) ;
//						  new MainModeIdentifierImpl() // yyyyyy ????
							  TripStructureUtils.getRoutingModeIdentifier() // yyyyyy ??????
						) ;

			final JointPlan jp = groupAllPlansInJointPlan( scenario.getPopulation() );


			final Set<Id<Person>> agentsToIgnore = new HashSet< >();
			while ( true ) {
				final ActedUponInformation actedUpon =
							algo.run( jp , agentsToIgnore );

				if (actedUpon == null) break;
				agentsToIgnore.add( actedUpon.getDriverId() );
				agentsToIgnore.add( actedUpon.getPassengerId() );

				Assertions.assertTrue(
						sn.getAlters( actedUpon.getDriverId() ).contains( actedUpon.getPassengerId() ),
						"passenger not alter of driver!" );
			}

			log.info( "there were "+agentsToIgnore.size()+" agents handled" );
		}
	}

	private JointPlan groupAllPlansInJointPlan(final Population population) {
		final Map<Id<Person>, Plan> plans = new HashMap< >();

		for ( Person person : population.getPersons().values() ) {
			plans.put(
					person.getId(),
					person.getSelectedPlan() );
		}

		return new JointPlanFactory().createJointPlan( plans );
	}

	private Scenario generateScenario() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		final Population population = sc.getPopulation();
		final PopulationFactory factory = population.getFactory();

		final Coord coordHome = new Coord((double) 0, (double) 0);
		final Id linkHome = Id.create( "link" , Link.class );
		final int nAgents = 100;
		for ( int i = 0; i < nAgents; i++ ) {
			final Person person = factory.createPerson( Id.create( i , Person.class ) );
			final Plan plan = factory.createPlan();

			final Activity firstAct = (Activity) factory.createActivityFromCoord( "h" , coordHome );
			firstAct.setEndTime( 10 );
			firstAct.setLinkId( linkHome );
			plan.addActivity( firstAct );

			final Leg leg = factory.createLeg( i % 2 == 0 ? TransportMode.car : TransportMode.pt );
			TripStructureUtils.setRoutingMode( leg, leg.getMode() );
			plan.addLeg( leg );

			final Activity secondAct = (Activity) factory.createActivityFromCoord( "h" , coordHome );
			secondAct.setLinkId( linkHome );
			plan.addActivity( secondAct );

			person.addPlan( plan );
			population.addPerson( person );
		}

		final SocialNetwork sn = new SocialNetworkImpl( true );
		sc.addScenarioElement( SocialNetwork.ELEMENT_NAME , sn );

		for ( int i=0; i < nAgents; i++ ) sn.addEgo( Id.create( i , Person.class ) );
		for ( int i=0; i < nAgents - 1; i++ ) {
			sn.addBidirectionalTie( Id.create( i , Person.class ) , Id.create( i + 1 , Person.class ) );
		}

		return sc;
	}
}

