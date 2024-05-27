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

import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
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

/**
 * @author thibautd
 */
public class InsertionRemovalIgnoranceBehaviorTest {
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
	void testRemoverIgnorance() throws Exception {
		final JointTripRemoverAlgorithm algo = new JointTripRemoverAlgorithm( random , new MainModeIdentifierImpl() );

		JointPlan jointPlan = createPlanWithJointTrips();

		assertNull(
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ),
				"unexpected removed trips" );

	}

	@Test
	void testInsertorIgnorance() throws Exception {
		final JointTripInsertorAlgorithm algo =
			new JointTripInsertorAlgorithm(
					random,
					null,
					(JointTripInsertorConfigGroup) config.getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
					TripStructureUtils.getRoutingModeIdentifier() ); // yyyyyy ??????

		JointPlan jointPlan = createPlanWithoutJointTrips();

		assertNull(
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ),
				"unexpected removed trips" );

	}

	private JointPlan createPlanWithoutJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		for (int i=0; i < 100; i++) {
			Id driverId = Id.create( "driver"+i , Person.class );
			final Id<Person> id = driverId;
			Person person = PopulationUtils.getFactory().createPerson(id);
			Plan plan = PopulationUtils.createPlan(person);
			individualPlans.put( driverId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_d"+i, Id.create( "some_link" , Link.class )).setEndTime( 10 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.car );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_d"+i, Id.create( "nowhere" , Link.class ));

			Id passengerId = Id.create( "passenger"+i , Person.class );
			final Id<Person> id1 = passengerId;
			person = PopulationUtils.getFactory().createPerson(id1);
			plan = PopulationUtils.createPlan(person);
			individualPlans.put( passengerId , plan );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "first_act_p"+i, Id.create( "earth" , Link.class )).setEndTime( 10 );
			PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
			PopulationUtils.createAndAddActivityFromLinkId(plan, "second_act_p"+i, Id.create( "space" , Link.class ));
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithJointTrips() {
		final Map<Id<Person>, Plan> individualPlans = new HashMap< >();

		Id puLink = Id.create( "pu" , Link.class );
		Id doLink = Id.create( "do" , Link.class );

		for (int i=0; i < 100; i++) {
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
}

