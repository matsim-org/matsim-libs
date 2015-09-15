/* *********************************************************************** *
 * project: org.matsim.*
 * IgnoranceBehaviorTest.java
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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertNull;

/**
 * @author thibautd
 */
public class InsertionRemovalIgnoranceBehaviorTest {
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
	public void testRemoverIgnorance() throws Exception {
		final JointTripRemoverAlgorithm algo = new JointTripRemoverAlgorithm( random , EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() );
		
		JointPlan jointPlan = createPlanWithJointTrips();

		assertNull(
				"unexpected removed trips",
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ) );

	}

	@Test
	public void testInsertorIgnorance() throws Exception {
		final JointTripInsertorAlgorithm algo =
			new JointTripInsertorAlgorithm(
					random,
					null,
					(JointTripInsertorConfigGroup) config.getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
					tripRouter );
		
		JointPlan jointPlan = createPlanWithoutJointTrips();

		assertNull(
				"unexpected removed trips",
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ) );

	}

	private JointPlan createPlanWithoutJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		for (int i=0; i < 100; i++) {
			Id driverId = Id.create( "driver"+i , Person.class );
			Person person = PersonImpl.createPerson(driverId);
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId , plan );
			plan.createAndAddActivity( "first_act_d"+i , Id.create( "some_link" , Link.class ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( "second_act_d"+i , Id.create( "nowhere" , Link.class ) );

			Id passengerId = Id.create( "passenger"+i , Person.class );
			person = PersonImpl.createPerson(passengerId);
			plan = new PlanImpl( person );
			individualPlans.put( passengerId , plan );
			plan.createAndAddActivity( "first_act_p"+i , Id.create( "earth" , Link.class ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( "second_act_p"+i , Id.create( "space" , Link.class ) );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		Id puLink = Id.create( "pu" , Link.class );
		Id doLink = Id.create( "do" , Link.class );

		for (int i=0; i < 100; i++) {
			Id driverId = Id.create( "driver"+i , Person.class );
			Person person = PersonImpl.createPerson(driverId);
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId , plan );
			plan.createAndAddActivity( "first_act_d"+i , Id.create( "some_link" , Link.class ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , puLink ).setMaximumDuration( 0 );
			Leg driverLeg1 = plan.createAndAddLeg( JointActingTypes.DRIVER );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( "second_act_d"+i , Id.create( "nowhere" , Link.class ) );

			Id passengerId = Id.create( "passenger"+i , Person.class );
			person = PersonImpl.createPerson(passengerId);
			plan = new PlanImpl( person );
			individualPlans.put( passengerId , plan );
			plan.createAndAddActivity( "first_act_p"+i , Id.create( "earth" , Link.class ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , puLink ).setMaximumDuration( 0 );
			Leg passengerLeg1 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( "second_act_p"+i , Id.create( "space" , Link.class ) );

			DriverRoute driverRoute = new DriverRoute( puLink , doLink );
			driverRoute.addPassenger( passengerId );
			driverLeg1.setRoute( driverRoute );

			PassengerRoute passengerRoute = new PassengerRoute( puLink , doLink );
			passengerRoute.setDriverId( driverId );
			passengerLeg1.setRoute( passengerRoute );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}
}

