/* *********************************************************************** *
 * project: org.matsim.*
 * RandomSelectorsTest.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection.RandomGroupLevelSelector;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.Counter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class RandomSelectorsTest {

	private final List<ReplanningGroup> testGroups = new ArrayList<ReplanningGroup>();
	private JointPlans jointPlans = new JointPlans();

	@AfterEach
	public void clear() {
		testGroups.clear();
		jointPlans = new JointPlans();
	}

	@BeforeEach
	public void createIndividualPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		Person person = PopulationUtils.getFactory().createPerson(Id.create("tintin", Person.class));
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		person = PopulationUtils.getFactory().createPerson(Id.create("milou", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );

		person = PopulationUtils.getFactory().createPerson(Id.create("tim", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );

		person = PopulationUtils.getFactory().createPerson(Id.create("struppy", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
	}

	@BeforeEach
	public void createFullyJointPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -10d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp3.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
	}

	@BeforeEach
	public void createPartiallyJointPlansMessOfJointPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();
		Map<Id<Person>, Plan> jp4 = new HashMap< >();
		Map<Id<Person>, Plan> jp5 = new HashMap< >();
		Map<Id<Person>, Plan> jp6 = new HashMap< >();
		Map<Id<Person>, Plan> jp7 = new HashMap< >();
		Map<Id<Person>, Plan> jp8 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 20d );
		jp3.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 30d );
		jp5.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 40d );
		jp7.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -200d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -100d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 500d );
		jp5.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 200d );
		jp8.put( id , plan );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 100d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 11d );
		jp6.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 101d );
		jp7.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 333d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 666d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 777d );
		jp6.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 444d );
		jp8.put( id , plan );

		id = Id.createPersonId( "haddock" );
		final Id<Person> id5 = id;
		person = PopulationUtils.getFactory().createPerson(id5);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 500d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );
		jp3.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 100d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp5.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp4 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp5 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp6 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp7 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp8 ) );
	}

	@BeforeEach
	public void createPartiallyJointPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 145d );
		plan = PersonUtils.createAndAddPlan(person, true);
		plan.setScore( 142d );
		jp1.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 116d );
		plan = PersonUtils.createAndAddPlan(person, true);
		plan.setScore( 115d );
		jp1.put( id , plan );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 150.6 );
		plan = PersonUtils.createAndAddPlan(person, true);
		plan.setScore( 150.8 );
		jp2.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 171.7 );
		plan = PersonUtils.createAndAddPlan(person, true);
		plan.setScore( 171.5 );
		jp2.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
	}

	@BeforeEach
	public void createRandomFixtures() {
		final Random random = new Random( 42 );
		for ( int i=0; i < 100; i++ ) {
			ReplanningGroup group = new ReplanningGroup();
			testGroups.add( group );

			final List<List<Plan>> planLists = new ArrayList<>();
			for ( int pNr=0; pNr < 5; pNr++ ) {
				Person person = PopulationUtils.getFactory().createPerson( Id.create( pNr , Person.class ) );
				group.addPerson( person );

				final List<Plan> plans = new ArrayList<>();
				planLists.add( plans );

				for ( int planNr=0; planNr < 5; planNr++ ) {
					final Plan plan = PopulationUtils.getFactory().createPlan();
					plan.setScore( random.nextDouble() );
					person.addPlan( plan );
					// keep one individual plan
					if ( planNr > 0 ) plans.add( plan );
				}
			}

			// create random joint plans.
			while ( !planLists.isEmpty() ) {
				int n = 1 + random.nextInt( planLists.size() );
				final Map<Id<Person>,Plan> jp = new HashMap<>();

				Collections.shuffle( planLists , random );
				final Iterator<List<Plan>> it = planLists.iterator();
				while ( n-- > 0 ) {
					final List<Plan> list = it.next();

					final Plan plan = list.remove( 0 );
					if ( list.isEmpty() ) it.remove();

					jp.put( plan.getPerson().getId() , plan );
				}

				jointPlans.addJointPlan(
						jointPlans.getFactory().createJointPlan( jp ) );
			}
		}
	}

	@Test
	void testDeterminism() throws Exception {
		final int seed = 1264;

		final Counter count = new Counter( "selection # " );
		for (ReplanningGroup group : testGroups) {
			GroupPlans previous = null;
			for (int i=0; i<100; i++) {
				count.incCounter();
				final GroupLevelPlanSelector selector =
						new RandomGroupLevelSelector(
								new Random( seed ),
								new EmptyIncompatiblePlansIdentifierFactory() );

				final GroupPlans selected = selector.selectPlans(
						jointPlans , group );
				if (previous != null) {
					assertEquals(
							previous,
							selected,
							"different results with the same random seed");
				}

				if (selected == null) throw new NullPointerException( "test is useless if the selector returns null" );
				previous = selected;
			}
		}
		count.printCounter();
	}

	@Test
	void testNoFailuresWithVariousSeeds() throws Exception {
		final RandomGroupLevelSelector selector = new RandomGroupLevelSelector(
				new Random( 123 ),
				new EmptyIncompatiblePlansIdentifierFactory());

		final Counter count = new Counter( "selection # " );
		final Counter groupCount = new Counter( "group # " );
		for (ReplanningGroup group : testGroups) {
			groupCount.incCounter();
			for (int i=0; i<500; i++) {
				count.incCounter();

				final GroupPlans selected = selector.selectPlans(
						jointPlans , group );

				if (selected == null) throw new NullPointerException( "test is useless if the selector returns null" );

				Assertions.assertEquals( selected.getAllIndividualPlans().size(),
						group.getPersons().size(),
						"unexpected selected plan size" );
			}
		}
		groupCount.printCounter();
		count.printCounter();
	}

}

