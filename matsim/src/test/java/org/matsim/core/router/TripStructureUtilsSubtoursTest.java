/* *********************************************************************** *
 * project: org.matsim.*
 * TripStructureUtilsSubtoursTest.java
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
package org.matsim.core.router;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 */
public class TripStructureUtilsSubtoursTest {
	private static final String STAGE = "stage_activity interaction";
	// /////////////////////////////////////////////////////////////////////////
	// fixtures
	// /////////////////////////////////////////////////////////////////////////
	private static class Fixture {
		private final boolean useFacilitiesAsAnchorPoint;
		private final Plan plan;
		private final List<Subtour> expectedSubtours;

		public Fixture(
				final boolean useFacilitiesAsAnchorPoint,
				final Plan plan,
				final List<Subtour> subtoursIfLink) {
			this.useFacilitiesAsAnchorPoint = useFacilitiesAsAnchorPoint;
			this.plan = plan;
			this.expectedSubtours = subtoursIfLink;
		}
	}

	private static Activity createActivityFromLocationId(
			final boolean anchorAtFacilities,
			final PopulationFactory fact,
			final String type,
			final Id<?> loc) {
		final Id<Link> linkLoc = anchorAtFacilities ? Id.create( "nowhere", Link.class ) : Id.create(loc, Link.class);

		final Activity act = fact.createActivityFromLinkId( type , linkLoc );

		if (anchorAtFacilities) {
			final Id<ActivityFacility> facLoc = Id.create(loc, ActivityFacility.class) ;
			((Activity) act).setFacilityId( facLoc );
		}

		return act;
	}

	private static Id<?> createId(long no, boolean anchorAtFacilities){
		Id<?> id;
		if (anchorAtFacilities){
			id = Id.create(no, ActivityFacility.class);
		}else
		{
			id = Id.create(no, Link.class);

		}

		return id;
	}

