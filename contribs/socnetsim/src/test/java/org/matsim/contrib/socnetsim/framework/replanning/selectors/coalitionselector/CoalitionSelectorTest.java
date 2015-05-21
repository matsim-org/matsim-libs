/* *********************************************************************** *
 * project: org.matsim.*
 * CoalitionSelectorTest.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class CoalitionSelectorTest {
	private final FixtureFactory fixtureFactory;

	public static interface FixtureFactory {
		public Fixture create();
	}

	public static class Fixture {
		final String name;
		final ReplanningGroup group;
		final GroupPlans expectedSelectedPlans;
		final JointPlans jointPlans;

		public Fixture(
				final String name,
				final ReplanningGroup group,
				final GroupPlans expectedPlans,
				final JointPlans jointPlans) {
			this.name = name;
			this.group = group;
			this.expectedSelectedPlans = expectedPlans;
			this.jointPlans = jointPlans;
		}
	}

	// XXX the SAME instance is used for all tests!
	// should not be a problem, but this is contrary to the idea of "fixture"
	// Should be a "fixture factory", but it becomes messy.
	public CoalitionSelectorTest(final FixtureFactory fixture) {
		this.fixtureFactory = fixture;
	}

	@Parameterized.Parameters
	public static Collection<FixtureFactory[]> fixtures() {
		return toArgList(
				new FixtureFactory() {
					@Override
					public Fixture create() {
						final JointPlans jointPlans = new JointPlans();
						final ReplanningGroup group = new ReplanningGroup();

						final List<Plan> toBeSelected = new ArrayList<Plan>();

						PersonImpl person = new PersonImpl( Id.create( "tintin" , Person.class ) );
						group.addPerson( person );
						PlanImpl plan = person.createAndAddPlan( false );
						plan.setScore( 1d );
						plan = person.createAndAddPlan( false );
						plan.setScore( 5d );
						toBeSelected.add( plan );

						person = new PersonImpl( Id.create( "milou" , Person.class ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( 10d );
						plan = person.createAndAddPlan( false );
						plan.setScore( 5000d );
						toBeSelected.add( plan );

						person = new PersonImpl( Id.create( "tim" , Person.class ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( 10d );
						plan = person.createAndAddPlan( false );
						plan.setScore( 5000d );
						toBeSelected.add( plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( -5000d );

						person = new PersonImpl( Id.create( "struppy" , Person.class ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( -10d );
						toBeSelected.add( plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( -5000d );
						plan = person.createAndAddPlan( false );
						plan.setScore( -5000d );

						final GroupPlans exp = new GroupPlans( Collections.<JointPlan>emptyList() , toBeSelected );
						return new Fixture(
								"all individual",
								group,
								exp,
								jointPlans);
					}
				},
				new FixtureFactory() {
					@Override
					public Fixture create() {
						final JointPlans jointPlans = new JointPlans();
						final ReplanningGroup group = new ReplanningGroup();

						final Map<Id<Person>, Plan> jp1 = new HashMap< >();
						final Map<Id<Person>, Plan> jp2 = new HashMap< >();
						final Map<Id<Person>, Plan> jp3 = new HashMap< >();

						PersonImpl person = new PersonImpl( Id.createPersonId( "tintin" ) );
						group.addPerson( person );
						PlanImpl plan = person.createAndAddPlan( false );
						plan.setScore( 1d );
						jp1.put( person.getId() , plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( -1d );

						person = new PersonImpl( Id.createPersonId( "milou" ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( -10d );
						jp1.put( person.getId() , plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( 5000000000d );
						jp2.put( person.getId() , plan );

						person = new PersonImpl( Id.createPersonId( "tim" ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( 10d );
						jp3.put( person.getId() , plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( 5d );
						jp2.put( person.getId() , plan );

						person = new PersonImpl( Id.createPersonId( "struppy" ) );
						group.addPerson( person );
						plan = person.createAndAddPlan( false );
						plan.setScore( -10d );
						jp3.put( person.getId() , plan );
						plan = person.createAndAddPlan( false );
						plan.setScore( -500d );

						jointPlans.addJointPlan(
								jointPlans.getFactory().createJointPlan(
									jp2 ) );
						final JointPlan jpl1 =
							jointPlans.getFactory().createJointPlan(
									jp1 );
						jointPlans.addJointPlan( jpl1 );
						final JointPlan jpl3 =
							jointPlans.getFactory().createJointPlan(
									jp3 );
						jointPlans.addJointPlan( jpl3 );

						final GroupPlans exp =
							new GroupPlans(
									Arrays.asList( jpl1 , jpl3 ),
									Collections.<Plan>emptyList() );
						return new Fixture(
								"well-defined",
								group,
								exp,
								jointPlans);
					}
				});

	}

	private static Collection<FixtureFactory[]> toArgList(final FixtureFactory... args) {
		final List<FixtureFactory[]> list = new ArrayList<FixtureFactory[]>();

		for ( FixtureFactory arg : args ) list.add( new FixtureFactory[]{ arg } );
		return list;
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// Tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testSelectedPlans() {
		final Fixture fixture = fixtureFactory.create();
		final CoalitionSelector selector = new CoalitionSelector();

		try {
			final GroupPlans selected =
					selector.selectPlans(
							fixture.jointPlans,
							fixture.group );

			final GroupPlans expected = fixture.expectedSelectedPlans;

			Assert.assertEquals(
					"unexpected selected plan in test instance <<"+fixture.name+">> ",
					expected,
					selected);
		}
		catch (Exception e) {
			throw new RuntimeException( "exception thrown for instance <<"+fixture.name+">>", e );
		}
	}
}
