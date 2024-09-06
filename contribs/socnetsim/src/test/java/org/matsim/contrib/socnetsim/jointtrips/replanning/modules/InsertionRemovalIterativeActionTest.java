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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class InsertionRemovalIterativeActionTest {
	private final static int N_COUPLES = 100;
	private Config config;
	private TripRouter tripRouter;
	private Random random;

	@BeforeEach
	public void init() {
		config = JointScenarioUtils.createConfig();
//		tripRouter = new  TripRouter();
		tripRouter = new TripRouter.Builder( config ).build() ;
		random = new Random( 1234 );
	}

	@Test
	void testNonIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					random,
					false,
					TripStructureUtils.getRoutingModeIdentifier() // yyyyyy ??????
					);
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
				N_COUPLES - 1,
				d,
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);

		assertEquals(
				N_COUPLES - 1,
				p,
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);
	}

	@Test
	void testIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					random,
					true,
					TripStructureUtils.getRoutingModeIdentifier() // yyyyyy ??????
					);
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
				0,
				d,
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);

		assertEquals(
				0,
				p,
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);
	}

	@Test
	void testNonIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					random,
					false,
					TripStructureUtils.getRoutingModeIdentifier() // yyyyyy ??????
					);
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
					Assertions.assertNotNull(
							l.getRoute(),
							"route must not be null" );
					Assertions.assertTrue(
							l.getRoute() instanceof DriverRoute,
							"unexpected route type "+l.getRoute().getClass().getName() );
					d++;
				}
				if ( JointActingTypes.PASSENGER.equals( mode ) ) {
					Assertions.assertNotNull(
							l.getRoute(),
							"route must not be null" );
					Assertions.assertTrue(
							l.getRoute() instanceof PassengerRoute,
							"unexpected route type "+l.getRoute().getClass().getName() );
					p++;
				}
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				1,
				d,
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);

		assertEquals(
				1,
				p,
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);
	}

	@Test
	void testIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					ScenarioUtils.createScenario( config ),
					random,
					true,
					TripStructureUtils.getRoutingModeIdentifier() // yyyyyy ??????
					);
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
					Assertions.assertNotNull(
							l.getRoute(),
							"route must not be null" );
					Assertions.assertTrue(
							l.getRoute() instanceof DriverRoute,
							"unexpected route type "+l.getRoute().getClass().getName() );
					d++;
				}
				if ( JointActingTypes.PASSENGER.equals( mode ) ) {
					Assertions.assertNotNull(
							l.getRoute(),
							"route must not be null" );
					Assertions.assertTrue(
							l.getRoute() instanceof PassengerRoute,
							"unexpected route type "+l.getRoute().getClass().getName() );
					p++;
				}
			}
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				N_COUPLES,
				d,
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);

		assertEquals(
				N_COUPLES,
				p,
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr);
	}


	private JointPlan createPlanWithJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		Id<Link> puLink = Id.create( "pu" , Link.class );
		Id<Link> doLink = Id.create( "do" , Link.class );

		for (int i=0; i < N_COUPLES; i++) {
			Id<Person> driverId = Id.create( "driver"+i , Person.class );
			Person person = PopulationUtils.getFactory().createPerson(driverId);
			Plan plan = PopulationUtils.createPlan(person);
			individualPlans.put( driverId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_d"+i, Id.create( "some_link" , Link.class )).setEndTime( 10 );
			Leg leg5 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			final Id<Link> linkId = puLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId).setMaximumDuration( 0 );
			Leg leg4 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER );
			Leg driverLeg1 = leg4;
			final Id<Link> linkId1 = doLink;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId1).setMaximumDuration( 0 );
			Leg leg3 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_d"+i, Id.create( "nowhere" , Link.class ));

			Id<Person> passengerId = Id.create( "passenger"+i , Person.class );
			person = PopulationUtils.getFactory().createPerson( passengerId );
			plan = PopulationUtils.createPlan(person);
			individualPlans.put( passengerId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_p"+i, Id.create( "earth" , Link.class )).setEndTime( 10 );
			Leg leg2 = PopulationUtils.createAndAddLeg( plan, TransportMode.walk ) ;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, puLink ).setMaximumDuration( 0 );
			Leg leg1 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER ) ;
			Leg passengerLeg1 = leg1;
			PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, doLink ).setMaximumDuration( 0 );
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.walk ) ;
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
			Leg leg1 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_d"+i, Id.create( "nowhere" , Link.class ));
			act.setCoord( coord2 );

			Id<Person> passengerId1 = Id.create( "passenger"+i , Person.class );
			person = PopulationUtils.getFactory().createPerson( passengerId1 );
			plan = PopulationUtils.createPlan(person);
			individualPlans.put( passengerId1 , plan );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_p"+i, Id.create( "earth" , Link.class ));
			act.setEndTime( 10 );
			act.setCoord( coord1 );
			Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			act = PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_p"+i, Id.create( "space" , Link.class ));
			act.setCoord( coord2 );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}
}

