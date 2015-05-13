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
package playground.thibautd.socnetsim.replanning;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.ReplanningContext;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlanFactory;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.framework.replanning.selectors.HighestScoreSumSelector;

/**
 * @author thibautd
 */
public class GroupPlanStrategyTest {
	private static final int N_INITIALLY_JOINT_PLANS = 6;
	private static final int N_INITIALLY_INDIV_PLANS = 8;

	@Test
	public void testNewPlanIsSelected() throws Exception {
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
				if ( plan.isSelected() ) {
					// new plan: selection status inverted
					assertFalse(
							"old plan still selected",
							selectedPlans.contains( plan ));
				}
				else {
					assertTrue(
							"old plan still selected",
							selectedPlans.contains( plan ));
				}
			}
		}
	}

	@Test
	public void testNumberOfPlans() throws Exception {
		final JointPlans jointPlans = new JointPlans();
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new HighestScoreSumSelector(
						new EmptyIncompatiblePlansIdentifierFactory()) );
		strategy.addStrategyModule( new JointStructureInvertingModule( jointPlans.getFactory() ) );

		final ReplanningGroup group = createTestGroup( jointPlans );
		final int groupSize = group.getPersons().size();
		strategy.run( createContext() , jointPlans , Arrays.asList( group ) );

		assertEquals(
				"group size changed by strategy!",
				groupSize,
				group.getPersons().size());
	}

	@Test
	public void testNumberOfSelectedJointPlans() throws Exception {
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
				if (plan.isSelected() && jointPlans.getJointPlan( plan ) != null) {
					countSelectedJoint++;
				}
				if (plan.isSelected() && jointPlans.getJointPlan( plan ) == null) {
					countSelectedIndiv++;
				}
			}
		}

		assertEquals(
				"wrong number of selected plans in joint plans",
				N_INITIALLY_INDIV_PLANS,
				countSelectedJoint );
		assertEquals(
				"wrong number of selected plans in individual plans",
				N_INITIALLY_JOINT_PLANS,
				countSelectedIndiv );
	}

	@Test
	public void testNumberOfNonSelectedJointPlans() throws Exception {
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
				if (!plan.isSelected() && jointPlans.getJointPlan( plan ) != null) {
					countNonSelectedJoint++;
				}
				if (!plan.isSelected() && jointPlans.getJointPlan( plan ) == null) {
					countNonSelectedIndiv++;
				}
			}
		}

		assertEquals(
				"wrong number of non selected plans in joint plans",
				N_INITIALLY_JOINT_PLANS,
				countNonSelectedJoint );
		assertEquals(
				"wrong number of non selected plans in individual plans",
				N_INITIALLY_INDIV_PLANS,
				countNonSelectedIndiv );
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
		Person person = new PersonImpl( id );
		PlanImpl plan = new PlanImpl( person );
		person.addPlan( plan );
		if (joint) jointPlan.put( id , plan );
		if ( !plan.isSelected() ) throw new RuntimeException();

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
