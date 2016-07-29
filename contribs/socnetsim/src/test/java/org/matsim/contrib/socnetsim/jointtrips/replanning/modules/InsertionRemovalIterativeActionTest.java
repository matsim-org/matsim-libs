/* *********************************************************************** *
 * project: org.matsim.*
 * IterativeActionTest.java
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class InsertionRemovalIterativeActionTest {
	private static int N_COUPLES = 100;
	private Config config;
	private TripRouter tripRouter;
	private Random random;

	@Before
	public void configureLogging() {
		Logger.getLogger( JointTripInsertorAndRemoverAlgorithm.class ).setLevel( Level.TRACE );
	}

	@Before
	public void init() {
		config = JointScenarioUtils.createConfig();
		tripRouter = new  TripRouter();
		random = new Random( 1234 );
	}

	@Test
	public void testNonIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (Plan individualPlan : jointPlan.getIndividualPlans().values()) {
			for (PlanElement pe : individualPlan.getPlanElements()) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg l = (Leg) pe;
				final String mode = l.getMode();
				if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
				if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES - 1,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES - 1,
				p);
	}

	@Test
	public void testIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (Plan individualPlan : jointPlan.getIndividualPlans().values()) {
			for (PlanElement pe : individualPlan.getPlanElements()) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg l = (Leg) pe;
				final String mode = l.getMode();
				if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
				if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				0,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				0,
				p);
	}

	@Test
	public void testNonIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithoutJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (Plan individualPlan : jointPlan.getIndividualPlans().values()) {
			for (PlanElement pe : individualPlan.getPlanElements()) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg l = (Leg) pe;
				final String mode = l.getMode();
				if ( JointActingTypes.DRIVER.equals( mode ) ) {
					Assert.assertNotNull(
							"route must not be null",
							l.getRoute() );
					Assert.assertTrue(
							"unexpected route type "+l.getRoute().getClass().getName(),
							l.getRoute() instanceof DriverRoute );
					d++;
				}
				if ( JointActingTypes.PASSENGER.equals( mode ) ) {
					Assert.assertNotNull(
							"route must not be null",
							l.getRoute() );
					Assert.assertTrue(
							"unexpected route type "+l.getRoute().getClass().getName(),
							l.getRoute() instanceof PassengerRoute );
					p++;
				}
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				1,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				1,
				p);
	}

	@Test
	public void testIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithoutJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (Plan individualPlan : jointPlan.getIndividualPlans().values()) {
			for (PlanElement pe : individualPlan.getPlanElements()) {
				if ( !(pe instanceof Leg) ) continue;
				final Leg l = (Leg) pe;
				final String mode = l.getMode();
				if ( JointActingTypes.DRIVER.equals( mode ) ) {
					Assert.assertNotNull(
							"route must not be null",
							l.getRoute() );
					Assert.assertTrue(
							"unexpected route type "+l.getRoute().getClass().getName(),
							l.getRoute() instanceof DriverRoute );
					d++;
				}
				if ( JointActingTypes.PASSENGER.equals( mode ) ) {
					Assert.assertNotNull(
							"route must not be null",
							l.getRoute() );
					Assert.assertTrue(
							"unexpected route type "+l.getRoute().getClass().getName(),
							l.getRoute() instanceof PassengerRoute );
					p++;
				}
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES,
				p);
	}


	private JointPlan createPlanWithJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		Id puLink = Id.create( "pu" , Link.class );
		Id doLink = Id.create( "do" , Link.class );

		for (int i=0; i < N_COUPLES; i++) {
			Id driverId = Id.create( "driver"+i , Person.class );
			final Id<Person> id = driverId;
			Person person = PopulationUtils.getFactory().createPerson(id);
			Plan plan = PopulationUtils.createPlan(person);
			individualPlans.put( driverId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_d"+i, Id.create( "some_link" , Link.class )).setEndTime( 10 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			final Id<Link> linkId = puLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId).setMaximumDuration( 0 );
			Leg driverLeg1 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
			final Id<Link> linkId1 = doLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId1).setMaximumDuration( 0 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_d"+i, Id.create( "nowhere" , Link.class ));

			Id passengerId = Id.create( "passenger"+i , Person.class );
			final Id<Person> id1 = passengerId;
			person = PopulationUtils.getFactory().createPerson(id1);
			plan = PopulationUtils.createPlan(person);
			individualPlans.put( passengerId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_p"+i, Id.create( "earth" , Link.class )).setEndTime( 10 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			final Id<Link> linkId2 = puLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId2).setMaximumDuration( 0 );
			Leg passengerLeg1 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
			final Id<Link> linkId3 = doLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId3).setMaximumDuration( 0 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_p"+i, Id.create( "space" , Link.class ));

			DriverRoute driverRoute = new DriverRoute( puLink , doLink );
			driverRoute.addPassenger( passengerId );
			driverLeg1.setRoute( driverRoute );

			PassengerRoute passengerRoute = new PassengerRoute( puLink , doLink );
			passengerRoute.setDriverId( driverId );
			passengerLeg1.setRoute( passengerRoute );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithoutJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		Coord coord1 = new Coord((double) 0, (double) 0);
		Coord coord2 = new Coord((double) 3600, (double) 21122012);

		for (int i=0; i < N_COUPLES; i++) {
			Id driverId1 = Id.create( "driver"+i , Person.class );
			final Id<Person> id = driverId1;
			Person person = PopulationUtils.getFactory().createPerson(id);
			Plan plan = PopulationUtils.createPlan(person);
			individualPlans.put( driverId1 , plan );
			Activity act = PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_d"+i, Id.create( "some_link" , Link.class ));
			act.setEndTime( 10 );
			act.setCoord( coord1 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_d"+i, Id.create( "nowhere" , Link.class ));
			act.setCoord( coord2 );

			Id passengerId1 = Id.create( "passenger"+i , Person.class );
			final Id<Person> id1 = passengerId1;
			person = PopulationUtils.getFactory().createPerson(id1);
			plan = PopulationUtils.createPlan(person);
			individualPlans.put( passengerId1 , plan );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_p"+i, Id.create( "earth" , Link.class ));
			act.setEndTime( 10 );
			act.setCoord( coord1 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_p"+i, Id.create( "space" , Link.class ));
			act.setCoord( coord2 );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}
}

