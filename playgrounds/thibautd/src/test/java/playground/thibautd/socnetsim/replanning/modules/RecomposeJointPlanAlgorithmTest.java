/* *********************************************************************** *
 * project: org.matsim.*
 * RecomposeJointPlanAlgorithmTest.java
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
package playground.thibautd.socnetsim.replanning.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public class RecomposeJointPlanAlgorithmTest {
	private static class Fixture {
		public final GroupPlans groupPlans;
		public final Collection<Set<Id>> expectedJointPlanStructure;
		public final int expectedNJointPlans;
		public final int expectedNIndivPlans;
		public final PlanLinkIdentifier identifier;

		public Fixture(
				final GroupPlans groupPlans,
				final Collection<Set<Id>> expectedJointPlanStructure) {
			this( groupPlans,
					expectedJointPlanStructure,
					new PlanLinkIdentifier() {
						@Override
						public boolean areLinked(
							final Plan p1,
							final Plan p2) {
							final Id id1 = p1.getPerson().getId();
							final Id id2 = p2.getPerson().getId();

							for (Set<Id> ids : expectedJointPlanStructure) {
								if (ids.contains( id1 )) return ids.contains( id2 );
							}

							throw new RuntimeException( id1+" not in "+expectedJointPlanStructure );
						}
					});
		}

		public Fixture(
				final GroupPlans groupPlans,
				final Collection<Set<Id>> expectedJointPlanStructure,
				final PlanLinkIdentifier identifier) {
			this.groupPlans = groupPlans;
			this.expectedJointPlanStructure = expectedJointPlanStructure;
			this.identifier = identifier;

			int jps = 0;
			int indps = 0;
			for (Set<Id> jp : expectedJointPlanStructure) {
				assert jp.size() > 0;
				if (jp.size() > 1) jps++;
				else indps++;
			}
			this.expectedNJointPlans = jps;
			this.expectedNIndivPlans = indps;
		}
	}

	private Fixture createRandomFixtureWithIndividualPlans(final Random random) {
		final List<Plan> plans = new ArrayList<Plan>();

		Set<Id> currentJointPlan = new HashSet<Id>();
		final List<Set<Id>> jointPlansToExpect = new ArrayList<Set<Id>>();
		jointPlansToExpect.add( currentJointPlan );

		for (int i=0; i < 100; i++) {
			final Id id = new IdImpl( i );
			final Person person = new PersonImpl( id );
			final Plan plan = new PlanImpl( person );
			plans.add( plan );

			if (random.nextDouble() < 0.2) {
				currentJointPlan = new HashSet<Id>();
				jointPlansToExpect.add( currentJointPlan );
			}

			currentJointPlan.add( id );
		}

		return new Fixture(
				new GroupPlans(
					Collections.<JointPlan>emptyList(),
					plans),
				jointPlansToExpect);
	}


	private Fixture createRandomFixtureWithOneBigJointPlan(final Random random) {
		final List<Plan> plans = new ArrayList<Plan>();

		final Map<Id, Plan> jointPlan = new HashMap<Id, Plan>();
		Set<Id> currentJointPlan = new HashSet<Id>();
		final List<Set<Id>> jointPlansToExpect = new ArrayList<Set<Id>>();
		jointPlansToExpect.add( currentJointPlan );

		for (int i=0; i < 100; i++) {
			final Id id = new IdImpl( i );
			final Person person = new PersonImpl( id );
			final Plan plan = new PlanImpl( person );
			jointPlan.put( id , plan );

			if (random.nextDouble() < 0.2) {
				currentJointPlan = new HashSet<Id>();
				jointPlansToExpect.add( currentJointPlan );
			}

			currentJointPlan.add( id );
		}

		return new Fixture(
				new GroupPlans(
					Collections.singleton(
						new JointPlanFactory().createJointPlan(
							jointPlan ) ),
					plans),
				jointPlansToExpect);
	}

	private Fixture createRandomFixtureWithJointAndIndividualPlans(final Random random) {
		final JointPlanFactory factory = new JointPlanFactory();
		Map<Id, Plan> currentJointPlan = new HashMap<Id, Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();
		final List<Plan> plans = new ArrayList<Plan>();
		Set<Id> currentExpectedJointPlan = new HashSet<Id>();
		final List<Set<Id>> jointPlansToExpect = new ArrayList<Set<Id>>();
		jointPlansToExpect.add( currentExpectedJointPlan );

		for (int i=0; i < 100; i++) {
			final Id id = new IdImpl( i );
			final Person person = new PersonImpl( id );
			final Plan plan = new PlanImpl( person );
			if ( random.nextDouble() < 0.2 ) {
				plans.add( plan );
			}
			else {
				if ( random.nextDouble() < 0.4 ) {
					final JointPlan jp = factory.createJointPlan( currentJointPlan );
					jointPlans.add( jp );
					currentJointPlan = new HashMap<Id, Plan>();
				}
				currentJointPlan.put( id , plan );
			}

			if (random.nextDouble() < 0.2) {
				currentExpectedJointPlan = new HashSet<Id>();
				jointPlansToExpect.add( currentExpectedJointPlan );
			}

			currentExpectedJointPlan.add( id );
		}

		if ( !currentJointPlan.isEmpty() ) {
			final JointPlan jp = factory.createJointPlan( currentJointPlan );
			jointPlans.add( jp );
		}

		return new Fixture(
				new GroupPlans(
					jointPlans,
					plans),
				jointPlansToExpect);
	}

	@Test
	public void testIndividualPlans() throws Exception {
		test( createRandomFixtureWithIndividualPlans( new Random( 1234 ) ) );
	}

	@Test
	public void testUniqueJointPlan() throws Exception {
		test( createRandomFixtureWithOneBigJointPlan( new Random( 1234 ) ) );
	}

	@Test
	public void testJointAndIndividualPlans() throws Exception {
		test( createRandomFixtureWithJointAndIndividualPlans( new Random( 1234 ) ) );
	}

	private static void test( final Fixture fixture ) {
		final RecomposeJointPlanAlgorithm algo =
			new RecomposeJointPlanAlgorithm(
					new JointPlanFactory(),
					fixture.identifier );

		algo.run( fixture.groupPlans );

		assertEquals(
				"unexpected number of joint plans",
				fixture.expectedNJointPlans,
				fixture.groupPlans.getJointPlans().size());

		for (JointPlan jp : fixture.groupPlans.getJointPlans()) {
			final Set<Id> ids = jp.getIndividualPlans().keySet();
			assertTrue(
					"unexpected joint plan "+ids+": not in "+fixture.expectedJointPlanStructure,
					fixture.expectedJointPlanStructure.contains( ids ));
		}

		assertEquals(
				"unexpected number of individual plans",
				fixture.expectedNIndivPlans,
				fixture.groupPlans.getIndividualPlans().size());

		for (Plan p : fixture.groupPlans.getIndividualPlans()) {
			final Set<Id> ids = Collections.singleton( p.getPerson().getId() );
			assertTrue(
					"unexpected individual plan "+ids+": not in "+fixture.expectedJointPlanStructure,
					fixture.expectedJointPlanStructure.contains( ids ));
		}
	}
}

