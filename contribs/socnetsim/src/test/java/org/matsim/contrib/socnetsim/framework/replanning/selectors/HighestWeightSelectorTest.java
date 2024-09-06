/* *********************************************************************** *
 * project: org.matsim.*
 * HighestWeightSelectorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.contrib.socnetsim.framework.cliques.Clique;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class HighestWeightSelectorTest {
	private static final Logger log =
		LogManager.getLogger(HighestWeightSelectorTest.class);

	public static class Fixture {
		final String name;
		final ReplanningGroup group;
		final GroupPlans expectedSelectedPlans;
		final GroupPlans expectedSelectedPlansWhenBlocking;
		final GroupPlans expectedSelectedPlansWhenForbidding;
		final JointPlans jointPlans;
		final IncompatiblePlansIdentifierFactory forbidder;

		public Fixture(
				final String name,
				final ReplanningGroup group,
				final GroupPlans expectedPlans,
				final GroupPlans expectedSelectedPlansWhenBlocking,
				final GroupPlans expectedSelectedPlansWhenForbidding,
				final IncompatiblePlansIdentifierFactory forbiddenPlans,
				final JointPlans jointPlans) {
			this.name = name;
			this.group = group;
			this.expectedSelectedPlans = expectedPlans;
			this.expectedSelectedPlansWhenBlocking = expectedSelectedPlansWhenBlocking;
			this.expectedSelectedPlansWhenForbidding = expectedSelectedPlansWhenForbidding;
			this.forbidder = forbiddenPlans;
			this.jointPlans = jointPlans;
		}
	}

	public static Stream<Fixture> arguments() {
		return Stream.of(
				createIndividualPlans(),
				createFullyJointPlans(),
				createPartiallyJointPlansOneSelectedJp(),
				createPartiallyJointPlansTwoSelectedJps(),
				createPartiallyJointPlansMessOfJointPlans(),
				createPartiallyJointPlansNoSelectedJp(),
				createOneBigJointPlanDifferentNPlansPerAgent(),
				createOneBigJointPlanDifferentNPlansPerAgent2(),
				createOneBigJointPlanDifferentNPlansPerAgentWithNullScores(),
				createPlanWithDifferentSolutionIfBlocked(),
				createPlanWithNoSolutionIfBlocked(),
				createIndividualPlansWithSpecialForbidder(),
				createDifferentForbidGroupsPerJointPlan());
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	public static Fixture createIndividualPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();
		List<Plan> toBeSelectedIfForbid = new ArrayList<Plan>();

		Person person = PopulationUtils.getFactory().createPerson(Id.create("tintin", Person.class));
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		toBeSelectedIfForbid.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );
		final Plan forbiddenPlan = plan;
		toBeSelected.add( plan );

		person = PopulationUtils.getFactory().createPerson(Id.create("milou", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		toBeSelectedIfForbid.add( plan );

		person = PopulationUtils.getFactory().createPerson(Id.create("tim", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		toBeSelectedIfForbid.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );

		person = PopulationUtils.getFactory().createPerson(Id.create("struppy", Person.class));
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -10d );
		toBeSelected.add( plan );
		toBeSelectedIfForbid.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );

		GroupPlans exp = new GroupPlans( Collections.<JointPlan>emptyList() , toBeSelected );
		GroupPlans expForbid = new GroupPlans( Collections.<JointPlan>emptyList() , toBeSelectedIfForbid );
		return new Fixture(
				"all individual",
				group,
				exp,
				exp,
				expForbid,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.singleton( forbiddenPlan ) ),
				jointPlans);
	}

	public static Fixture createFullyJointPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

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
		JointPlan sel = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.<Plan>emptyList() );
		return new Fixture(
				"fully joint",
				group,
				expected,
				expected,
				// not much we can forbid...
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansOneSelectedJp() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();

		Id id = Id.createPersonId( "tintin" );
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
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -10d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );

		JointPlan selForbidding = jointPlans.getFactory().createJointPlan( jp1 );
		jointPlans.addJointPlan( selForbidding );
		JointPlan sel = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					toBeSelected );

		GroupPlans expectedForbidding = new GroupPlans(
					Arrays.asList( selForbidding ),
					toBeSelected );

		return new Fixture(
				"partially joint, one selected joint plan",
				group,
				expected,
				expected,
				expectedForbidding,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					new HashSet<Plan>(jp2.values()) ),
				jointPlans);

	}

	public static Fixture createPartiallyJointPlansTwoSelectedJps() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();
		Map<Id<Person>, Plan> jp4 = new HashMap< >();

		Id id = Id.createPersonId( "tintin" );
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
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -15d );
		final Plan indivPlanIfForbid = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );

		jointPlans.addJointPlan(
			jointPlans.getFactory().createJointPlan( jp1 ) );
		JointPlan sel1 = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel1 );
		JointPlan selForbidding = jointPlans.getFactory().createJointPlan( jp3 );
		jointPlans.addJointPlan( selForbidding );
		JointPlan sel2 = jointPlans.getFactory().createJointPlan( jp4 );
		jointPlans.addJointPlan( sel2 );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.<Plan>emptyList() );
		GroupPlans expectedForbid = new GroupPlans(
					Collections.singleton( selForbidding ),
					Collections.singleton( indivPlanIfForbid ) );
		return new Fixture(
				"partially joint, two selected joint plans",
				group,
				expected,
				expected,
				expectedForbid,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					new HashSet<Plan>( jp2.values() ) ),
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansMessOfJointPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();
		Map<Id<Person>, Plan> jp4 = new HashMap< >();
		Map<Id<Person>, Plan> jp5 = new HashMap< >();
		Map<Id<Person>, Plan> jp6 = new HashMap< >();
		Map<Id<Person>, Plan> jp7 = new HashMap< >();
		Map<Id<Person>, Plan> jp8 = new HashMap< >();

		Id id = Id.createPersonId( "tintin" );
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
		plan.setScore( 7000d );
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
		plan.setScore( 1700d );
		final Plan indivIfForbid = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );
		jp3.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 100d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp5.put( id , plan );

		jointPlans.addJointPlan( jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp4 ) );
		JointPlan sel1 = jointPlans.getFactory().createJointPlan( jp5 );
		jointPlans.addJointPlan( sel1 );
		JointPlan sel2 = jointPlans.getFactory().createJointPlan( jp6 );
		jointPlans.addJointPlan( sel2 );
		JointPlan selF1 = jointPlans.getFactory().createJointPlan( jp7 );
		jointPlans.addJointPlan( selF1 );
		JointPlan selF2 = jointPlans.getFactory().createJointPlan( jp8 );
		jointPlans.addJointPlan( selF2 );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.<Plan>emptyList() );
		GroupPlans expectedForbid = new GroupPlans(
					Arrays.asList( selF1 , selF2 ),
					Collections.singleton( indivIfForbid ) );
		return new Fixture(
				"partially joint, multiple combinations",
				group,
				expected,
				expected,
				expectedForbid,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					new HashSet<Plan>( jp5.values() ) ),
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgent() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		jp.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -15d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -15000d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.<Plan>emptyList() );

		return new Fixture(
				"one big joint plan",
				group,
				expected,
				null,
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansNoSelectedJp() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();
		Map<Id<Person>, Plan> jp4 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1000d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		jp3.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1000d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1000d );
		jp3.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		id = Id.createPersonId( "haddock" );
		final Id<Person> id5 = id;
		person = PopulationUtils.getFactory().createPerson(id5);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		toBeSelected.add( plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1000d );
		jp4.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5d );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp4 ) );

		GroupPlans expected = new GroupPlans(
					Collections.<JointPlan>emptyList(),
					toBeSelected );

		return new Fixture(
				"partially joint, no selected joint trips",
				group,
				expected,
				expected,
				//TODO: forbid something
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgent2() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp = new HashMap< >();

		Id<Person> id = Id.createPersonId( "milou" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		jp.put( id , plan );

		id = Id.createPersonId( "tintin" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 5000d );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10d );
		jp.put( id , plan );

		id = Id.createPersonId( "tim" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -15d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -15000d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );

		return new Fixture(
				"one big joint plan order 2",
				group,
				expected,
				null,
				//TODO: forbid something
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgentWithNullScores() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1295836d );
		jp.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -12348597d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( null );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1043872360d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( null );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -159484723d );
		jp.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -5000d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( null );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );

		return new Fixture(
				"one big joint plan, null scores",
				group,
				expected,
				null,
				//TODO: forbid something
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createPlanWithDifferentSolutionIfBlocked() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();
		Map<Id<Person>, Plan> jp3 = new HashMap< >();


		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		Plan p1 = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1295836d );
		jp3.put( id , plan );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -123445d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		Plan p2 = plan;

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		Plan p3 = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -123454d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1295836d );
		jp3.put( id , plan );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 1000d );
		Plan p4 = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -1295836d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp1 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.asList( p1 , p2 , p3 , p4 ) );
		GroupPlans expectedBlock = new GroupPlans(
					Arrays.asList( sel ),
					Arrays.asList( p3 , p4 ) );

		return new Fixture(
				"different plans if blocking",
				group,
				expected,
				expectedBlock,
				//TODO: forbid something
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createPlanWithNoSolutionIfBlocked() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id<Person>, Plan> jp1 = new HashMap< >();
		Map<Id<Person>, Plan> jp2 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		Plan plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10000d );
		Plan p1 = plan;

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 100000d );
		Plan p2 = plan;

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( -123454d );
		jp2.put( id , plan );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 10000d );
		Plan p3 = plan;

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 100000d );
		Plan p4 = plan;
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore( 0d );
		jp2.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.asList( p1 , p2 , p3 , p4 ) );
		GroupPlans expectedBlock = null;

		return new Fixture(
				"no plans if blocking",
				group,
				expected,
				expectedBlock,
				//TODO: forbid something
				expected,
				new CollectionBasedPlanForbidderFactory(
					group,
					jointPlans,
					Collections.<Plan>emptySet() ),
				jointPlans);
	}

	public static Fixture createIndividualPlansWithSpecialForbidder() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Id id = Id.create( "tintin" , Person.class );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		final Plan p11 = PersonUtils.createAndAddPlan(person, false);
		p11.setScore( 1000d );
		final Plan p12 = PersonUtils.createAndAddPlan(person, false);
		p12.setScore( 1d );

		id = Id.create( "milou" , Person.class );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		final Plan p21 = PersonUtils.createAndAddPlan(person, false);
		p21.setScore( 1000d );
		final Plan p22 = PersonUtils.createAndAddPlan(person, false);
		p22.setScore( 0d );

		id = Id.create( "tim" , Person.class );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		final Plan p31 = PersonUtils.createAndAddPlan(person, false);
		p31.setScore( 1000d );
		final Plan p32 = PersonUtils.createAndAddPlan(person, false);
		p32.setScore( 1d );

		id = Id.create( "struppy" , Person.class );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		final Plan p41 = PersonUtils.createAndAddPlan(person, false);
		p41.setScore( 1000d );
		final Plan p42 = PersonUtils.createAndAddPlan(person, false);
		p42.setScore( 0d );

		final IncompatiblePlansIdentifierImpl<Clique> identifier = new IncompatiblePlansIdentifierImpl<>();
		identifier.put( p11 , Collections.<Id<Clique>>singleton( Id.create( 1 , Clique.class ) ) );
		identifier.put( p21 , Collections.<Id<Clique>>singleton( Id.create( 1 , Clique.class ) ) );
		identifier.put( p12 , Collections.<Id<Clique>>singleton( Id.create( 2 , Clique.class ) ) );
		identifier.put( p22 , Collections.<Id<Clique>>singleton( Id.create( 2 , Clique.class ) ) );
		identifier.put( p31 , Collections.<Id<Clique>>singleton( Id.create( 3 , Clique.class ) ) );
		identifier.put( p41 , Collections.<Id<Clique>>singleton( Id.create( 3 , Clique.class ) ) );
		identifier.put( p32 , Collections.<Id<Clique>>singleton( Id.create( 4 , Clique.class ) ) );
		identifier.put( p42 , Collections.<Id<Clique>>singleton( Id.create( 4 , Clique.class ) ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.<Plan>asList( p11 , p21 , p31 , p41 ) );
		GroupPlans expectedForbid = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.<Plan>asList( p12 , p21 , p32 , p41 ) );

		return new Fixture(
				"forbid couples",
				group,
				expected,
				expected,
				expectedForbid,
				new IncompatiblePlansIdentifierFactory() {
					@Override
					public IncompatiblePlansIdentifier createIdentifier(
							final JointPlans jps,
							final ReplanningGroup g) {
						return identifier;
					}
				},
				jointPlans);
	}

	public static Fixture createDifferentForbidGroupsPerJointPlan() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		final Map<Id<Person>, Plan> jp1 = new HashMap< >();
		final Map<Id<Person>, Plan> jp2 = new HashMap< >();
		final Map<Id<Person>, Plan> jp3 = new HashMap< >();
		final Map<Id<Person>, Plan> jp4 = new HashMap< >();

		Id<Person> id = Id.createPersonId( "tintin" );
		final Id<Person> id1 = id;
		Person person = PopulationUtils.getFactory().createPerson(id1);
		group.addPerson( person );
		final Plan p11 = PersonUtils.createAndAddPlan(person, false);
		p11.setScore( 1d );
		jp1.put( id , p11 );
		final Plan p12 = PersonUtils.createAndAddPlan(person, false);
		p12.setScore( 1d );
		jp2.put( id , p12 );

		id = Id.createPersonId( "milou" );
		final Id<Person> id2 = id;
		person = PopulationUtils.getFactory().createPerson(id2);
		group.addPerson( person );
		final Plan p21 = PersonUtils.createAndAddPlan(person, false);
		p21.setScore( 0d );
		jp1.put( id , p21 );
		final Plan p22 = PersonUtils.createAndAddPlan(person, false);
		p22.setScore( 10d );
		jp2.put( id , p22 );

		id = Id.createPersonId( "tim" );
		final Id<Person> id3 = id;
		person = PopulationUtils.getFactory().createPerson(id3);
		group.addPerson( person );
		final Plan p31 = PersonUtils.createAndAddPlan(person, false);
		p31.setScore( 1d );
		jp3.put( id , p31 );
		final Plan p32 = PersonUtils.createAndAddPlan(person, false);
		p32.setScore( 1d );
		jp4.put( id , p32 );

		id = Id.createPersonId( "struppy" );
		final Id<Person> id4 = id;
		person = PopulationUtils.getFactory().createPerson(id4);
		group.addPerson( person );
		final Plan p41 = PersonUtils.createAndAddPlan(person, false);
		p41.setScore( 0d );
		jp3.put( id , p41 );
		final Plan p42 = PersonUtils.createAndAddPlan(person, false);
		p42.setScore( 1d );
		jp4.put( id , p42 );

		final IncompatiblePlansIdentifierImpl<Clique> identifier = new IncompatiblePlansIdentifierImpl<>();
		identifier.put( p11 , Collections.<Id<Clique>>singleton( Id.create( 1 , Clique.class ) ) );
		identifier.put( p12 , Collections.<Id<Clique>>singleton( Id.create( 2 , Clique.class ) ) );
		identifier.put( p31 , Collections.<Id<Clique>>singleton( Id.create( 3 , Clique.class ) ) );
		identifier.put( p32 , Collections.<Id<Clique>>singleton( Id.create( 3 , Clique.class ) ) );
		identifier.put( p41 , Collections.<Id<Clique>>singleton( Id.create( 1 , Clique.class ) ) );
		identifier.put( p42 , Collections.<Id<Clique>>singleton( Id.create( 2 , Clique.class ) ) );

		final JointPlan jointPlan1 = jointPlans.getFactory().createJointPlan( jp1 );
		jointPlans.addJointPlan( jointPlan1 );
		final JointPlan jointPlan2 = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( jointPlan2 );
		final JointPlan jointPlan3 = jointPlans.getFactory().createJointPlan( jp3 );
		jointPlans.addJointPlan( jointPlan3 );
		final JointPlan jointPlan4 = jointPlans.getFactory().createJointPlan( jp4 );
		jointPlans.addJointPlan( jointPlan4 );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( jointPlan2 , jointPlan4 ),
					Collections.<Plan>emptyList());

		GroupPlans expectedForbidding = new GroupPlans(
					Arrays.asList( jointPlan2 , jointPlan3 ),
					Collections.<Plan>emptyList());

		return new Fixture(
				"different forbids in jointPlans",
				group,
				// TODO: tune so that different output in the different situations
				expected,
				expected,
				expectedForbidding,
				new IncompatiblePlansIdentifierFactory() {
					@Override
					public IncompatiblePlansIdentifier createIdentifier(
							final JointPlans jps,
							final ReplanningGroup g) {
						return identifier;
					}
				},
				jointPlans);
	}

	@BeforeEach
	public void setupLogging() {
		//Logger.getRootLogger().setLevel( Level.TRACE );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Tests
	// /////////////////////////////////////////////////////////////////////////
	@ParameterizedTest
	@MethodSource("arguments")
	void testSelectedPlansNonBlocking(Fixture fixture) throws Exception {
		testSelectedPlans( fixture, false , false );
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testSelectedPlansForbidding(Fixture fixture) throws Exception {
		testSelectedPlans( fixture, false , true );
	}

	@ParameterizedTest
	@MethodSource("arguments")
	void testSelectedPlansBlocking(Fixture fixture) throws Exception {
		testSelectedPlans( fixture, true , false );
	}

	/**
	 * Check that plans are not removed from the plans DB in the selection process,
	 * particularly when pruning unplausible plans.
	 */
	@ParameterizedTest
	@MethodSource("arguments")
	void testNoSideEffects(Fixture fixture) throws Exception {
		HighestScoreSumSelector selector =
				new HighestScoreSumSelector(
					new EmptyIncompatiblePlansIdentifierFactory(),
					false );
		final Map<Id, Integer> planCounts = new HashMap<Id, Integer>();

		final int initialGroupSize = fixture.group.getPersons().size();
		for (Person p : fixture.group.getPersons()) {
			planCounts.put( p.getId() , p.getPlans().size() );
		}

		selector.selectPlans(
					fixture.jointPlans,
					fixture.group );

		Assertions.assertEquals(
				initialGroupSize,
				fixture.group.getPersons().size(),
				"unexpected change in group size for fixture "+fixture.name );

		for (Person p : fixture.group.getPersons()) {
			Assertions.assertEquals(
					planCounts.get( p.getId() ).intValue(),
					p.getPlans().size(),
					"unexpected change in the number of plans for agent "+p.getId()+" in fixture "+fixture.name );
		}
	}

	private void testSelectedPlans(
			Fixture fixture,
			final boolean blocking,
			final boolean forbidding) {
		if ( blocking && forbidding ) throw new UnsupportedOperationException();
		HighestScoreSumSelector selector =
				new HighestScoreSumSelector(
					forbidding ?
						fixture.forbidder :
						new EmptyIncompatiblePlansIdentifierFactory(),
					blocking );
		GroupPlans selected = null;
		try {
			selected = selector.selectPlans(
					fixture.jointPlans,
					fixture.group );
		}
		catch (Exception e) {
			throw new RuntimeException( "exception thrown for instance <<"+fixture.name+">>", e );
		}

		final GroupPlans expected =
				blocking ?
					fixture.expectedSelectedPlansWhenBlocking :
					(forbidding ?
					 	fixture.expectedSelectedPlansWhenForbidding :
						fixture.expectedSelectedPlans);
		Assertions.assertEquals(
				expected,
				selected,
				"unexpected selected plan in test instance <<"+fixture.name+">> ");
	}

	private static class CollectionBasedPlanForbidderFactory implements IncompatiblePlansIdentifierFactory {
		private final Map<Plan, Set<Id<Clique>>> map = new HashMap<>();

		public CollectionBasedPlanForbidderFactory(
				final ReplanningGroup group,
				final JointPlans jointPlans,
				final Set<Plan> forbiddenPlans) {
			int id = 0;
			final Set<Id<Clique>> allGroups = new HashSet<>();
			for ( Person person : group.getPersons() ) {
				for ( Plan plan : person.getPlans() ) {
					if ( map.containsKey( plan ) ) continue;
					if ( forbiddenPlans.contains( plan ) ) continue;
					final JointPlan jp = jointPlans.getJointPlan( plan );
					final Iterable<Plan> plansToAdd = jp == null ?
						Collections.singleton( plan ) :
						jp.getIndividualPlans().values();

					final Id<Clique> groupId = Id.create( id++ , Clique.class );
					allGroups.add( groupId );
					for ( Plan toAdd : plansToAdd ) {
						map.put( toAdd , Collections.singleton( groupId ) );
					}
				}
			}

			for ( Plan plan : forbiddenPlans ) map.put( plan , allGroups );
		}

		@Override
		public IncompatiblePlansIdentifier createIdentifier(
				final JointPlans jointPlans,
				final ReplanningGroup group) {
			return new IncompatiblePlansIdentifier() {
				@Override
				public Set<Id<Clique>> identifyIncompatibilityGroups(
						final Plan plan) {
					return map.get( plan );
				}
			};
		}
	}
}

