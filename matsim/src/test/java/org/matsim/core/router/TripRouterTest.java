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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author thibautd
 */
public class TripRouterTest {
	@Test
	void testTripInsertion() {
		Plan plan = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivity(plan, "-4");
		PopulationUtils.createAndAddLeg( plan, "-3" );
		PopulationUtils.createAndAddActivity(plan, "-2");
		PopulationUtils.createAndAddLeg( plan, "-1" );
		Activity o = PopulationUtils.createAndAddActivity(plan, "1");
		Activity d = PopulationUtils.createAndAddActivity(plan, "5");
		PopulationUtils.createAndAddLeg( plan, "6" );
		PopulationUtils.createAndAddActivity(plan, "7");
		PopulationUtils.createAndAddLeg( plan, "8" );
		PopulationUtils.createAndAddActivity(plan, "9");

		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( PopulationUtils.createLeg("2") );
		trip.add( PopulationUtils.createActivityFromLinkId("3", Id.create( "coucou", Link.class )) );
		trip.add( PopulationUtils.createLeg("4") );

		TripRouter.insertTrip( plan , o , trip , d );

		assertEquals(
				13,
				plan.getPlanElements().size(),
				"insertion did not produce the expected plan length!");

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
					newIndex > oldIndex,
					"wrong inserted sequence: "+plan.getPlanElements());
			oldIndex = newIndex;
		}
	}

	@Test
	void testTripInsertionIfActivitiesImplementEquals() {
		Plan plan = PopulationUtils.createPlan();
		plan.addActivity( new EqualsActivity( "-4" , Id.create( 1, Link.class ) ) );
		PopulationUtils.createAndAddLeg( plan, "-3" );
		plan.addActivity( new EqualsActivity( "-2" , Id.create( 1, Link.class ) ) );
		PopulationUtils.createAndAddLeg( plan, "-1" );
		Activity o = new EqualsActivity( "1" , Id.create( 1, Link.class ) );
		plan.addActivity( o );
		Activity d = new EqualsActivity( "5" , Id.create( 1, Link.class ) );
		plan.addActivity( d );
		PopulationUtils.createAndAddLeg( plan, "6" );
		plan.addActivity( new EqualsActivity( "7" , Id.create( 1, Link.class ) ) );
		PopulationUtils.createAndAddLeg( plan, "8" );
		plan.addActivity( new EqualsActivity( "9" , Id.create( 1, Link.class ) ) );

		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( PopulationUtils.createLeg("2") );
		trip.add( PopulationUtils.createActivityFromLinkId("3", Id.create( "coucou", Link.class )) );
		trip.add( PopulationUtils.createLeg("4") );

		TripRouter.insertTrip( plan , o , trip , d );

		assertEquals(
				13,
				plan.getPlanElements().size(),
				"insertion did not produce the expected plan length!");

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
					newIndex > oldIndex,
					"wrong inserted sequence: "+plan.getPlanElements());
			oldIndex = newIndex;
		}
	}

	@Test
	void testReturnedOldTrip() throws Exception {
		List<PlanElement> expected = new ArrayList<PlanElement>();

		Plan plan = PopulationUtils.createPlan();
		PopulationUtils.createAndAddActivity(plan, "-4");
		PopulationUtils.createAndAddLeg( plan, "-3" );
		PopulationUtils.createAndAddActivity(plan, "-2");
		PopulationUtils.createAndAddLeg( plan, "-1" );
		Activity o = PopulationUtils.createAndAddActivity(plan, "1");
		expected.add( PopulationUtils.createAndAddLeg( plan, "some mode" ) );
		expected.add( PopulationUtils.createAndAddActivity(plan, "stage") );
		expected.add( PopulationUtils.createAndAddLeg( plan, "some other mode" ) );
		expected.add( PopulationUtils.createAndAddActivity(plan, "another stage") );
		expected.add( PopulationUtils.createAndAddLeg( plan, "yet  another mode" ) );
		Activity d = PopulationUtils.createAndAddActivity(plan, "5");
		PopulationUtils.createAndAddLeg( plan, "6" );
		PopulationUtils.createAndAddActivity(plan, "7");
		PopulationUtils.createAndAddLeg( plan, "8" );
		PopulationUtils.createAndAddActivity(plan, "9");
		
		List<PlanElement> trip = new ArrayList<PlanElement>();
		trip.add( PopulationUtils.createLeg("2") );
		trip.add( PopulationUtils.createActivityFromLinkId("3", Id.create( "coucou", Link.class )) );
		trip.add( PopulationUtils.createLeg("4") );

		assertEquals(
				expected,
				TripRouter.insertTrip( plan , o , trip , d ),
				"wrong old trip" );
	}

	private static class EqualsActivity implements Activity {
		private final Activity delegate ;
		@Override
		public OptionalTime getEndTime() {
			return this.delegate.getEndTime();
		}
		@Override
		public void setEndTime(double seconds) {
			this.delegate.setEndTime(seconds);
		}

		@Override
		public void setEndTimeUndefined() {
			this.delegate.setEndTimeUndefined();
		}

		@Override
		public String getType() {
			return this.delegate.getType();
		}
		@Override
		public void setType(String type) {
			this.delegate.setType(type);
		}
		@Override
		public Coord getCoord() {
			return this.delegate.getCoord();
		}
		@Override
		public OptionalTime getStartTime() {
			return this.delegate.getStartTime();
		}
		@Override
		public void setStartTime(double seconds) {
			this.delegate.setStartTime(seconds);
		}

		@Override
		public void setStartTimeUndefined() {
			delegate.setStartTimeUndefined();
		}

		@Override
		public OptionalTime getMaximumDuration() {
			return this.delegate.getMaximumDuration();
		}
		@Override
		public void setMaximumDuration(double seconds) {
			this.delegate.setMaximumDuration(seconds);
		}
		@Override
		public void setMaximumDurationUndefined() {
			this.delegate.setMaximumDurationUndefined();
		}
		@Override
		public Id<Link> getLinkId() {
			return this.delegate.getLinkId();
		}
		@Override
		public Id<ActivityFacility> getFacilityId() {
			return this.delegate.getFacilityId();
		}
		public EqualsActivity(final String type, final Id<Link> link) {
			delegate = PopulationUtils.createActivityFromLinkId(type, link) ;
		}
		@Override
		public int hashCode() {
			return 1;
		}
		@Override
		public boolean equals(final Object o) {
			return true;
		}
		@Override
		public void setLinkId(Id<Link> id) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		public void setFacilityId(Id<ActivityFacility> id) {
			throw new RuntimeException("not implemented") ;
		}
		@Override
		public void setCoord(Coord coord) {
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			return delegate.getAttributes();
		}
	}
}

