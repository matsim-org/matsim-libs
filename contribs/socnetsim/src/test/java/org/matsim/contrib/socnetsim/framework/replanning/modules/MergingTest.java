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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class MergingTest {
	private JointPlans jointPlans = new JointPlans();
	private final List<GroupPlans> testPlans = new ArrayList<GroupPlans>();

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	@After
	public void clean() {
		testPlans.clear();
		jointPlans = new JointPlans();
	}

	@Before
	public void allIndividuals() {
		List<Plan> plans = new ArrayList<Plan>();

		for (int i=0; i<20; i++) {
			plans.add( new PlanImpl(PopulationUtils.createPerson(Id.create(i, Person.class))) );
		}

		testPlans.add( new GroupPlans( Collections.EMPTY_LIST , plans ) );
	}

	@Before
	public void allJoints() {
		List<JointPlan> plans = new ArrayList<JointPlan>();

		for (int i=0; i<10; i++) {
			final Map<Id<Person>, Plan> indivPlans = new HashMap< >();
			for (int j=0; j<1000; j+=100) {
				Id<Person> id = Id.create( i + j , Person.class );
				indivPlans.put(
						id,
						new PlanImpl(PopulationUtils.createPerson(id)) );
			}
			plans.add( jointPlans.getFactory().createJointPlan( indivPlans ) );
		}

		testPlans.add( new GroupPlans( plans , Collections.EMPTY_LIST ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testProbOne() throws Exception {
		JointPlanMergingAlgorithm algo =
			new JointPlanMergingAlgorithm(
					jointPlans.getFactory(),
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
					jointPlans.getFactory(),
					0d,
					new Random( 12 ));

		for (GroupPlans gp : testPlans) {
			int nplans = countPlans( gp );
			int nindivplans = gp.getIndividualPlans().size();
			int njointplans = gp.getJointPlans().size();
			algo.run( gp );
			assertEquals(
					"unexpected number of joint plans",
					njointplans,
					gp.getJointPlans().size());
			assertEquals(
					"unexpected number of individual plans",
					nindivplans,
					gp.getIndividualPlans().size());
			assertEquals(
					"unexpected overall number of plans",
					nplans,
					countPlans( gp ));
		}
	}

	private int countPlans(final GroupPlans gp) {
		int c = 0;

		for (JointPlan jp : gp.getJointPlans()) c += jp.getIndividualPlans().size();
		c += gp.getIndividualPlans().size();

		return c;
	}
}

