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
package playground.thibautd.socnetsim.replanning.removers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class LexicographicRemoverTest {

	@Test
	public void testOnlyIndividualPlans() {
		final Map<Id, Plan> toRemove = new LinkedHashMap<Id, Plan>();
		final ReplanningGroup group = new ReplanningGroup();
		final JointPlans jointPlans = new JointPlans();

		for ( int i=0; i < 4; i++ ) {
			final Person person = new PersonImpl( new IdImpl( i ) );
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
	public void testOnlyOneComposition() {
		final ReplanningGroup group = new ReplanningGroup();

		for ( int i=0; i < 4; i++ ) {
			group.addPerson( new PersonImpl( new IdImpl( i ) ) );
		}
		
		final JointPlans jointPlans = new JointPlans();

		final JointPlan toRemove =
				createJointPlan(
					jointPlans.getFactory(),
					(List<Person>) group.getPersons(),
					1, 1, 2, 3 );
		jointPlans.addJointPlan( toRemove );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					(List<Person>) group.getPersons(),
					2, 2, 1, 2 ) );
		jointPlans.addJointPlan(
				createJointPlan(
					jointPlans.getFactory(),
					(List<Person>) group.getPersons(),
					3, 3, 3, 1 ) );

		test( new Fixture(
					2, 2,
					jointPlans,
					group,
					toRemove.getIndividualPlans() ) );
	}

	private static JointPlan createJointPlan(
			final JointPlanFactory factory,
			final List<Person> persons,
			final double... scores) {
		final Map<Id, Plan> plans = new LinkedHashMap<Id, Plan>();

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
			Assert.assertFalse(
					expectedRemoved+" not removed for person "+p,
					p.getPlans().contains( expectedRemoved ) );
		}
	}

	private static class Fixture {
		public final int maxPerComposition;
		public final int maxPerAgent;
		public final JointPlans jointPlans;
		public final ReplanningGroup group;
		public final Map<Id, Plan> expectedRemovedPlans;

		public Fixture(
				final int maxPerComposition,
				final int maxPerAgent,
				final JointPlans jointPlans,
				final ReplanningGroup group,
				final Map<Id, Plan> expectedRemovedPlans) {
			this.maxPerComposition = maxPerComposition;
			this.maxPerAgent = maxPerAgent;
			this.jointPlans = jointPlans;
			this.group = group;
			this.expectedRemovedPlans = expectedRemovedPlans;
		}
	}
}

