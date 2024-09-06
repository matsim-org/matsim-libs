/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToPlansInGroupPlanAlgorithmTest.java
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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class AllocateVehicleToPlansInGroupPlanAlgorithmTest {
	private static final Logger log =
		LogManager.getLogger(AllocateVehicleToPlansInGroupPlanAlgorithmTest.class);

	private static final String MODE = "the_vehicular_mode";

	// uncomment to get more information in case of failure
	//@BeforeEach
	//public void setupLog() {
	//	LogManager.getLogger( AllocateVehicleToPlansInGroupPlanAlgorithm.class ).setLevel( Level.TRACE );
	//}

	private GroupPlans createTestPlan() {
		final List<Plan> indivPlans = new ArrayList<Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		for ( int i = 0; i < 5; i++ ) {
			final Person person = popFact.createPerson( Id.createPersonId( "indiv-"+i ) );
			final Plan plan = popFact.createPlan();
			person.addPlan( plan );
			plan.setPerson( person );
			indivPlans.add( plan );

			plan.addActivity( popFact.createActivityFromCoord( "h" , new Coord((double) 12, (double) 34)) );
			if ( i % 2 == 0 ) {
				plan.addLeg( popFact.createLeg( "some_non_vehicular_mode" ) );
			}
			else {
				final Leg l = popFact.createLeg( MODE );
				l.setRoute( RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId( 1 ), Id.createLinkId( 12 )) );
				plan.addLeg( l );
			}
			plan.addActivity( popFact.createActivityFromLinkId( "h" , Id.createLinkId( 42 ) ) );
		}

		final JointPlanFactory jpFact = new JointPlanFactory();
		for ( int i = 1; i < 5; i++ ) {
			final Map<Id<Person>, Plan> plans = new HashMap< >();
			for ( int j = 0; j < i; j++ ) {
				final Person person = popFact.createPerson( Id.createPersonId( "joint-"+i+"-"+j ) );
				final Plan plan = popFact.createPlan();
				person.addPlan( plan );
				plan.setPerson( person );
				plans.put( person.getId() , plan );

				plan.addActivity( popFact.createActivityFromCoord( "h" , new Coord((double) 12, (double) 34)) );
				if ( (j+1) % 2 == 0 ) {
					plan.addLeg( popFact.createLeg( "some_non_vehicular_mode" ) );
				}
				else {
					final Leg l = popFact.createLeg( MODE );
					l.setRoute( RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId( 1 ), Id.createLinkId( 12 )) );
					plan.addLeg( l );
				}
				plan.addActivity( popFact.createActivityFromLinkId( "h" , Id.createLinkId( 42 ) ) );
			}
			jointPlans.add( jpFact.createJointPlan( plans ) );
		}

		return new GroupPlans( jointPlans , indivPlans );
	}

	@Test
	void testEnoughVehiclesForEverybody() {
		// tests that one vehicle is allocated to each one if possible
		final Random random = new Random( 1234 );

		for ( int i = 0; i < 10 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createEnoughVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			final Set<Id> allocated = new HashSet<Id>();
			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				Assertions.assertTrue(
						v == null || allocated.add( v ),
						"vehicle "+v+" already allocated" );
			}
		}
	}

	@Test
	void testOneVehiclePerTwoPersons() {
		// tests that the allocation minimizes overlaps
		final Random random = new Random( 1234 );

		for ( int i = 0; i < 10 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			final Map<Id, Integer> counts = new LinkedHashMap<Id, Integer>();
			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				Integer c = counts.get( v );
				counts.put( v , c == null ? 1 : c + 1 );
			}

			final int max = Collections.max( counts.values() );
			Assertions.assertTrue(
					max <= 2,
					"one vehicle was allocated "+max+" times while maximum expected was 2 in "+counts );
		}
	}

	@Test
	void testRandomness() {
		final Random random = new Random( 1234 );

		final Map<Id, Id> allocations = new HashMap<Id, Id>();
		final Set<Id> agentsWithSeveralVehicles = new HashSet<Id>();
		for ( int i = 0; i < 50 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				final Id person = p.getPerson().getId();
				final Id oldV = allocations.get( person );

				if ( oldV == null ) {
					allocations.put( person , v );
				}
				else if ( !oldV.equals( v ) ) {
					agentsWithSeveralVehicles.add( person );
				}
			}
		}

		Assertions.assertEquals(
				allocations.size(),
				agentsWithSeveralVehicles.size(),
				"unexpected number of agents having got several vehicles" );
	}

	@Test
	void testDeterminism() {
		final Map<Id, Id> allocations = new HashMap<Id, Id>();
		final Set<Id> agentsWithSeveralVehicles = new HashSet<Id>();
		for ( int i = 0; i < 50 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						new Random( 1432 ),
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				final Id person = p.getPerson().getId();
				final Id oldV = allocations.get( person );

				if ( oldV == null ) {
					allocations.put( person , v );
				}
				else if ( !oldV.equals( v ) ) {
					agentsWithSeveralVehicles.add( person );
				}
			}
		}

		Assertions.assertEquals(
				0,
				agentsWithSeveralVehicles.size(),
				"unexpected number of agents having got several vehicles" );
	}

	private static Id assertSingleVehicleAndGetVehicleId(final Plan p) {
		Id v = null;

		for ( PlanElement pe : p.getPlanElements() ) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg leg = (Leg) pe;

			if ( !MODE.equals( leg.getMode() ) ) continue;
			final NetworkRoute r = (NetworkRoute) leg.getRoute();

			Assertions.assertNotNull(
					r.getVehicleId(),
					"null vehicle id in route" );

			Assertions.assertTrue(
					v == null || r.getVehicleId().equals( v ),
					"vehicle "+r.getVehicleId()+" not the same as "+v );

			v = r.getVehicleId();
		}

		return v;
	}

	private static VehicleRessources createEnoughVehicles( final GroupPlans plans ) {
		final Set<Id<Vehicle>> vehs = new HashSet<>();

		for ( int i = 0; i < plans.getAllIndividualPlans().size() ; i++ ) {
			vehs.add( Id.create( i , Vehicle.class ) );
		}

		log.trace( "created "+vehs.size()+" vehicles" );

		return new VehicleRessources() {
				@Override
				public Set<Id<Vehicle>> identifyVehiclesUsableForAgent(final Id<Person> person) {
					return vehs;
				}
			};
	}

	private static VehicleRessources createHalfVehicles( final GroupPlans plans ) {
		final Set<Id<Vehicle>> vehs = new HashSet<Id<Vehicle>>();

		// half the agents have no vehicular route: divide by 4
		for ( int i = 0; i < plans.getAllIndividualPlans().size() / 4. ; i++ ) {
			vehs.add( Id.create( i , Vehicle.class) );
		}

		log.trace( "created "+vehs.size()+" vehicles" );

		return new VehicleRessources() {
				@Override
				public Set<Id<Vehicle>> identifyVehiclesUsableForAgent(final Id<Person> person) {
					return vehs;
				}
			};
	}
}

