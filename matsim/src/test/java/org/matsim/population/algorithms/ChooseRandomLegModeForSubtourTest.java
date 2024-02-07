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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.ChooseRandomLegModeForSubtour;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Subtour;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
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

	@RegisterExtension
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

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testHandleEmptyPlan(double probaForRandomSingleTripMode) {
		String[] modes = new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk};
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() , new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(),
				SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Plan plan = PopulationUtils.createPlan(null);
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testHandlePlanWithoutLeg(double probaForRandomSingleTripMode) {
		String[] modes = new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk};
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(),
				SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Plan plan = PopulationUtils.createPlan(null);
		PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord((double) 0, (double) 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}


	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testSubTourMutationNetworkBased(double probaForRandomSingleTripMode) {
		Config config = utils.loadConfig(CONFIGFILE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).parse(config.network().getInputFileURL(config.getContext()));
		this.testSubTourMutationToCar(network, probaForRandomSingleTripMode);
		this.testSubTourMutationToPt(network, probaForRandomSingleTripMode);
		this.testUnknownModeDoesntMutate(network, probaForRandomSingleTripMode);
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testSubTourMutationFacilitiesBased(double probaForRandomSingleTripMode) {
		Config config = utils.loadConfig(CONFIGFILE);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		ActivityFacilitiesImpl facilities = (ActivityFacilitiesImpl) scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).parse(config.facilities().getInputFileURL(config.getContext()));
		this.testSubTourMutationToCar(facilities, probaForRandomSingleTripMode);
		this.testSubTourMutationToPt(facilities, probaForRandomSingleTripMode);
		this.testUnknownModeDoesntMutate(facilities, probaForRandomSingleTripMode);
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testCarDoesntTeleportFromHome(double probaForRandomSingleTripMode) {
		Config config = utils.loadConfig(CONFIGFILE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).parse(config.network().getInputFileURL(config.getContext()));
		testCarDoesntTeleport(network, TransportMode.car, TransportMode.pt, probaForRandomSingleTripMode);
		testCarDoesntTeleport(network, TransportMode.pt, TransportMode.car, probaForRandomSingleTripMode);
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testSingleTripSubtourHandling(double probaForRandomSingleTripMode) {
		String[] modes = new String[] {"car", "pt", "walk"};

		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, new Random(15102011), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		Plan plan = PopulationUtils.createPlan();
		person.addPlan(plan);
		Id<Link> linkId = Id.create(1, Link.class);
		Activity home1 = PopulationUtils.createActivityFromLinkId("home", linkId);
		Leg leg = PopulationUtils.createLeg("car");
		Activity home2 = PopulationUtils.createActivityFromLinkId("home", linkId);
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
						3,
						plan.getPlanElements().size(),
						"unexpected plan size");

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
					hasCar && hasPt && hasWalk,
					"expected subtours with car, pt and walk, got"+
					(hasCar ? " car" : " NO car")+","+
					(hasPt ? " pt" : " NO pt")+","+
					(hasWalk ? " walk" : " NO walk"));
		}
		{ // test with special single trip subtour settings
			testee.setSingleTripSubtourModes(new String[] {"pt", "walk"});
			boolean hasCar = false;
			boolean hasPt = false;
			boolean hasWalk = false;

			for (int i = 0; i < 50; i++) {
				testee.run(plan);

				assertEquals(
						3,
						plan.getPlanElements().size(),
						"unexpected plan size");

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
					!hasCar && hasPt && hasWalk,
					"expected subtours with NO car, pt and walk, got"+
					(hasCar ? " car" : " NO car")+","+
					(hasPt ? " pt" : " NO pt")+","+
					(hasWalk ? " walk" : " NO walk"));
		}

	}


	@ParameterizedTest
	@ValueSource(doubles = {0.0, 0.5})
	void testUnclosedSubtour(double probaForRandomSingleTripMode) {

		String[] modes = new String[] {"car", "pt", "walk"};

		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() , new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, new Random(15102011), SubtourModeChoice.Behavior.betweenAllAndFewerConstraints, probaForRandomSingleTripMode);

		PopulationFactory fact = PopulationUtils.getFactory();

		Person person = fact.createPerson(Id.create("1000", Person.class));
		final Plan plan = fact.createPlan();

		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));

		plan.addActivity(fact.createActivityFromActivityFacilityId("work", Id.create(1, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));

		plan.addActivity(fact.createActivityFromActivityFacilityId("leisure", Id.create(2, ActivityFacility.class)));

		List<ChooseRandomLegModeForSubtour.Candidate> candidates = testee.determineChoiceSet(plan);

		assertThat(candidates)
				.isNotEmpty();

	}


	private void testSubTourMutationToCar(Network network, double probaForRandomSingleTripMode) {
		String expectedMode = TransportMode.car;
		String originalMode = TransportMode.pt;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(network, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, false);
		}
	}

	private void testSubTourMutationToCar(ActivityFacilities facilities, double probaForRandomSingleTripMode) {
		String expectedMode = TransportMode.car;
		String originalMode = TransportMode.pt;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(facilities, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, true);
		}
	}

	private void testUnknownModeDoesntMutate(Network network, double probaForRandomSingleTripMode) {
		String originalMode = TransportMode.walk;
		String[] modes = new String[] {TransportMode.car, TransportMode.pt};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(network, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
		}
	}

	private void testUnknownModeDoesntMutate(ActivityFacilities facilities, double probaForRandomSingleTripMode) {
		String originalMode = TransportMode.walk;
		String[] modes = new String[] {TransportMode.car, TransportMode.pt};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(facilities, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
		}
	}

	private void testSubTourMutationToPt(ActivityFacilities facilities, double probaForRandomSingleTripMode) {
		String expectedMode = TransportMode.pt;
		String originalMode = TransportMode.car;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(facilities, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, true);
		}
	}

	private void testSubTourMutationToPt(Network network, double probaForRandomSingleTripMode) {
		String expectedMode = TransportMode.pt;
		String originalMode = TransportMode.car;
		String[] modes = new String[] {expectedMode, originalMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(network, activityChainString, originalMode);
			Plan originalPlan = PopulationUtils.createPlan(person);
			PopulationUtils.copyFromTo(plan, originalPlan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, false);
		}
	}

	private void testCarDoesntTeleport(Network network, String originalMode, String otherMode, double probaForRandomSingleTripMode) {
		String[] modes = new String[] {originalMode, otherMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);

		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(network, activityChainString, originalMode);
			testee.run(plan);
			Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
			Activity firstActivity = (Activity) peIterator.next();
			Id<Link> firstLocation = firstActivity.getLinkId();
			Id<Link> carLocation = firstLocation;
			Id<Link> currentLocation = firstLocation;
			int legCount = 0;
			while (peIterator.hasNext()) {
				legCount++;
				Leg nextLeg = (Leg) peIterator.next();
				Activity nextActivity = (Activity) peIterator.next();
				Id<Link> nextLocation = nextActivity.getLinkId();
				if (nextLeg.getMode().equals(TransportMode.car)) {
					assertEquals(
							currentLocation,
							carLocation,
							"wrong car location at leg "+legCount+" in "+plan.getPlanElements());
					carLocation = nextLocation;
				}
				currentLocation = nextLocation;
			}
			assertEquals(
					firstLocation,
					carLocation,
					"wrong car location at the end of "+plan.getPlanElements());
		}
	}

	private void testCarDoesntTeleport(ActivityFacilities facilities, String originalMode, String otherMode, double probaForRandomSingleTripMode) {
		String[] modes = new String[] {originalMode, otherMode};
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour( new MainModeIdentifierImpl() ,new AllowTheseModesForEveryone(modes), modes, CHAIN_BASED_MODES, MatsimRandom.getRandom(), SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes, probaForRandomSingleTripMode);

		for (String activityChainString : activityChainStrings) {
			Plan plan = createPlan(facilities, activityChainString, originalMode);
			testee.run(plan);
			Iterator<PlanElement> peIterator = plan.getPlanElements().iterator();
			Activity firstActivity = (Activity) peIterator.next();
			Id<Link> firstLocation = firstActivity.getLinkId();
			Id<Link> carLocation = firstLocation;
			Id<Link> currentLocation = firstLocation;
			int legCount = 0;
			while (peIterator.hasNext()) {
				legCount++;
				Leg nextLeg = (Leg) peIterator.next();
				Activity nextActivity = (Activity) peIterator.next();
				Id<Link> nextLocation = nextActivity.getLinkId();
				if (nextLeg.getMode().equals(TransportMode.car)) {
					assertEquals(
							currentLocation,
							carLocation,
							"wrong car location at leg "+legCount+" in "+plan.getPlanElements());
					carLocation = nextLocation;
				}
				currentLocation = nextLocation;
			}
			assertEquals(
					firstLocation,
					carLocation,
					"wrong car location at the end of "+plan.getPlanElements());
		}
	}

	private static void assertSubTourMutated(
			final Plan plan,
			final Plan originalPlan,
			final String expectedMode,
			final boolean useFacilities) {
		final Collection<Subtour> originalSubtours =
			TripStructureUtils.getSubtours( originalPlan );
		final Collection<Subtour> mutatedSubtours =
			TripStructureUtils.getSubtours(	plan );

		assertEquals(
				originalSubtours.size(),
				mutatedSubtours.size(),
				"number of subtour changed");

		final List<Subtour> mutateds = new ArrayList<Subtour>();
		for (Subtour mutated : mutatedSubtours) {
			boolean isFirst = true;
			boolean containsMutatedMode = false;

			for (Trip trip : mutated.getTripsWithoutSubSubtours()) {
				if ( expectedMode.equals( trip.getLegsOnly().get( 0 ).getMode() ) ) {
					assertTrue(
							isFirst || containsMutatedMode,
							"inconsistent mode chain" );
					containsMutatedMode = true;
				}
				else {
					assertFalse(
							containsMutatedMode,
							"inconsistent mode chain" );
				}
				isFirst = false;
			}

			if ( containsMutatedMode ) mutateds.add( mutated );
		}

		assertFalse(
				mutateds.isEmpty(),
				"no mutated subtour" );

		int nMutatedWithoutMutatedFather = 0;
		for ( Subtour s : mutateds ) {
			if ( !mutateds.contains( s.getParent() ) ) {
				nMutatedWithoutMutatedFather++;
			}
		}

		assertEquals(
				1,
				nMutatedWithoutMutatedFather,
				"unexpected number of roots in mutated subtours");
	}

	private static Plan createPlan(ActivityFacilities facilities, String facString, String mode) {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		Plan plan = TestsUtil.createPlanFromFacilities((ActivityFacilitiesImpl) facilities, person, mode, facString);
		return plan;
	}

	private static Plan createPlan(Network network, String facString, String mode) {
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1000", Person.class));
		Plan plan = TestsUtil.createPlanFromLinks(network, person, mode, facString);
		return plan;
	}

}
