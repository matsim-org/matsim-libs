/* *********************************************************************** *
 * project: org.matsim.*
 * SplittingTest.java
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
package playground.thibautd.socnetsim.replanning.modules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class SplittingTest {
	private final List<Fixture> fixtures = new ArrayList<Fixture>();
	private final JointPlanFactory factory = new JointPlanFactory();

	private static class Fixture {
		final GroupPlans plan;
		final int expectedNJp;
		final int expectedNIndividualPlans;
		final String name;

		public Fixture(
				final GroupPlans plan,
				final int enjp,
				final int enip,
				final String name ) {
			this.plan = plan;
			this.expectedNJp = enjp;
			this.expectedNIndividualPlans = enip;
			this.name = name;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	@After
	public void clean() {
		fixtures.clear();
	}

	@Before
	public void fromOneToNone() {
		Map<Id<Person>, Plan> plans = new HashMap< >();

		for (int i=0; i < 10; i++) {
			Id<Person> id = Id.createPersonId( i );
			plans.put( id , new PlanImpl( new PersonImpl( id ) ) );
		}

		fixtures.add(
				new Fixture(
					new GroupPlans(
						Collections.singleton( factory.createJointPlan( plans ) ),
						Collections.EMPTY_LIST),
					0,
					plans.size(),
					"from one to no JP empty plans"));
	}

	@Before
	public void fromOneToNoneNotEmpty() {
		Map<Id<Person>, Plan> plans = new HashMap< >();

		final Id origin = new IdImpl( "origin" );
		final Id destination = new IdImpl( "destination" );

		for (int i=0; i < 10; i++) {
			Id id = new IdImpl( i );
			PlanImpl p = new PlanImpl( new PersonImpl( id ) );
			plans.put( id , p );
			p.createAndAddActivity( "orig" , origin );
			p.createAndAddLeg( "unicycle" );
			p.createAndAddActivity( "dest" , destination );
		}

		fixtures.add(
				new Fixture(
					new GroupPlans(
						Collections.singleton( factory.createJointPlan( plans ) ),
						Collections.EMPTY_LIST),
					0,
					plans.size(),
					"from one to no JP non empty plans"));
	}

	@Before
	public void fromOneToOne() {
		Map<Id<Person>, Plan> plans = new HashMap< >();

		final Id origin = new IdImpl( "origin" );
		final Id destination = new IdImpl( "destination" );

		final Id<Person> driverId = Id.createPersonId( "driver" );
		final DriverRoute driverRoute = new DriverRoute( origin , destination );
		for (int i=0; i < 10; i++) {
			Id<Person> id = Id.createPersonId( i );
			driverRoute.addPassenger( id );
			PlanImpl p = new PlanImpl( new PersonImpl( id ) );
			plans.put( id , p );
			p.createAndAddActivity( "orig" , origin );
			Leg l = p.createAndAddLeg( JointActingTypes.PASSENGER );
			p.createAndAddActivity( "dest" , destination );
			PassengerRoute r = new PassengerRoute( origin , destination );
			r.setDriverId( driverId );
			l.setRoute( r );
		}

		PlanImpl p = new PlanImpl( new PersonImpl( driverId ) );
		plans.put( driverId , p );
		p.createAndAddActivity( "orig" , origin );
		Leg l = p.createAndAddLeg( JointActingTypes.DRIVER );
		p.createAndAddActivity( "dest" , destination );
		l.setRoute( driverRoute );

		fixtures.add(
				new Fixture(
					new GroupPlans(
						Collections.singleton( factory.createJointPlan( plans ) ),
						Collections.EMPTY_LIST),
					1,
					0,
					"from one to one JP"));
	}

	@Before
	public void fromOneToOneTwoJointTrips() {
		Map<Id<Person>, Plan> plans = new HashMap< >();

		final Id origin = new IdImpl( "origin" );
		final Id destination = new IdImpl( "destination" );

		final Id<Person> driverId = Id.createPersonId( "driver" );
		final DriverRoute driverRoute1 = new DriverRoute( origin , destination );
		for (int i=0; i < 10; i++) {
			Id<Person> id = Id.createPersonId( i );
			driverRoute1.addPassenger( id );
			PlanImpl p = new PlanImpl( new PersonImpl( id ) );
			plans.put( id , p );
			p.createAndAddActivity( "orig" , origin );
			Leg l = p.createAndAddLeg( JointActingTypes.PASSENGER );
			p.createAndAddActivity( "dest" , destination );
			PassengerRoute r = new PassengerRoute( origin , destination );
			r.setDriverId( driverId );
			l.setRoute( r );
		}

		final DriverRoute driverRoute2 = new DriverRoute( destination , origin );
		for (int i=100; i < 110; i++) {
			Id<Person> id = Id.createPersonId( i );
			driverRoute2.addPassenger( id );
			PlanImpl p = new PlanImpl( new PersonImpl( id ) );
			plans.put( id , p );
			p.createAndAddActivity( "dest" , destination );
			Leg l = p.createAndAddLeg( JointActingTypes.PASSENGER );
			p.createAndAddActivity( "orig" , origin );
			PassengerRoute r = new PassengerRoute( origin , destination );
			r.setDriverId( driverId );
			l.setRoute( r );
		}

		PlanImpl p = new PlanImpl( new PersonImpl( driverId ) );
		plans.put( driverId , p );
		p.createAndAddActivity( "orig" , origin );
		Leg l = p.createAndAddLeg( JointActingTypes.DRIVER );
		l.setRoute( driverRoute1 );
		p.createAndAddActivity( "dest" , destination );
		l = p.createAndAddLeg( JointActingTypes.DRIVER );
		l.setRoute( driverRoute2 );
		p.createAndAddActivity( "orig" , origin );

		fixtures.add(
				new Fixture(
					new GroupPlans(
						Collections.singleton( factory.createJointPlan( plans ) ),
						Collections.EMPTY_LIST),
					1,
					0,
					"from one to one JP with two joint trips"));
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testNumberOfJointPlans() throws Exception {
		SplitJointPlansBasedOnJointTripsAlgorithm algo =
				new SplitJointPlansBasedOnJointTripsAlgorithm(
						factory );
		for (Fixture f : fixtures) {
			algo.run( f.plan );
			assertEquals(
					"wrong number of joint plans for <<"+f.name+">>",
					f.expectedNJp,
					f.plan.getJointPlans().size());
		}
	}

	@Test
	public void testNumberOfIndividualPlans() throws Exception {
		SplitJointPlansBasedOnJointTripsAlgorithm algo =
				new SplitJointPlansBasedOnJointTripsAlgorithm(
						factory );
		for (Fixture f : fixtures) {
			algo.run( f.plan );
			assertEquals(
					"wrong number of individual plans for <<"+f.name+">>",
					f.expectedNIndividualPlans,
					f.plan.getIndividualPlans().size());
		}
	}
}

