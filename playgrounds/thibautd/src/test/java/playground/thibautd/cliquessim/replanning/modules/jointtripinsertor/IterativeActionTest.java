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
package playground.thibautd.cliquessim.replanning.modules.jointtripinsertor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class IterativeActionTest {
	private Config config;
	private TripRouter tripRouter;
	private Random random;

	@Before
	public void configureLogging() {
		Logger.getLogger( JointTripInsertorAndRemoverAlgorithm.class ).setLevel( Level.TRACE );
	}

	@Before
	public void init() {
		config = JointControlerUtils.createConfig( null );
		tripRouter = new  TripRouter();
		random = new Random( 1234 );
	}

	@Test
	public void testNonIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithFourPersonsTwoJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
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
	public void testIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithFourPersonsTwoJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
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
					config,
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithFourPersonsNoJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
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
					config,
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithFourPersonsNoJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				2,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				2,
				p);
	}


	private JointPlan createPlanWithFourPersonsTwoJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		Id puLink = new IdImpl( "pu" );
		Id doLink = new IdImpl( "do" );

		Id driverId1 = new IdImpl( "driver1" );
		Person person = new PersonImpl( driverId1 );
		PlanImpl plan = new PlanImpl( person );
		individualPlans.put( driverId1 , plan );
		plan.createAndAddActivity( "first_act_d1" , new IdImpl( "some_link" ) ).setEndTime( 10 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
		Leg driverLeg1 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "second_act_d1" , new IdImpl( "nowhere" ) );

		Id driverId2 = new IdImpl( "driver2" );
		person = new PersonImpl( driverId2 );
		plan = new PlanImpl( person );
		individualPlans.put( driverId2 , plan );
		plan.createAndAddActivity( "first_act_d2" , new IdImpl( "some_other_link" ) ).setEndTime( 10 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
		Leg driverLeg2 = plan.createAndAddLeg( JointActingTypes.DRIVER );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "second_act_d2" , new IdImpl( "also_nowhere" ) );

		Id passengerId1 = new IdImpl( "passenger1" );
		person = new PersonImpl( passengerId1 );
		plan = new PlanImpl( person );
		individualPlans.put( passengerId1 , plan );
		plan.createAndAddActivity( "first_act_p1" , new IdImpl( "earth" ) ).setEndTime( 10 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
		Leg passengerLeg1 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "second_act_p1" , new IdImpl( "space" ) );

		Id passengerId2 = new IdImpl( "passenger2" );
		person = new PersonImpl( passengerId2 );
		plan = new PlanImpl( person );
		individualPlans.put( passengerId2 , plan );
		plan.createAndAddActivity( "first_act_p2" , new IdImpl( "sea" ) ).setEndTime( 10 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
		Leg passengerLeg2 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.walk );
		plan.createAndAddActivity( "second_act_p2" , new IdImpl( "mountain" ) );

		DriverRoute driverRoute = new DriverRoute( puLink , doLink );
		driverRoute.addPassenger( passengerId1 );
		driverLeg1.setRoute( driverRoute );

		PassengerRoute passengerRoute = new PassengerRoute( puLink , doLink );
		passengerRoute.setDriverId( driverId1 );
		passengerLeg1.setRoute( passengerRoute );

		driverRoute = new DriverRoute( puLink , doLink );
		driverRoute.addPassenger( passengerId2 );
		driverLeg2.setRoute( driverRoute );

		passengerRoute = new PassengerRoute( puLink , doLink );
		passengerRoute.setDriverId( driverId2 );
		passengerLeg2.setRoute( passengerRoute );

		return JointPlanFactory.createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithFourPersonsNoJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		Coord coord1 = new CoordImpl( 0 , 0 );
		Coord coord2 = new CoordImpl( 3600 , 21122012 );

		Id driverId1 = new IdImpl( "driver1" );
		Person person = new PersonImpl( driverId1 );
		PlanImpl plan = new PlanImpl( person );
		individualPlans.put( driverId1 , plan );
		ActivityImpl act = plan.createAndAddActivity( "first_act_d1" , new IdImpl( "some_link" ) );
		act.setEndTime( 10 );
		act.setCoord( coord1 );
		plan.createAndAddLeg( TransportMode.car );
		act = plan.createAndAddActivity( "second_act_d1" , new IdImpl( "nowhere" ) );
		act.setCoord( coord2 );

		Id driverId2 = new IdImpl( "driver2" );
		person = new PersonImpl( driverId2 );
		plan = new PlanImpl( person );
		individualPlans.put( driverId2 , plan );
		act = plan.createAndAddActivity( "first_act_d2" , new IdImpl( "some_other_link" ) );
		act.setEndTime( 10 );
		act.setCoord( coord1 );
		plan.createAndAddLeg( TransportMode.car );
		act = plan.createAndAddActivity( "second_act_d2" , new IdImpl( "also_nowhere" ) );
		act.setCoord( coord2 );

		Id passengerId1 = new IdImpl( "passenger1" );
		person = new PersonImpl( passengerId1 );
		plan = new PlanImpl( person );
		individualPlans.put( passengerId1 , plan );
		act = plan.createAndAddActivity( "first_act_p1" , new IdImpl( "earth" ) );
		act.setEndTime( 10 );
		act.setCoord( coord1 );
		plan.createAndAddLeg( TransportMode.walk );
		act = plan.createAndAddActivity( "second_act_p1" , new IdImpl( "space" ) );
		act.setCoord( coord2 );

		Id passengerId2 = new IdImpl( "passenger2" );
		person = new PersonImpl( passengerId2 );
		plan = new PlanImpl( person );
		individualPlans.put( passengerId2 , plan );
		act = plan.createAndAddActivity( "first_act_p2" , new IdImpl( "sea" ) );
		act.setEndTime( 10 );
		act.setCoord( coord1 );
		plan.createAndAddLeg( TransportMode.walk );
		act = plan.createAndAddActivity( "second_act_p2" , new IdImpl( "mountain" ) );
		act.setCoord( coord2 );

		return JointPlanFactory.createJointPlan( individualPlans );
	}
}

