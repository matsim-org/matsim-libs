/* *********************************************************************** *
 * project: org.matsim.*
 * RemovalTest.java
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
package playground.thibautd.hitchhiking.replanning;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import playground.thibautd.hitchiking.HitchHikingConstants;
import playground.thibautd.hitchiking.replanning.HitchHikingInsertionAlgorithm;
import playground.thibautd.hitchiking.replanning.HitchHikingInsertionRemovalAlgorithm;
import playground.thibautd.hitchiking.replanning.HitchHikingRemovalAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author thibautd
 */
public class RemovalTest {
	private List<Plan> plans;

	@Before
	public void initPlans() {
		plans = new ArrayList<Plan>();

		Id<Link> link1 = Id.create( "link1" , Link.class );
		Id<Link> link2 = Id.create( "link2" , Link.class );

		PlanImpl plan = new PlanImpl(PersonImpl.createPerson(Id.create("one passenger trip", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl(PersonImpl.createPerson(Id.create("one driver trip", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.DRIVER_MODE );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl(PersonImpl.createPerson(Id.create("one tour with one passenger trip", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl(PersonImpl.createPerson(Id.create("one tour with one driver trip", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.DRIVER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 2 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl(PersonImpl.createPerson(Id.create("two tours with one passenger trip each", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 3 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 4 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 );

		plan = new PlanImpl(PersonImpl.createPerson(Id.create("two tours with one and two passenger trips", Person.class)));
		plans.add( plan );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 1 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 2 );
		plan.createAndAddLeg( TransportMode.pt );
		plan.createAndAddActivity( "h" , link1 ).setEndTime( 3 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "w" , link2 ).setEndTime( 4 );
		plan.createAndAddLeg( HitchHikingConstants.PASSENGER_MODE );
		plan.createAndAddActivity( "h" , link1 );
	}

	@Test
	public void testRemoval() throws Exception {
		HitchHikingRemovalAlgorithm testee = new HitchHikingRemovalAlgorithm( new Random( 1 ) );

		for (Plan plan : plans) {
			int oldHhCount = countHhTrips( plan );
			int oldCount = countTrips( plan );
			testee.run( plan );

			assertEquals(
					"unexpected number of Hh trips after removal in "+toString( plan ),
					oldHhCount -1,
					countHhTrips( plan ));

			assertEquals(
					"unexpected number of trips after removal in "+toString( plan ),
					oldCount,
					countTrips( plan ));
		}
	}

	@Test
	public void testInsertion() throws Exception {
		HitchHikingInsertionAlgorithm testee = 
			new HitchHikingInsertionAlgorithm(
					new Random( 1 ) );

		for (Plan plan : plans) {
			int oldHhCount = countHhTrips( plan );
			int oldCount = countTrips( plan );

			if ( oldCount - oldHhCount > 0 ) {
				testee.run( plan );

				assertEquals(
						"unexpected number of Hh trips after insertion in "+toString( plan ),
						oldHhCount + 1,
						countHhTrips( plan ));

				assertEquals(
						"unexpected number of trips after removal in "+toString( plan ),
						oldCount,
						countTrips( plan ));
			}
		}
	}

	@Test
	public void testInsertionRemoval() throws Exception {
		HitchHikingInsertionRemovalAlgorithm testee =
			new HitchHikingInsertionRemovalAlgorithm(
					new Random( 1 ) );
		int insertions = 0;
		int removals = 0;

		for (int i=0; i<100; i++) {
			for (Plan plan : plans) {
				int oldCount = countHhTrips( plan );
				testee.run( plan );
				int newCount = countHhTrips( plan );

				if ( newCount > oldCount ) {
					insertions++;
				}
				else if ( newCount < oldCount ) {
					removals++;
				}
				else {
					fail( "neither insertion nor removal on "+toString( plan ) );
				}
			}
		}

		assertTrue(
				"no insertion performed!",
				insertions > 0);

		assertTrue(
				"no removal performed!",
				removals > 0);
	}

	private static int countHhTrips(final Plan plan) {
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			String m = ((Leg) pe).getMode();
			if ( m.equals( HitchHikingConstants.PASSENGER_MODE ) ||
					m.equals( HitchHikingConstants.DRIVER_MODE ) ) {
				c++;
			}
		}
		return c;
	}

	private static int countTrips(final Plan plan) {
		int c = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if ( pe instanceof Leg ) c++;
		}
		return c;
	}

	private static String toString(final Plan plan) {
		return plan.getPerson().getId()+": "+plan.getPlanElements();
	}
}

