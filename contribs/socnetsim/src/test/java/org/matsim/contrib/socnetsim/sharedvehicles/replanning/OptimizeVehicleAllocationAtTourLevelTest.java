/* *********************************************************************** *
 * project: org.matsim.*
 * OptimizeVehicleAllocationAtTourLevelTest.java
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.vehicles.Vehicle;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class OptimizeVehicleAllocationAtTourLevelTest {
	private static final String MODE = "the_vehicular_mode";

	private final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();

	private GroupPlans createTestPlan(final Random random) {
		// attempt to get a high diversity of joint structures.
		final int nMembers = random.nextInt( 100 );
		//final int nJointPlans = random.nextInt( nMembers );
		//final double pJoin = 0.1;
		//final JointPlansFactory jointPlansFact = new JointPlans().getFactory();

		final List<Plan> individualPlans = new ArrayList<Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		// create plans
		int currentId = 0;
		for (int j=0; j < nMembers; j++) {
			final Id<Person> id = Id.create( currentId++ , Person.class );
			final Person person = PopulationUtils.getFactory().createPerson(id);

			final Plan plan = PopulationUtils.createPlan(person);
			fillPlan( plan , random );
			person.addPlan( plan );

			individualPlans.add( plan );
		}

		// TODO: join some plans in joint plans

		return new GroupPlans( jointPlans , individualPlans );
	}

	private void fillPlan(
			final Plan plan,
			final Random random) {
		final Id<Link> homeLinkId = Id.create( "versailles" , Link.class );

		final Activity firstAct = popFact.createActivityFromLinkId( "h" , homeLinkId );
		firstAct.setEndTime( random.nextDouble() * 24 * 3600d );
		plan.addActivity( firstAct );

		final Leg l = popFact.createLeg( MODE );
		l.setRoute( RouteUtils.createLinkNetworkRouteImpl(Id.create( 1 , Link.class ), Id.create( 12 , Link.class )) );
		l.setTravelTime( random.nextDouble() * 36000 );
		plan.addLeg( l );

		plan.addActivity( popFact.createActivityFromLinkId( "h" , homeLinkId ) );
	}

	@Test
	@Disabled("TODO")
	void testVehiclesAreAllocatedAtTheTourLevel() throws Exception {
		throw new UnsupportedOperationException( "TODO" );
	}

	@Test
	void testCannotFindBetterAllocationRandomly() throws Exception {
		Set<String> stages = new HashSet<>();// formerly EmptyStageActivityTypes.INSTANCE;

		for ( int i = 0; i < 5; i++ ) {
			final GroupPlans optimized = createTestPlan( new Random( i ) );

			final VehicleRessources vehs = createRessources( optimized );

			final OptimizeVehicleAllocationAtTourLevelAlgorithm algo =
				new OptimizeVehicleAllocationAtTourLevelAlgorithm(
						stages,
						new Random( 1234 ),
						vehs,
						Collections.singleton( MODE ),
						false,
						TimeInterpretation.create(ConfigUtils.createConfig()));
			algo.run( optimized );
			final double optimizedOverlap = algo.calcOverlap( optimized );
			final Counter counter = new Counter( "test plan # "+(i+1)+", test # " );
			for ( int j = 0; j < 500; j++ ) {
				counter.incCounter();
				final GroupPlans randomized = createTestPlan( new Random( i ) );
				 new AllocateVehicleToPlansInGroupPlanAlgorithm(
						new Random( j ),
						vehs,
						Collections.singleton( MODE ),
						false,
						false).run( randomized );
				 final double randomizedOverlap = algo.calcOverlap( randomized );
				 Assertions.assertTrue(
						 optimizedOverlap <= randomizedOverlap,
						 "["+i+","+j+"] found better solution than optimized one: "+randomizedOverlap+" < "+optimizedOverlap );
			}
			counter.printCounter();
		}
	}

	private VehicleRessources createRessources(final GroupPlans optimized) {
		final Set<Id<Vehicle>> ids = new HashSet<>();
		final int nVehicles = optimized.getAllIndividualPlans().size() / 2;

		int i = 0;
		for ( Plan p : optimized.getAllIndividualPlans() ) {
			if ( i++ == nVehicles ) break;
			ids.add( Id.create( p.getPerson().getId() , Vehicle.class ) );
		}

		return new VehicleRessources() {
			@Override
			public Set<Id<Vehicle>> identifyVehiclesUsableForAgent(final Id<Person> person) {
				return ids;
			}
		};
	}
}

