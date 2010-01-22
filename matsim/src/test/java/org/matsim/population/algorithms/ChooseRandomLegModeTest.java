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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.Layer;

/**
 * @author mrieser
 */
public class ChooseRandomLegModeTest extends MatsimTestCase {
	
	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";
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

	public void testRandomChoice() {
		PlanomatConfigGroup planomatConfigGroup = new PlanomatConfigGroup();
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new TransportMode[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), planomatConfigGroup);
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		boolean foundCarMode = false;
		boolean foundPtMode = false;
		boolean foundWalkMode = false;
		TransportMode previousMode = leg.getMode();
		for (int i = 0; i < 5; i++) {
			algo.run(plan);
			assertNotSame("leg mode must have changed.", previousMode, leg.getMode());
			previousMode = leg.getMode();
			if (TransportMode.car.equals(previousMode)) {
				foundCarMode = true;
			} else if (TransportMode.pt.equals(previousMode)) {
				foundPtMode = true;
			} else if (TransportMode.walk.equals(previousMode)) {
				foundWalkMode = true;
			} else {
				fail("unexpected mode: " + previousMode.toString());
			}
		}
		assertTrue("expected to find car-mode", foundCarMode);
		assertTrue("expected to find pt-mode", foundPtMode);
		assertTrue("expected to find walk-mode", foundWalkMode);
	}

	public void testHandleEmptyPlan() {
		PlanomatConfigGroup planomatConfigGroup = new PlanomatConfigGroup();
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new TransportMode[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), planomatConfigGroup);
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	public void testHandlePlanWithoutLeg() {
		PlanomatConfigGroup planomatConfigGroup = new PlanomatConfigGroup();
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new TransportMode[] {TransportMode.car, TransportMode.pt, TransportMode.walk}, MatsimRandom.getRandom(), planomatConfigGroup);
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		algo.run(plan);
		// no specific assert, but there should also be no NullPointerException or similar stuff that could theoretically happen
	}

	/**
	 * Test that all the legs have the same, changed mode
	 */
	public void testMultipleLegs() {
		PlanomatConfigGroup planomatConfigGroup = new PlanomatConfigGroup();
		ChooseRandomLegMode algo = new ChooseRandomLegMode(new TransportMode[] {TransportMode.car, TransportMode.pt}, MatsimRandom.getRandom(), planomatConfigGroup);
		PlanImpl plan = new org.matsim.core.population.PlanImpl(null);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("work", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("shop", new CoordImpl(0, 0));
		plan.createAndAddLeg(TransportMode.car);
		plan.createAndAddActivity("home", new CoordImpl(0, 0));
		algo.run(plan);
		assertEquals("unexpected leg mode in leg 1.", TransportMode.pt, ((Leg) plan.getPlanElements().get(1)).getMode());
		assertEquals("unexpected leg mode in leg 2.", TransportMode.pt, ((Leg) plan.getPlanElements().get(3)).getMode());
		assertEquals("unexpected leg mode in leg 3.", TransportMode.pt, ((Leg) plan.getPlanElements().get(5)).getMode());
	}
	
	public void testSubTourMutationNetworkBased() {
		Config config = loadConfig(CONFIGFILE);
		Scenario scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);
		this.testSubTourMutationToCar((NetworkLayer) network, config.planomat());
		this.testSubTourMutationToPt((NetworkLayer) network, config.planomat());
	}
	
	public void testSubTourMutationFacilitiesBased() {
		Config config = loadConfig(CONFIGFILE);
		ScenarioImpl scenario = new ScenarioImpl(config);
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		new MatsimFacilitiesReader(scenario).readFile(config.facilities().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility);
		this.testSubTourMutationToCar(facilities, config.planomat());
		this.testSubTourMutationToPt(facilities, config.planomat());
	}
	
	public void testCarDoesntTeleportFromHome() {
		Config config = loadConfig(CONFIGFILE);
		Scenario scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(config.network().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);
		testCarDoesntTeleport((NetworkLayer) network, config.planomat(), TransportMode.car, TransportMode.pt);
		testCarDoesntTeleport((NetworkLayer) network, config.planomat(), TransportMode.pt, TransportMode.car);
	}
	
	public void testSubTourMutationToCar(Layer layer, PlanomatConfigGroup planomatConfigGroup) {
		TransportMode expectedMode = TransportMode.car;
		TransportMode originalMode = TransportMode.pt;
		ChooseRandomLegMode testee = new ChooseRandomLegMode(new TransportMode[] {expectedMode, originalMode}, MatsimRandom.getRandom(), planomatConfigGroup);
		testee.setChangeOnlyOneSubtour(true);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = TestsUtil.createPlan(layer, person, originalMode, activityChainString, planomatConfigGroup);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyPlan(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, planomatConfigGroup, expectedMode);
		}

	}
	
	public void testSubTourMutationToPt(Layer layer, PlanomatConfigGroup planomatConfigGroup) {
		TransportMode expectedMode = TransportMode.pt;
		TransportMode originalMode = TransportMode.car;
		ChooseRandomLegMode testee = new ChooseRandomLegMode(new TransportMode[] {expectedMode, originalMode}, MatsimRandom.getRandom(), planomatConfigGroup);
		testee.setChangeOnlyOneSubtour(true);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = TestsUtil.createPlan(layer, person, originalMode, activityChainString, planomatConfigGroup);
			PlanImpl originalPlan = new PlanImpl(person);
			originalPlan.copyPlan(plan);
			assertTrue(TestsUtil.equals(plan.getPlanElements(), originalPlan.getPlanElements()));
			testee.run(plan);
			assertSubTourMutated(plan, originalPlan, planomatConfigGroup, expectedMode);
		}

	}
	
	public void testCarDoesntTeleport(Layer layer, PlanomatConfigGroup planomatConfigGroup, TransportMode originalMode, TransportMode otherMode) {
		ChooseRandomLegMode testee = new ChooseRandomLegMode(new TransportMode[] {originalMode, otherMode}, MatsimRandom.getRandom(), planomatConfigGroup);
		testee.setChangeOnlyOneSubtour(true);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		for (String activityChainString : activityChainStrings) {
			PlanImpl plan = TestsUtil.createPlan(layer, person, originalMode, activityChainString, planomatConfigGroup);
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
			PlanomatConfigGroup planomatConfigGroup, TransportMode expectedMode) {
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours(
				planomatConfigGroup);
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

	private boolean isThisSubTourMutated(Plan plan, int subTourFromIndex, int subTourToIndex, Plan originalPlan, TransportMode expectedMode) {
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

}
