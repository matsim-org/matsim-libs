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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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
	@AfterEach
	public void clean() {
		testPlans.clear();
		jointPlans = new JointPlans();
	}

	@BeforeEach
	public void allIndividuals() {
		List<Plan> plans = new ArrayList<Plan>();

		for (int i=0; i<20; i++) {
			plans.add( PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(i, Person.class))) );
		}

		testPlans.add( new GroupPlans( Collections.EMPTY_LIST , plans ) );
	}

	@BeforeEach
	public void allJoints() {
		List<JointPlan> plans = new ArrayList<JointPlan>();

		for (int i=0; i<10; i++) {
			final Map<Id<Person>, Plan> indivPlans = new HashMap< >();
			for (int j=0; j<1000; j+=100) {
				Id<Person> id = Id.create( i + j , Person.class );
				final Id<Person> id1 = id;
				indivPlans.put(
						id,
						PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(id1)) );
			}
			plans.add( jointPlans.getFactory().createJointPlan( indivPlans ) );
		}

		testPlans.add( new GroupPlans( plans , Collections.EMPTY_LIST ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	void testProbOne() throws Exception {
		JointPlanMergingAlgorithm algo =
			new JointPlanMergingAlgorithm(
					jointPlans.getFactory(),
					1d,
					new Random( 12 ));

		for (GroupPlans gp : testPlans) {
			int nplans = countPlans( gp );
			algo.run( gp );
			assertEquals(
					1,
					gp.getJointPlans().size(),
					"unexpected number of joint plans");
			assertEquals(
					0,
					gp.getIndividualPlans().size(),
					"unexpected number of individual plans");
			assertEquals(
					nplans,
					countPlans( gp ),
					"unexpected overall number of plans");

		}
	}

	@Test
	void testProbZero() throws Exception {
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
					njointplans,
					gp.getJointPlans().size(),
					"unexpected number of joint plans");
			assertEquals(
					nindivplans,
					gp.getIndividualPlans().size(),
					"unexpected number of individual plans");
			assertEquals(
					nplans,
					countPlans( gp ),
					"unexpected overall number of plans");
		}
	}

	private int countPlans(final GroupPlans gp) {
		int c = 0;

		for (JointPlan jp : gp.getJointPlans()) c += jp.getIndividualPlans().size();
		c += gp.getIndividualPlans().size();

		return c;
	}
}

