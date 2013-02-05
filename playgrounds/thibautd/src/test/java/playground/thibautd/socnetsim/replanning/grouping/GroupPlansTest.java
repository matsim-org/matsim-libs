/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlansTest.java
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
package playground.thibautd.socnetsim.replanning.grouping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;

/**
 * @author thibautd
 */
public class GroupPlansTest {
	private final List<GroupPlans> testPlans = new ArrayList<GroupPlans>();
	private final JointPlanFactory factory = new JointPlanFactory();

	@Before
	public void initPlanWithoutJointPlan() {
		final List<Plan> plans = new ArrayList<Plan>();

		for (int i=0; i < 5; i++) {
			plans.add( new PlanImpl( new PersonImpl( new IdImpl( i ) ) ) );
		}

		testPlans.add( new GroupPlans( Collections.EMPTY_LIST , plans ) );
	}

	@Before
	public void initPlanWithoutIndividualPlans() {
		final List<JointPlan> plans = new ArrayList<JointPlan>();

		for (int i=0; i < 5; i++) {
			final Map<Id, Plan> planMap = new HashMap<Id, Plan>();
			for (int j=0; j < 5; j++) {
				Id id = new IdImpl( i + (1000 * j) );
				planMap.put(
						id,
						new PlanImpl(
							new PersonImpl(
								id ) ) );
			}
			plans.add( factory.createJointPlan( planMap ) );
		}

		testPlans.add( new GroupPlans( plans , Collections.EMPTY_LIST ) );
	}

	@After
	public void clear() {
		testPlans.clear();
	}

	@Test
	public void testCopyLooksValid() throws Exception {
		for (GroupPlans plans : testPlans) {
			GroupPlans copy = GroupPlans.copyPlans( factory , plans );

			assertEquals(
					"wrong number of individual plans in copy",
					plans.getIndividualPlans().size(),
					copy.getIndividualPlans().size());

			assertEquals(
					"wrong number of joint plans in copy",
					plans.getJointPlans().size(),
					copy.getJointPlans().size());
		}
	}

	@Test
	public void testCopyIsNotSame() throws Exception {
		for (GroupPlans plans : testPlans) {
			GroupPlans copy = GroupPlans.copyPlans( factory , plans );

			assertNotSame(
					"copy is the same instance",
					plans,
					copy);

			for (Plan p : plans.getIndividualPlans()) {
				assertFalse(
						"the copy contains references from the copied",
						copy.getIndividualPlans().contains( p ));
			}

			for (JointPlan copiedJointPlan : plans.getJointPlans()) {
				assertFalse(
						"the copy contains references from the copied",
						copy.getJointPlans().contains( copiedJointPlan ));

				JointPlan copyJointPlan = getCopy( copiedJointPlan , copy );
				for (Plan copyIndivPlan : copyJointPlan.getIndividualPlans().values()) {
					// not necessary anymore (factory does not automatically registers in a global container)
					//assertSame(
					//		"wrong joint plan associated to individual plans in copy",
					//		copyJointPlan,
					//		JointPlanFactory.getPlanLinks().getJointPlan( copyIndivPlan ));

					assertFalse(
							"individual plans were not copied when copying joint plan",
							copiedJointPlan.getIndividualPlans().values().contains( copyIndivPlan ));
				}
			}
		}
	}

	private JointPlan getCopy(
			final JointPlan copiedJointPlan,
			final GroupPlans copy) {
		for (JointPlan jp : copy.getJointPlans()) {
			if (jp.getIndividualPlans().keySet().equals( copiedJointPlan.getIndividualPlans().keySet() )) {
				return jp;
			}
		}
		throw new RuntimeException();
	}
}

