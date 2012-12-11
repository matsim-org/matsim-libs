/* *********************************************************************** *
 * project: org.matsim.*
 * MergingTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class MergingTest {
	private final List<GroupPlans> testPlans = new ArrayList<GroupPlans>();

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	@After
	public void clean() {
		testPlans.clear();
	}

	@Before
	public void allIndividuals() {
		List<Plan> plans = new ArrayList<Plan>();

		for (int i=0; i<20; i++) {
			plans.add( new PlanImpl( new PersonImpl( new IdImpl( i ) ) ) );
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testProbOne() throws Exception {
		JointPlanMergingAlgorithm algo =
			new JointPlanMergingAlgorithm(
					1d,
					new Random( 12 ));

		for (GroupPlans gp : testPlans) {
			int nplans = countPlans( gp );
			algo.run( gp );
			assertEquals(
					"unexpected number of joint plans",
					1,
					gp.getJointPlans().size());
			assertEquals(
					"unexpected number of individual plans",
					0,
					gp.getIndividualPlans().size());
			assertEquals(
					"unexpected overall number of plans",
					nplans,
					countPlans( gp ));

		}
	}

	@Test
	public void testProbZero() throws Exception {
		JointPlanMergingAlgorithm algo =
			new JointPlanMergingAlgorithm(
					1d,
					new Random( 12 ));

		for (GroupPlans gp : testPlans) {
			int nplans = countPlans( gp );
			algo.run( gp );
			assertEquals(
					"unexpected number of joint plans",
					0,
					gp.getJointPlans().size());
			assertEquals(
					"unexpected number of individual plans",
					nplans,
					gp.getIndividualPlans().size());
		}
	}

	private int countPlans(final GroupPlans gp) {
		int c = 0;

		for (JointPlan jp : gp.getJointPlans()) c += jp.getIndividualPlans().size();
		c += gp.getIndividualPlans().size();

		return c;
	}
}

