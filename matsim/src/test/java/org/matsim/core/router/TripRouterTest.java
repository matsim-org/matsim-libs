/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterTest.java
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
package org.matsim.core.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * @author thibautd
 */
public class TripRouterTest {
	@Test
	public void testTripInsertion() {
		PlanImpl plan = new PlanImpl();
		plan.createAndAddActivity( "-4" );
		plan.createAndAddLeg( "-3" );
		plan.createAndAddActivity( "-2" );
		plan.createAndAddLeg( "-1" );
		Activity o = plan.createAndAddActivity( "1" );
		Activity d = plan.createAndAddActivity( "5" );
		plan.createAndAddLeg( "6" );
		plan.createAndAddActivity( "7" );
		plan.createAndAddLeg( "8" );
		plan.createAndAddActivity( "9" );

		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( new LegImpl( "2" ) );
		trip.add( new ActivityImpl( "3" , Id.create( "coucou", Link.class ) ) );
		trip.add( new LegImpl( "4" ) );

		TripRouter.insertTrip( plan , o , trip , d );

		assertEquals(
				"insertion did not produce the expected plan length!",
				13,
				plan.getPlanElements().size());

		int oldIndex = Integer.MIN_VALUE;
		for (PlanElement pe : plan.getPlanElements()) {
			int newIndex = -1;

			if (pe instanceof Activity) {
				newIndex = Integer.parseInt( ((Activity) pe).getType() );
			}
			else {
				newIndex = Integer.parseInt( ((Leg) pe).getMode() );
			}

			assertTrue(
					"wrong inserted sequence: "+plan.getPlanElements(),
					newIndex > oldIndex);
			oldIndex = newIndex;
		}
	}

	@Test
	public void testTripInsertionIfActivitiesImplementEquals() {
		PlanImpl plan = new PlanImpl();
		plan.addActivity( new EqualsActivity( "-4" , Id.create( 1, Link.class ) ) );
		plan.createAndAddLeg( "-3" );
		plan.addActivity( new EqualsActivity( "-2" , Id.create( 1, Link.class ) ) );
		plan.createAndAddLeg( "-1" );
		Activity o = new EqualsActivity( "1" , Id.create( 1, Link.class ) );
		plan.addActivity( o );
		Activity d = new EqualsActivity( "5" , Id.create( 1, Link.class ) );
		plan.addActivity( d );
		plan.createAndAddLeg( "6" );
		plan.addActivity( new EqualsActivity( "7" , Id.create( 1, Link.class ) ) );
		plan.createAndAddLeg( "8" );
		plan.addActivity( new EqualsActivity( "9" , Id.create( 1, Link.class ) ) );

		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( new LegImpl( "2" ) );
		trip.add( new ActivityImpl( "3" , Id.create( "coucou", Link.class ) ) );
		trip.add( new LegImpl( "4" ) );

		TripRouter.insertTrip( plan , o , trip , d );

		assertEquals(
				"insertion did not produce the expected plan length!",
				13,
				plan.getPlanElements().size());

		int oldIndex = Integer.MIN_VALUE;
		for (PlanElement pe : plan.getPlanElements()) {
			int newIndex = -1;

			if (pe instanceof Activity) {
				newIndex = Integer.parseInt( ((Activity) pe).getType() );
			}
			else {
				newIndex = Integer.parseInt( ((Leg) pe).getMode() );
			}

			assertTrue(
					"wrong inserted sequence: "+plan.getPlanElements(),
					newIndex > oldIndex);
			oldIndex = newIndex;
		}
	}

	@Test
	public void testReturnedOldTrip() throws Exception {
		List<PlanElement> expected = new ArrayList<PlanElement>();

		PlanImpl plan = new PlanImpl();
		plan.createAndAddActivity( "-4" );
		plan.createAndAddLeg( "-3" );
		plan.createAndAddActivity( "-2" );
		plan.createAndAddLeg( "-1" );
		Activity o = plan.createAndAddActivity( "1" );
		expected.add( plan.createAndAddLeg( "some mode" ) );
		expected.add( plan.createAndAddActivity( "stage" ) );
		expected.add( plan.createAndAddLeg( "some other mode" ) );
		expected.add( plan.createAndAddActivity( "another stage" ) );
		expected.add( plan.createAndAddLeg( "yet  another mode" ) );
		Activity d = plan.createAndAddActivity( "5" );
		plan.createAndAddLeg( "6" );
		plan.createAndAddActivity( "7" );
		plan.createAndAddLeg( "8" );
		plan.createAndAddActivity( "9" );
		
		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( new LegImpl( "2" ) );
		trip.add( new ActivityImpl( "3" , Id.create( "coucou", Link.class ) ) );
		trip.add( new LegImpl( "4" ) );

		assertEquals(
				"wrong old trip",
				expected,
				TripRouter.insertTrip( plan , o , trip , d ) );
	}

	private static class EqualsActivity extends ActivityImpl {
		public EqualsActivity(final String type, final Id<Link> link) {
			super( type , link );
		}

		@Override
		public int hashCode() {
			return 1;
		}

		@Override
		public boolean equals(final Object o) {
			return true;
		}
	}
}

