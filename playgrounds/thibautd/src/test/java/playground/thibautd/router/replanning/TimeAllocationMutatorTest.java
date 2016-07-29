/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router.replanning;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanMutateTimeAllocation;
import org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


/**
 * @author thibautd
 */
public class TimeAllocationMutatorTest {
	private static final Double MUTATION_RANGE = 7200.;
	private static final int SEED = 1230534;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	private List<Plan> plans;

	@Before
	public void initPlans() {
		Scenario s = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		PopulationReader reader = new PopulationReader( s );
		reader.readFile( utils.getPackageInputDirectory() + "/plans.xml.gz" );
		plans = new ArrayList<Plan>();
		for (Person p : s.getPopulation().getPersons().values()) {
			plans.add( (Plan) p.getSelectedPlan() );
		}
	}

	@Test
	public void testAgainstTransitTimeAllocationMutator() {
		boolean affectingDuration = true ;

		TripPlanMutateTimeAllocation transit =
			new TripPlanMutateTimeAllocation(
					new StageActivityTypesImpl( PtConstants.TRANSIT_ACTIVITY_TYPE ),
					MUTATION_RANGE,
					affectingDuration, new Random( SEED ));

		PlanMutateTimeAllocation regular =
			new PlanMutateTimeAllocation(
					MUTATION_RANGE,
					new Random( SEED ));

		TripsToLegsAlgorithm trips2legs =
			new TripsToLegsAlgorithm(
					new StageActivityTypesImpl(
						PtConstants.TRANSIT_ACTIVITY_TYPE ),
					new MainModeIdentifierImpl() );

		Counter counter = new Counter( getClass().getSimpleName()+": testing plan # " );
		for (Plan plan : plans) {
			counter.incCounter();
			Plan planTransit = PopulationUtils.createPlan();
			PopulationUtils.copyFromTo( plan, planTransit );
			transit.run( planTransit );

			Plan planTrips2Legs = PopulationUtils.createPlan();
			PopulationUtils.copyFromTo( plan, planTrips2Legs );
			trips2legs.run( planTrips2Legs );
			regular.run( planTrips2Legs );

			comparePlansActivityEnds( planTransit , planTrips2Legs );
		}
		counter.printCounter();
	}

	private static void comparePlansActivityEnds(
			final Plan planTransit,
			final Plan planTrips2Legs) {
		Iterator<PlanElement> transit = planTransit.getPlanElements().iterator();
		Iterator<PlanElement> trips2leg = planTrips2Legs.getPlanElements().iterator();

		while (transit.hasNext()) {
			PlanElement transitElement = transit.next();
			while ( !(transitElement instanceof Activity) ||
					((Activity) transitElement).getType().equals( PtConstants.TRANSIT_ACTIVITY_TYPE ) ) {
				transitElement = transit.next();
			}

			PlanElement t2lElement = trips2leg.next();
			while ( !(t2lElement instanceof Activity) ) {
				t2lElement = trips2leg.next();
			}

			Assert.assertEquals(
					"the two approaches give different results!",
					((Activity) transitElement).getEndTime(),
					((Activity) t2lElement).getEndTime(),
					MatsimTestUtils.EPSILON);
		}
	}
}

