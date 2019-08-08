/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorTest.java
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

package org.matsim.core.replanning.strategies;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup.TimeAllocationMutatorSubpopulationSettings;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.PlanMutateTimeAllocation;
import org.matsim.core.population.algorithms.PlanMutateTimeAllocationSimplified;
import org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestCase;

import javax.inject.Provider;

/**
 * Tests the functionality of {@link TimeAllocationMutatorModule}, mainly that the
 * correct mutation range is handed over to the underlying {@link PlanMutateTimeAllocation}.
 *
 * @author mrieser
 */
public class TimeAllocationMutatorModuleTest extends MatsimTestCase {

	/**
	 * Tests that the mutation range given in the constructor is respected.
	 *
	 * @author mrieser
	 */
	public void testMutationRangeParam() {
		boolean affectingDuration = true ;

		runMutationRangeTest(new TripPlanMutateTimeAllocation(new StageActivityTypesImpl(), 750, affectingDuration, MatsimRandom.getLocalInstance()), 750);
		runMutationRangeTest(new TripPlanMutateTimeAllocation(new StageActivityTypesImpl(), 7200, affectingDuration, MatsimRandom.getLocalInstance()), 7200);
	}

	public void testSimplifiedMutation() {
		boolean affectingDuration = true ;

		runSimplifiedMutationRangeTest(new PlanMutateTimeAllocationSimplified(StageActivityHandling.ExcludeStageActivities, 750, affectingDuration, MatsimRandom.getLocalInstance()), 750);
		runSimplifiedMutationRangeTest(new PlanMutateTimeAllocationSimplified(StageActivityHandling.ExcludeStageActivities, 7200, affectingDuration, MatsimRandom.getLocalInstance()), 7200);
	}

	public void testSubpopulations() {
		
		String cbSubpopulation = "cb";
		String freightSubpopulation = "freight";
		
		Config config  = ConfigUtils.createConfig();
		config.plans().setSubpopulationAttributeName("subpopulation");
		config.timeAllocationMutator().setUseIndividualSettingsForSubpopulations(true);
		config.timeAllocationMutator().setMutationRange(1800.0);
		
		TimeAllocationMutatorSubpopulationSettings cbSettings = (TimeAllocationMutatorSubpopulationSettings) config.timeAllocationMutator().createParameterSet(TimeAllocationMutatorSubpopulationSettings.SET_NAME);
		cbSettings.setSubpopulation(cbSubpopulation);
		cbSettings.setAffectingDuration(false);
		cbSettings.setMutationRange(100.0);
		config.timeAllocationMutator().addParameterSet(cbSettings);
		
		TimeAllocationMutatorSubpopulationSettings freightSettings = (TimeAllocationMutatorSubpopulationSettings) config.timeAllocationMutator().createParameterSet(TimeAllocationMutatorSubpopulationSettings.SET_NAME);
		freightSettings.setSubpopulation(freightSubpopulation);
		freightSettings.setAffectingDuration(true);
		freightSettings.setMutationRange(7200.0);
		config.timeAllocationMutator().addParameterSet(freightSettings);
		
		Provider<TripRouter> tripRouterProvider = null;
		
		TimeAllocationMutatorModule mutator = new TimeAllocationMutatorModule(tripRouterProvider, config.plans(), config.timeAllocationMutator(), config.global());
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		TimeAllocationMutatorModule cbMutator = new TimeAllocationMutatorModule(tripRouterProvider, config.plans(), config.timeAllocationMutator(), config.global(), scenario.getPopulation());
//		assertEquals(false, cbMutator.affectingDuration);
//		assertEquals(100.0, cbMutator.mutationRange);
//		
//		TimeAllocationMutator freightMutator = new TimeAllocationMutator(tripRouterProvider, config.plans(), config.timeAllocationMutator(), config.global(), scenario.getPopulation());
//		assertEquals(true, freightMutator.affectingDuration);
//		assertEquals(7200.0, freightMutator.mutationRange);
	}
	
	/**
	 * Internal helper method to run the real test, but with different setups.
	 * Basically, it creates one plan and calls the given TimeAllocationMutator
	 * several times with this plans, each time measuring how much the activity
	 * durations have changed and thus ensuring, the differences are within the
	 * expected range.
	 *
	 * @param tripPlanMutateTimeAllocation A preset TimeAllocationMutator to be used for the tests.
	 * @param expectedMutationRange The expected range for mutation.
	 */
	private void runMutationRangeTest(final PlanAlgorithm tripPlanMutateTimeAllocation, final int expectedMutationRange) {
		// setup network
		Network network = NetworkUtils.createNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 200, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 300, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("0", Link.class), fromNode, toNode, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode1, toNode1, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode2, toNode2, (double) 100, (double) 5, (double) 100, (double) 1 );

