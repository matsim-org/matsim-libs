/* *********************************************************************** *
 * project: org.matsim.*
 * WhoIsTheBossSelectorTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.whoisthebossselector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.ScoreWeight;

/**
 * @author thibautd
 */
public class WhoIsTheBossSelectorTest {

	@Test
	void testOnePlanSelectedForEachAgent() throws Exception {
		final WhoIsTheBossSelector testee =
			new WhoIsTheBossSelector(
					new Random( 9087 ),
					new EmptyIncompatiblePlansIdentifierFactory(),
					new ScoreWeight() );

		final Iterator<Id<Person>> ids = new IdIterator();
		final Random random = new Random( 2314 );
		final JointPlans jointPlans = new JointPlans();
		int countNull = 0;
		int countNonNull = 0;
		final Counter counter = new Counter( "test random clique # " );
		for ( int i = 0; i < 100; i++ ) {
			counter.incCounter();
			final ReplanningGroup group =
				createNextTestClique(
					ids,
					jointPlans,
					random );

			final GroupPlans selected = testee.selectPlans( jointPlans , group );

			if ( selected == null ) {
				countNull++;
			}
			else {
				countNonNull++;
				Assertions.assertEquals(
						group.getPersons().size(),
						selected.getAllIndividualPlans().size(),
						"unexpected number of plans in selected plan" );

				final Set<Id> groupIds = getGroupIds( group );
				final Set<Id> selectedIds = getPlanIds( selected );
				Assertions.assertEquals(
						groupIds,
						selectedIds,
						"unexpected agent ids in selected plan" );
			}
		}
		counter.printCounter();

		if ( countNull == 0 || countNonNull == 0 ) {
			throw new RuntimeException();
		}
	}

	private Set<Id> getPlanIds(final GroupPlans selected) {
		final Set<Id> ids = new HashSet<Id>();
		for ( Plan p : selected.getAllIndividualPlans() ) {
			ids.add( p.getPerson().getId() );
		}
		return ids;
	}

	private Set<Id> getGroupIds(final ReplanningGroup group) {
		final Set<Id> ids = new HashSet<Id>();
		for ( Person p : group.getPersons() ) {
			ids.add( p.getId() );
		}
		return ids;
	}

	@Test
	@Disabled("TODO")
	void testBestPlanIsSelectedIfPossible() throws Exception {
		throw new UnsupportedOperationException( "TODO" );
	}

	private static ReplanningGroup createNextTestClique(
			final Iterator<Id<Person>> idsIterator,
			final JointPlans jointPlans,
			final Random random) {
		// attempt to get a high diversity of joint structures.
		final int nMembers = 1 + random.nextInt( 20 );
		final int nPlans = 1 + random.nextInt( 10 );
		// this is the max number of attempts to create a joint plan
		final int maxJointPlans = 1000;
		final double pJoin = random.nextDouble();
		final ReplanningGroup group = new ReplanningGroup();
		final PopulationFactory factory = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();

		final Map<Id<Person>, Queue<Plan>> plansPerPerson = new LinkedHashMap< >();

		// create plans
		for (int j=0; j < nMembers; j++) {
			final Id<Person> id = idsIterator.next();
			final Person person = factory.createPerson( id );
			group.addPerson( person );
			for (int k=0; k < nPlans; k++) {
				final Plan plan = factory.createPlan();
				plan.setPerson( person );
				person.addPlan( plan );
				plan.setScore( random.nextDouble() * 1000 );
			}
			plansPerPerson.put( id , new LinkedList<Plan>( person.getPlans() ) );
		}

		// join plans randomly
		final int nJointPlans = random.nextInt( maxJointPlans );
		for (int p=0; p < nJointPlans; p++) {
			final Map<Id<Person>, Plan> jointPlan = new LinkedHashMap< >();
			for (Queue<Plan> plans : plansPerPerson.values()) {
				if ( random.nextDouble() > pJoin ) continue;
				final Plan plan = plans.poll();
				if (plan != null) jointPlan.put( plan.getPerson().getId() , plan );
			}
			if (jointPlan.size() <= 1) continue;
			jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jointPlan ) );
		}

		return group;
	}

	private static class IdIterator implements Iterator<Id<Person>> {
			@Override
			public boolean hasNext() {
				return true;
			}

			int currentId=0;
			@Override
			public Id<Person> next() {
				return Id.createPersonId( currentId++ );
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
	}
}

