/* *********************************************************************** *
 * project: org.matsim.*
 * LexicographicRemoverTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.replanning.removers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.ExtraPlanRemover;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.core.population.PopulationUtils;

/**
 * @author thibautd
 */
public class LexicographicRemoverTest {

	@Test
	void testOnlyIndividualPlans() {
		final Map<Id<Person>, Plan> toRemove = new LinkedHashMap< >();
		final ReplanningGroup group = new ReplanningGroup();
		final JointPlans jointPlans = new JointPlans();

		for ( int i=0; i < 4; i++ ) {
			final Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			group.addPerson( person );

			for ( double score = 0; score < 3; score++ ) {
				final Plan plan = jointPlans.getFactory().createIndividualPlan(  person );
				plan.setScore( score );
				person.addPlan( plan );
				if ( score == 0 ) toRemove.put( person.getId() , plan );
			}
		}
		
		test( new Fixture(
					2, 2,
					jointPlans,
					group,
					toRemove ) );

	}

	@Test
	void testOnlyOneComposition() {
		final ReplanningGroup group = new ReplanningGroup();

		for ( int i=0; i < 4; i++ ) {
			group.addPerson(PopulationUtils.getFactory().createPerson(Id.create(i, Person.class)));
		}
		
		final JointPlans jointPlans = new JointPlans();

		final JointPlan toRemove =
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					1, 1, 2, 3 );
		jointPlans.addJointPlan( toRemove );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					2, 2, 1, 2 ) );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					3, 3, 3, 1 ) );

		test( new Fixture(
					2, 2,
					jointPlans,
					group,
					toRemove.getIndividualPlans() ) );
	}

	@Test
	void testOneCompositionAndOneExcedentaryPlan() {
		final ReplanningGroup group = new ReplanningGroup();

		for ( int i=0; i < 4; i++ ) {
			group.addPerson(PopulationUtils.getFactory().createPerson(Id.create(i, Person.class)));
		}
		
		final JointPlans jointPlans = new JointPlans();

		final JointPlan toRemove =
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					1, 1, 2, 3 );
		jointPlans.addJointPlan( toRemove );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					2, 2, 1, 2 ) );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					group.getPersons(),
					3, 3, 3, 1 ) );

		final Person p = group.getPersons().get( 0 );
		final Plan indivPlan = jointPlans.getFactory().createIndividualPlan( p );
		indivPlan.setScore( 0d );
		p.addPlan( indivPlan );

		test( new Fixture(
					3, 3,
					jointPlans,
					group,
					toRemove.getIndividualPlans() ) );

	}

	private static JointPlan createJointPlan(
			final JointPlanFactory factory,
			final List<Person> persons,
			final double... scores) {
		final Map<Id<Person>, Plan> plans = new LinkedHashMap< >();

		if ( scores.length != persons.size() ) throw new IllegalArgumentException();

		for ( int i=0; i < scores.length; i++ ) {
			final Person person = persons.get( i );

			final Plan plan = factory.createIndividualPlan( person );
			person.addPlan( plan );
			plan.setScore( scores[ i ] );

			plans.put( person.getId() , plan );
		}

		return factory.createJointPlan( plans );
	}

	private static void test(final Fixture f) {
		final ExtraPlanRemover remover =
			new LexicographicForCompositionExtraPlanRemover(
					f.maxPerComposition,
					f.maxPerAgent );

		remover.removePlansInGroup(
				f.jointPlans,
				f.group );

		for ( Person p : f.group.getPersons() ) {
			final Plan expectedRemoved = f.expectedRemovedPlans.get( p.getId() );
			Assertions.assertFalse(
					p.getPlans().contains( expectedRemoved ),
					expectedRemoved+" not removed for person "+p );

			Assertions.assertNull(
					f.jointPlans.getJointPlan( expectedRemoved ),
					"MEMORY LEAK: There is still a joint plan associated to removed plan "+expectedRemoved );
		}
	}

	private static class Fixture {
		public final int maxPerComposition;
		public final int maxPerAgent;
		public final JointPlans jointPlans;
		public final ReplanningGroup group;
		public final Map<Id<Person>, Plan> expectedRemovedPlans;

		public Fixture(
				final int maxPerComposition,
				final int maxPerAgent,
				final JointPlans jointPlans,
				final ReplanningGroup group,
				final Map<Id<Person>, Plan> expectedRemovedPlans) {
			this.maxPerComposition = maxPerComposition;
			this.maxPerAgent = maxPerAgent;
			this.jointPlans = jointPlans;
			this.group = group;
			this.expectedRemovedPlans = expectedRemovedPlans;
		}
	}
}

