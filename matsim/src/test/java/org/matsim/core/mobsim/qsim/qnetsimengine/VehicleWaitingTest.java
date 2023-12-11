/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleWaitingTest.java
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Tests the behavior of the qsim with agents waiting for vehicles.
 * <br>
 * This is not really a "unit" test anymore (thus it artificially increases test
 * coverage of the qsim), but at least, it checks that recent bugfixes actually
 * fixed something. td, mar. 2013
 *
 * @author thibautd
 */
public class VehicleWaitingTest {

	@Test
	void testVehicleWaitingOneLapDoesntFailNoDummies() {
		testVehicleWaitingDoesntFail( 1 , false );
	}

	@Test
	void testVehicleWaitingOneLapDoesntFailDummies() {
		testVehicleWaitingDoesntFail( 1 , true );
	}

	@Test
	void testVehicleWaitingSeveralLapDoesntFailNoDummies() {
		testVehicleWaitingDoesntFail( 4 , false );
	}

	@Test
	void testVehicleWaitingSeveralLapDoesntFailDummies() {
		testVehicleWaitingDoesntFail( 4 , true );
	}

	private static void testVehicleWaitingDoesntFail(final int nLaps, final boolean insertActivities) {
		final Config config = ConfigUtils.createConfig();
		// test behavior when agents wait for the car
		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.wait );
		// fail if the simualtion hangs forever (for instance, if some agents
		// just vanish as in MATSIM-71)
		config.qsim().setEndTime( 48 * 3600 );

		final Scenario sc = ScenarioUtils.createScenario( config );
		
		final NetworkFactory netFact = sc.getNetwork().getFactory();

		final Node node1 = netFact.createNode( Id.create( 1, Node.class ) , new Coord((double) 0, (double) 0));
		final Node node2 = netFact.createNode( Id.create( 2, Node.class ) , new Coord((double) 0, (double) 1000));
		final Node node3 = netFact.createNode( Id.create( 3, Node.class ) , new Coord((double) 1000, (double) 1000));
		
		sc.getNetwork().addNode( node1 );
		sc.getNetwork().addNode( node2 );
		sc.getNetwork().addNode( node3 );

		final Link link1 = netFact.createLink( Id.create( 1, Link.class ) , node1 , node2 );
		final Link link2 = netFact.createLink( Id.create( 2, Link.class ) , node2 , node3 );
		final Link link3 = netFact.createLink( Id.create( 3, Link.class ) , node3 , node1 );

		sc.getNetwork().addLink( link1 );
		sc.getNetwork().addLink( link2 );
		sc.getNetwork().addLink( link3 );

		final PopulationFactory popFact = sc.getPopulation().getFactory();

		final List<Id<Person>> personIds = new ArrayList<>();
		final Id<Person> personId1 = Id.create( "A", Person.class );
		personIds.add( personId1 );
		personIds.add( Id.create( "B", Person.class ) );
		personIds.add( Id.create( "C", Person.class ) );
		personIds.add( Id.create( "D", Person.class ) );

		for ( Id<Person> id : personIds ) {
			final Person person = popFact.createPerson( id );
			final Plan plan = popFact.createPlan();
			plan.setPerson( person );
			person.addPlan( plan );
			sc.getPopulation().addPerson( person );

			final Activity orig = popFact.createActivityFromLinkId( "h" , link1.getId() );
			orig.setEndTime( 10 );
			plan.addActivity( orig );
			for ( int lap=0; lap < nLaps; lap++ ) {
				final Leg leg = popFact.createLeg( TransportMode.car );
				TripStructureUtils.setRoutingMode(leg, TransportMode.car );
				final NetworkRoute route =
					RouteUtils.createLinkNetworkRouteImpl(link1.getId(), Collections.singletonList( link2.getId() ), link3.getId());
				route.setVehicleId( Id.create(personId1, Vehicle.class) ); // QSim creates a vehicle per person, with the ids of the persons
				leg.setRoute( route );
				plan.addLeg( leg );

				if ( insertActivities ) {
					final Activity dummy = popFact.createActivityFromLinkId( "dummy" , link3.getId() );
					dummy.setMaximumDuration( 0 );
					plan.addActivity( dummy );
				}

				final Leg secondLeg = popFact.createLeg( TransportMode.car );
				TripStructureUtils.setRoutingMode(secondLeg, TransportMode.car );
				final NetworkRoute secondRoute =
					RouteUtils.createLinkNetworkRouteImpl(link3.getId(), Collections.<Id<Link>>emptyList(), link1.getId());

				secondRoute.setVehicleId( Id.create(personId1, Vehicle.class) ); // QSim creates a vehicle per person, with the ids of the persons
				secondLeg.setRoute( secondRoute );
				plan.addLeg( secondLeg );

				if ( insertActivities ) {
					final Activity dummy = popFact.createActivityFromLinkId( "dummy" , link3.getId() );
					dummy.setMaximumDuration( 0 );
					plan.addActivity( dummy );
				}
			}
		}

		final Map<Id<Person>, Integer> arrivalCounts = new HashMap<>();
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( new PersonArrivalEventHandler() {
			@Override
			public void reset(int iteration) {}

			@Override
			public void handleEvent(final PersonArrivalEvent event) {
				final Integer count = arrivalCounts.get( event.getPersonId() );
				arrivalCounts.put(
					event.getPersonId(),
					count == null ? 1 : count + 1 );

			}
		});

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();
		new QSimBuilder(sc.getConfig()) //
			.useDefaults() //
			.build(sc, events) //
			.run();

		for ( Id<Person> id : personIds ) {
			Assertions.assertNotNull(
					arrivalCounts.get( id ),
					"no arrivals for person "+id );
			Assertions.assertEquals(
					nLaps * 2,
					arrivalCounts.get( id ).intValue(),
					"unexpected number of arrivals for person "+id);
		}
	}
}

