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
import org.matsim.api.core.v01.population.Leg;
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
 * Tests specific to the 'probability-for-random-single-trip-mode' parameter.
 * 
 * @author ikaddoura based on thibautd
 */
@RunWith(Parameterized.class)
public class ChooseRandomLegModeForSubtourDiffModesTest {
	
	private static final String[] MODES = new String[]{TransportMode.pt, TransportMode.car, TransportMode.walk};
	private static final String[] CHAIN_BASED_MODES = new String[]{TransportMode.car};

	private final StageActivityTypes stagesActivities = new StageActivityTypesImpl();
	
	private final double probaForRandomSingleTripMode;
	
	// /////////////////////////////////////////////////////////////////////////
	// Fixtures
	// /////////////////////////////////////////////////////////////////////////
	private static interface Fixture {
		Plan createNewPlanInstance();
	}

	private static Fixture createOneTourFixture() {
		return new Fixture() {
			@Override
			public Plan createNewPlanInstance() {
				final PopulationFactory fact = createPopulationFactory();

				final Id<Link> id1 = Id.create( 1, Link.class );
				final Id<Link> id3 = Id.create( 3, Link.class );

				final Plan plan = fact.createPlan();
				
				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				plan.addLeg( fact.createLeg( TransportMode.walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "w" , id3 ) );

				plan.addLeg( fact.createLeg( TransportMode.walk ) );

				plan.addActivity( fact.createActivityFromLinkId( "h" , id1 ) );

				return plan;
			}
		};
	}
	
	private static Collection<Fixture> createFixtures() {
		return Arrays.asList(
				createOneTourFixture());
	}

	private static PopulationFactory createPopulationFactory() {
        return ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
    }
	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Parameterized.Parameters(name = "{index}: probaForChooseRandomSingleTripMode == {0}")
	public static Collection<Object> createTests() {
		return Arrays.asList(0., 1.0);
	}
	public ChooseRandomLegModeForSubtourDiffModesTest( double proba ) {
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
				final int initNTrips = TripStructureUtils.getTrips( plan ).size();
				final Collection<Subtour> initSubtours = TripStructureUtils.getSubtours( plan );
				testee.run( plan );

				final List<Trip> newTrips = TripStructureUtils.getTrips( plan );
				
				Assert.assertEquals(
						"number of trips changed with mode mutation!?",
						initNTrips,
						newTrips.size());

				final Collection<Subtour> newSubtours = TripStructureUtils.getSubtours( plan );

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
				
				
				for ( Subtour subtour : newSubtours ) {
					if (subtour.getChildren().isEmpty()) {
						checkSubtour(subtour);
					} else {
						throw new RuntimeException("This test is not (yet) made for subtours with children.");
					}
				}
			}
		}
	}
	
	private void checkSubtour(Subtour subtour) {
		boolean atLeastOneSubtourWithDifferentNonChainBasedModes = false;
		boolean atLeastOneSubtourWithDifferentChainBasedModes = false;

		if (subtour.getChildren().isEmpty()) {
			String modePreviousTripSameSubtour = null;

			for (Trip trip: subtour.getTrips()) {
				
				for (Leg leg : trip.getLegsOnly()) {
										
					if (modePreviousTripSameSubtour == null) {
						// first trip during this subtour
					} else {
						if (leg.getMode().equals(modePreviousTripSameSubtour)) {
							// same mode 
							
						} else {
							// different modes, should only occur for non-chain-based modes
														
							if (isChainBasedMode(leg.getMode()) || isChainBasedMode(modePreviousTripSameSubtour)) {
								// one of the two different modes is a chain-based mode which shouldn't just fall from the sky
								atLeastOneSubtourWithDifferentChainBasedModes = true;
								
							} else {
								// not a chain-based mode
								atLeastOneSubtourWithDifferentNonChainBasedModes = true;
							}
						}
					}
					modePreviousTripSameSubtour = leg.getMode();
				}
			}
		}
		
		if (atLeastOneSubtourWithDifferentChainBasedModes) {
			Assert.fail("Two different modes during one subtour where one of the two different modes is a chain-based mode.");
		}
						
		if (this.probaForRandomSingleTripMode > 0.) {
			if (atLeastOneSubtourWithDifferentNonChainBasedModes == false) {
				Assert.fail("There is not a single subtour with different non-chain-based modes even though the probability for random single trip mode is " + this.probaForRandomSingleTripMode);
			}
		} else {
			if (atLeastOneSubtourWithDifferentNonChainBasedModes == true) {
				Assert.fail("There is at least one subtour with different non-chain-based modes even though the probability for random single trip mode is " + this.probaForRandomSingleTripMode);
			}
		}
	}

	private boolean isChainBasedMode(String mode) {
		for (String chainbasedMode : CHAIN_BASED_MODES) {
			if (chainbasedMode.equals(mode)) {
				return true;
			}
		}
		return false;
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

