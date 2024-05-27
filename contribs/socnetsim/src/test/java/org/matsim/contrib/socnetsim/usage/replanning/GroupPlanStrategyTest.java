/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.replanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.GenericStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.HighestScoreSumSelector;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author thibautd
 */
public class GroupPlanStrategyTest {
	private static final int N_INITIALLY_JOINT_PLANS = 6;
	private static final int N_INITIALLY_INDIV_PLANS = 8;

	@Test
	void testNewPlanIsSelected() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new HighestScoreSumSelector(
						new EmptyIncompatiblePlansIdentifierFactory()) );
		strategy.addStrategyModule( new JointStructureInvertingModule( jointPlans.getFactory() ) );

		final List<Plan> selectedPlans = new ArrayList<Plan>();
		final ReplanningGroup group = createTestGroup( jointPlans );
		for (Person p : group.getPersons()) {
			selectedPlans.add( p.getSelectedPlan() );
		}

		strategy.run( createContext() , jointPlans , Arrays.asList( group ) );
		for ( Person person : group.getPersons() ) {
			for ( Plan plan : person.getPlans() ) {
				if ( PersonUtils.isSelected(plan) ) {
					// new plan: selection status inverted
					assertFalse(
							selectedPlans.contains( plan ),
							"old plan still selected");
				}
				else {
					assertTrue(
							selectedPlans.contains( plan ),
							"old plan still selected");
				}
			}
		}
	}

	@Test
	void testNumberOfPlans() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new HighestScoreSumSelector(
						new EmptyIncompatiblePlansIdentifierFactory()) );
		strategy.addStrategyModule( new JointStructureInvertingModule( jointPlans.getFactory() ) );

		final ReplanningGroup group = createTestGroup( jointPlans );
		final int groupSize = group.getPersons().size();
		strategy.run( createContext() , jointPlans , Arrays.asList( group ) );

		assertEquals(
				groupSize,
				group.getPersons().size(),
				"group size changed by strategy!");
	}

	@Test
	void testNumberOfSelectedJointPlans() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new HighestScoreSumSelector(
						new EmptyIncompatiblePlansIdentifierFactory()) );
		strategy.addStrategyModule( new JointStructureInvertingModule( jointPlans.getFactory() ) );

		final ReplanningGroup group = createTestGroup( jointPlans );
		strategy.run( createContext() , jointPlans , Arrays.asList( group ) );

		int countSelectedJoint = 0;
		int countSelectedIndiv = 0;
		for ( Person person : group.getPersons() ) {
			for (Plan plan : person.getPlans()) {
				if (PersonUtils.isSelected(plan) && jointPlans.getJointPlan( plan ) != null) {
					countSelectedJoint++;
				}
				if (PersonUtils.isSelected(plan) && jointPlans.getJointPlan( plan ) == null) {
					countSelectedIndiv++;
				}
			}
		}

		assertEquals(
				N_INITIALLY_INDIV_PLANS,
				countSelectedJoint,
				"wrong number of selected plans in joint plans" );
		assertEquals(
				N_INITIALLY_JOINT_PLANS,
				countSelectedIndiv,
				"wrong number of selected plans in individual plans" );
	}

	@Test
	void testNumberOfNonSelectedJointPlans() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new HighestScoreSumSelector(
						new EmptyIncompatiblePlansIdentifierFactory()) );
		strategy.addStrategyModule( new JointStructureInvertingModule( jointPlans.getFactory() ) );

		final ReplanningGroup group = createTestGroup( jointPlans );
		strategy.run( createContext() , jointPlans , Arrays.asList( group ) );

		int countNonSelectedJoint = 0;
		int countNonSelectedIndiv = 0;
		for ( Person person : group.getPersons() ) {
			for (Plan plan : person.getPlans()) {
				if (!PersonUtils.isSelected(plan) && jointPlans.getJointPlan( plan ) != null) {
					countNonSelectedJoint++;
				}
				if (!PersonUtils.isSelected(plan) && jointPlans.getJointPlan( plan ) == null) {
					countNonSelectedIndiv++;
				}
			}
		}

		assertEquals(
				N_INITIALLY_JOINT_PLANS,
				countNonSelectedJoint,
				"wrong number of non selected plans in joint plans" );
		assertEquals(
				N_INITIALLY_INDIV_PLANS,
				countNonSelectedIndiv,
				"wrong number of non selected plans in individual plans" );
	}

	private ReplanningGroup createTestGroup(final JointPlans jointPlans) {
		final ReplanningGroup group = new ReplanningGroup();

		final Map<Id<Person>, Plan> jointPlan = new LinkedHashMap< >();

		int i=0;
		for (int j=0; j < N_INITIALLY_JOINT_PLANS; j++) {
			group.addPerson( createPerson( i++ , true , jointPlan ) );
		}


		for (int j=0; j < N_INITIALLY_INDIV_PLANS; j++) {
			group.addPerson( createPerson( i++ , false , jointPlan ) );
		}

		if ( jointPlan.size() != N_INITIALLY_JOINT_PLANS ) {
			// this is basically an assertion, but I want an error,
			// not a failure in this case (it indicates a bug in the test)
			throw new RuntimeException();
		}

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jointPlan ));

		return group;
	}

	private static Person createPerson(
			final int count,
			final boolean joint,
			final Map<Id<Person>, Plan> jointPlan) {
		Id<Person> id = Id.createPersonId( count );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		Plan plan = PopulationUtils.createPlan(person);
		person.addPlan( plan );
		if (joint) jointPlan.put( id , plan );
		if ( !PersonUtils.isSelected(plan) ) throw new RuntimeException();

		return person;
	}

	private static ReplanningContext createContext() {
		return null;
	}

	private static class JointStructureInvertingModule implements GenericStrategyModule<GroupPlans> {
		private final JointPlanFactory factory;

		public JointStructureInvertingModule(final JointPlanFactory factory) {
			this.factory = factory;
		}

		@Override
		public void handlePlans(
				final ReplanningContext replanningContext,
				final Collection<GroupPlans> groupPlans) {
			for (GroupPlans plans : groupPlans) handlePlans( plans );
		}

		private void handlePlans(final GroupPlans plans) {
			final Map<Id<Person>, Plan> newJointPlan = new LinkedHashMap<Id<Person>, Plan>();
			final List<Plan> newIndividualPlans = new ArrayList<Plan>();

			for (JointPlan jp : plans.getJointPlans()) {
				newIndividualPlans.addAll( jp.getIndividualPlans().values() );
			}

			for (Plan p : plans.getIndividualPlans()) {
				newJointPlan.put( p.getPerson().getId() , p );
			}

			plans.clear();
			plans.addJointPlan( factory.createJointPlan( newJointPlan ) );
			plans.addIndividualPlans( newIndividualPlans );
		}
	}
}
