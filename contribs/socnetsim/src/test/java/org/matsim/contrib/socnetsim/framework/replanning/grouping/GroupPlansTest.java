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
package org.matsim.contrib.socnetsim.framework.replanning.grouping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;

/**
 * @author thibautd
 */
public class GroupPlansTest {
	private final List<GroupPlans> testPlans = new ArrayList<GroupPlans>();
	private final JointPlanFactory factory = new JointPlanFactory();

	@BeforeEach
	public void initPlanWithoutJointPlan() {
		final List<Plan> plans = new ArrayList<Plan>();

		for (int i=0; i < 5; i++) {
			plans.add( PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(i, Person.class))) );
		}

		testPlans.add( new GroupPlans( Collections.EMPTY_LIST , plans ) );
	}

	@BeforeEach
	public void initPlanWithoutIndividualPlans() {
		final List<JointPlan> plans = new ArrayList<JointPlan>();

		for (int i=0; i < 5; i++) {
			final Map<Id<Person>, Plan> planMap = new HashMap< >();
			for (int j=0; j < 5; j++) {
				Id id = Id.createPersonId( i + (1000 * j) );
				final Id<Person> id1 = id;
				planMap.put(
						id,
						PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(id1)) );
			}
			plans.add( factory.createJointPlan( planMap ) );
		}

		testPlans.add( new GroupPlans( plans , Collections.EMPTY_LIST ) );
	}

	@AfterEach
	public void clear() {
		testPlans.clear();
	}

	@Test
	void testGetters() throws Exception {
		final List<Plan> indivPlans = new ArrayList<Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		for ( int i = 0; i < 5; i++ ) {
			final Person person = popFact.createPerson( Id.create( "indiv-"+i , Person.class ) );
			final Plan plan = popFact.createPlan();
			person.addPlan( plan );
			plan.setPerson( person );
			indivPlans.add( plan );
		}

		int nIndivInJoints = 0;
		final JointPlanFactory jpFact = new JointPlanFactory();
		for ( int i = 1; i < 5; i++ ) {
			final Map<Id<Person>, Plan> plans = new HashMap< >();
			for ( int j = 0; j < i; j++ ) {
				nIndivInJoints++;
				final Person person = popFact.createPerson( Id.create( "joint-"+i+"-"+j , Person.class ) );
				final Plan plan = popFact.createPlan();
				person.addPlan( plan );
				plan.setPerson( person );
				plans.put( person.getId() , plan );
			}
			jointPlans.add( jpFact.createJointPlan( plans ) );
		}

		final GroupPlans testee = new GroupPlans( jointPlans , indivPlans );

		assertEquals(
				indivPlans.size(),
				testee.getIndividualPlans().size(),
				"wrong number of individual plans");

		assertEquals(
				jointPlans.size(),
				testee.getJointPlans().size(),
				"wrong number of joint plans");

		assertEquals(
				indivPlans.size() + nIndivInJoints,
				testee.getAllIndividualPlans().size(),
				"wrong total number of indiv plans");
	}

	@Test
	void testCopyLooksValid() throws Exception {
		for (GroupPlans plans : testPlans) {
			GroupPlans copy = GroupPlans.copyPlans( factory , plans );

			assertEquals(
					plans.getIndividualPlans().size(),
					copy.getIndividualPlans().size(),
					"wrong number of individual plans in copy");

			assertEquals(
					plans.getJointPlans().size(),
					copy.getJointPlans().size(),
					"wrong number of joint plans in copy");
		}
	}

	@Test
	void testCopyIsNotSame() throws Exception {
		for (GroupPlans plans : testPlans) {
			GroupPlans copy = GroupPlans.copyPlans( factory , plans );

			assertNotSame(
					plans,
					copy,
					"copy is the same instance");

			for (Plan p : plans.getIndividualPlans()) {
				assertFalse(
						copy.getIndividualPlans().contains( p ),
						"the copy contains references from the copied");
			}

			for (JointPlan copiedJointPlan : plans.getJointPlans()) {
				assertFalse(
						copy.getJointPlans().contains( copiedJointPlan ),
						"the copy contains references from the copied");

				JointPlan copyJointPlan = getCopy( copiedJointPlan , copy );
				for (Plan copyIndivPlan : copyJointPlan.getIndividualPlans().values()) {
					// not necessary anymore (factory does not automatically registers in a global container)
					//assertSame(
					//		"wrong joint plan associated to individual plans in copy",
					//		copyJointPlan,
					//		JointPlanFactory.getPlanLinks().getJointPlan( copyIndivPlan ));

					assertFalse(
							copiedJointPlan.getIndividualPlans().values().contains( copyIndivPlan ),
							"individual plans were not copied when copying joint plan");
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

