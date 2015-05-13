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
package playground.thibautd.socnetsim.replanning.selectors;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.RandomGroupLevelSelector;

/**
 * @author thibautd
 */
public class RandomSelectorsTest {
	private static interface SelectorFactory {
		public GroupLevelPlanSelector create(Random r);
	}

	private final List<ReplanningGroup> testGroups = new ArrayList<ReplanningGroup>();
	private JointPlans jointPlans = new JointPlans();

	@After
	public void clear() {
		testGroups.clear();
		jointPlans = new JointPlans();
	}

	@Before
	public void createIndividualPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		PersonImpl person = new PersonImpl( Id.create( "tintin" , Person.class ) );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		person = new PersonImpl( Id.create( "milou" , Person.class ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );

		person = new PersonImpl( Id.create( "tim" , Person.class ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		person = new PersonImpl( Id.create( "struppy" , Person.class ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
	}

	@Before
	public void createFullyJointPlans() {
		ReplanningGroup group = new ReplanningGroup();
		testGroups.add( group );

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
	}

	@Before
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
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 20d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 30d );
		jp5.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 40d );
		jp7.put( id , plan );

		id = Id.createPersonId( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -200d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -100d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 500d );
		jp5.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 200d );
		jp8.put( id , plan );

		id = Id.createPersonId( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 11d );
		jp6.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 101d );
		jp7.put( id , plan );

		id = Id.createPersonId( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 333d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 666d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 777d );
		jp6.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 444d );
		jp8.put( id , plan );

		id = Id.createPersonId( "haddock" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 500d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100d );
		plan = person.createAndAddPlan( false );
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

	@Test
	public void testRandomSelector() throws Exception {
		testDeterminism( new SelectorFactory() {
			@Override
			public GroupLevelPlanSelector create(final Random r) {
				return new RandomGroupLevelSelector(
						r,
						new EmptyIncompatiblePlansIdentifierFactory());
			}
		});
	}

	private void testDeterminism(
			final SelectorFactory factory) {
		final int seed = 1264;

		final Counter count = new Counter( "selection # " );
		for (ReplanningGroup group : testGroups) {
			GroupPlans previous = null;
			for (int i=0; i<100; i++) {
				count.incCounter();
				GroupLevelPlanSelector selector = factory.create( new Random( seed ) );

				final GroupPlans selected = selector.selectPlans(
						jointPlans , group );
				if (previous != null) {
					assertEquals(
							"different results with the same random seed",
							previous,
							selected);
				}

				if (selected == null) throw new NullPointerException( "test is useless if the selector returns null" );
				previous = selected;
			}
		}
		count.printCounter();
	}
}

