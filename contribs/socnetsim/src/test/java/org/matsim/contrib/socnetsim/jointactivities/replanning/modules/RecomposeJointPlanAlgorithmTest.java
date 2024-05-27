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
package org.matsim.contrib.socnetsim.jointactivities.replanning.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.RecomposeJointPlanAlgorithm;

/**
 * @author thibautd
 */
public class RecomposeJointPlanAlgorithmTest {
	private static class Fixture {
		public final GroupPlans groupPlans;
		public final Collection<Set<Id<Person>>> expectedJointPlanStructure;
		public final int expectedNJointPlans;
		public final int expectedNIndivPlans;
		public final PlanLinkIdentifier identifier;

		public Fixture(
				final GroupPlans groupPlans,
				final Collection<Set<Id<Person>>> expectedJointPlanStructure) {
			this( groupPlans,
					expectedJointPlanStructure,
					new PlanLinkIdentifier() {
						@Override
						public boolean areLinked(
							final Plan p1,
							final Plan p2) {
							final Id id1 = p1.getPerson().getId();
							final Id id2 = p2.getPerson().getId();

							for (Set<Id<Person>> ids : expectedJointPlanStructure) {
								if (ids.contains( id1 )) return ids.contains( id2 );
							}

							throw new RuntimeException( id1+" not in "+expectedJointPlanStructure );
						}
					});
		}

