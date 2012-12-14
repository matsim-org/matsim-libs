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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.HighestScoreSumSelector;

/**
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class HighestWeightSelectorTest {
	private static final Logger log =
		Logger.getLogger(HighestWeightSelectorTest.class);

	private final Fixture fixture;

	public static class Fixture {
		final String name;
		final ReplanningGroup group;
		final GroupPlans expectedSelectedPlans;

		public Fixture(
				final String name,
				final ReplanningGroup group,
				final GroupPlans expectedPlans) {
			this.name = name;
			this.group = group;
			this.expectedSelectedPlans = expectedPlans;
		}
	}

	public HighestWeightSelectorTest(final Fixture fixture) {
		this.fixture = fixture;
		log.info( "fixture "+fixture.name );
	}

	@Parameterized.Parameters
	public static Collection<Fixture[]> fixtures() {
		return Arrays.asList(
				new Fixture[]{createIndividualPlans()},
				new Fixture[]{createFullyJointPlans()},
				new Fixture[]{createPartiallyJointPlansOneSelectedJp()},
				new Fixture[]{createPartiallyJointPlansTwoSelectedJps()},
				new Fixture[]{createPartiallyJointPlansMessOfJointPlans()},
				new Fixture[]{createPartiallyJointPlansNoSelectedJp()},
				new Fixture[]{createOneBigJointPlanDifferentNPlansPerAgent()});
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	public static Fixture createIndividualPlans() {
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		PersonImpl person = new PersonImpl( new IdImpl( "tintin" ) );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		toBeSelected.add( plan );

		person = new PersonImpl( new IdImpl( "milou" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );

		person = new PersonImpl( new IdImpl( "tim" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		person = new PersonImpl( new IdImpl( "struppy" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		return new Fixture(
				"all individual",
				group,
				new GroupPlans( Collections.EMPTY_LIST , toBeSelected ) );
	}

	public static Fixture createFullyJointPlans() {
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
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

		id = new IdImpl( "milou" );
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

		id = new IdImpl( "tim" );
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

		id = new IdImpl( "struppy" );
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

		JointPlanFactory.createJointPlan( jp1 );
		JointPlan sel = JointPlanFactory.createJointPlan( jp2 );
		JointPlanFactory.createJointPlan( jp3 );

		return new Fixture(
				"fully joint",
				group,
				new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST ) );
	}

	public static Fixture createPartiallyJointPlansOneSelectedJp() {
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
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

		id = new IdImpl( "milou" );
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

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		JointPlanFactory.createJointPlan( jp1 );
		JointPlan sel = JointPlanFactory.createJointPlan( jp2 );
		JointPlanFactory.createJointPlan( jp3 );

		return new Fixture(
				"partially joint, one selected joint plan",
				group,
				new GroupPlans(
					Arrays.asList( sel ),
					toBeSelected ) );
	}

	public static Fixture createPartiallyJointPlansTwoSelectedJps() {
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
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

		id = new IdImpl( "milou" );
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

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		JointPlanFactory.createJointPlan( jp1 );
		JointPlan sel1 = JointPlanFactory.createJointPlan( jp2 );
		JointPlanFactory.createJointPlan( jp3 );
		JointPlan sel2 = JointPlanFactory.createJointPlan( jp4 );

		return new Fixture(
				"partially joint, two selected joint plans",
				group,
				new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.EMPTY_LIST ) );
	}

	public static Fixture createPartiallyJointPlansMessOfJointPlans() {
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp5 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp6 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp7 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp8 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
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

		id = new IdImpl( "milou" );
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

		id = new IdImpl( "tim" );
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

		id = new IdImpl( "struppy" );
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

		id = new IdImpl( "haddock" );
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

		JointPlanFactory.createJointPlan( jp1 );
		JointPlanFactory.createJointPlan( jp2 );
		JointPlanFactory.createJointPlan( jp3 );
		JointPlanFactory.createJointPlan( jp4 );
		JointPlan sel1 = JointPlanFactory.createJointPlan( jp5 );
		JointPlan sel2 = JointPlanFactory.createJointPlan( jp6 );
		JointPlanFactory.createJointPlan( jp7 );
		JointPlanFactory.createJointPlan( jp8 );

		return new Fixture(
				"partially joint, multiple combinations",
				group,
				new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.EMPTY_LIST ) );
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgent() {
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15000d );

		JointPlan sel = JointPlanFactory.createJointPlan( jp );

		return new Fixture(
				"one big joint plan",
				group,
				new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST ) );
	}

	public static Fixture createPartiallyJointPlansNoSelectedJp() {
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "haddock" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		JointPlanFactory.createJointPlan( jp1 );
		JointPlanFactory.createJointPlan( jp2 );
		JointPlanFactory.createJointPlan( jp3 );
		JointPlanFactory.createJointPlan( jp4 );

		return new Fixture(
				"partially joint, no selected joint trips",
				group,
				new GroupPlans(
					Collections.EMPTY_LIST,
					toBeSelected ) );
	}
	@Before
	public void setupLogging() {
		Logger.getRootLogger().setLevel( Level.TRACE );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testSelectedPlans() throws Exception {
		HighestScoreSumSelector selector = new HighestScoreSumSelector();

		GroupPlans selected = selector.selectPlans( fixture.group );

		Assert.assertEquals(
				"unexpected selected plan in test instance <<"+fixture.name+">> ",
				fixture.expectedSelectedPlans,
				selected);
	}
}

