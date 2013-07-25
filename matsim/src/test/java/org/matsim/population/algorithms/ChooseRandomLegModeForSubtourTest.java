/* *********************************************************************** *
 * project: org.matsim.*
 * ChooseRandomLegMode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.BasicLocations;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser, michaz
 */
public class ChooseRandomLegModeForSubtourTest {

	private static class AllowTheseModesForEveryone implements
	PermissibleModesCalculator {

		private List<String> availableModes;

		public AllowTheseModesForEveryone(String[] availableModes) {
			this.availableModes = Arrays.asList(availableModes);
		}

		@Override
		public Collection<String> getPermissibleModes(Plan plan) {
			return availableModes; 
		}

	}
	
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	public static enum TripStructureAnalysisLayerOption {facility,link}

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
	private static final String[] CHAIN_BASED_MODES = new String[] { TransportMode.car }; 
	private static final Collection<String> activityChainStrings = Arrays.asList(
			"1 2 1",
			"1 2 20 1",
			"1 2 1 2 1",
			"1 2 1 3 1",
			"1 2 2 1",
			"1 2 2 2 2 2 2 2 1",
			"1 2 3 2 1",
			"1 2 3 4 3 2 1",
			"1 2 14 2 14 2 1",
			"1 2 14 14 2 14 2 1",
			"1 2 3 4 3 2 5 4 5 1",
			"1 2 3 2 3 2 1 2 1",
			"1 1 1 1 1 2 1",
			"1 2 1 1",
			"1 2 2 3 2 2 2 1 4 1",
	"1 2 3 4 3 1");

