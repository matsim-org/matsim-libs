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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.BasicLocations;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup.TripStructureAnalysisLayerOption;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser, michaz
 */
public class ChooseRandomLegModeForSubtourTest extends MatsimTestCase {

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

	public void testHandleEmptyPlan() {
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour(new AllowTheseModesForEveryone(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}), CHAIN_BASED_MODES, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	public void testHandlePlanWithoutLeg() {
		ChooseRandomLegModeForSubtour algo = new ChooseRandomLegModeForSubtour(new AllowTheseModesForEveryone(new String[] {TransportMode.car, TransportMode.pt, TransportMode.walk}), CHAIN_BASED_MODES, MatsimRandom.getRandom());
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}


	public void testSubTourMutationNetworkBased() {
		Config config = loadConfig(CONFIGFILE);
		Scenario scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		TripStructureAnalysisLayerOption tripStructureAnalysisLayer = PlanomatConfigGroup.TripStructureAnalysisLayerOption.link;
		config.planomat().setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		this.testSubTourMutationToCar((NetworkImpl) network, tripStructureAnalysisLayer);
		this.testSubTourMutationToPt((NetworkImpl) network, tripStructureAnalysisLayer);
	}

	public void testSubTourMutationFacilitiesBased() {
		Config config = loadConfig(CONFIGFILE);
		ScenarioImpl scenario = new ScenarioImpl(config);
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		TripStructureAnalysisLayerOption tripStructureAnalysisLayer = PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility;
		config.planomat().setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		this.testSubTourMutationToCar(facilities, tripStructureAnalysisLayer);
		this.testSubTourMutationToPt(facilities, tripStructureAnalysisLayer);
	}

	public void testCarDoesntTeleportFromHome() {
		Config config = loadConfig(CONFIGFILE);
		Scenario scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);
		testCarDoesntTeleport((NetworkImpl) network, config.planomat(), TransportMode.car, TransportMode.pt);
		testCarDoesntTeleport((NetworkImpl) network, config.planomat(), TransportMode.pt, TransportMode.car);
	}

	public void testSubTourMutationToCar(BasicLocations layer, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		String expectedMode = TransportMode.car;
		String originalMode = TransportMode.pt;
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour(new AllowTheseModesForEveryone(new String[] {expectedMode, originalMode}), CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, tripStructureAnalysisLayer, originalMode);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyPlan(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, tripStructureAnalysisLayer);
		}

	}

	public void testSubTourMutationToPt(BasicLocations layer, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		String expectedMode = TransportMode.pt;
		String originalMode = TransportMode.car;
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour(new AllowTheseModesForEveryone(new String[] {expectedMode, originalMode}), CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, tripStructureAnalysisLayer, originalMode);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyPlan(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, expectedMode, tripStructureAnalysisLayer);
		}

	}

	public void testCarDoesntTeleport(BasicLocations layer, PlanomatConfigGroup planomatConfigGroup, String originalMode, String otherMode) {
		ChooseRandomLegModeForSubtour testee = new ChooseRandomLegModeForSubtour(new AllowTheseModesForEveryone(new String[] {originalMode, otherMode}), CHAIN_BASED_MODES, MatsimRandom.getRandom());
		testee.setTripStructureAnalysisLayer(planomatConfigGroup.getTripStructureAnalysisLayer());
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = createPlan(layer, activityChainString, planomatConfigGroup.getTripStructureAnalysisLayer(), originalMode);
			testee.run(plan);
			Iterator<PlanElement> i = plan.getPlanElements().iterator();
			Activity firstActivity = (Activity) i.next();
			Id firstLocation = firstActivity.getLinkId();
			Id carLocation = firstLocation;
			Id currentLocation = firstLocation;
			while (i.hasNext()) {
				Leg nextLeg = (Leg) i.next();
				Activity nextActivity = (Activity) i.next();
				Id nextLocation = nextActivity.getLinkId();
				if (nextLeg.getMode() == TransportMode.car) {
					assertEquals(currentLocation, carLocation);
					carLocation = nextLocation;
				}
				currentLocation = nextLocation;
			}
			assertEquals(firstLocation, carLocation);
		}
	}

	private void assertSubTourMutated(Plan plan, Plan originalPlan,
			String expectedMode, TripStructureAnalysisLayerOption tripStructureAnalysisLayer) {
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.setTripStructureAnalysisLayer(tripStructureAnalysisLayer);
		planAnalyzeSubtours.run(plan);
		Integer mutatedSubTourIndex = null;
		int numSubtours = planAnalyzeSubtours.getNumSubtours();
		if (numSubtours == 0) {
			return;
		} else {
			for (int subTourIndex = 0; subTourIndex < numSubtours; subTourIndex++) {
				int subTourFromIndex = planAnalyzeSubtours
				.getFromIndexOfSubtours().get(subTourIndex);
				int subTourToIndex = planAnalyzeSubtours.getToIndexOfSubtours()
				.get(subTourIndex);
				if (isThisSubTourMutated(plan, subTourFromIndex,
						subTourToIndex, originalPlan, expectedMode)) {
					mutatedSubTourIndex = subTourToIndex;
				}
			}
			assertNotNull("Couldn't find a mutated subtour.",
					mutatedSubTourIndex);
		}
	}

	private boolean isThisSubTourMutated(Plan plan, int subTourFromIndex, int subTourToIndex, Plan originalPlan, String expectedMode) {
		List<PlanElement> prefix = plan.getPlanElements().subList(0, subTourFromIndex);
		List<PlanElement> subTour = plan.getPlanElements().subList(subTourFromIndex, subTourToIndex);
		List<PlanElement> suffix = plan.getPlanElements().subList(subTourToIndex, plan.getPlanElements().size());
		List<PlanElement> originalPrefix = originalPlan.getPlanElements().subList(0, subTourFromIndex);
		List<PlanElement> originalSubTour = originalPlan.getPlanElements().subList(subTourFromIndex, subTourToIndex);
		List<PlanElement> originalSuffix = originalPlan.getPlanElements().subList(subTourToIndex, originalPlan.getPlanElements().size());
		if (!TestsUtil.equals(originalPrefix, prefix)) {
			return false;
		}
		if (!TestsUtil.equals(originalSuffix, suffix)) {
			return false;
		}
		for (PlanElement planElement : originalSubTour) {
			if (planElement instanceof Leg) {
				Leg leg = (Leg) planElement;
				leg.setMode(expectedMode);
			}
		}
		if (!TestsUtil.equals(originalSubTour, subTour)) {
			return false;
		}
		return true;
	}

	private PlanImpl createPlan(BasicLocations layer, String facString,
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
