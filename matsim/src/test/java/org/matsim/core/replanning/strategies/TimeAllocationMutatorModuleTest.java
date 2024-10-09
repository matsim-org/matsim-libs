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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.MutateActivityTimeAllocation;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Random;

/**
 * Tests the functionality of {@link TimeAllocationMutatorModule}, mainly that the
 * correct mutation range is handed over to the underlying {@link MutateActivityTimeAllocation}.
 *
 * @author mrieser
 */
public class TimeAllocationMutatorModuleTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testSimplifiedMutation() {
		boolean affectingDuration = true ;

		runSimplifiedMutationRangeTest(new MutateActivityTimeAllocation( 750, affectingDuration, MatsimRandom.getLocalInstance(),24*3600,false,1), 750);
		runSimplifiedMutationRangeTest(new MutateActivityTimeAllocation( 7200, affectingDuration, MatsimRandom.getLocalInstance(),24*3600,false,1), 7200);
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
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(100, 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(200, 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(300, 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		Link link1 = NetworkUtils.createAndAddLink(network,Id.create("0", Link.class), fromNode, toNode, 100, 5, 100, 1);
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode1, toNode1, 100, 5, 100, 1);
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode2, toNode2, 100, 5, 100, 1);

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
		double act1End = act1.getEndTime().seconds();
		double minDiff1 = Double.POSITIVE_INFINITY;
		double maxDiff1 = Double.NEGATIVE_INFINITY;
		double act2Dur = act2.getMaximumDuration().seconds();
		double minDiff2 = Double.POSITIVE_INFINITY;
		double maxDiff2 = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 150; i++) {
			tripPlanMutateTimeAllocation.run(plan);
			// get end time of act1
			double diff = act1End - act1.getEndTime().seconds();
			if (diff > maxDiff1) maxDiff1 = diff;
			if (diff < minDiff1) minDiff1 = diff;
			act1End = act1.getEndTime().seconds();
			assertTrue(act1End >= 0.0, "activity end time cannot be smaller than 0, is " + act1End);
			// test end time of act2
			diff = act2Dur - act2.getMaximumDuration().seconds();
			if (diff > maxDiff2) maxDiff2 = diff;
			if (diff < minDiff2) minDiff2 = diff;
			act2Dur = act2.getMaximumDuration().seconds();
			assertTrue(act2Dur >= 0.0, "activity duration cannot be smaller than 0, is " + act2Dur);
		}
		assertTrue(minDiff1 <= maxDiff1, "mutation range differences wrong (act1).");
		assertTrue(minDiff2 <= maxDiff2, "mutation range differences wrong (act2).");

		/* The following asserts are dependent on random numbers.
		 * But I would still expect that we get up to at least 95% of the limit...   */
		assertValueInRange("mutation range out of range (maxDiff1).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff1).", minDiff1, -expectedMutationRange, -expectedMutationRange*0.95);
		assertValueInRange("mutation range out of range (maxDiff2).", maxDiff1, expectedMutationRange*0.95, expectedMutationRange);
		assertValueInRange("mutation range out of range (minDiff2).", minDiff2, -expectedMutationRange, -expectedMutationRange*0.95);
	}

	private static void assertValueInRange(final String message, final double actual, final double lowerLimit, final double upperLimit) {
		assertTrue((lowerLimit <= actual) && (actual <= upperLimit), message + " actual: " + actual + ", range: " + lowerLimit + "..." + upperLimit);
	}


	@Test
	void testRun() {
		// setup population with one person
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		act.setEndTime(8.0 * 3600);
		PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		Activity ptAct1 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord(0, 100));
		ptAct1.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		Activity ptAct2 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord(0, 100));
		ptAct2.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		act = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));
		act.setEndTime(16*3600);
		PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		Activity ptAct3 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord(0, 100));
		ptAct3.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg( plan, TransportMode.pt );
		Activity ptAct4 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE, new Coord(0, 100));
		ptAct4.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg( plan, TransportMode.transit_walk );
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));
		boolean affectingDuration = true ;

		MutateActivityTimeAllocation mutator =
				new MutateActivityTimeAllocation(
						3600.,
						affectingDuration, new Random(2011),24*3600,false,1);
		mutator.run(plan);

		Assertions.assertEquals(0.0, ptAct1.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct2.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct3.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct4.getMaximumDuration().seconds(), 1e-8);
	}

	@Test
	void testRunLatestEndTime() {
		// setup population with one person
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		act.setEndTime(8.0 * 3600);
		PopulationUtils.createAndAddLeg(plan, TransportMode.transit_walk);
		Activity ptAct1 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE,
				new Coord(0, 100));
		ptAct1.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		Activity ptAct2 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE,
				new Coord(0, 100));
		ptAct2.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg(plan, TransportMode.transit_walk);
		act = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));
		act.setEndTime(38 * 3600);
		PopulationUtils.createAndAddLeg(plan, TransportMode.transit_walk);
		Activity ptAct3 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE,
				new Coord(0, 100));
		ptAct3.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg(plan, TransportMode.pt);
		Activity ptAct4 = PopulationUtils.createAndAddActivityFromCoord(plan, PtConstants.TRANSIT_ACTIVITY_TYPE,
				new Coord(0, 100));
		ptAct4.setMaximumDuration(0);
		PopulationUtils.createAndAddLeg(plan, TransportMode.transit_walk);
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));

		boolean affectingDuration = true;
		final double latestEndTime = 30. * 3600;

		MutateActivityTimeAllocation mutator =
				new MutateActivityTimeAllocation(
						3600.,
						affectingDuration, new Random(2011),latestEndTime,false,1);

		mutator.run(plan);

		Assertions.assertEquals(0.0, ptAct1.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct2.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct3.getMaximumDuration().seconds(), 1e-8);
		Assertions.assertEquals(0.0, ptAct4.getMaximumDuration().seconds(), 1e-8);

		// check whether activity times are equal or less than latestEndTime
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity activity) {
				if (activity.getStartTime().isDefined()) {
					Assertions.assertTrue(activity.getStartTime().seconds() <= latestEndTime);
				}
				if (activity.getEndTime().isDefined()) {
					Assertions.assertTrue(activity.getEndTime().seconds() <= latestEndTime);
				}
			}
		}
	}

	@Test
	void testLegTimesAreSetCorrectly() {
		// setup population with one person
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan = PersonUtils.createAndAddPlan(person, true);
		Activity act = PopulationUtils.createAndAddActivityFromCoord(plan, "home", new Coord(0, 0));
		act.setEndTime(8.0 * 3600);
		Leg leg1 = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		leg1.setDepartureTime(8.0*3600);
		leg1.setTravelTime(1800.);
		Activity act2 = PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));
		act2.setMaximumDuration(8*3600);
		Leg leg2 = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		leg2.setDepartureTime(16.5*3600);
		leg2.setTravelTime(1800.0);
		PopulationUtils.createAndAddActivityFromCoord(plan, "work", new Coord(0, 500));
		boolean affectingDuration = true ;

		MutateActivityTimeAllocation mutator =
				new MutateActivityTimeAllocation(
						3600.,
						affectingDuration, new Random(2011),24*3600,false,1);
		mutator.run(plan);

		double firstActEndTime = act.getEndTime().seconds();
		double secondActDuration = act2.getMaximumDuration().seconds();
		Assertions.assertEquals(firstActEndTime,leg1.getDepartureTime().seconds(), MatsimTestUtils.EPSILON);
		Assertions.assertEquals(firstActEndTime+secondActDuration+leg1.getTravelTime().seconds(),leg2.getDepartureTime().seconds(), MatsimTestUtils.EPSILON);



	}


}