	private static Fixture createMonoSubtourFixture(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities));
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final Activity act2b = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2b );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg( "walk" );
		plan.addLeg( leg2 );
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act3 );

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2b , trip2 , act3 ) ),
						true) ) );
	}

	private static Fixture createTwoNestedSubtours(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg("walk");
		plan.addLeg( leg2 );
		trip2.add(leg2);
		final Activity stage = createActivityFromLocationId(anchorAtFacilities, fact, STAGE, createId(2,anchorAtFacilities));
		plan.addActivity(stage);
		trip2.add( stage );
		final Leg leg3 = fact.createLeg("swim");
		plan.addLeg(leg3);
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add(leg4);

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(2,anchorAtFacilities) );
		plan.addActivity(act3);

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg( "velo" );
		plan.addLeg( leg5 );
		trip3.add(leg5);

		final Activity act4 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity(act4);

		final Subtour rootSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2 , trip2 , act3 ),
							new Trip( act3 , trip3 , act4 ) ),
						true);
		final Subtour childSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act2 , trip2 , act3 ) ),
						true);
		childSubtour.parent = rootSubtour;
		rootSubtour.children.add(childSubtour);

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					rootSubtour,
					childSubtour));
	}

	private static Fixture createComplexSubtours(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg( "walk" );
		plan.addLeg( leg2 );
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "s" , createId(3,anchorAtFacilities) );
		plan.addActivity( act3 );

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg( "velo" );
		plan.addLeg( leg5 );
		trip3.add( leg5 );

		final Activity act4 = createActivityFromLocationId( anchorAtFacilities , fact , "t" , createId(1,anchorAtFacilities) );
		plan.addActivity( act4 );

		final List<PlanElement> trip4 = new ArrayList<PlanElement>();
		final Leg leg6 = fact.createLeg( "skateboard" );
		plan.addLeg( leg6 );
		trip4.add( leg6 );

		final Activity act5 = createActivityFromLocationId( anchorAtFacilities , fact , "aa" , createId(3,anchorAtFacilities) );
		plan.addActivity( act5 );

		final List<PlanElement> trip5 = new ArrayList<PlanElement>();
		final Leg leg7 = fact.createLeg( "skateboard" );
		plan.addLeg( leg7 );
		trip5.add( leg7 );

		final Activity act6 = createActivityFromLocationId( anchorAtFacilities , fact , "l" , createId(2,anchorAtFacilities) );
		plan.addActivity( act6 );

		final List<PlanElement> trip6 = new ArrayList<PlanElement>();
		final Leg leg8 = fact.createLeg( "skateboard" );
		plan.addLeg( leg8 );
		trip6.add( leg8 );

		final Activity act7 = createActivityFromLocationId( anchorAtFacilities , fact , "s" , createId(3,anchorAtFacilities) );
		plan.addActivity( act7 );

		final List<PlanElement> trip7 = new ArrayList<PlanElement>();
		final Leg leg9 = fact.createLeg( "velo" );
		plan.addLeg( leg9 );
		trip7.add( leg9 );

		final Activity act8 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act8 );


		final Subtour rootSubtour1 =
			new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2 , trip2 , act3 ),
							new Trip( act3 , trip3 , act4 ) ),
						true);
		final Subtour rootSubtour2 =
			new Subtour(
						Arrays.asList(
							new Trip( act4 , trip4 , act5 ),
							new Trip( act5 , trip5 , act6 ),
							new Trip( act6 , trip6 , act7 ),
							new Trip( act7 , trip7 , act8 )),
						true);

		final Subtour childSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act5 , trip5 , act6 ),
							new Trip( act6 , trip6 , act7 )),
						true);
		childSubtour.parent = rootSubtour2;
		rootSubtour2.children.add( childSubtour );

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					rootSubtour1,
					rootSubtour2,
					childSubtour));
	}

	private static Fixture createOpenPlan(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg( "walk" );
		plan.addLeg( leg2 );
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(2,anchorAtFacilities) );
		plan.addActivity( act3 );

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg( "velo" );
		plan.addLeg( leg5 );
		trip3.add( leg5 );

		final Activity act4 = createActivityFromLocationId( anchorAtFacilities , fact , "camping" , createId(3,anchorAtFacilities) );
		plan.addActivity( act4 );

		final Subtour rootSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2 , trip2 , act3 ),
							new Trip( act3 , trip3 , act4 ) ),
						false);
		final Subtour childSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act2 , trip2 , act3 ) ),
						true);
		childSubtour.parent = rootSubtour;
		rootSubtour.children.add( childSubtour );

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					rootSubtour,
					childSubtour));
	}

	private static Fixture createTwoChildren(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg( "walk" );
		plan.addLeg( leg2 );
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(2,anchorAtFacilities) );
		plan.addActivity( act3 );

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg( "velo" );
		plan.addLeg( leg5 );
		trip3.add( leg5 );

		final Activity act4 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(3,anchorAtFacilities) );
		plan.addActivity( act4 );

		final List<PlanElement> trip4 = new ArrayList<PlanElement>();
		final Leg leg6 = fact.createLeg( "bike" );
		plan.addLeg( leg6 );
		trip4.add( leg6 );

		final Activity act5 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(3,anchorAtFacilities) );
		plan.addActivity( act5 );

		final List<PlanElement> trip5 = new ArrayList<PlanElement>();
		final Leg leg7 = fact.createLeg( "flying_carpet" );
		plan.addLeg( leg7 );
		trip5.add( leg7 );

		final Activity act6 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act6 );

		final Subtour rootSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2 , trip2 , act3 ),
							new Trip( act3 , trip3 , act4 ),
							new Trip( act4 , trip4 , act5 ),
							new Trip( act5 , trip5 , act6 ) ),
						true);
		final Subtour childSubtour1 =
			new Subtour(
						Arrays.asList(
							new Trip( act2 , trip2 , act3 ) ),
						true);
		final Subtour childSubtour2 =
			new Subtour(
						Arrays.asList(
							new Trip( act4 , trip4 , act5 ) ),
						true);
		childSubtour1.parent = rootSubtour;
		childSubtour2.parent = rootSubtour;
		rootSubtour.children.add( childSubtour1 );
		rootSubtour.children.add( childSubtour2 );

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					rootSubtour,
					childSubtour1,
					childSubtour2));
	}

	private static Fixture createSingleTourComingFromSomewhereElse(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();
		final Plan plan = fact.createPlan();
		Activity somewhereElse = createActivityFromLocationId(anchorAtFacilities, fact, "somewhere else", createId(1,anchorAtFacilities));
		plan.addActivity(somewhereElse);
		Leg leg1 = fact.createLeg("some mode");
		plan.addLeg(leg1);
		Activity home1 = createActivityFromLocationId(anchorAtFacilities, fact, "home", createId(2,anchorAtFacilities));
		plan.addActivity(home1);
		Leg leg2 = fact.createLeg("some mode");
		plan.addLeg(leg2);
		Activity work = createActivityFromLocationId(anchorAtFacilities, fact, "work", createId(3,anchorAtFacilities));
		plan.addActivity(work);
		Leg leg3 = fact.createLeg("some mode");
		plan.addLeg(leg3);
		Activity home2 = createActivityFromLocationId(anchorAtFacilities, fact, "home", createId(2,anchorAtFacilities));
		plan.addActivity(home2);
		Subtour subtour1 = new Subtour(
				Arrays.asList(
					new Trip(somewhereElse, Collections.<PlanElement>singletonList(leg1), home1),
					new Trip(home1, Collections.<PlanElement>singletonList(leg2), work),
					new Trip(work, Collections.<PlanElement>singletonList(leg3), home2)),
				false);
		Subtour subtour2 = new Subtour(Arrays.asList(
				new Trip(home1, Collections.<PlanElement>singletonList(leg2), work),
				new Trip(work, Collections.<PlanElement>singletonList(leg3), home2)), true);
		subtour2.parent = subtour1;
		subtour1.children.add(subtour2);
		return new Fixture(anchorAtFacilities, plan, Arrays.asList(
				subtour1,
				subtour2));
	}

	private static Fixture createSingleTourGoingToSomewhereElse(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();
		final Plan plan = fact.createPlan();

		Activity home1 = createActivityFromLocationId(anchorAtFacilities, fact, "home", createId(2,anchorAtFacilities));
		plan.addActivity(home1);
		Leg leg2 = fact.createLeg("some mode");
		plan.addLeg(leg2);

		Activity work = createActivityFromLocationId(anchorAtFacilities, fact, "work", createId(3,anchorAtFacilities));
		plan.addActivity(work);
		Leg leg3 = fact.createLeg("some mode");
		plan.addLeg(leg3);

		Activity home2 = createActivityFromLocationId(anchorAtFacilities, fact, "home", createId(2,anchorAtFacilities));
		plan.addActivity(home2);
		Leg leg1 = fact.createLeg("some mode");
		plan.addLeg(leg1);

		Activity somewhereElse = createActivityFromLocationId(anchorAtFacilities, fact, "somewhere else", createId(1,anchorAtFacilities));
		plan.addActivity(somewhereElse);

		Subtour subtour1 = new Subtour(
				Arrays.asList(
					new Trip(home1, Collections.<PlanElement>singletonList(leg2), work),
					new Trip(work, Collections.<PlanElement>singletonList(leg3), home2),
					new Trip(home2, Collections.<PlanElement>singletonList(leg1), somewhereElse)),
				false);
		Subtour subtour2 = new Subtour(Arrays.asList(
				new Trip(home1, Collections.<PlanElement>singletonList(leg2), work),
				new Trip(work, Collections.<PlanElement>singletonList(leg3), home2)), true);
		subtour2.parent = subtour1;
		subtour1.children.add(subtour2);
		return new Fixture(anchorAtFacilities, plan, Arrays.asList(
				subtour1,
				subtour2));
	}

	private static Fixture createPlanWithLoops(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final List<Trip> trips = new ArrayList<Trip>();
		final List<Subtour> childrenSubtours = new ArrayList<Subtour>();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId(anchorAtFacilities, fact, "h", createId(1,anchorAtFacilities));
		plan.addActivity( act1 );

		final Leg leg1 = fact.createLeg("walk");
		plan.addLeg(leg1);

		final Activity act2= createActivityFromLocationId(anchorAtFacilities, fact, "w", createId(2,anchorAtFacilities));
		plan.addActivity( act2 );

		trips.add(new Trip(act1, Collections.<PlanElement>singletonList(leg1), act2));

		Activity lastAct = act2;
		for (int i=0; i < 10; i++) {
			final List<PlanElement> trip = new ArrayList<PlanElement>();
			final Leg leg2 = fact.createLeg( "walk" );
			plan.addLeg( leg2 );
			trip.add( leg2 );
			final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
			plan.addActivity( stage );
			trip.add( stage );
			final Leg leg3 = fact.createLeg( "swim" );
			plan.addLeg( leg3 );
			trip.add( leg3 );
			final Leg leg4 = fact.createLeg( "walk" );
			plan.addLeg( leg4 );
			trip.add( leg4 );

			final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
			plan.addActivity( act3 );

			final Trip tripObject = new Trip( lastAct , trip , act3 );
			trips.add( tripObject );
			childrenSubtours.add(
					new Subtour(
						Collections.singletonList( tripObject ),
						true) );
			lastAct = act3;
		}

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg("velo");
		plan.addLeg( leg5 );
		trip3.add(leg5);

		final Activity act4 = createActivityFromLocationId(anchorAtFacilities, fact, "h", createId(1,anchorAtFacilities));
		plan.addActivity( act4 );

		trips.add(new Trip(lastAct, trip3, act4));

		final Subtour rootSubtour =
			new Subtour(
					trips,
					true);

		for (Subtour childSubtour : childrenSubtours) {
			childSubtour.parent = rootSubtour;
			rootSubtour.children.add( childSubtour );
		}

		childrenSubtours.add(rootSubtour);

		return new Fixture(
				anchorAtFacilities,
				plan,
				childrenSubtours);
	}

	private static Fixture createInconsistentTrips(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final Activity act2b = createActivityFromLocationId(anchorAtFacilities, fact, "w", createId(1,anchorAtFacilities));
		plan.addActivity(act2b);

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg("walk");
		plan.addLeg(leg2);
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg("walk");
		plan.addLeg(leg4);
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(2,anchorAtFacilities) );
		plan.addActivity(act3);

		return new Fixture(
				anchorAtFacilities,
				plan,
				null);
	}

	private static Fixture createTwoIndependentTours(final boolean anchorAtFacilities) {
		final PopulationFactory fact = createPopulationFactory();

		final Plan plan = fact.createPlan();
		final Activity act1 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act1 );

		final List<PlanElement> trip1 = new ArrayList<PlanElement>();
		final Leg leg1 = fact.createLeg( "velo" );
		plan.addLeg( leg1 );
		trip1.add( leg1 );

		final Activity act2 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act2 );

		final List<PlanElement> trip2 = new ArrayList<PlanElement>();
		final Leg leg2 = fact.createLeg( "walk" );
		plan.addLeg( leg2 );
		trip2.add( leg2 );
		final Activity stage = createActivityFromLocationId( anchorAtFacilities , fact , STAGE , createId(2,anchorAtFacilities) );
		plan.addActivity( stage );
		trip2.add( stage );
		final Leg leg3 = fact.createLeg( "swim" );
		plan.addLeg( leg3 );
		trip2.add( leg3 );
		final Leg leg4 = fact.createLeg( "walk" );
		plan.addLeg( leg4 );
		trip2.add( leg4 );

		final Activity act3 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act3 );

		final List<PlanElement> trip3 = new ArrayList<PlanElement>();
		final Leg leg5 = fact.createLeg( "velo" );
		plan.addLeg( leg5 );
		trip3.add( leg5 );

		final Activity act4 = createActivityFromLocationId( anchorAtFacilities , fact , "w" , createId(2,anchorAtFacilities) );
		plan.addActivity( act4 );

		final List<PlanElement> trip4 = new ArrayList<PlanElement>();
		final Leg leg6 = fact.createLeg( "velo" );
		plan.addLeg( leg6 );
		trip4.add( leg6 );

		final Activity act5 = createActivityFromLocationId( anchorAtFacilities , fact , "h" , createId(1,anchorAtFacilities) );
		plan.addActivity( act5 );

		final Subtour firstSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act1 , trip1 , act2 ),
							new Trip( act2 , trip2 , act3 ) ),
						true);
		final Subtour secondSubtour =
			new Subtour(
						Arrays.asList(
							new Trip( act3 , trip3 , act4 ),
							new Trip( act4 , trip4 , act5 ) ),
						true);

		return new Fixture(
				anchorAtFacilities,
				plan,
				Arrays.asList(
					firstSubtour,
					secondSubtour));
	}

	private static Collection<Fixture> allFixtures(final boolean anchorAtFacilities) {
		return Arrays.asList(
				createMonoSubtourFixture(anchorAtFacilities),
				createTwoNestedSubtours(anchorAtFacilities),
				createTwoChildren(anchorAtFacilities),
				createComplexSubtours(anchorAtFacilities),
				createOpenPlan(anchorAtFacilities),
				createPlanWithLoops(anchorAtFacilities),
				createTwoIndependentTours(anchorAtFacilities));
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testOneSubtour(boolean useFacilitiesAsAnchorPoint) {
		performTest( createMonoSubtourFixture( useFacilitiesAsAnchorPoint ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTwoNestedSubtours(boolean useFacilitiesAsAnchorPoint) {
		performTest( createTwoNestedSubtours(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTwoChildren(boolean useFacilitiesAsAnchorPoint) {
		performTest( createTwoChildren(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testComplexSubtours(boolean useFacilitiesAsAnchorPoint) {
		performTest( createComplexSubtours(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testOpenPlan(boolean useFacilitiesAsAnchorPoint) {
		performTest( createOpenPlan(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testLoops(boolean useFacilitiesAsAnchorPoint) {
		performTest( createPlanWithLoops(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTwoIndependentTours(boolean useFacilitiesAsAnchorPoint) {
		performTest( createTwoIndependentTours(useFacilitiesAsAnchorPoint) );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTripFromSomewhereElse(boolean useFacilitiesAsAnchorPoint) { performTest( createSingleTourComingFromSomewhereElse(useFacilitiesAsAnchorPoint));}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTripToSomewhereElse(boolean useFacilitiesAsAnchorPoint) { performTest( createSingleTourGoingToSomewhereElse(useFacilitiesAsAnchorPoint));}

	private static void performTest(final Fixture fixture) {
		final Collection<Subtour> subtours =
			TripStructureUtils.getSubtours( fixture.plan );

		assertEquals(
				fixture.expectedSubtours.size(),
				subtours.size(),
				"[anchorAtFacilities="+fixture.useFacilitiesAsAnchorPoint+"] "+
				"unexpected number of subtours in "+subtours );

		assertEquals(
				new HashSet<Subtour>( fixture.expectedSubtours ),
				new HashSet<Subtour>( subtours ),
				"[anchorAtFacilities="+fixture.useFacilitiesAsAnchorPoint+"] "+
				"uncompatible subtours" );
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testInconsistentPlan(boolean useFacilitiesAsAnchorPoint) throws Exception {
		final Fixture fixture = createInconsistentTrips( useFacilitiesAsAnchorPoint );
		boolean hadException = false;
		try {
			TripStructureUtils.getSubtours( fixture.plan );
		}
		catch (RuntimeException e) {
			hadException = true;
		}

		assertTrue(
				hadException,
				"[anchorAtFacilities="+fixture.useFacilitiesAsAnchorPoint+"] "+
				"no exception was thrown!");
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testGetTripsWithoutSubSubtours(boolean useFacilitiesAsAnchorPoint) throws Exception {
		for (Fixture f : allFixtures( useFacilitiesAsAnchorPoint )) {
			final int nTrips = TripStructureUtils.getTrips( f.plan ).size();
			final Collection<Subtour> subtours =
				TripStructureUtils.getSubtours(	f.plan );
			int countTrips = 0;

			for (Subtour s : subtours) {
				countTrips += s.getTripsWithoutSubSubtours().size();
			}

			assertEquals(
					countTrips,
					nTrips,
					"[anchorAtFacilities="+f.useFacilitiesAsAnchorPoint+"] "+
					"unexpected total number of trips in subtours without subsubtours");
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testFatherhood(boolean useFacilitiesAsAnchorPoint) throws Exception {
		for (Fixture f : allFixtures( useFacilitiesAsAnchorPoint )) {
			final Collection<Subtour> subtours = TripStructureUtils.getSubtours( f.plan );

			for (Subtour s : subtours) {
				for ( Subtour child : s.getChildren() ) {
					assertEquals(
							child.getParent(),
							s,
							"[anchorAtFacilities="+f.useFacilitiesAsAnchorPoint+"] "+
							"wrong father!");
				}

				if ( s.getParent() != null ) {
					assertTrue(
							s.getParent().getChildren().contains( s ),
							"[anchorAtFacilities="+f.useFacilitiesAsAnchorPoint+"] "+
							"father does not have subtour has a child");
				}
			}
		}
	}

	private static PopulationFactory createPopulationFactory() {
        return ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
    }
}