		// setup person
		Plan plan;
		Activity act1, act2;
		try {
			/* The chosen times for the activity durations are such that it is likely
			 * for the random mutation to reach midnight (either at 00:00:00 or at 24:00:00).
			 */
			Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
			plan = PersonUtils.createAndAddPlan(person, true);
			act1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
			act1.setEndTime(4*3600);
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			act2 = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link1.getId());
			act2.setMaximumDuration(14*3600);
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// run test
		double act1Dur = act1.getEndTime();
		double minDiff1 = Double.POSITIVE_INFINITY;
		double maxDiff1 = Double.NEGATIVE_INFINITY;
		double act2Dur = act2.getMaximumDuration();
		double minDiff2 = Double.POSITIVE_INFINITY;
		double maxDiff2 = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 150; i++) {
			tripPlanMutateTimeAllocation.run(plan);
			// test duration of act1
			double diff = act1Dur - act1.getMaximumDuration();
			if (diff > maxDiff1) maxDiff1 = diff;
			if (diff < minDiff1) minDiff1 = diff;
			act1Dur = act1.getMaximumDuration();
			if (!Time.isUndefinedTime(act1Dur)) {
				assertTrue("activity duration cannot be smaller than 0, is " + act1Dur, act1Dur >= 0.0);
			}
			// test duration of act2
			diff = act2Dur - act2.getMaximumDuration();
			if (diff > maxDiff2) maxDiff2 = diff;
			if (diff < minDiff2) minDiff2 = diff;
			act2Dur = act2.getMaximumDuration();
			if (!Time.isUndefinedTime(act2Dur)) {
				assertTrue("activity duration cannot be smaller than 0, is " + act2Dur, act2Dur >= 0.0);
			}
		}
		assertTrue("mutation range differences wrong (act1).", minDiff1 <= maxDiff1);
		assertTrue("mutation range differences wrong (act2).", minDiff2 <= maxDiff2);

		/* The following asserts are dependent on random numbers.
		 * But I would still expect that we get up to at least 95% of the limit...   */
		assertValueInRange("mutation range out of range (maxDiff1).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff1).", minDiff1, -expectedMutationRange, -expectedMutationRange*0.95);
		assertValueInRange("mutation range out of range (maxDiff2).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff2).", minDiff2, -expectedMutationRange, -expectedMutationRange*0.95);
	}

	/**
	 * Internal helper method to run the real test, but with different setups.
	 * Basically, it creates one plan and calls the given TimeAllocationMutator
	 * several times with this plans, each time measuring how much the activity
	 * durations have changed and thus ensuring, the differences are within the
	 * expected range.
	 *
	 * @param tripPlanMutateTimeAllocation A preset TimeAllocationMutator to be used for the tests.
	 * @param expectedMutationRange The expected range for mutation.
	 */
	private void runSimplifiedMutationRangeTest(final PlanAlgorithm tripPlanMutateTimeAllocation, final int expectedMutationRange) {
		// setup network
		Network network = NetworkUtils.createNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 200, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 300, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("0", Link.class), fromNode, toNode, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode1, toNode1, (double) 100, (double) 5, (double) 100, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode2, toNode2, (double) 100, (double) 5, (double) 100, (double) 1 );

		// setup person
		Plan plan;
		Activity act1, act2;
		try {
			/* The chosen times for the activity durations are such that it is likely
			 * for the random mutation to reach midnight (either at 00:00:00 or at 24:00:00).
			 */
			Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
			plan = PersonUtils.createAndAddPlan(person, true);
			act1 = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
			act1.setEndTime(4*3600);
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			act2 = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", link1.getId());
			act2.setMaximumDuration(14*3600);
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "h", link1.getId());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// run test
		double act1End = act1.getEndTime();
		double minDiff1 = Double.POSITIVE_INFINITY;
		double maxDiff1 = Double.NEGATIVE_INFINITY;
		double act2Dur = act2.getMaximumDuration();
		double minDiff2 = Double.POSITIVE_INFINITY;
		double maxDiff2 = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 150; i++) {
			tripPlanMutateTimeAllocation.run(plan);
			// get end time of act1
			double diff = act1End - act1.getEndTime();
			if (diff > maxDiff1) maxDiff1 = diff;
			if (diff < minDiff1) minDiff1 = diff;
			act1End = act1.getEndTime();
			if (!Time.isUndefinedTime(act1End)) {
				assertTrue("activity end time cannot be smaller than 0, is " + act1End, act1End >= 0.0);
			}
			// test end time of act2
			diff = act2Dur - act2.getMaximumDuration();
			if (diff > maxDiff2) maxDiff2 = diff;
			if (diff < minDiff2) minDiff2 = diff;
			act2Dur = act2.getMaximumDuration();
			if (!Time.isUndefinedTime(act2Dur)) {
				assertTrue("activity duration cannot be smaller than 0, is " + act2Dur, act2Dur >= 0.0);
			}
		}
		assertTrue("mutation range differences wrong (act1).", minDiff1 <= maxDiff1);
		assertTrue("mutation range differences wrong (act2).", minDiff2 <= maxDiff2);

		/* The following asserts are dependent on random numbers.
		 * But I would still expect that we get up to at least 95% of the limit...   */
		assertValueInRange("mutation range out of range (maxDiff1).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff1).", minDiff1, -expectedMutationRange, -expectedMutationRange*0.95);
		assertValueInRange("mutation range out of range (maxDiff2).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff2).", minDiff2, -expectedMutationRange, -expectedMutationRange*0.95);
	}

	private static void assertValueInRange(final String message, final double actual, final double lowerLimit, final double upperLimit) {
		assertTrue(message + " actual: " + actual + ", range: " + lowerLimit + "..." + upperLimit, (lowerLimit <= actual) && (actual <= upperLimit));
	}
}