	@Test
	public void testHandleEmptyPlan() {
		String[] modes = new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk};
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() , new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	@Test
	public void testHandlePlanWithoutLeg() {
		String[] modes = new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk};
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}


	@Test
	public void testSubTourMutationNetworkBased() {
		Config config = utils.loadConfig(CONFIGFILE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		TripStructureAnalysisLayerOption tripStructureAnalysisLayer = TripStructureAnalysisLayerOption.link;
		this.testSubTourMutationToCar((NetworkImpl) network, tripStructureAnalysisLayer);
		this.testSubTourMutationToPt((NetworkImpl) network, tripStructureAnalysisLayer);
		this.testUnknownModeDoesntMutate((NetworkImpl) network, tripStructureAnalysisLayer);
	}

	@Test
	public void testSubTourMutationFacilitiesBased() {
		Config config = utils.loadConfig(CONFIGFILE);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		TripStructureAnalysisLayerOption tripStructureAnalysisLayer = TripStructureAnalysisLayerOption.facility;
		this.testSubTourMutationToCar(facilities, tripStructureAnalysisLayer);
		this.testSubTourMutationToPt(facilities, tripStructureAnalysisLayer);
		this.testUnknownModeDoesntMutate(facilities, tripStructureAnalysisLayer);
	}

	@Test
	public void testCarDoesntTeleportFromHome() {
		Config config = utils.loadConfig(CONFIGFILE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		testCarDoesntTeleport((NetworkImpl) network, TripStructureAnalysisLayerOption.link, TransportMode.car, TransportMode.pt);
		testCarDoesntTeleport((NetworkImpl) network, TripStructureAnalysisLayerOption.link, TransportMode.pt, TransportMode.car);
	}

	@Test
	public void testSingleTripSubtourHandling() {
		String[] modes = new String[] {"car", "pt", "walk"};
		
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, new Random(15102011));
		testee.setAnchorSubtoursAtFacilitiesInsteadOfLinks( false );
		Person person = new PersonImpl(new IdImpl("1000"));
		Plan plan = new PlanImpl();
		person.addPlan(plan);
		Id linkId = new IdImpl(1);
		Activity home1 = new ActivityImpl("home", linkId);
		Leg leg = new LegImpl("car");
		Activity home2 = new ActivityImpl("home", linkId);
		plan.addActivity(home1);
		plan.addLeg(leg);
		plan.addActivity(home2);
		
		{ // test default
			boolean hasCar = false;
			boolean hasPt = false;
			boolean hasWalk = false;
			
			for (int i = 0; i < 50; i++) {
				testee.run(plan);

				assertEquals(
						"unexpected plan size",
						3,
						plan.getPlanElements().size());

				final Leg newLeg = (Leg) plan.getPlanElements().get( 1 );

				if (newLeg.getMode().equals("car")) {
					hasCar = true;
				}
				if (newLeg.getMode().equals("pt")) {
					hasPt = true;
				}
				if (newLeg.getMode().equals("walk")) {
					hasWalk = true;
				}
			}
			assertTrue(
					"expected subtours with car, pt and walk, got"+
					(hasCar ? " car" : " NO car")+","+
					(hasPt ? " pt" : " NO pt")+","+
					(hasWalk ? " walk" : " NO walk"),
					hasCar && hasPt && hasWalk);
		}
		{ // test with special single trip subtour settings
			testee.setSingleTripSubtourModes(new String[] {"pt", "walk"});
			boolean hasCar = false;
			boolean hasPt = false;
			boolean hasWalk = false;
			
			for (int i = 0; i < 50; i++) {
				testee.run(plan);

				assertEquals(
						"unexpected plan size",
						3,
						plan.getPlanElements().size());

				final Leg newLeg = (Leg) plan.getPlanElements().get( 1 );

				if (newLeg.getMode().equals("car")) {
					hasCar = true;
				}
				if (newLeg.getMode().equals("pt")) {
					hasPt = true;
				}
				if (newLeg.getMode().equals("walk")) {
					hasWalk = true;
				}
			}
			assertTrue(
					"expected subtours with NO car, pt and walk, got"+
					(hasCar ? " car" : " NO car")+","+
					(hasPt ? " pt" : " NO pt")+","+
					(hasWalk ? " walk" : " NO walk"),
					!hasCar && hasPt && hasWalk);
		}

	}


	private void testSubTourMutationToCar(BasicLocations layer, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		String expectedMode = TransportMode.car;
		String originalMode = TransportMode.pt;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setAnchorSubtoursAtFacilitiesInsteadOfLinks(
				tripStructureAnalysisLayer.equals( TripStructureAnalysisLayerOption.facility ));
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, tripStructureAnalysisLayer, originalMode);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyFrom(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, tripStructureAnalysisLayer);
		}

	}
	
	private void testUnknownModeDoesntMutate(BasicLocations layer, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		String originalMode = TransportMode.walk;
		String[] modes = new String[] {TransportMode.car, TransportMode.pt};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setAnchorSubtoursAtFacilitiesInsteadOfLinks(
				tripStructureAnalysisLayer.equals( TripStructureAnalysisLayerOption.facility ));
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, tripStructureAnalysisLayer, originalMode);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyFrom(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
		}

	}

	private void testSubTourMutationToPt(BasicLocations layer, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		String expectedMode = TransportMode.pt;
		String originalMode = TransportMode.car;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setAnchorSubtoursAtFacilitiesInsteadOfLinks(
				tripStructureAnalysisLayer.equals( TripStructureAnalysisLayerOption.facility ));
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, tripStructureAnalysisLayer, originalMode);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyFrom(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, tripStructureAnalysisLayer);
		}

	}

	private void testCarDoesntTeleport(BasicLocations layer, TripStructureAnalysisLayerOption link, String originalMode, String otherMode) {
		String[] modes = new String[] {originalMode, otherMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setAnchorSubtoursAtFacilitiesInsteadOfLinks(
				link.equals( TripStructureAnalysisLayerOption.facility ));

		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, link, originalMode);
			testee.run(plan);
			Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
			Activity firstActivity = (Activity) peIterator.next();
			Id firstLocation = firstActivity.getLinkId();
			Id carLocation = firstLocation;
			Id currentLocation = firstLocation;
			int legCount = 0;
			while (peIterator.hasNext()) {
				legCount++;
				Leg nextLeg = (Leg) peIterator.next();
				Activity nextActivity = (Activity) peIterator.next();
				Id nextLocation = nextActivity.getLinkId();
				if (nextLeg.getMode().equals(TransportMode.car)) {
					assertEquals(
							"wrong car location at leg "+legCount+" in "+plan.getPlanElements(),
							currentLocation,
							carLocation);
					carLocation = nextLocation;
				}
				currentLocation = nextLocation;
			}
			assertEquals(
					"wrong car location at the end of "+plan.getPlanElements(),
					firstLocation,
					carLocation);
		}
	}

	private static void assertSubTourMutated(
			final Plan plan,
			final Plan originalPlan,
			final String expectedMode,
			final TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		final Collection<Subtour> originalSubtours =
			TripStructureUtils.getSubtours(
						originalPlan,
						EmptyStageActivityTypes.INSTANCE,
						tripStructureAnalysisLayer.equals(
							TripStructureAnalysisLayerOption.facility ));
		final Collection<Subtour> mutatedSubtours =
			TripStructureUtils.getSubtours(
						plan,
						EmptyStageActivityTypes.INSTANCE,
						tripStructureAnalysisLayer.equals(
							TripStructureAnalysisLayerOption.facility ));

		assertEquals(
				"number of subtour changed",
				originalSubtours.size(),
				mutatedSubtours.size());

		final List<Subtour> mutateds = new ArrayList<Subtour>();
		for (Subtour mutated : mutatedSubtours) {
			boolean isFirst = true;
			boolean containsMutatedMode = false;

			for (Trip trip : mutated.getTripsWithoutSubSubtours()) {
				if ( expectedMode.equals( trip.getLegsOnly().get( 0 ).getMode() ) ) {
					assertTrue(
							"inconsistent mode chain",
							isFirst || containsMutatedMode );
					containsMutatedMode = true;
				}
				else {
					assertFalse(
							"inconsistent mode chain",
							containsMutatedMode );
				}
				isFirst = false;
			}

			if ( containsMutatedMode ) mutateds.add( mutated );
		}

		assertFalse(
				"no mutated subtour",
				mutateds.isEmpty() );

		int nMutatedWithoutMutatedFather = 0;
		for ( Subtour s : mutateds ) {
			if ( !mutateds.contains( s.getParent() ) ) {
				nMutatedWithoutMutatedFather++;
			}
		}

		assertEquals(
				"unexpected number of roots in mutated subtours",
				1,
				nMutatedWithoutMutatedFather);
	}

	private static PlanImpl createPlan(BasicLocations layer, String facString,
			TripStructureAnalysisLayerOption linksOrFacilities, String mode) {
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		PlanImpl plan;
		if (linksOrFacilities.equals(TripStructureAnalysisLayerOption.facility)) {
			plan = TestsUtil.createPlanFromFacilities((ActivityFacilitiesImpl) layer, person, mode, facString);
		} else if (linksOrFacilities.equals(TripStructureAnalysisLayerOption.link)) {
			plan = TestsUtil.createPlanFromLinks((NetworkImpl) layer, person, mode, facString);
		} else {
			throw new RuntimeException("Unknown option.");
		}
		return plan;
	}

}
