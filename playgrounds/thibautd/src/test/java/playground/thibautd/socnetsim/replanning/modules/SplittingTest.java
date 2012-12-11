/* *********************************************************************** *
 * project: org.matsim.*
 * SplittingTest.java
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class SplittingTest {
	private final List<Fixture> fixtures = new ArrayList<Fixture>();

	private static class Fixture {
		final GroupPlans plan;
		final int expectedNJp;
		final int expectedNIndividualPlans;
		final String name;

		public Fixture(
				final GroupPlans plan,
				final int enjp,
				final int enip,
				final String name ) {
			this.plan = plan;
			this.expectedNJp = enjp;
			this.expectedNIndividualPlans = enip;
			this.name = name;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	@After
	public void clean() {
		fixtures.clear();
	}

	@Before
	public void fromOneToNone() {
		Map<Id, Plan> plans = new HashMap<Id, Plan>();

		for (int i=0; i < 10; i++) {
			Id id = new IdImpl( i );
			plans.put( id , new PlanImpl( new PersonImpl( id ) ) );
		}

		fixtures.add(
				new Fixture(
					new GroupPlans(
						Collections.singleton( JointPlanFactory.createJointPlan( plans ) ),
						Collections.EMPTY_LIST),
					0,
					plans.size(),
					"from one to no JP"));
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testNumberOfJointPlans() throws Exception {
		SplitJointPlansBasedOnJointTripsAlgorithm algo = new SplitJointPlansBasedOnJointTripsAlgorithm();
		for (Fixture f : fixtures) {
			algo.run( f.plan );
			assertEquals(
					"wrong number of joint plans for <<"+f.name+">>",
					f.expectedNJp,
					f.plan.getJointPlans().size());
		}
	}

	@Test
	public void testNumberOfIndividualPlans() throws Exception {
		SplitJointPlansBasedOnJointTripsAlgorithm algo = new SplitJointPlansBasedOnJointTripsAlgorithm();
		for (Fixture f : fixtures) {
			algo.run( f.plan );
			assertEquals(
					"wrong number of individual plans for <<"+f.name+">>",
					f.expectedNIndividualPlans,
					f.plan.getIndividualPlans().size());
		}
	}
}

