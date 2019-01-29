/* *********************************************************************** *
 * project: org.matsim.*
 * ChooseRandomLegModeForSubtourComplexTripsTest.java
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
package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculatorImpl;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.PtConstants;

/**
 * Tests specific to the "complex trip" handling (tests in ChooseRandomLegModeForSubtourTest
 * were designed for mono-leg trips, and I tried to keep them as close to the original
 * as possible.
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class ChooseRandomLegModeForSubtourComplexTripsTest {
	// transit_walk is not here but is in the fixtures: thus, pt trips are
	// identified as "known mode" only if trip-level mode detection is done
	// properly
	private static final String[] MODES = new String[]{TransportMode.pt, TransportMode.car};
	private static final String[] CHAIN_BASED_MODES = new String[]{TransportMode.car};

	private static final String STAGE = PtConstants.TRANSIT_ACTIVITY_TYPE;
	private final StageActivityTypes stagesActivities = new StageActivityTypesImpl( STAGE );
	private final double probaForRandomSingleTripMode;
	
	// /////////////////////////////////////////////////////////////////////////
	// Fixtures
	// /////////////////////////////////////////////////////////////////////////
	private static interface Fixture {
		Plan createNewPlanInstance();
	}

	private static Fixture createOnePtTourFixture() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id2 = Id.create( 2, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				return plan;
			}
		};
	}

	private static Fixture createTwoPtToursFixture() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id2 = Id.create( 2, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				for (int i =0; i < 2; i++) {
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
					plan.addLeg( fact.createLeg( TransportMode.pt ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

					plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
					plan.addLeg( fact.createLeg( TransportMode.pt ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
					plan.addLeg( fact.createLeg( TransportMode.pt ) );
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
					plan.addLeg( fact.createLeg( TransportMode.pt ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
					plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
					plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

					plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );
				}

				return plan;
			}
		};
	}

	private static Fixture createPtWithSubsubtoursFixture() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id2 = Id.create( 2, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );
				final Id<Link> id4 = Id.create( 4, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "l" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.pt ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "l" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				return plan;
			}
		};
	}

	private static Fixture createCarTourWithPtSubtours() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id2 = Id.create( 2, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );
				final Id<Link> id4 = Id.create( 4, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				plan.addLeg( fact.createLeg( TransportMode.car ) );
				plan.addLeg( fact.createLeg( TransportMode.car ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.car ) );

				plan.addActivity( fact.createActivityFromLinkId( "l" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.car ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.pt ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.car ) );

				plan.addActivity( fact.createActivityFromLinkId( "l" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.car ) );

				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				return plan;
			}
		};
	}

	private static Fixture createPtWithSubsubtoursAndConsecutiveActivitesFixture() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id2 = Id.create( 2, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );
				final Id<Link> id4 = Id.create( 4, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "sleep" , id1 ) );
				plan.addActivity( fact.createActivityFromLinkId( "shower" , id1 ) );
				plan.addActivity( fact.createActivityFromLinkId( "breakfast" , id1 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "check_e-mails" , id3 ) );
				plan.addActivity( fact.createActivityFromLinkId( "fix_bugs" , id3 ) );
				plan.addActivity( fact.createActivityFromLinkId( "write_paper" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id3 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "buy_food" , id4 ) );
				plan.addActivity( fact.createActivityFromLinkId( "buy_books" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );
				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "run" , id2 ) );
				plan.addActivity( fact.createActivityFromLinkId( "stretch" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "s" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "read" , id3 ) );
				plan.addActivity( fact.createActivityFromLinkId( "meeting" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.pt ) );

				plan.addActivity( fact.createActivityFromLinkId( "buy_bread" , id4 ) );
				plan.addActivity( fact.createActivityFromLinkId( "buy_milk" , id4 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "swim" , id2 ) );
				plan.addActivity( fact.createActivityFromLinkId( "shower" , id2 ) );

				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.pt ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id1 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );
				plan.addActivity(  fact.createActivityFromLinkId( STAGE , id2 ) );
				plan.addLeg( fact.createLeg( TransportMode.transit_walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "have_dinner" , id1 ) );
				plan.addActivity( fact.createActivityFromLinkId( "sleep" , id1 ) );

				return plan;
			}
		};
	}

	private static Collection<Fixture> createFixtures() {
		return Arrays.asList(
				createOnePtTourFixture(),
				createTwoPtToursFixture(),
				createPtWithSubsubtoursFixture(),
				createCarTourWithPtSubtours(),
				createPtWithSubsubtoursAndConsecutiveActivitesFixture());
	}

	private static PopulationFactory createPopulationFactory() {
        return ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
    }
	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Parameterized.Parameters(name = "{index}: probaForChooseRandomSingleTripMode == {0}")
	public static Collection<Object> createTests() {
		return Arrays.asList(0., 0.5);
	}
	public ChooseRandomLegModeForSubtourComplexTripsTest( double proba ) {
		this.probaForRandomSingleTripMode = proba ;
	}
	
	
	@Test
	public void testMutatedTrips() {
		final ChooseRandomLegModeForSubtour testee =
			new ChooseRandomLegModeForSubtour(
					stagesActivities,
					new MainModeIdentifierImpl(),
					new PermissibleModesCalculatorImpl( MODES , false ),
					MODES,
					CHAIN_BASED_MODES,
					new Random( 20130225 ), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes,
					probaForRandomSingleTripMode);

		for ( Fixture f : createFixtures() ) {
			for (int i = 0; i < 5; i++) {
				final Plan plan = f.createNewPlanInstance();
				final int initNTrips = TripStructureUtils.getTrips( plan , stagesActivities ).size();
				final Collection<Subtour> initSubtours = TripStructureUtils.getSubtours( plan , stagesActivities );
				testee.run( plan );

				final List<Trip> newTrips = TripStructureUtils.getTrips( plan , stagesActivities );
				
				Assert.assertEquals(
						"number of trips changed with mode mutation!?",
						initNTrips,
						newTrips.size());

				final Collection<Subtour> newSubtours = TripStructureUtils.getSubtours( plan , stagesActivities );

				Assert.assertEquals(
						"number of subtours changed with mode mutation!?",
						initSubtours.size(),
						newSubtours.size());

				final List<Subtour> mutated = new ArrayList<Subtour>();
				for ( Subtour newSubtour : newSubtours ) {
					if ( !contains( initSubtours , newSubtour ) ) {
						mutated.add( newSubtour );
					}
				}

				Assert.assertFalse(
						"no mutated subtours",
						mutated.isEmpty() );

				int nMutatedWithoutMutatedFather = 0;
				for ( Subtour s : mutated ) {
					if ( !mutated.contains( s.getParent() ) ) {
						nMutatedWithoutMutatedFather++;
					}

					for ( Trip t : s.getTrips() ) {
						Assert.assertEquals(
								"unexpected mutated trip length",
								1,
								t.getTripElements().size());
					}
				}

				Assert.assertEquals(
						"unexpected number of roots in mutated subtours",
						1,
						nMutatedWithoutMutatedFather);
			}
		}
	}

	private static boolean contains(
			final Collection<Subtour> subtours,
			final Subtour subtour) {
		for ( Subtour s : subtours ) {
			if ( areEqual( s , subtour ) ) return true;
		}

		return false;
	}

	private static boolean areEqual(
			final Subtour s1,
			final Subtour s2) {
		return s1.getTripsWithoutSubSubtours().equals(
				s2.getTripsWithoutSubSubtours() );
	}
}

