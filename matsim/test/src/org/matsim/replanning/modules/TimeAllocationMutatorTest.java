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

package org.matsim.replanning.modules;

import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.misc.Time;

/**
 * Tests the functionality of {@link TimeAllocationMutator}, mainly that the
 * correct mutation range is handed over to the underlying {@link PlanMutateTimeAllocation}.
 *
 * @author mrieser
 */
public class TimeAllocationMutatorTest extends MatsimTestCase {

	/**
	 * Tests that the default value is respected.
	 *
	 * @author mrieser
	 */
	public void testMutationRangeDefault() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);
		runMutationRangeTest(new TimeAllocationMutator(), 1800);
	}

	/**
	 * Tests that the mutation range set in the configuration file is respected.
	 *
	 * @author mrieser
	 */
	public void testMutationRangeConfig() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);

		// test smaller value than default
		config.setParam(TimeAllocationMutator.CONFIG_GROUP, TimeAllocationMutator.CONFIG_MUTATION_RANGE, "900");
		runMutationRangeTest(new TimeAllocationMutator(), 900);

		// test bigger value than default
		config.setParam(TimeAllocationMutator.CONFIG_GROUP, TimeAllocationMutator.CONFIG_MUTATION_RANGE, "2700");
		runMutationRangeTest(new TimeAllocationMutator(), 2700);
	}

	/**
	 * Tests that the mutation range given in the constructor is respected.
	 *
	 * @author mrieser
	 */
	public void testMutationRangeParam() {
		Config config = loadConfig(null);
		config.global().setNumberOfThreads(0);

		// test smaller value than default
		runMutationRangeTest(new TimeAllocationMutator(750), 750);

		// test bigger value than default
		config.setParam(TimeAllocationMutator.CONFIG_GROUP, TimeAllocationMutator.CONFIG_MUTATION_RANGE, "2700");
		runMutationRangeTest(new TimeAllocationMutator(7200), 7200);
	}

	/**
	 * Internal helper method to run the real test, but with different setups.
	 * Basically, it creates one plan and calls the given TimeAllocationMutator
	 * several times with this plans, each time measuring how much the activity
	 * durations have changed and thus ensuring, the differences are within the
	 * expected range.
	 *
	 * @param mutator A preset TimeAllocationMutator to be used for the tests.
	 * @param expectedMutationRange The expected range for mutation.
	 */
	private void runMutationRangeTest(final TimeAllocationMutator mutator, final int expectedMutationRange) {
		// setup network
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("01:00:00");
		network.createNode("1", "0", "0", null);
		network.createNode("2", "100", "0", null);
		network.createNode("3", "200", "0", null);
		network.createNode("4", "300", "0", null);
		Link link1 = network.createLink("0", "1", "2", "100", "5", "100", "1", null, null);
		network.createLink("1", "2", "3", "100", "5", "100", "1", null, null);
		network.createLink("2", "3", "4", "100", "5", "100", "1", null, null);
		Gbl.getWorld().setNetworkLayer(network);

		// setup person
		Plan plan;
		Act act1, act2;
		try {
			/* The chosen times for the activity durations are such that it is likely
			 * for the random mutation to reach midnight (either at 00:00:00 or at 24:00:00).
			 */
			Person person = new Person(new IdImpl("1"));
			plan = person.createPlan(true);
			act1 = plan.createAct("h", 0, 0, link1, 0, 4*3600, 4*3600, false);
			plan.createLeg("car", 6*3600, 0, Time.UNDEFINED_TIME);
			act2 = plan.createAct("w", 0, 0, link1, 4*3600, 20*3600, 16*3600, false);
			plan.createLeg("car", 16*3600, 0, Time.UNDEFINED_TIME);
			plan.createAct("h", 0, 0, link1, 16*3600, Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, false);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// setup mutator
		mutator.init();

		// run test
		double act1Dur = act1.getDur();
		double minDiff1 = Double.POSITIVE_INFINITY;
		double maxDiff1 = Double.NEGATIVE_INFINITY;
		double act2Dur = act2.getDur();
		double minDiff2 = Double.POSITIVE_INFINITY;
		double maxDiff2 = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < 150; i++) {
			mutator.handlePlan(plan);
			// test duration of act1
			double diff = act1Dur - act1.getDur();
			if (diff > maxDiff1) maxDiff1 = diff;
			if (diff < minDiff1) minDiff1 = diff;
			act1Dur = act1.getDur();
			assertTrue("activity duration cannot be smaller than 0.", act1Dur >= 0.0);
			// test duration of act2
			diff = act2Dur - act2.getDur();
			if (diff > maxDiff2) maxDiff2 = diff;
			if (diff < minDiff2) minDiff2 = diff;
			act2Dur = act2.getDur();
			assertTrue("activity duration cannot be smaller than 0.", act2Dur >= 0.0);
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
