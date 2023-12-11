/* *********************************************************************** *
 * project: org.matsim.*
 * LeastPointedPlanPruningConflictSolver.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;
import org.matsim.core.population.PopulationUtils;

/**
 * @author thibautd
 */
public class LeastPointedPlanPruningConflictSolverTest {

	@Test
	void testPruneSmallestJointPlan() {
		final JointPlans jointPlans = new JointPlans();
		final Map<Id<Person>, Plan> smallJp = new HashMap< >();
		final Map<Id<Person>, Plan> bigJp = new HashMap< >();

		final ReplanningGroup group = new ReplanningGroup();

		Id<Person> id = Id.createPersonId( 1 );
		{
			final Id<Person> id1 = id;
			final Person person = PopulationUtils.getFactory().createPerson(id1);
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 0d );
				person.addPlan( plan );
				smallJp.put( id , plan );
			}
		}

		id = Id.createPersonId( 2 );
		{
			final Id<Person> id1 = id;
			final Person person = PopulationUtils.getFactory().createPerson(id1);
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 0d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1d );
				person.addPlan( plan );
				smallJp.put( id , plan );
			}
		}

		id = Id.createPersonId( 3 );
		{
			final Id<Person> id1 = id;
			final Person person = PopulationUtils.getFactory().createPerson(id1);
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 0d );
				person.addPlan( plan );
			}
		}

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan(
					bigJp ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan(
					smallJp ) );

		test( new ConflictSolverTestsFixture(
					jointPlans,
					group,
					smallJp.values() ) );
	}

	private static void test(final ConflictSolverTestsFixture fixture) {
		final ConflictSolver testee = new LeastPointedPlanPruningConflictSolver();

		testee.attemptToSolveConflicts( fixture.recordsPerJointPlan );

		for ( PlanRecord r : fixture.allRecords ) {
			if ( fixture.expectedUnfeasiblePlans.contains( r.getPlan() ) ) {
				Assertions.assertFalse(
						r.isFeasible(),
						"plan "+r.getPlan()+" unexpectedly feasible" );
			}
			else {
				Assertions.assertTrue(
						r.isFeasible(),
						"plan "+r.getPlan()+" unexpectedly unfeasible" );
			}
		}
	}
}