		public Fixture(
				final GroupPlans groupPlans,
				final Collection<Set<Id<Person>>> expectedJointPlanStructure,
				final PlanLinkIdentifier identifier) {
			this.groupPlans = groupPlans;
			this.expectedJointPlanStructure = expectedJointPlanStructure;
			this.identifier = identifier;

			int jps = 0;
			int indps = 0;
			for (Set<Id<Person>> jp : expectedJointPlanStructure) {
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

		Set<Id<Person>> currentJointPlan = new HashSet< >();
		final List<Set<Id<Person>>> jointPlansToExpect = new ArrayList< >();
		jointPlansToExpect.add( currentJointPlan );

		for (int i=0; i < 100; i++) {
			final Id<Person> id = Id.create( i , Person.class );
			final Person person = PopulationUtils.getFactory().createPerson(id);
			final Plan plan = PopulationUtils.createPlan(person);
			plans.add( plan );

			if (random.nextDouble() < 0.2) {
				currentJointPlan = new HashSet< >();
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

		final Map<Id<Person>, Plan> jointPlan = new HashMap< >();
		Set<Id<Person>> currentJointPlan = new HashSet< >();
		final List<Set<Id<Person>>> jointPlansToExpect = new ArrayList< >();
		jointPlansToExpect.add( currentJointPlan );

		for (int i=0; i < 100; i++) {
			final Id<Person> id = Id.createPersonId( i );
			final Person person = PopulationUtils.getFactory().createPerson(id);
			final Plan plan = PopulationUtils.createPlan(person);
			jointPlan.put( id , plan );

			if (random.nextDouble() < 0.2) {
				currentJointPlan = new HashSet< >();
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
		Map<Id<Person>, Plan> currentJointPlan = new HashMap< >();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();
		final List<Plan> plans = new ArrayList<Plan>();
		Set<Id<Person>> currentExpectedJointPlan = new HashSet< >();
		final List<Set<Id<Person>>> jointPlansToExpect = new ArrayList< >();
		jointPlansToExpect.add( currentExpectedJointPlan );

		for (int i=0; i < 100; i++) {
			final Id<Person> id = Id.createPersonId( i );
			final Person person = PopulationUtils.getFactory().createPerson(id);
			final Plan plan = PopulationUtils.createPlan(person);
			if ( random.nextDouble() < 0.2 ) {
				plans.add( plan );
			}
			else {
				if ( random.nextDouble() < 0.4 ) {
					final JointPlan jp = factory.createJointPlan( currentJointPlan );
					jointPlans.add( jp );
					currentJointPlan = new HashMap< >();
				}
				currentJointPlan.put( id , plan );
			}

			if (random.nextDouble() < 0.2) {
				currentExpectedJointPlan = new HashSet< >();
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

	private Fixture createRandomFixtureWithIncompleteLinks(final Random random) {
		final JointPlanFactory factory = new JointPlanFactory();
		Map<Id<Person>, Plan> currentJointPlan = new HashMap< >();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();
		final List<Plan> plans = new ArrayList<Plan>();
		Set<Id<Person>> currentExpectedJointPlan = new HashSet< >();
		final List<Set<Id<Person>>> jointPlansToExpect = new ArrayList< >();
		jointPlansToExpect.add( currentExpectedJointPlan );
		final Collection<PlanPair> links = new ArrayList<PlanPair>();

		Plan lastPlan = null;
		for (int i=0; i < 100; i++) {
			final Id<Person> id = Id.create( i , Person.class );
			final Person person = PopulationUtils.getFactory().createPerson(id);
			final Plan plan = PopulationUtils.createPlan(person);
			if ( random.nextDouble() < 0.2 ) {
				plans.add( plan );
			}
			else {
				if ( random.nextDouble() < 0.4 ) {
					final JointPlan jp = factory.createJointPlan( currentJointPlan );
					jointPlans.add( jp );
					currentJointPlan = new HashMap< >();
				}
				currentJointPlan.put( id , plan );
			}

			if (random.nextDouble() < 0.2) {
				currentExpectedJointPlan = new HashSet< >();
				jointPlansToExpect.add( currentExpectedJointPlan );
			}

			if ( !currentExpectedJointPlan.isEmpty() ) links.add( new PlanPair( lastPlan , plan ) );
			currentExpectedJointPlan.add( id );
			lastPlan = plan;
		}

		if ( !currentJointPlan.isEmpty() ) {
			final JointPlan jp = factory.createJointPlan( currentJointPlan );
			jointPlans.add( jp );
		}

		return new Fixture(
				new GroupPlans(
					jointPlans,
					plans),
				jointPlansToExpect,
				new PlanLinkIdentifier() {
					@Override
					public boolean areLinked(final Plan p1, final Plan p2) {
						return links.contains( new PlanPair( p1 , p2 ) );
					}
				});
	}

	@Test
	void testIndividualPlans() throws Exception {
		test( createRandomFixtureWithIndividualPlans( new Random( 1234 ) ) );
	}

	@Test
	void testUniqueJointPlan() throws Exception {
		test( createRandomFixtureWithOneBigJointPlan( new Random( 1234 ) ) );
	}

	@Test
	void testJointAndIndividualPlans() throws Exception {
		test( createRandomFixtureWithJointAndIndividualPlans( new Random( 1234 ) ) );
	}

	@Test
	void testIncompleteLinks() throws Exception {
		test( createRandomFixtureWithIncompleteLinks( new Random( 1234 ) ) );
	}

	private static void test( final Fixture fixture ) {
		final RecomposeJointPlanAlgorithm algo =
			new RecomposeJointPlanAlgorithm(
					new JointPlanFactory(),
					fixture.identifier );

		final int initialNPlans = fixture.groupPlans.getAllIndividualPlans().size();
		algo.run( fixture.groupPlans );

		assertEquals(
				initialNPlans,
				fixture.groupPlans.getAllIndividualPlans().size(),
				"unexpected number of plans");

		assertEquals(
				fixture.expectedNJointPlans,
				fixture.groupPlans.getJointPlans().size(),
				"unexpected number of joint plans");

		for (JointPlan jp : fixture.groupPlans.getJointPlans()) {
			final Set<Id<Person>> ids = jp.getIndividualPlans().keySet();
			assertTrue(
					fixture.expectedJointPlanStructure.contains( ids ),
					"unexpected joint plan "+ids+": not in "+fixture.expectedJointPlanStructure);
		}

		assertEquals(
				fixture.expectedNIndivPlans,
				fixture.groupPlans.getIndividualPlans().size(),
				"unexpected number of individual plans");

		for (Plan p : fixture.groupPlans.getIndividualPlans()) {
			final Set<Id<Person>> ids = Collections.singleton( p.getPerson().getId() );
			assertTrue(
					fixture.expectedJointPlanStructure.contains( ids ),
					"unexpected individual plan "+ids+": not in "+fixture.expectedJointPlanStructure);
		}
	}
}

class PlanPair {
	private final Plan p1, p2;

	public PlanPair(final Plan p1, final Plan p2) {
		this.p1 = p1;
		this.p2 = p2;
	}

	@Override
	public boolean equals(final Object o) {
		if ( !(o instanceof PlanPair) ) return false;
		if (p1.equals( ((PlanPair) o).p1 ) && p2.equals( ((PlanPair) o).p2 )) return true;
		if (p1.equals( ((PlanPair) o).p2 ) && p2.equals( ((PlanPair) o).p1 )) return true;
		return false;
	}

	@Override
	public int hashCode() {
		return p1.hashCode() + p2.hashCode();
	}
}
